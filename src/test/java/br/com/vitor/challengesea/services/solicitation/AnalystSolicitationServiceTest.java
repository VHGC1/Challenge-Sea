package br.com.vitor.challengesea.services.solicitation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

import br.com.vitor.challengesea.exceptions.ConflictException;
import br.com.vitor.challengesea.exceptions.ResourceNotFoundException;
import br.com.vitor.challengesea.mappers.solicitation.SolicitationMapperImpl;
import br.com.vitor.challengesea.model.solicitation.Decision;
import br.com.vitor.challengesea.model.solicitation.DecisionDTO;
import br.com.vitor.challengesea.model.solicitation.Solicitation;
import br.com.vitor.challengesea.model.solicitation.SolicitationResponseDTO;
import br.com.vitor.challengesea.model.solicitation.SolicitationStatus;
import br.com.vitor.challengesea.model.user.AppUser;
import br.com.vitor.challengesea.model.user.Role;
import br.com.vitor.challengesea.repositories.solicitation.SolicitationRepository;
import br.com.vitor.challengesea.repositories.user.UserRepository;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;

/** Unit tests for {@link AnalystSolicitationService}: state coverage and the review transitions. */
@ExtendWith(MockitoExtension.class)
class AnalystSolicitationServiceTest {

    private static final Long ID = 1L;
    private static final Long REVIEWER_ID = 20L;

    @Mock
    private SolicitationRepository solicitationRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private SolicitationIndexer solicitationIndexer;

    private AnalystSolicitationService service;

    @BeforeEach
    void setUp() {
        service = new AnalystSolicitationService(
                solicitationRepository, userRepository, new SolicitationMapperImpl(), solicitationIndexer);
        lenient().when(solicitationRepository.saveAndFlush(any())).thenAnswer(invocation -> invocation.getArgument(0));
    }

    @Test
    void stateOutsideCoverage_isForbidden() {
        reviewer(Role.ANALYST, "RJ");
        stored(SolicitationStatus.SUBMITTED, "SP");

        assertThatThrownBy(() -> service.findForReview(ID, REVIEWER_ID)).isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void stateInsideCoverage_isVisible() {
        reviewer(Role.ANALYST, "SP", "RJ");
        stored(SolicitationStatus.SUBMITTED, "SP");

        assertThat(service.findForReview(ID, REVIEWER_ID).id()).isEqualTo(ID);
    }

    @Test
    void admin_seesEveryState() {
        reviewer(Role.ADMIN);
        stored(SolicitationStatus.SUBMITTED, "AM");

        assertThat(service.findForReview(ID, REVIEWER_ID).state()).isEqualTo("AM");
    }

    @Test
    void draft_isNotFoundForReviewers() {
        reviewer(Role.ADMIN);
        stored(SolicitationStatus.DRAFT, "SP");

        assertThatThrownBy(() -> service.findForReview(ID, REVIEWER_ID)).isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void startSubmitted_becomesInReviewAssignedToReviewer() {
        reviewer(Role.ANALYST, "SP");
        stored(SolicitationStatus.SUBMITTED, "SP");

        SolicitationResponseDTO response = service.startReview(ID, REVIEWER_ID);

        assertThat(response.status()).isEqualTo(SolicitationStatus.IN_REVIEW);
        assertThat(response.analystId()).isEqualTo(REVIEWER_ID);
    }

    @Test
    void startInReview_isConflict() {
        reviewer(Role.ANALYST, "SP");
        stored(SolicitationStatus.IN_REVIEW, "SP");

        assertThatThrownBy(() -> service.startReview(ID, REVIEWER_ID)).isInstanceOf(ConflictException.class);
    }

    @Test
    void rejectSubmittedWithoutStarting_recordsDecision() {
        reviewer(Role.ANALYST, "SP");
        stored(SolicitationStatus.SUBMITTED, "SP");

        SolicitationResponseDTO response =
                service.decide(ID, REVIEWER_ID, new DecisionDTO(Decision.REJECT, "Endereço fora da área atendida"));

        assertThat(response.status()).isEqualTo(SolicitationStatus.REJECTED);
        assertThat(response.decisionComment()).isEqualTo("Endereço fora da área atendida");
        assertThat(response.decidedAt()).isNotNull();
    }

    @Test
    void decideAlreadyApproved_isConflict() {
        reviewer(Role.ANALYST, "SP");
        stored(SolicitationStatus.APPROVED, "SP");

        assertThatThrownBy(() -> service.decide(ID, REVIEWER_ID, new DecisionDTO(Decision.REJECT, "Mudança de ideia")))
                .isInstanceOf(ConflictException.class);
    }

    private void reviewer(Role role, String... states) {
        AppUser reviewer = new AppUser();
        reviewer.setId(REVIEWER_ID);
        reviewer.setRole(role);
        reviewer.setCoveredStates(Set.of(states));
        when(userRepository.findById(REVIEWER_ID)).thenReturn(Optional.of(reviewer));
    }

    private void stored(SolicitationStatus status, String state) {
        Solicitation solicitation = new Solicitation();
        solicitation.setId(ID);
        solicitation.setStatus(status);
        solicitation.setState(state);
        when(solicitationRepository.findById(ID)).thenReturn(Optional.of(solicitation));
    }
}
