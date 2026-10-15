package br.com.vitor.challengesea.services.solicitation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import br.com.vitor.challengesea.exceptions.BadRequestException;
import br.com.vitor.challengesea.mappers.solicitation.SolicitationMapperImpl;
import br.com.vitor.challengesea.model.solicitation.SolicitationSearchFilterDTO;
import br.com.vitor.challengesea.model.user.AppUser;
import br.com.vitor.challengesea.model.user.Role;
import br.com.vitor.challengesea.repositories.user.UserRepository;
import java.time.LocalDate;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;

/** Unit tests for the query {@link SolicitationSearchService} sends to Elasticsearch. */
@ExtendWith(MockitoExtension.class)
class SolicitationSearchServiceTest {

    private static final SolicitationSearchFilterDTO NO_FILTER =
            new SolicitationSearchFilterDTO(null, null, null, null, null, null, null);

    @Mock
    private ElasticsearchOperations elasticsearchOperations;

    @Mock
    private UserRepository userRepository;

    private SolicitationSearchService service;

    @BeforeEach
    void setUp() {
        service = new SolicitationSearchService(elasticsearchOperations, userRepository, new SolicitationMapperImpl());
    }

    @Test
    void analyst_isAlwaysLimitedToCoverage_evenWhenAskingForAnotherState() {
        String query = service.buildQuery(
                        user(Role.ANALYST, "SP"),
                        new SolicitationSearchFilterDTO(null, null, null, null, "RJ", null, null))
                .toString();

        assertThat(query)
                .contains("\"terms\":{\"state\":[\"SP\"]}")
                .contains("\"term\":{\"state\":{\"value\":\"RJ\"}}");
    }

    @Test
    void admin_hasNoCoverageFilter() {
        String query = service.buildQuery(user(Role.ADMIN), NO_FILTER).toString();

        assertThat(query).doesNotContain("\"terms\"");
    }

    @Test
    void drafts_areAlwaysExcluded() {
        String query = service.buildQuery(user(Role.ADMIN), NO_FILTER).toString();

        assertThat(query).contains("\"must_not\":[{\"term\":{\"status\":{\"value\":\"DRAFT\"}}}]");
    }

    @Test
    void textAndDates_becomeMultiMatchAndInclusiveDayRange() {
        String query = service.buildQuery(
                        user(Role.ADMIN),
                        new SolicitationSearchFilterDTO(
                                "split", null, null, null, null, LocalDate.of(2026, 1, 1), LocalDate.of(2026, 1, 31)))
                .toString();

        assertThat(query)
                .contains("\"multi_match\"")
                .contains("\"gte\":\"2026-01-01\"")
                .contains("\"lt\":\"2026-02-01\"");
    }

    @Test
    void sortOnUnknownField_isBadRequest() {
        PageRequest pageable = PageRequest.of(0, 20, Sort.by("title"));

        assertThatThrownBy(() -> service.search(1L, NO_FILTER, pageable)).isInstanceOf(BadRequestException.class);
    }

    private static AppUser user(Role role, String... states) {
        AppUser user = new AppUser();
        user.setRole(role);
        user.setCoveredStates(Set.of(states));
        return user;
    }
}
