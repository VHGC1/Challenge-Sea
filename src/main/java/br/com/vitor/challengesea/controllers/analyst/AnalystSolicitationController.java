package br.com.vitor.challengesea.controllers.analyst;

import br.com.vitor.challengesea.model.PageResponseDTO;
import br.com.vitor.challengesea.model.solicitation.DecisionDTO;
import br.com.vitor.challengesea.model.solicitation.SolicitationResponseDTO;
import br.com.vitor.challengesea.model.solicitation.SolicitationSearchFilterDTO;
import br.com.vitor.challengesea.model.solicitation.SolicitationSummaryDTO;
import br.com.vitor.challengesea.services.solicitation.AnalystSolicitationService;
import br.com.vitor.challengesea.services.solicitation.SolicitationSearchService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** ANALYST and ADMIN (SecurityConfiguration): search and review, limited to an analyst's states. */
@RestController
@RequestMapping("/analyst/solicitations")
@Tag(name = "Analyst", description = "Search and review of submitted solicitations, by state coverage")
public class AnalystSolicitationController {

    private final AnalystSolicitationService analystSolicitationService;
    private final SolicitationSearchService solicitationSearchService;

    public AnalystSolicitationController(
            AnalystSolicitationService analystSolicitationService,
            SolicitationSearchService solicitationSearchService) {
        this.analystSolicitationService = analystSolicitationService;
        this.solicitationSearchService = solicitationSearchService;
    }

    @GetMapping("/search")
    @Operation(
            summary = "Search solicitations",
            description = "Elasticsearch full-text (q on title and description) and filters; dateFrom/dateTo bound "
                    + "the creation date. For an ANALYST the state filter is always their coverage. "
                    + "Sort on createdAt, submittedAt, preferredDate, estimatedValue, priority or status.")
    public PageResponseDTO<SolicitationSummaryDTO> search(
            @AuthenticationPrincipal Jwt jwt,
            @ParameterObject SolicitationSearchFilterDTO filter,
            @ParameterObject @PageableDefault(sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        return solicitationSearchService.search(userId(jwt), filter, pageable);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get a solicitation", description = "403 when its state is outside the analyst's coverage.")
    public SolicitationResponseDTO findForReview(@AuthenticationPrincipal Jwt jwt, @PathVariable Long id) {
        return analystSolicitationService.findForReview(id, userId(jwt));
    }

    @PostMapping("/{id}/start")
    @Operation(summary = "Start the review", description = "SUBMITTED → IN_REVIEW, assigned to the caller.")
    public SolicitationResponseDTO startReview(@AuthenticationPrincipal Jwt jwt, @PathVariable Long id) {
        return analystSolicitationService.startReview(id, userId(jwt));
    }

    @PostMapping("/{id}/decide")
    @Operation(
            summary = "Approve or reject",
            description = "SUBMITTED or IN_REVIEW → APPROVED or REJECTED, with a 10 to 1000 character comment.")
    public SolicitationResponseDTO decide(
            @AuthenticationPrincipal Jwt jwt, @PathVariable Long id, @Valid @RequestBody DecisionDTO dto) {
        return analystSolicitationService.decide(id, userId(jwt), dto);
    }

    private static Long userId(Jwt jwt) {
        return Long.valueOf(jwt.getSubject());
    }
}
