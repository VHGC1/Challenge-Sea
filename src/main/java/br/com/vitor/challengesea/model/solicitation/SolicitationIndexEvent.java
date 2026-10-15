package br.com.vitor.challengesea.model.solicitation;

/** Published on every write to a solicitation; indexed once the transaction commits. */
public record SolicitationIndexEvent(SolicitationDocument document) {}
