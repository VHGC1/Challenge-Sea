package br.com.vitor.challengesea.exceptions;

/** Answered with 409 when the resource's state forbids the action (wrong status, duplicate email). */
public class ConflictException extends RuntimeException {

    public ConflictException(String message) {
        super(message);
    }

    public ConflictException(String message, Throwable cause) {
        super(message, cause);
    }
}
