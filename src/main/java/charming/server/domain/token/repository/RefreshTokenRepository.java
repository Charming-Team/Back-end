package charming.server.domain.token.repository;

import charming.server.domain.token.entity.RefreshToken;
import java.time.LocalDateTime;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long> {

    /**
     * Refresh Token 해시로 저장된 토큰 이력을 조회한다.
     */
    Optional<RefreshToken> findByTokenHash(String tokenHash);

    /**
     * 아직 폐기되지 않았고 만료되지 않은 Refresh Token만 원자적으로 폐기한다.
     * 같은 토큰으로 동시 재발급 요청이 들어와도 update count가 1인 요청만 성공한다.
     */
    @Modifying(flushAutomatically = true)
    @Query("""
            update RefreshToken refreshToken
            set refreshToken.revoked = true,
                refreshToken.updatedAt = :now
            where refreshToken.tokenHash = :tokenHash
              and refreshToken.revoked = false
              and refreshToken.expiresAt > :now
            """)
    int revokeIfActive(
            @Param("tokenHash") String tokenHash,
            @Param("now") LocalDateTime now
    );

    /**
     * 보관 기준 시각보다 먼저 만료된 Refresh Token 이력을 삭제한다.
     * 주기적인 정리 배치에서 테이블 누적을 줄이기 위해 사용한다.
     */
    @Modifying
    @Query("delete from RefreshToken refreshToken where refreshToken.expiresAt < :cutoff")
    int deleteExpiredBefore(@Param("cutoff") LocalDateTime cutoff);
}
