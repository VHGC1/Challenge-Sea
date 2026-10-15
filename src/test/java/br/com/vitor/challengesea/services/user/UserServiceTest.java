package br.com.vitor.challengesea.services.user;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import br.com.vitor.challengesea.exceptions.BadRequestException;
import br.com.vitor.challengesea.exceptions.ConflictException;
import br.com.vitor.challengesea.mappers.user.UserMapperImpl;
import br.com.vitor.challengesea.model.user.CreateUserDTO;
import br.com.vitor.challengesea.model.user.RegisterRequestDTO;
import br.com.vitor.challengesea.model.user.Role;
import br.com.vitor.challengesea.model.user.UserResponseDTO;
import br.com.vitor.challengesea.repositories.user.UserRepository;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

/** Unit tests for {@link UserService}. */
@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    private UserService service;

    @BeforeEach
    void setUp() {
        service = new UserService(userRepository, new UserMapperImpl(), passwordEncoder);
    }

    @Test
    void register_alwaysCreatesClientWithNormalizedEmailAndHashedPassword() {
        when(passwordEncoder.encode("s3cret-pass")).thenReturn("hash");
        when(userRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        UserResponseDTO user = service.register(new RegisterRequestDTO("Ana", " Ana@Mail.com ", "s3cret-pass"));

        assertThat(user.role()).isEqualTo(Role.CLIENT);
        assertThat(user.email()).isEqualTo("ana@mail.com");
    }

    @Test
    void register_takenEmail_isConflict() {
        when(userRepository.existsByEmail("ana@mail.com")).thenReturn(true);

        assertThatThrownBy(() -> service.register(new RegisterRequestDTO("Ana", "ana@mail.com", "s3cret-pass")))
                .isInstanceOf(ConflictException.class);
    }

    @Test
    void createInternalUser_client_isRejected() {
        assertThatThrownBy(() -> service.createInternalUser(
                        new CreateUserDTO("Ana", "ana@mail.com", "s3cret-pass", Role.CLIENT, null)))
                .isInstanceOf(BadRequestException.class);
    }

    @Test
    void createInternalUser_statesOnAdmin_areRejected() {
        assertThatThrownBy(() -> service.createInternalUser(
                        new CreateUserDTO("Ana", "ana@mail.com", "s3cret-pass", Role.ADMIN, Set.of("SP"))))
                .isInstanceOf(BadRequestException.class);
    }
}
