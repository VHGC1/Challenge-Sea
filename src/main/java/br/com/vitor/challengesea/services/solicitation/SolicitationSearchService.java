package br.com.vitor.challengesea.services.solicitation;

import br.com.vitor.challengesea.exceptions.BadRequestException;
import br.com.vitor.challengesea.exceptions.ResourceNotFoundException;
import br.com.vitor.challengesea.mappers.solicitation.SolicitationMapper;
import br.com.vitor.challengesea.model.PageResponseDTO;
import br.com.vitor.challengesea.model.solicitation.SolicitationDocument;
import br.com.vitor.challengesea.model.solicitation.SolicitationSearchFilterDTO;
import br.com.vitor.challengesea.model.solicitation.SolicitationStatus;
import br.com.vitor.challengesea.model.solicitation.SolicitationSummaryDTO;
import br.com.vitor.challengesea.model.user.AppUser;
import br.com.vitor.challengesea.model.user.Role;
import br.com.vitor.challengesea.repositories.user.UserRepository;
import co.elastic.clients.elasticsearch._types.FieldValue;
import co.elastic.clients.elasticsearch._types.query_dsl.BoolQuery;
import co.elastic.clients.elasticsearch._types.query_dsl.Query;
import java.util.List;
import java.util.Set;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.elasticsearch.client.elc.NativeQuery;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.SearchHit;
import org.springframework.data.elasticsearch.core.SearchHits;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

/**
 * The analyst search, on the Elasticsearch index. Drafts never appear.
 * <p>
 * For an ANALYST the state filter is always their coverage, added on the server. A {@code state}
 * in the request only narrows it further: asking for a state outside the coverage finds nothing.
 */
@Service
public class SolicitationSearchService {

    static final Set<String> SORTABLE_FIELDS =
            Set.of("createdAt", "submittedAt", "preferredDate", "estimatedValue", "priority", "status");

    /** Calendar days in the filter are Brazilian days, not UTC ones. */
    private static final String TIME_ZONE = "America/Sao_Paulo";

    private final ElasticsearchOperations elasticsearchOperations;
    private final UserRepository userRepository;
    private final SolicitationMapper solicitationMapper;

    public SolicitationSearchService(
            ElasticsearchOperations elasticsearchOperations,
            UserRepository userRepository,
            SolicitationMapper solicitationMapper) {
        this.elasticsearchOperations = elasticsearchOperations;
        this.userRepository = userRepository;
        this.solicitationMapper = solicitationMapper;
    }

    /**
     * @param reviewerId the authenticated ANALYST or ADMIN
     * @param filter the optional filters
     * @param pageable page, size and sort, on one of {@link #SORTABLE_FIELDS}
     * @return one page of hits and the total count
     * @throws BadRequestException for a sort on any other field, or dateFrom after dateTo
     */
    @Transactional(readOnly = true)
    public PageResponseDTO<SolicitationSummaryDTO> search(
            Long reviewerId, SolicitationSearchFilterDTO filter, Pageable pageable) {
        for (Sort.Order order : pageable.getSort()) {
            if (!SORTABLE_FIELDS.contains(order.getProperty())) {
                throw new BadRequestException("Não é possível ordenar por " + order.getProperty() + ".");
            }
        }
        if (filter.dateFrom() != null
                && filter.dateTo() != null
                && filter.dateFrom().isAfter(filter.dateTo())) {
            throw new BadRequestException("dateFrom deve ser anterior ou igual a dateTo.");
        }
        AppUser reviewer = userRepository
                .findById(reviewerId)
                .orElseThrow(() -> new ResourceNotFoundException("Usuário não encontrado."));

        NativeQuery query = NativeQuery.builder()
                .withQuery(buildQuery(reviewer, filter))
                .withPageable(pageable)
                .withTrackTotalHits(true)
                .build();
        SearchHits<SolicitationDocument> hits = elasticsearchOperations.search(query, SolicitationDocument.class);

        List<SolicitationSummaryDTO> items = hits.getSearchHits().stream()
                .map(SearchHit::getContent)
                .map(solicitationMapper::toSummary)
                .toList();
        return new PageResponseDTO<>(items, pageable.getPageNumber(), pageable.getPageSize(), hits.getTotalHits());
    }

    Query buildQuery(AppUser reviewer, SolicitationSearchFilterDTO filter) {
        BoolQuery.Builder bool = new BoolQuery.Builder();
        bool.mustNot(term("status", SolicitationStatus.DRAFT.name()));

        if (reviewer.getRole() == Role.ANALYST) {
            List<FieldValue> coverage =
                    reviewer.getCoveredStates().stream().map(FieldValue::of).toList();
            bool.filter(f -> f.terms(t -> t.field("state").terms(v -> v.value(coverage))));
        }
        if (StringUtils.hasText(filter.q())) {
            bool.must(m -> m.multiMatch(mm -> mm.query(filter.q()).fields("title^2", "description")));
        }
        if (filter.status() != null) {
            bool.filter(term("status", filter.status().name()));
        }
        if (filter.serviceType() != null) {
            bool.filter(term("serviceType", filter.serviceType().name()));
        }
        if (filter.priority() != null) {
            bool.filter(term("priority", filter.priority().name()));
        }
        if (StringUtils.hasText(filter.state())) {
            bool.filter(term("state", filter.state()));
        }
        if (filter.dateFrom() != null || filter.dateTo() != null) {
            bool.filter(f -> f.range(r -> r.date(d -> {
                d.field("createdAt").format("strict_date").timeZone(TIME_ZONE);
                if (filter.dateFrom() != null) {
                    d.gte(filter.dateFrom().toString());
                }
                if (filter.dateTo() != null) {
                    d.lt(filter.dateTo().plusDays(1).toString());
                }
                return d;
            })));
        }
        return bool.build()._toQuery();
    }

    private static Query term(String field, String value) {
        return Query.of(q -> q.term(t -> t.field(field).value(value)));
    }
}
