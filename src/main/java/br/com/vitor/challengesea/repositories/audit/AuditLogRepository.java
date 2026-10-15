package br.com.vitor.challengesea.repositories.audit;

import br.com.vitor.challengesea.model.audit.AuditLog;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AuditLogRepository extends JpaRepository<AuditLog, Long> {}
