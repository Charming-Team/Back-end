package charming.server;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import charming.server.domain.token.entity.RefreshToken;
import charming.server.domain.token.repository.RefreshTokenRepository;
import charming.server.domain.user.entity.Role;
import charming.server.domain.user.entity.User;
import charming.server.domain.user.entity.UserStatus;
import charming.server.domain.user.repository.UserRepository;
import jakarta.persistence.EntityManager;
import java.time.LocalDateTime;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@ActiveProfiles("test")
class RefreshTokenRotationIntegrationTest {

    @Autowired
    private RefreshTokenRepository refreshTokenRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private EntityManager entityManager;

    @BeforeEach
    void setUp() {
        refreshTokenRepository.deleteAllInBatch();
        userRepository.deleteAllInBatch();
    }

    @Test
    @Transactional
    @DisplayName("활성 Refresh Token 폐기는 같은 토큰에 대해 한 번만 성공한다")
    void revokeIfActiveSucceedsOnlyOnce() {
        User user = saveUser();
        LocalDateTime now = LocalDateTime.now();
        refreshTokenRepository.save(refreshToken(user, "active-token", now.plusDays(1)));

        int firstRevokedCount = refreshTokenRepository.revokeIfActive("active-token", now);
        int secondRevokedCount = refreshTokenRepository.revokeIfActive("active-token", now);

        entityManager.flush();
        entityManager.clear();

        RefreshToken refreshToken = refreshTokenRepository.findByTokenHash("active-token").orElseThrow();
        assertEquals(1, firstRevokedCount);
        assertEquals(0, secondRevokedCount);
        assertTrue(refreshToken.isRevoked());
    }

    private User saveUser() {
        User user = User.builder()
                .name("OPERATOR 사용자")
                .email("operator@example.com")
                .password("encoded-password")
                .role(Role.OPERATOR)
                .department("생산관리팀")
                .companyName("Charming")
                .phoneNumber("010-0000-0000")
                .status(UserStatus.ACTIVE)
                .build();
        return userRepository.save(user);
    }

    private RefreshToken refreshToken(User user, String tokenHash, LocalDateTime expiresAt) {
        return RefreshToken.builder()
                .user(user)
                .tokenHash(tokenHash)
                .expiresAt(expiresAt)
                .build();
    }
}
