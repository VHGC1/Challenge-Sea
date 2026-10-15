package br.com.vitor.challengesea.controllers.option;

import br.com.vitor.challengesea.model.OptionDTO;
import br.com.vitor.challengesea.model.solicitation.Decision;
import br.com.vitor.challengesea.model.solicitation.Priority;
import br.com.vitor.challengesea.model.solicitation.ServiceType;
import br.com.vitor.challengesea.model.solicitation.SolicitationStatus;
import br.com.vitor.challengesea.model.solicitation.SolicitationStep;
import br.com.vitor.challengesea.model.user.Role;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * The values each enum accepts, with the pt-BR text to show, so the frontend never hardcodes
 * them. Any authenticated user.
 */
@RestController
@RequestMapping("/options")
@Tag(name = "Options", description = "Enum values with label and description, for frontend selects")
public class OptionController {

    @GetMapping("/service-types")
    @Operation(summary = "Service types", description = "Values for serviceType in step 1 and the search filter.")
    public List<OptionDTO> serviceTypes() {
        return OptionDTO.of(ServiceType.values());
    }

    @GetMapping("/priorities")
    @Operation(summary = "Priorities", description = "Values for priority in step 3 and the search filter.")
    public List<OptionDTO> priorities() {
        return OptionDTO.of(Priority.values());
    }

    @GetMapping("/statuses")
    @Operation(summary = "Statuses", description = "Every solicitation status, for display and the search filter.")
    public List<OptionDTO> statuses() {
        return OptionDTO.of(SolicitationStatus.values());
    }

    @GetMapping("/decisions")
    @Operation(summary = "Decisions", description = "Values for decision when an analyst decides.")
    public List<OptionDTO> decisions() {
        return OptionDTO.of(Decision.values());
    }

    @GetMapping("/steps")
    @Operation(summary = "Steps", description = "The three steps, in order, as named in pendingFields.")
    public List<OptionDTO> steps() {
        return OptionDTO.of(SolicitationStep.values());
    }

    @GetMapping("/roles")
    @Operation(
            summary = "Roles an admin can assign",
            description = "ANALYST and ADMIN; clients register themselves through /auth/register.")
    public List<OptionDTO> roles() {
        return OptionDTO.of(Role.ANALYST, Role.ADMIN);
    }
}
