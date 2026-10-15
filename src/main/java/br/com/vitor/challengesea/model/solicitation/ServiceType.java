package br.com.vitor.challengesea.model.solicitation;

import br.com.vitor.challengesea.model.OptionEnum;

public enum ServiceType implements OptionEnum {
    INSTALLATION("INSTALLATION", "Instalação", "Instalação de um equipamento ou serviço novo"),
    MAINTENANCE("MAINTENANCE", "Manutenção", "Reparo ou manutenção de algo existente"),
    INSPECTION("INSPECTION", "Vistoria", "Inspeção técnica no local");

    private final String option;
    private final String label;
    private final String description;

    ServiceType(String option, String label, String description) {
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
