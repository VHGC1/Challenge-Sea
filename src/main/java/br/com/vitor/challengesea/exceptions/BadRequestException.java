package br.com.vitor.challengesea.exceptions;

/** Answered with 400 for client input that passes Bean Validation but breaks a business rule. */
public class BadRequestException extends RuntimeException {

    public BadRequestException(String message) {
        super(message);
    }

    public BadRequestException(String message, Throwable cause) {
        super(message, cause);
    }
}
