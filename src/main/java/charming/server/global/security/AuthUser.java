package charming.server.global.security;

import charming.server.domain.user.entity.Role;

public record AuthUser(
        Long id,
        String email,
        Role role
) {
}
