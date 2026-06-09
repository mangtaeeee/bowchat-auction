package com.example.userservice.auth.dto;

import java.util.List;
import java.util.Map;

public record KeycloakUserRepresentation(
        String id,
        String username,
        String email,
        Boolean enabled,
        Boolean emailVerified,
        Map<String, List<String>> attributes
) {
}
