package br.com.vitor.challengesea.audit;

import br.com.vitor.challengesea.model.audit.AuditLog;
import br.com.vitor.challengesea.repositories.audit.AuditLogRepository;
import io.micrometer.core.instrument.MeterRegistry;
import org.aspectj.lang.annotation.AfterReturning;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;

/**
 * Writes one {@link AuditLog} row per successful {@link Audit} call and counts it in the
 * {@code audit.actions} metric, tagged by action.
 * <p>
 * Only successes are recorded: advice that runs after returning never sees a call that threw.
 * The aspect has no {@code @Order}, so whether it runs inside or outside the method's
 * {@code @Transactional} is not fixed; either way a failed action leaves no audit row.
 */
@Aspect
@Component
public class AuditAspect {

    private final AuditLogRepository auditLogRepository;
    private final MeterRegistry meterRegistry;

    public AuditAspect(AuditLogRepository auditLogRepository, MeterRegistry meterRegistry) {
        this.auditLogRepository = auditLogRepository;
        this.meterRegistry = meterRegistry;
    }

    @AfterReturning(pointcut = "@annotation(audit)", returning = "result")
    public void record(Audit audit, Object result) {
        AuditLog log = new AuditLog();
        log.setAction(audit.action());
        log.setActorId(currentUserId());
        if (result instanceof Identifiable identifiable) {
            log.setResourceId(identifiable.id());
        }
        auditLogRepository.save(log);
        meterRegistry.counter("audit.actions", "action", audit.action()).increment();
    }

    private static Long currentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof Jwt jwt) {
            return Long.valueOf(jwt.getSubject());
        }
        return null;
    }
}
