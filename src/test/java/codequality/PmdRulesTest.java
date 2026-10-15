package codequality;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import net.sourceforge.pmd.PMDConfiguration;
import net.sourceforge.pmd.PmdAnalysis;
import net.sourceforge.pmd.lang.document.FileId;
import net.sourceforge.pmd.lang.java.JavaLanguageModule;
import net.sourceforge.pmd.reporting.RuleViolation;
import org.junit.jupiter.api.Test;

/**
 * Self-check of the custom rules in {@code pmd-ruleset.xml}: each one must still catch its case and
 * leave the allowed form alone. Runs the real ruleset, so a broken XPath or a renamed rule fails here.
 * <p>
 * Kept outside the application's package so the same file works in every repository that copies the
 * ruleset.
 */
class PmdRulesTest {

    @Test
    void autowiredOrValueField_isReported() {
        String code = """
                import org.springframework.beans.factory.annotation.Autowired;
                import org.springframework.beans.factory.annotation.Value;
                class Sample {
                    @Autowired private Object dependency;
                    @Value("${some.property}") private String property;
                }
                """;

        assertThat(violations(code, "NoFieldInjection")).isEqualTo(2);
    }

    @Test
    void constructorInjection_isNotReported() {
        String code = """
                import org.springframework.beans.factory.annotation.Value;
                class Sample {
                    private final String property;
                    Sample(@Value("${some.property}") String property) { this.property = property; }
                }
                """;

        assertThat(violations(code, "NoFieldInjection")).isZero();
    }

    @Test
    void transactionalOrAsyncPrivateMethod_isReported() {
        String code = """
                import org.springframework.scheduling.annotation.Async;
                import org.springframework.transaction.annotation.Transactional;
                class Sample {
                    @Transactional private void save() {}
                    @Async private void send() {}
                    void run() { save(); send(); }
                }
                """;

        assertThat(violations(code, "ProxyAnnotationOnPrivateMethod")).isEqualTo(2);
    }

    @Test
    void transactionalPublicMethod_isNotReported() {
        String code = """
                import org.springframework.transaction.annotation.Transactional;
                class Sample {
                    @Transactional public void save() {}
                }
                """;

        assertThat(violations(code, "ProxyAnnotationOnPrivateMethod")).isZero();
    }

    @Test
    void restControllerWithoutTag_isReported() {
        String code = """
                import org.springframework.web.bind.annotation.RestController;
                @RestController
                class Sample {}
                """;

        assertThat(violations(code, "RestControllerWithoutTag")).isEqualTo(1);
    }

    @Test
    void restControllerWithTag_isNotReported() {
        String code = """
                import io.swagger.v3.oas.annotations.tags.Tag;
                import org.springframework.web.bind.annotation.RestController;
                @RestController
                @Tag(name = "Samples", description = "Sample operations")
                class Sample {}
                """;

        assertThat(violations(code, "RestControllerWithoutTag")).isZero();
    }

    private static long violations(String code, String ruleName) {
        PMDConfiguration configuration = new PMDConfiguration();
        configuration.setDefaultLanguageVersion(JavaLanguageModule.getInstance().getVersion("17"));
        configuration.addRuleSet("pmd-ruleset.xml"); // Surefire runs from the project root
        // Resolves the Spring and Swagger annotations the rules look for (Surefire and the IDE
        // both set java.class.path to the test classpath)
        configuration.prependAuxClasspath(System.getProperty("java.class.path"));
        configuration.setIgnoreIncrementalAnalysis(true); // nothing to cache for in-memory samples

        try (PmdAnalysis pmd = PmdAnalysis.create(configuration)) {
            pmd.files().addSourceFile(FileId.fromPathLikeString("Sample.java"), code);
            List<RuleViolation> violations =
                    pmd.performAnalysisAndCollectReport().getViolations();
            return violations.stream()
                    .filter(violation -> violation.getRule().getName().equals(ruleName))
                    .count();
        }
    }
}
