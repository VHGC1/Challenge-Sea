package br.com.vitor.challengesea.services.solicitation;

import br.com.vitor.challengesea.audit.Audit;
import br.com.vitor.challengesea.exceptions.ConflictException;
import br.com.vitor.challengesea.exceptions.PendingFieldsException;
import br.com.vitor.challengesea.exceptions.ResourceNotFoundException;
import br.com.vitor.challengesea.mappers.solicitation.SolicitationMapper;
import br.com.vitor.challengesea.model.PageResponseDTO;
import br.com.vitor.challengesea.model.address.ViaCepAddressDTO;
import br.com.vitor.challengesea.model.solicitation.AddressDTO;
import br.com.vitor.challengesea.model.solicitation.BasicDataDTO;
import br.com.vitor.challengesea.model.solicitation.ConfirmationDTO;
import br.com.vitor.challengesea.model.solicitation.PendingFieldDTO;
import br.com.vitor.challengesea.model.solicitation.Solicitation;
import br.com.vitor.challengesea.model.solicitation.SolicitationResponseDTO;
import br.com.vitor.challengesea.model.solicitation.SolicitationStatus;
import br.com.vitor.challengesea.model.solicitation.SolicitationStep;
import br.com.vitor.challengesea.repositories.solicitation.SolicitationRepository;
import br.com.vitor.challengesea.repositories.user.UserRepository;
import br.com.vitor.challengesea.services.address.ViaCepClient;
import jakarta.validation.Validator;
import java.time.Instant;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * A client's side of a solicitation: the draft, its three steps and the submit.
 * <p>
 * A step may be saved partially, so a client can stop and come back. A step counts as done when
 * the entity passes that step's validation group (see {@link SolicitationStep}); {@code currentStep}
 * is the first step that is not done, and a client cannot jump past it. Submit runs every group
 * and answers with every pending field at once.
 * <p>
 * A solicitation the caller does not own answers 404, the same as one that does not exist, so ids
 * of other clients' solicitations cannot be probed.
 */
@Service
public class SolicitationService {

    private final SolicitationRepository solicitationRepository;
    private final UserRepository userRepository;
    private final SolicitationMapper solicitationMapper;
    private final SolicitationIndexer solicitationIndexer;
    private final ViaCepClient viaCepClient;
    private final Validator validator;

    public SolicitationService(
            SolicitationRepository solicitationRepository,
            UserRepository userRepository,
            SolicitationMapper solicitationMapper,
            SolicitationIndexer solicitationIndexer,
            ViaCepClient viaCepClient,
            Validator validator) {
        this.solicitationRepository = solicitationRepository;
        this.userRepository = userRepository;
        this.solicitationMapper = solicitationMapper;
        this.solicitationIndexer = solicitationIndexer;
        this.viaCepClient = viaCepClient;
        this.validator = validator;
    }

    /**
     * Starts an empty draft at step 1.
     *
     * @param ownerId the authenticated client
     * @return the draft, with every required field pending
     */
    @Audit(action = "SOLICITATION_CREATE")
    @Transactional
    public SolicitationResponseDTO create(Long ownerId) {
        Solicitation solicitation = new Solicitation();
        solicitation.setOwner(userRepository.getReferenceById(ownerId));
        return saveAndIndex(solicitation, pendingFields(solicitation));
    }

    /**
     * @param id the solicitation
     * @param ownerId the authenticated client
     * @return the solicitation, with what is still pending while it is a draft
     * @throws ResourceNotFoundException when it does not exist or belongs to someone else
     */
    @Transactional(readOnly = true)
    public SolicitationResponseDTO findOwn(Long id, Long ownerId) {
        Solicitation solicitation = findOwned(id, ownerId);
        return solicitationMapper.toResponse(solicitation, pendingFieldsIfDraft(solicitation));
    }

    /**
     * @param ownerId the authenticated client
     * @param pageable page, size and sort
     * @return the client's solicitations, every status
     */
    @Transactional(readOnly = true)
    public PageResponseDTO<SolicitationResponseDTO> listOwn(Long ownerId, Pageable pageable) {
        return PageResponseDTO.of(solicitationRepository
                .findByOwnerId(ownerId, pageable)
                .map(solicitation -> solicitationMapper.toResponse(solicitation, pendingFieldsIfDraft(solicitation))));
    }

    /**
     * Saves step 1. Fields left null keep their saved value.
     *
     * @param id the solicitation
     * @param ownerId the authenticated client
     * @param dto the step's fields, possibly partial
     * @return the solicitation, with its new {@code currentStep} and pending fields
     * @throws ResourceNotFoundException when it does not exist or belongs to someone else
     * @throws ConflictException when it is no longer a draft
     */
    @Transactional
    public SolicitationResponseDTO saveBasicData(Long id, Long ownerId, BasicDataDTO dto) {
        return saveStep(
                id,
                ownerId,
                SolicitationStep.BASIC_DATA,
                solicitation -> solicitationMapper.updateBasicData(dto, solicitation));
    }

    /**
     * Saves step 2. A new CEP is looked up on ViaCEP, which fills street, neighborhood, city and
     * state; when the lookup fails the address is cleared and the step stays incomplete.
     *
     * @param id the solicitation
     * @param ownerId the authenticated client
     * @param dto the step's fields, possibly partial
     * @return the solicitation, with its new {@code currentStep} and pending fields
     * @throws ResourceNotFoundException when it does not exist or belongs to someone else
     * @throws ConflictException when it is no longer a draft, or step 1 is not done
     */
    @Transactional
    public SolicitationResponseDTO saveAddress(Long id, Long ownerId, AddressDTO dto) {
        return saveStep(id, ownerId, SolicitationStep.ADDRESS, solicitation -> {
            solicitationMapper.updateAddress(dto, solicitation);
            boolean needsLookup =
                    dto.cep() != null && (!dto.cep().equals(solicitation.getCep()) || !solicitation.isCepValidated());
            if (needsLookup) {
                solicitation.setCep(dto.cep());
                applyViaCepAddress(solicitation, viaCepClient.lookup(dto.cep()));
            }
        });
    }

    /**
     * Saves step 3.
     *
     * @param id the solicitation
     * @param ownerId the authenticated client
     * @param dto the step's fields, possibly partial
     * @return the solicitation, with its new {@code currentStep} and pending fields
     * @throws ResourceNotFoundException when it does not exist or belongs to someone else
     * @throws ConflictException when it is no longer a draft, or an earlier step is not done
     */
    @Transactional
    public SolicitationResponseDTO saveConfirmation(Long id, Long ownerId, ConfirmationDTO dto) {
        return saveStep(
                id,
                ownerId,
                SolicitationStep.CONFIRMATION,
                solicitation -> solicitationMapper.updateConfirmation(dto, solicitation));
    }

    /**
     * DRAFT → SUBMITTED, once every step passes its validation.
     *
     * @param id the solicitation
     * @param ownerId the authenticated client
     * @return the submitted solicitation
     * @throws ResourceNotFoundException when it does not exist or belongs to someone else
     * @throws ConflictException when it is not a draft
     * @throws PendingFieldsException listing every pending field, by step
     */
    @Audit(action = "SOLICITATION_SUBMIT")
    @Transactional
    public SolicitationResponseDTO submit(Long id, Long ownerId) {
        Solicitation solicitation = findDraft(id, ownerId);
        List<PendingFieldDTO> pending = pendingFields(solicitation);
        if (!pending.isEmpty()) {
            throw new PendingFieldsException(pending);
        }
        solicitation.setStatus(SolicitationStatus.SUBMITTED);
        solicitation.setSubmittedAt(Instant.now());
        return saveAndIndex(solicitation, List.of());
    }

    private SolicitationResponseDTO saveStep(
            Long id, Long ownerId, SolicitationStep step, Consumer<Solicitation> applyChanges) {
        Solicitation solicitation = findDraft(id, ownerId);
        if (step.number() > solicitation.getCurrentStep()) {
            throw new ConflictException(String.format(
                    "Conclua a etapa %d antes de preencher a etapa %d.", solicitation.getCurrentStep(), step.number()));
        }
        applyChanges.accept(solicitation);
        List<PendingFieldDTO> pending = pendingFields(solicitation);
        solicitation.setCurrentStep(firstIncompleteStep(pending));
        return saveAndIndex(solicitation, pending);
    }

    private static void applyViaCepAddress(Solicitation solicitation, Optional<ViaCepAddressDTO> address) {
        solicitation.setCepValidated(address.isPresent());
        solicitation.setStreet(address.map(ViaCepAddressDTO::logradouro).orElse(null));
        solicitation.setNeighborhood(address.map(ViaCepAddressDTO::bairro).orElse(null));
        solicitation.setCity(address.map(ViaCepAddressDTO::localidade).orElse(null));
        solicitation.setState(address.map(ViaCepAddressDTO::uf).orElse(null));
    }

    /** Every constraint violation of every step, ordered by step and then field. */
    List<PendingFieldDTO> pendingFields(Solicitation solicitation) {
        return Arrays.stream(SolicitationStep.values())
                .flatMap(step -> validator.validate(solicitation, step.group()).stream()
                        .map(violation -> new PendingFieldDTO(
                                step, violation.getPropertyPath().toString(), violation.getMessage())))
                .sorted(Comparator.comparing(PendingFieldDTO::step).thenComparing(PendingFieldDTO::field))
                .toList();
    }

    private List<PendingFieldDTO> pendingFieldsIfDraft(Solicitation solicitation) {
        return solicitation.getStatus() == SolicitationStatus.DRAFT ? pendingFields(solicitation) : List.of();
    }

    /** The step to resume at: the first with a pending field, or the last when all are done. */
    private static int firstIncompleteStep(List<PendingFieldDTO> pending) {
        return pending.stream()
                .map(field -> field.step().number())
                .findFirst()
                .orElse(SolicitationStep.CONFIRMATION.number());
    }

    private Solicitation findOwned(Long id, Long ownerId) {
        return solicitationRepository
                .findByIdAndOwnerId(id, ownerId)
                .orElseThrow(() -> new ResourceNotFoundException("Solicitação não encontrada."));
    }

    private Solicitation findDraft(Long id, Long ownerId) {
        Solicitation solicitation = findOwned(id, ownerId);
        if (solicitation.getStatus() != SolicitationStatus.DRAFT) {
            throw new ConflictException("Só é possível alterar uma solicitação em DRAFT.");
        }
        return solicitation;
    }

    private SolicitationResponseDTO saveAndIndex(Solicitation solicitation, List<PendingFieldDTO> pending) {
        Solicitation saved = solicitationRepository.saveAndFlush(solicitation);
        solicitationIndexer.indexAfterCommit(saved);
        return solicitationMapper.toResponse(saved, pending);
    }
}
