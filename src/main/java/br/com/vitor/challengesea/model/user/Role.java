package br.com.vitor.challengesea.model.user;

import br.com.vitor.challengesea.model.OptionEnum;

public enum Role implements OptionEnum {
    CLIENT("CLIENT", "Cliente", "Cria e acompanha as próprias solicitações"),
    ANALYST("ANALYST", "Analista", "Analisa as solicitações das UFs que cobre"),
    ADMIN("ADMIN", "Administrador", "Acesso total: gerencia usuários e coberturas");

    private final String option;
    private final String label;
    private final String description;

    Role(String option, String label, String description) {
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
