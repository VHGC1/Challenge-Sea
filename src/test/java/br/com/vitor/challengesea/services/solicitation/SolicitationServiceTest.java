package br.com.vitor.challengesea.services.solicitation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import br.com.vitor.challengesea.exceptions.ConflictException;
import br.com.vitor.challengesea.exceptions.PendingFieldsException;
import br.com.vitor.challengesea.exceptions.ResourceNotFoundException;
import br.com.vitor.challengesea.mappers.solicitation.SolicitationMapperImpl;
import br.com.vitor.challengesea.model.address.ViaCepAddressDTO;
import br.com.vitor.challengesea.model.solicitation.AddressDTO;
import br.com.vitor.challengesea.model.solicitation.BasicDataDTO;
import br.com.vitor.challengesea.model.solicitation.ConfirmationDTO;
import br.com.vitor.challengesea.model.solicitation.PendingFieldDTO;
import br.com.vitor.challengesea.model.solicitation.Priority;
import br.com.vitor.challengesea.model.solicitation.ServiceType;
import br.com.vitor.challengesea.model.solicitation.Solicitation;
import br.com.vitor.challengesea.model.solicitation.SolicitationResponseDTO;
import br.com.vitor.challengesea.model.solicitation.SolicitationStatus;
import br.com.vitor.challengesea.model.solicitation.SolicitationStep;
import br.com.vitor.challengesea.model.user.AppUser;
import br.com.vitor.challengesea.repositories.solicitation.SolicitationRepository;
import br.com.vitor.challengesea.repositories.user.UserRepository;
import br.com.vitor.challengesea.services.address.ViaCepClient;
import jakarta.validation.Validation;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/** Unit tests for {@link SolicitationService}, with the real mapper and the real Bean Validation rules. */
@ExtendWith(MockitoExtension.class)
class SolicitationServiceTest {

    private static final Long OWNER_ID = 7L;
    private static final Long ID = 1L;
    private static final String CEP = "01001000";

    @Mock
    private SolicitationRepository solicitationRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private SolicitationIndexer solicitationIndexer;

    @Mock
    private ViaCepClient viaCepClient;

    private SolicitationService service;

    @BeforeEach
    void setUp() {
        service = new SolicitationService(
                solicitationRepository,
                userRepository,
                new SolicitationMapperImpl(),
                solicitationIndexer,
                viaCepClient,
                Validation.buildDefaultValidatorFactory().getValidator());
    }

    @Test
    void completeBasicData_advancesToAddressStep() {
        Solicitation draft = owned(new Solicitation());

        SolicitationResponseDTO response = service.saveBasicData(
                ID,
                OWNER_ID,
                new BasicDataDTO(ServiceType.INSTALLATION, "  Ar-condicionado  ", "Instalar dois aparelhos split"));

        assertThat(response.currentStep()).isEqualTo(2);
        assertThat(response.title()).isEqualTo("Ar-condicionado");
        assertThat(response.pendingFields()).noneMatch(field -> field.step() == SolicitationStep.BASIC_DATA);
        verify(solicitationIndexer).indexAfterCommit(draft);
    }

    @Test
    void partialBasicData_keepsStepOneAndListsWhatIsMissing() {
        owned(new Solicitation());

        SolicitationResponseDTO response = service.saveBasicData(ID, OWNER_ID, new BasicDataDTO(null, "Título", null));

        assertThat(response.currentStep()).isEqualTo(1);
        assertThat(response.pendingFields())
                .filteredOn(field -> field.step() == SolicitationStep.BASIC_DATA)
                .extracting(PendingFieldDTO::field)
                .containsExactly("description", "serviceType");
    }

    @Test
    void addressBeforeBasicDataIsDone_isRejected() {
        owned(new Solicitation());

        assertThatThrownBy(() -> service.saveAddress(ID, OWNER_ID, new AddressDTO(CEP, "12A", null)))
                .isInstanceOf(ConflictException.class);
    }

    @Test
    void cepFoundOnViaCep_fillsAddressAndAdvances() {
        owned(withBasicData(new Solicitation()));
        when(viaCepClient.lookup(CEP))
                .thenReturn(Optional.of(new ViaCepAddressDTO("01001-000", "Praça da Sé", "Sé", "São Paulo", "SP")));

        SolicitationResponseDTO response = service.saveAddress(ID, OWNER_ID, new AddressDTO("01001-000", "12A", null));

        assertThat(response.cep()).isEqualTo(CEP);
        assertThat(response.state()).isEqualTo("SP");
        assertThat(response.city()).isEqualTo("São Paulo");
        assertThat(response.currentStep()).isEqualTo(3);
    }

    @Test
    void cepNotFoundOrViaCepDown_addressStepStaysIncomplete() {
        owned(withBasicData(new Solicitation()));
        when(viaCepClient.lookup(CEP)).thenReturn(Optional.empty());

        SolicitationResponseDTO response = service.saveAddress(ID, OWNER_ID, new AddressDTO(CEP, "12A", null));

        assertThat(response.cepValidated()).isFalse();
        assertThat(response.state()).isNull();
        assertThat(response.currentStep()).isEqualTo(2);
        assertThat(response.pendingFields()).extracting(PendingFieldDTO::field).contains("cepValidated");
    }

    @Test
    void highPriorityBelowMinimumValue_isPending() {
        owned(withAddress(withBasicData(new Solicitation())));

        SolicitationResponseDTO response = service.saveConfirmation(
                ID,
                OWNER_ID,
                new ConfirmationDTO(Priority.HIGH, LocalDate.now().plusDays(3), new BigDecimal("50"), true));

        assertThat(response.pendingFields())
                .extracting(PendingFieldDTO::field)
                .containsExactly("estimatedValueAllowedForPriority");
    }

    @Test
    void submitWithPendingFields_listsEveryOneByStepAndStaysDraft() {
        Solicitation draft = owned(withBasicData(new Solicitation()));

        assertThatThrownBy(() -> service.submit(ID, OWNER_ID))
                .isInstanceOfSatisfying(
                        PendingFieldsException.class,
                        ex -> assertThat(ex.getPendingFields())
                                .extracting(PendingFieldDTO::step)
                                .containsOnly(SolicitationStep.ADDRESS, SolicitationStep.CONFIRMATION));
        assertThat(draft.getStatus()).isEqualTo(SolicitationStatus.DRAFT);
        verify(solicitationRepository, never()).saveAndFlush(any());
    }

    @Test
    void submitComplete_becomesSubmittedAndIsIndexed() {
        Solicitation draft = owned(complete(new Solicitation()));

        SolicitationResponseDTO response = service.submit(ID, OWNER_ID);

        assertThat(response.status()).isEqualTo(SolicitationStatus.SUBMITTED);
        assertThat(response.submittedAt()).isNotNull();
        verify(solicitationIndexer).indexAfterCommit(draft);
    }

    @Test
    void submitAlreadySubmitted_isConflict() {
        Solicitation submitted = complete(new Solicitation());
        submitted.setStatus(SolicitationStatus.SUBMITTED);
        when(solicitationRepository.findByIdAndOwnerId(ID, OWNER_ID)).thenReturn(Optional.of(submitted));

        assertThatThrownBy(() -> service.submit(ID, OWNER_ID)).isInstanceOf(ConflictException.class);
    }

    @Test
    void someoneElsesSolicitation_isNotFound() {
        when(solicitationRepository.findByIdAndOwnerId(ID, OWNER_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.findOwn(ID, OWNER_ID)).isInstanceOf(ResourceNotFoundException.class);
    }

    private Solicitation owned(Solicitation solicitation) {
        AppUser owner = new AppUser();
        owner.setId(OWNER_ID);
        solicitation.setId(ID);
        solicitation.setOwner(owner);
        when(solicitationRepository.findByIdAndOwnerId(ID, OWNER_ID)).thenReturn(Optional.of(solicitation));
        // Not reached on the paths that throw before saving
        lenient().when(solicitationRepository.saveAndFlush(any())).thenAnswer(invocation -> invocation.getArgument(0));
        return solicitation;
    }

    private static Solicitation withBasicData(Solicitation solicitation) {
        solicitation.setServiceType(ServiceType.MAINTENANCE);
        solicitation.setTitle("Manutenção");
        solicitation.setDescription("Revisão completa do quadro elétrico");
        solicitation.setCurrentStep(2);
        return solicitation;
    }

    private static Solicitation withAddress(Solicitation solicitation) {
        solicitation.setCep(CEP);
        solicitation.setCepValidated(true);
        solicitation.setState("SP");
        solicitation.setNumber("100");
        solicitation.setCurrentStep(3);
        return solicitation;
    }

    private static Solicitation complete(Solicitation solicitation) {
        withAddress(withBasicData(solicitation));
        solicitation.setPriority(Priority.HIGH);
        solicitation.setPreferredDate(LocalDate.now().plusDays(1));
        solicitation.setEstimatedValue(new BigDecimal("150.00"));
        solicitation.setTermsAccepted(true);
        return solicitation;
    }
}
