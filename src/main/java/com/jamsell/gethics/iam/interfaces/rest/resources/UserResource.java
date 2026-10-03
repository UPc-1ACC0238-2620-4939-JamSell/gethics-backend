package com.jamsell.gethics.iam.interfaces.rest.resources;

public record UserResource(Long id, String name, String email, String role, String phone, String photoUrl) {
}
