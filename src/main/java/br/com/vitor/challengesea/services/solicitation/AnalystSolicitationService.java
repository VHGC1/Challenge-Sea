package br.com.vitor.challengesea.services.solicitation;

import br.com.vitor.challengesea.audit.Audit;
import br.com.vitor.challengesea.exceptions.ConflictException;
import br.com.vitor.challengesea.exceptions.ResourceNotFoundException;
import br.com.vitor.challengesea.mappers.solicitation.SolicitationMapper;
import br.com.vitor.challengesea.model.solicitation.Decision;
import br.com.vitor.challengesea.model.solicitation.DecisionDTO;
import br.com.vitor.challengesea.model.solicitation.Solicitation;
import br.com.vitor.challengesea.model.solicitation.SolicitationResponseDTO;
import br.com.vitor.challengesea.model.solicitation.SolicitationStatus;
import br.com.vitor.challengesea.model.user.AppUser;
import br.com.vitor.challengesea.model.user.Role;
import br.com.vitor.challengesea.repositories.solicitation.SolicitationRepository;
import br.com.vitor.challengesea.repositories.user.UserRepository;
import java.time.Instant;
import java.util.List;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * The review of submitted solicitations: SUBMITTED → IN_REVIEW → APPROVED or REJECTED. Starting
 * the review is optional; a SUBMITTED solicitation can be decided directly.
 * <p>
 * An ANALYST only reaches solicitations whose state is in their coverage (403 otherwise); an
 * ADMIN reaches every one. Drafts belong to their client and answer 404 here.
 */
@Service
public class AnalystSolicitationService {

    private final SolicitationRepository solicitationRepository;
    private final UserRepository userRepository;
    private final SolicitationMapper solicitationMapper;
    private final SolicitationIndexer solicitationIndexer;

    public AnalystSolicitationService(
            SolicitationRepository solicitationRepository,
            UserRepository userRepository,
            SolicitationMapper solicitationMapper,
            SolicitationIndexer solicitationIndexer) {
        this.solicitationRepository = solicitationRepository;
        this.userRepository = userRepository;
        this.solicitationMapper = solicitationMapper;
        this.solicitationIndexer = solicitationIndexer;
    }

    /**
     * @param id the solicitation
     * @param reviewerId the authenticated ANALYST or ADMIN
     * @return the solicitation
     * @throws ResourceNotFoundException when it does not exist or is a draft
     * @throws AccessDeniedException when its state is outside the analyst's coverage
     */
    @Transactional(readOnly = true)
    public SolicitationResponseDTO findForReview(Long id, Long reviewerId) {
        return solicitationMapper.toResponse(findReviewable(id, findReviewer(reviewerId)), List.of());
    }

    /**
     * SUBMITTED → IN_REVIEW, assigned to the caller.
     *
     * @param id the solicitation
     * @param reviewerId the authenticated ANALYST or ADMIN
     * @return the solicitation in review
     * @throws ResourceNotFoundException when it does not exist or is a draft
     * @throws AccessDeniedException when its state is outside the analyst's coverage
     * @throws ConflictException when it is not SUBMITTED
     */
    @Audit(action = "SOLICITATION_START_REVIEW")
    @Transactional
    public SolicitationResponseDTO startReview(Long id, Long reviewerId) {
        AppUser reviewer = findReviewer(reviewerId);
        Solicitation solicitation = findReviewable(id, reviewer);
        if (solicitation.getStatus() != SolicitationStatus.SUBMITTED) {
            throw new ConflictException("Só é possível iniciar a análise de uma solicitação SUBMITTED.");
        }
        solicitation.setStatus(SolicitationStatus.IN_REVIEW);
        solicitation.setAnalyst(reviewer);
        return saveAndIndex(solicitation);
    }

    /**
     * SUBMITTED or IN_REVIEW → APPROVED or REJECTED, with the reviewer's comment.
     *
     * @param id the solicitation
     * @param reviewerId the authenticated ANALYST or ADMIN
     * @param dto the decision and its comment
     * @return the decided solicitation
     * @throws ResourceNotFoundException when it does not exist or is a draft
     * @throws AccessDeniedException when its state is outside the analyst's coverage
     * @throws ConflictException when it was already decided
     */
    @Audit(action = "SOLICITATION_DECIDE")
    @Transactional
    public SolicitationResponseDTO decide(Long id, Long reviewerId, DecisionDTO dto) {
        AppUser reviewer = findReviewer(reviewerId);
        Solicitation solicitation = findReviewable(id, reviewer);
        if (solicitation.getStatus() != SolicitationStatus.SUBMITTED
                && solicitation.getStatus() != SolicitationStatus.IN_REVIEW) {
            throw new ConflictException("Esta solicitação já foi decidida.");
        }
        solicitation.setStatus(
                dto.decision() == Decision.APPROVE ? SolicitationStatus.APPROVED : SolicitationStatus.REJECTED);
        solicitation.setAnalyst(reviewer);
        solicitation.setDecisionComment(dto.comment());
        solicitation.setDecidedAt(Instant.now());
        return saveAndIndex(solicitation);
    }

    private AppUser findReviewer(Long reviewerId) {
        return userRepository
                .findById(reviewerId)
                .orElseThrow(() -> new ResourceNotFoundException("Usuário não encontrado."));
    }

    private Solicitation findReviewable(Long id, AppUser reviewer) {
        Solicitation solicitation = solicitationRepository
                .findById(id)
                .filter(found -> found.getStatus() != SolicitationStatus.DRAFT)
                .orElseThrow(() -> new ResourceNotFoundException("Solicitação não encontrada."));
        if (reviewer.getRole() == Role.ANALYST && !reviewer.getCoveredStates().contains(solicitation.getState())) {
            throw new AccessDeniedException(
                    String.format("A UF %s não está na sua cobertura.", solicitation.getState()));
        }
        return solicitation;
    }

    private SolicitationResponseDTO saveAndIndex(Solicitation solicitation) {
        Solicitation saved = solicitationRepository.saveAndFlush(solicitation);
        solicitationIndexer.indexAfterCommit(saved);
        return solicitationMapper.toResponse(saved, List.of());
    }
}
