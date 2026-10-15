package br.com.vitor.challengesea.controllers.solicitation;

import br.com.vitor.challengesea.model.PageResponseDTO;
import br.com.vitor.challengesea.model.solicitation.AddressDTO;
import br.com.vitor.challengesea.model.solicitation.BasicDataDTO;
import br.com.vitor.challengesea.model.solicitation.ConfirmationDTO;
import br.com.vitor.challengesea.model.solicitation.SolicitationResponseDTO;
import br.com.vitor.challengesea.services.solicitation.SolicitationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** CLIENT only (SecurityConfiguration): a client's own solicitations, step by step. */
@RestController
@RequestMapping("/solicitations")
@Tag(name = "Solicitations", description = "A client's solicitations: draft, the three steps and submit")
public class SolicitationController {

    private final SolicitationService solicitationService;

    public SolicitationController(SolicitationService solicitationService) {
        this.solicitationService = solicitationService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Start a draft", description = "An empty DRAFT at currentStep 1.")
    public SolicitationResponseDTO create(@AuthenticationPrincipal Jwt jwt) {
        return solicitationService.create(userId(jwt));
    }

    @GetMapping
    @Operation(summary = "List my solicitations", description = "Every status, newest first by default.")
    public PageResponseDTO<SolicitationResponseDTO> listOwn(
            @AuthenticationPrincipal Jwt jwt,
            @ParameterObject @PageableDefault(sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        return solicitationService.listOwn(userId(jwt), pageable);
    }

    @GetMapping("/{id}")
    @Operation(
            summary = "Get one of my solicitations",
            description = "While DRAFT, pendingFields lists what each step still needs.")
    public SolicitationResponseDTO findOwn(@AuthenticationPrincipal Jwt jwt, @PathVariable Long id) {
        return solicitationService.findOwn(id, userId(jwt));
    }

    @PutMapping("/{id}/steps/basic-data")
    @Operation(
            summary = "Save step 1: basic data",
            description = "Partial saves are allowed; null fields keep their value. Only while DRAFT.")
    public SolicitationResponseDTO saveBasicData(
            @AuthenticationPrincipal Jwt jwt, @PathVariable Long id, @Valid @RequestBody BasicDataDTO dto) {
        return solicitationService.saveBasicData(id, userId(jwt), dto);
    }

    @PutMapping("/{id}/steps/address")
    @Operation(
            summary = "Save step 2: address",
            description = "The CEP is looked up on ViaCEP, which fills street, neighborhood, city and state. "
                    + "An invalid CEP or a ViaCEP failure leaves the step incomplete. Needs step 1 done.")
    public SolicitationResponseDTO saveAddress(
            @AuthenticationPrincipal Jwt jwt, @PathVariable Long id, @Valid @RequestBody AddressDTO dto) {
        return solicitationService.saveAddress(id, userId(jwt), dto);
    }

    @PutMapping("/{id}/steps/confirmation")
    @Operation(
            summary = "Save step 3: confirmation",
            description =
                    "HIGH priority needs estimatedValue >= 100; termsAccepted must be true. Needs steps 1 and 2 done.")
    public SolicitationResponseDTO saveConfirmation(
            @AuthenticationPrincipal Jwt jwt, @PathVariable Long id, @Valid @RequestBody ConfirmationDTO dto) {
        return solicitationService.saveConfirmation(id, userId(jwt), dto);
    }

    @PostMapping("/{id}/submit")
    @Operation(
            summary = "Submit",
            description = "DRAFT → SUBMITTED. Validates every step; 422 lists every pending field by step.")
    public SolicitationResponseDTO submit(@AuthenticationPrincipal Jwt jwt, @PathVariable Long id) {
        return solicitationService.submit(id, userId(jwt));
    }

    private static Long userId(Jwt jwt) {
        return Long.valueOf(jwt.getSubject());
    }
}
