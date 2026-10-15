package br.com.vitor.challengesea.model.solicitation;

import br.com.vitor.challengesea.model.OptionEnum;

public enum SolicitationStatus implements OptionEnum {
    DRAFT("DRAFT", "Rascunho", "Em preenchimento pelo cliente"),
    SUBMITTED("SUBMITTED", "Enviada", "Aguardando análise"),
    IN_REVIEW("IN_REVIEW", "Em análise", "Um analista está avaliando a solicitação"),
    APPROVED("APPROVED", "Aprovada", "Aprovada pelo analista"),
    REJECTED("REJECTED", "Reprovada", "Reprovada pelo analista");

    private final String option;
    private final String label;
    private final String description;

    SolicitationStatus(String option, String label, String description) {
        this.option = option;
        this.label = label;
        this.description = description;
    }

    @Override
    public String option() {
        return option;
    }

    @Override
    public String label() {
        return label;
    }

    @Override
    public String description() {
        return description;
    }
}
