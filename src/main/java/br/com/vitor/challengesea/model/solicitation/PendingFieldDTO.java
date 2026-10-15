package br.com.vitor.challengesea.model.solicitation;

/** A field that keeps its step from being complete, and why. */
public record PendingFieldDTO(SolicitationStep step, String field, String message) {}
