package br.com.vitor.challengesea.repositories.solicitation;

import br.com.vitor.challengesea.model.solicitation.Solicitation;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SolicitationRepository extends JpaRepository<Solicitation, Long> {
    Optional<Solicitation> findByIdAndOwnerId(Long id, Long ownerId);

    Page<Solicitation> findByOwnerId(Long ownerId, Pageable pageable);
}
