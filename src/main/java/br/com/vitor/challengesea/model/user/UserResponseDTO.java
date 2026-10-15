package br.com.vitor.challengesea.model.user;

import br.com.vitor.challengesea.audit.Identifiable;
import java.util.Set;

public record UserResponseDTO(Long id, String name, String email, Role role, Set<String> coveredStates)
        implements Identifiable {}
