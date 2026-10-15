package br.com.vitor.challengesea.audit;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Records a successful call of the annotated bean method in {@code audit_logs}: the action, the
 * authenticated user and, when the method returns an {@link Identifiable}, its id.
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface Audit {
    String action();
}
