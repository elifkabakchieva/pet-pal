package com.petpal.identity;

public enum AccountType {
    OWNER,
    PROVIDER;

    Role toRole() {
        return switch (this) {
            case OWNER -> Role.OWNER;
            case PROVIDER -> Role.PROVIDER;
        };
    }
}
