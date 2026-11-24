package com.petpal.common.error;

public abstract sealed class DomainException extends RuntimeException
        permits ResourceNotFoundException, BusinessRuleException, ConflictException,
                ForbiddenOperationException, InvalidCredentialsException {

    private final String code;

    protected DomainException(String code, String message) {
        super(message);
        this.code = code;
    }

    public String getCode() {
        return code;
    }
}
