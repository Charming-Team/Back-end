package s_map.server.global.security;

import s_map.server.domain.user.entity.Role;

public record AuthUser(
        Long id,
        String email,
        Role role
) {
}
