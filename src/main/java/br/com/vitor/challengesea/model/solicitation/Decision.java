package br.com.vitor.challengesea.model.solicitation;

import br.com.vitor.challengesea.model.OptionEnum;

public enum Decision implements OptionEnum {
    APPROVE("APPROVE", "Aprovar", "A solicitação segue para atendimento"),
    REJECT("REJECT", "Reprovar", "A solicitação é encerrada sem atendimento");

    private final String option;
    private final String label;
    private final String description;

    Decision(String option, String label, String description) {
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
