package com.petpal.common.error;

public final class ConflictException extends DomainException {

    public ConflictException(String code, String message) {
        super(code, message);
    }
}
