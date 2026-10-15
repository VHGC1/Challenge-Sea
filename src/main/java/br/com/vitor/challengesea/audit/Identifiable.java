package br.com.vitor.challengesea.audit;

/** A result whose id {@link AuditAspect} records as the audited resource. */
public interface Identifiable {
    Long id();
}
