package br.com.vitor.challengesea.exceptions;

/** Answered with 404 for a missing resource, or one the caller may not know exists. */
public class ResourceNotFoundException extends RuntimeException {

    public ResourceNotFoundException(String message) {
        super(message);
    }

    public ResourceNotFoundException(String message, Throwable cause) {
        super(message, cause);
    }
}
