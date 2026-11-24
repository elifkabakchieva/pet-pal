package com.petpal.common.error;

public final class InvalidCredentialsException extends DomainException {

    public InvalidCredentialsException() {
        super("INVALID_CREDENTIALS", "E-mail or password is not correct.");
    }
}
