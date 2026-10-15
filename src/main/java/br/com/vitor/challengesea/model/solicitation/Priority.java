package br.com.vitor.challengesea.model.solicitation;

import br.com.vitor.challengesea.model.OptionEnum;

public enum Priority implements OptionEnum {
    LOW("LOW", "Baixa", "Sem urgência"),
    MEDIUM("MEDIUM", "Média", "Atendimento no prazo normal"),
    HIGH("HIGH", "Alta", "Urgente; exige valor estimado de no mínimo 100");

    private final String option;
    private final String label;
    private final String description;

    Priority(String option, String label, String description) {
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
