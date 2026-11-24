package com.petpal.identity;

public record UserResponse(
        Long id, String email, String firstName, String lastName, String phone, String city, Role role) {

    public static UserResponse from(User user) {
        return new UserResponse(user.getId(), user.getEmail(), user.getFirstName(), user.getLastName(),
                user.getPhone(), user.getCity(), user.getRole());
    }
}
