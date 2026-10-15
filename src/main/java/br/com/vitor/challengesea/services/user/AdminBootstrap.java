package br.com.vitor.challengesea.services.user;

import br.com.vitor.challengesea.model.user.AppUser;
import br.com.vitor.challengesea.model.user.Role;
import br.com.vitor.challengesea.repositories.user.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

/**
 * Creates the first ADMIN at startup, since only an ADMIN can create another. Runs only while no
 * ADMIN exists and {@code admin.bootstrap.password} is set, so no default password ships.
 */
@Component
public class AdminBootstrap implements ApplicationRunner {

    private static final Logger LOG = LoggerFactory.getLogger(AdminBootstrap.class);

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final String email;
    private final String password;

    public AdminBootstrap(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            @Value("${admin.bootstrap.email:admin@sea.local}") String email,
            @Value("${admin.bootstrap.password:}") String password) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.email = email;
        this.password = password;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (password.isBlank() || userRepository.existsByRole(Role.ADMIN)) {
            return;
        }
        AppUser admin = new AppUser();
        admin.setName("Administrador");
        admin.setEmail(email);
        admin.setPasswordHash(passwordEncoder.encode(password));
        admin.setRole(Role.ADMIN);
        userRepository.save(admin);
        LOG.info("Created the bootstrap ADMIN {}", email);
    }
}
