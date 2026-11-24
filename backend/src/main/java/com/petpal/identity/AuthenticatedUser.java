package com.petpal.identity;

public record AuthenticatedUser(Long id, Role role) {

    public boolean is(Role expected) {
        return role == expected;
    }
}
