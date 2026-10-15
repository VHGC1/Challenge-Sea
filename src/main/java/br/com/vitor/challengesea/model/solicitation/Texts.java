package br.com.vitor.challengesea.model.solicitation;

final class Texts {
    private Texts() {}

    static String strip(String value) {
        return value == null ? null : value.strip();
    }
}
