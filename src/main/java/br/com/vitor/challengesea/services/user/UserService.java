package br.com.vitor.challengesea.services.user;

import br.com.vitor.challengesea.audit.Audit;
import br.com.vitor.challengesea.exceptions.BadRequestException;
import br.com.vitor.challengesea.exceptions.ConflictException;
import br.com.vitor.challengesea.exceptions.ResourceNotFoundException;
import br.com.vitor.challengesea.mappers.user.UserMapper;
import br.com.vitor.challengesea.model.user.AppUser;
import br.com.vitor.challengesea.model.user.CoveredStatesDTO;
import br.com.vitor.challengesea.model.user.CreateUserDTO;
import br.com.vitor.challengesea.model.user.RegisterRequestDTO;
import br.com.vitor.challengesea.model.user.Role;
import br.com.vitor.challengesea.model.user.UserResponseDTO;
import br.com.vitor.challengesea.repositories.user.UserRepository;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Creates users: clients register themselves, an ADMIN creates analysts and admins and sets the
 * states (UF) each analyst covers.
 */
@Service
public class UserService {

    private final UserRepository userRepository;
    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;

    public UserService(UserRepository userRepository, UserMapper userMapper, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.userMapper = userMapper;
        this.passwordEncoder = passwordEncoder;
    }

    /**
     * Self-registration; always creates a CLIENT.
     *
     * @param request name, email and password
     * @return the new user
     * @throws ConflictException when the email is taken
     */
    @Audit(action = "USER_REGISTER")
    @Transactional
    public UserResponseDTO register(RegisterRequestDTO request) {
        return persistNew(userMapper.toEntity(request), request.password());
    }

    /**
     * An internal user, created by an ADMIN.
     *
     * @param request the user; {@code coveredStates} only for an ANALYST
     * @return the new user
     * @throws BadRequestException for a CLIENT (they self-register) or states on a non-analyst
     * @throws ConflictException when the email is taken
     */
    @Audit(action = "USER_CREATE")
    @Transactional
    public UserResponseDTO createInternalUser(CreateUserDTO request) {
        if (request.role() == Role.CLIENT) {
            throw new BadRequestException("Clientes se cadastram por /auth/register.");
        }
        Set<String> states = request.coveredStates() == null ? Set.of() : request.coveredStates();
        if (request.role() != Role.ANALYST && !states.isEmpty()) {
            throw new BadRequestException("Apenas analistas têm UFs de cobertura.");
        }
        AppUser user = userMapper.toEntity(request);
        user.setCoveredStates(new HashSet<>(states));
        return persistNew(user, request.password());
    }

    /**
     * Replaces the states an analyst covers.
     *
     * @param userId the analyst
     * @param request the new set of states, possibly empty
     * @return the updated user
     * @throws ResourceNotFoundException when the user does not exist
     * @throws BadRequestException when the user is not an ANALYST
     */
    @Audit(action = "ANALYST_COVERAGE_UPDATE")
    @Transactional
    public UserResponseDTO updateCoveredStates(Long userId, CoveredStatesDTO request) {
        AppUser user = userRepository
                .findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Usuário não encontrado."));
        if (user.getRole() != Role.ANALYST) {
            throw new BadRequestException("Apenas analistas têm UFs de cobertura.");
        }
        user.getCoveredStates().clear();
        user.getCoveredStates().addAll(request.states());
        return userMapper.toResponse(user);
    }

    private UserResponseDTO persistNew(AppUser user, String rawPassword) {
        String email = user.getEmail().strip().toLowerCase(Locale.ROOT);
        if (userRepository.existsByEmail(email)) {
            throw new ConflictException("E-mail já cadastrado.");
        }
        user.setEmail(email);
        user.setPasswordHash(passwordEncoder.encode(rawPassword));
        return userMapper.toResponse(userRepository.save(user));
    }
}
