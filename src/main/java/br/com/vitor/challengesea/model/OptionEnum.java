package br.com.vitor.challengesea.model;

/**
 * An enum the frontend lists through {@code /options}: {@code option} is the value the API
 * accepts and returns, {@code label} and {@code description} are the pt-BR text to show.
 */
public interface OptionEnum {
    String option();

    String label();

    String description();
}
