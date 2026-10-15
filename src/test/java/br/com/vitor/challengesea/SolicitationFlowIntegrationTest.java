package br.com.vitor.challengesea;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import br.com.vitor.challengesea.model.address.ViaCepAddressDTO;
import br.com.vitor.challengesea.model.solicitation.SolicitationDocument;
import br.com.vitor.challengesea.repositories.audit.AuditLogRepository;
import br.com.vitor.challengesea.services.address.ViaCepClient;
import com.jayway.jsonpath.JsonPath;
import java.time.LocalDate;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.elasticsearch.ElasticsearchContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/**
 * The whole flow against real PostgreSQL and Elasticsearch: a client fills and submits a
 * solicitation, an analyst of another state is refused, the analyst of its state finds it in the
 * search and approves it. ViaCEP is the only fake. Skipped when Docker is not available.
 */
@SpringBootTest(properties = {"admin.bootstrap.email=admin@test.local", "admin.bootstrap.password=admin-pass-123"})
@AutoConfigureMockMvc
@Testcontainers(disabledWithoutDocker = true)
class SolicitationFlowIntegrationTest {

    @Container
    @ServiceConnection
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:17-alpine");

    @Container
    @ServiceConnection
    static final ElasticsearchContainer ELASTICSEARCH = new ElasticsearchContainer(
                    "docker.elastic.co/elasticsearch/elasticsearch:8.18.8")
            .withEnv("xpack.security.enabled", "false")
            .withEnv("cluster.routing.allocation.disk.threshold_enabled", "false")
            .withEnv("ES_JAVA_OPTS", "-Xms512m -Xmx512m");

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ElasticsearchOperations elasticsearchOperations;

    @Autowired
    private AuditLogRepository auditLogRepository;

    @MockitoBean
    private ViaCepClient viaCepClient;

    @Test
    void clientSubmits_analystOfThatStateFindsAndApprovesIt() throws Exception {
        when(viaCepClient.lookup(anyString()))
                .thenReturn(Optional.of(new ViaCepAddressDTO("01001-000", "Praça da Sé", "Sé", "São Paulo", "SP")));

        call(post("/auth/register"), null, """
                        {"name": "Cliente", "email": "cliente@test.local", "password": "cliente-pass"}""")
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.role").value("CLIENT"));
        String client = login("cliente@test.local", "cliente-pass");

        long id = idOf(call(post("/solicitations"), client, null).andExpect(status().isCreated()));
        String base = "/solicitations/" + id;

        call(post(base + "/submit"), client, null)
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.errors[0].step").value("BASIC_DATA"));

        call(put(base + "/steps/basic-data"), client, """
                        {"serviceType": "INSTALLATION", "title": "Instalar ar-condicionado",
                         "description": "Dois aparelhos split na sala e no quarto"}""")
                .andExpect(jsonPath("$.currentStep").value(2));
        call(put(base + "/steps/address"), client, """
                        {"cep": "01001-000", "number": "12A"}""")
                .andExpect(jsonPath("$.state").value("SP"))
                .andExpect(jsonPath("$.currentStep").value(3));
        call(put(base + "/steps/confirmation"), client, """
                        {"priority": "HIGH", "preferredDate": "%s", "estimatedValue": 250.00, "termsAccepted": true}""".formatted(
                                LocalDate.now().plusDays(5)))
                .andExpect(jsonPath("$.pendingFields").isEmpty());
        call(post(base + "/submit"), client, null)
                .andExpect(jsonPath("$.status").value("SUBMITTED"));

        String admin = login("admin@test.local", "admin-pass-123");
        createAnalyst(admin, "analista.sp@test.local", "SP");
        createAnalyst(admin, "analista.rj@test.local", "RJ");
        String analystSp = login("analista.sp@test.local", "analyst-pass");
        String analystRj = login("analista.rj@test.local", "analyst-pass");

        call(get("/analyst/solicitations/" + id), analystRj, null).andExpect(status().isForbidden());
        call(post("/analyst/solicitations/" + id + "/start"), analystRj, null).andExpect(status().isForbidden());

        elasticsearchOperations.indexOps(SolicitationDocument.class).refresh();
        call(get("/analyst/solicitations/search?q=ar-condicionado&state=SP"), analystRj, null)
                .andExpect(jsonPath("$.total").value(0));
        call(get("/analyst/solicitations/search?q=ar-condicionado&status=SUBMITTED"), analystSp, null)
                .andExpect(jsonPath("$.total").value(1))
                .andExpect(jsonPath("$.items[0].id").value(id));

        call(post("/analyst/solicitations/" + id + "/start"), analystSp, null)
                .andExpect(jsonPath("$.status").value("IN_REVIEW"));
        call(post("/analyst/solicitations/" + id + "/decide"), analystSp, """
                        {"decision": "APPROVE", "comment": "Endereço e valores conferidos"}""")
                .andExpect(jsonPath("$.status").value("APPROVED"));

        call(put(base + "/steps/basic-data"), client, "{\"title\": \"Outro título\"}")
                .andExpect(status().isConflict());
        call(get("/analyst/solicitations/" + id), client, null).andExpect(status().isForbidden());
        assertThat(auditLogRepository.findAll())
                .extracting(log -> log.getAction())
                .contains("SOLICITATION_SUBMIT", "SOLICITATION_START_REVIEW", "SOLICITATION_DECIDE");
    }

    private void createAnalyst(String adminToken, String email, String state) throws Exception {
        call(post("/admin/users"), adminToken, """
                        {"name": "Analista", "email": "%s", "password": "analyst-pass",
                         "role": "ANALYST", "coveredStates": ["%s"]}""".formatted(email, state)).andExpect(status().isCreated());
    }

    private String login(String email, String password) throws Exception {
        String body = call(post("/auth/login"), null, """
                        {"email": "%s", "password": "%s"}""".formatted(email, password))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();
        return JsonPath.read(body, "$.accessToken");
    }

    private ResultActions call(MockHttpServletRequestBuilder request, String token, String json) throws Exception {
        if (token != null) {
            request.header(HttpHeaders.AUTHORIZATION, "Bearer " + token);
        }
        if (json != null) {
            request.contentType(MediaType.APPLICATION_JSON).content(json);
        }
        return mockMvc.perform(request);
    }

    private static long idOf(ResultActions result) throws Exception {
        Number id = JsonPath.read(result.andReturn().getResponse().getContentAsString(), "$.id");
        return id.longValue();
    }
}
