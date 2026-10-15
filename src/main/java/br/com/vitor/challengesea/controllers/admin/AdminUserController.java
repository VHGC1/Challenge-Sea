package br.com.vitor.challengesea.controllers.admin;

import br.com.vitor.challengesea.model.user.CoveredStatesDTO;
import br.com.vitor.challengesea.model.user.CreateUserDTO;
import br.com.vitor.challengesea.model.user.UserResponseDTO;
import br.com.vitor.challengesea.services.user.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** ADMIN only (SecurityConfiguration): internal users and analyst coverage. */
@RestController
@RequestMapping("/admin/users")
@Tag(name = "Admin", description = "Internal users (ANALYST, ADMIN) and the states each analyst covers")
public class AdminUserController {

    private final UserService userService;

    public AdminUserController(UserService userService) {
        this.userService = userService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(
            summary = "Create an analyst or admin",
            description = "coveredStates (UF) only applies to an ANALYST. CLIENT is refused: clients self-register.")
    public UserResponseDTO create(@Valid @RequestBody CreateUserDTO request) {
        return userService.createInternalUser(request);
    }

    @PutMapping("/{id}/covered-states")
    @Operation(
            summary = "Set an analyst's states",
            description = "Replaces the whole set; takes effect on the next request.")
    public UserResponseDTO updateCoveredStates(@PathVariable Long id, @Valid @RequestBody CoveredStatesDTO request) {
        return userService.updateCoveredStates(id, request);
    }
}
