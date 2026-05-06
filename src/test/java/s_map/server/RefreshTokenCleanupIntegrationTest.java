package s_map.server;

import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import s_map.server.domain.token.entity.RefreshToken;
import s_map.server.domain.token.repository.RefreshTokenRepository;
import s_map.server.domain.token.service.RefreshTokenService;
import s_map.server.domain.user.entity.Role;
import s_map.server.domain.user.entity.User;
import s_map.server.domain.user.entity.UserStatus;
import s_map.server.domain.user.repository.UserRepository;

@SpringBootTest
@ActiveProfiles("test")
class RefreshTokenCleanupIntegrationTest {

    @Autowired
    private RefreshTokenService refreshTokenService;

    @Autowired
    private RefreshTokenRepository refreshTokenRepository;

    @Autowired
    private UserRepository userRepository;

    @BeforeEach
    void setUp() {
        refreshTokenRepository.deleteAll();
        userRepository.deleteAll();
    }

    @Test
    @DisplayName("보관 기간이 지난 만료 Refresh Token은 revoked 여부와 상관없이 삭제한다")
    void deletesExpiredRefreshTokensBeforeCutoff() {
        User user = saveUser();
        LocalDateTime cutoff = LocalDateTime.now().minusDays(7);

        refreshTokenRepository.saveAll(List.of(
                refreshToken(user, "old-revoked-token", cutoff.minusSeconds(1), true),
                refreshToken(user, "old-active-token", cutoff.minusSeconds(1), false),
                refreshToken(user, "recent-expired-token", cutoff.plusSeconds(1), true),
                refreshToken(user, "future-token", LocalDateTime.now().plusDays(1), false)
        ));

        int deletedCount = refreshTokenService.deleteExpiredBefore(cutoff);

        List<String> remainingTokenHashes = refreshTokenRepository.findAll()
                .stream()
                .map(RefreshToken::getTokenHash)
                .toList();

        Assertions.assertEquals(2, deletedCount);
        Assertions.assertFalse(remainingTokenHashes.contains("old-revoked-token"));
        Assertions.assertFalse(remainingTokenHashes.contains("old-active-token"));
        Assertions.assertTrue(remainingTokenHashes.contains("recent-expired-token"));
        Assertions.assertTrue(remainingTokenHashes.contains("future-token"));
    }

    private User saveUser() {
        User user = User.builder()
                .name("OPERATOR 사용자")
                .email("operator@example.com")
                .password("encoded-password")
                .role(Role.OPERATOR)
                .department("생산관리팀")
                .companyName("s_map")
                .phoneNumber("010-0000-0000")
                .status(UserStatus.ACTIVE)
                .build();
        return userRepository.save(user);
    }

    private RefreshToken refreshToken(User user, String tokenHash, LocalDateTime expiresAt, boolean revoked) {
        return RefreshToken.builder()
                .user(user)
                .tokenHash(tokenHash)
                .expiresAt(expiresAt)
                .revoked(revoked)
                .build();
    }
}
