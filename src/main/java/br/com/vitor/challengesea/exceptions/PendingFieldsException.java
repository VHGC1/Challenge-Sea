package br.com.vitor.challengesea.exceptions;

import br.com.vitor.challengesea.model.solicitation.PendingFieldDTO;
import java.util.List;

/** Answered with 422 and every field that keeps the solicitation from being submitted. */
public class PendingFieldsException extends RuntimeException {

    private final transient List<PendingFieldDTO> pendingFields;

    public PendingFieldsException(List<PendingFieldDTO> pendingFields) {
        super("A solicitação tem campos ou etapas pendentes.");
        this.pendingFields = List.copyOf(pendingFields);
    }

    public List<PendingFieldDTO> getPendingFields() {
        return pendingFields;
    }
}
