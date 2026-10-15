package br.com.vitor.challengesea.model.solicitation;

import br.com.vitor.challengesea.model.OptionEnum;

/**
 * The three steps a client fills in before submitting. Each step's completeness rules are the
 * Bean Validation constraints on {@link Solicitation} in that step's group, so the step check and
 * the submit check read the same rules.
 */
public enum SolicitationStep implements OptionEnum {
    BASIC_DATA("BASIC_DATA", "Dados básicos", "Tipo de serviço, título e descrição", 1, BasicData.class),
    ADDRESS("ADDRESS", "Endereço", "CEP, número e complemento", 2, Address.class),
    CONFIRMATION(
            "CONFIRMATION",
            "Confirmação",
            "Prioridade, data preferida, valor estimado e aceite dos termos",
            3,
            Confirmation.class);

    private final String option;
    private final String label;
    private final String description;
    private final int number;
    private final Class<?> group;

    SolicitationStep(String option, String label, String description, int number, Class<?> group) {
        this.option = option;
        this.label = label;
        this.description = description;
        this.number = number;
        this.group = group;
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

    public int number() {
        return number;
    }

    public Class<?> group() {
        return group;
    }

    public interface BasicData {}

    public interface Address {}

    public interface Confirmation {}
}
