package com.petpal.common.error;

public final class ForbiddenOperationException extends DomainException {

    public ForbiddenOperationException(String code, String message) {
        super(code, message);
    }
}
