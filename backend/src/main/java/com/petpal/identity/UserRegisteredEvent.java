package com.petpal.identity;

public record UserRegisteredEvent(Long userId, Role role, String fullName, String city) {
}
