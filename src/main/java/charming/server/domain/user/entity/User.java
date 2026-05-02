package charming.server.domain.user.entity;

import charming.server.global.common.BaseEntity;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.validation.constraints.Email;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Table(name = "users")
@Schema(description = "서비스 사용자 엔티티")
public class User extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Schema(description = "사용자 ID", example = "1")
    private Long id;

    @Column(nullable = false, length = 50)
    @Schema(description = "사용자 이름", example = "김길동")
    private String name;

    @Column(nullable = false, unique = true, length = 50)
    @Schema(description = "로그인 아이디", example = "operator01")
    private String username;

    @Email
    @Column(nullable = false, unique = true, length = 100)
    @Schema(description = "이메일", example = "operator01@example.com")
    private String email;

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    @Schema(description = "성별", example = "MALE")
    private Gender gender;

    @Column(nullable = false)
    @Schema(description = "암호화된 비밀번호", example = "$2a$10$encryptedPassword")
    private String password;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    @Schema(description = "사용자 권한", example = "OPERATOR")
    private Role role;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Schema(description = "계정 상태", example = "ACTIVE")
    private UserStatus status = UserStatus.ACTIVE;

    @Builder.Default
    @Column(nullable = false)
    @Schema(description = "로그인 실패 횟수", example = "0")
    private int loginFailCount = 0;

    @Column
    @Schema(description = "로그인 n번 실패시 잠금 시간", example = "2026-04-30T09:30:00")
    private LocalDateTime lockedUntil;

    @Column
    @Schema(description = "마지막 로그인 시간", example = "2026-04-30T08:40:00")
    private LocalDateTime lastLoginAt;

    @Column(length = 100)
    @Schema(description = "소속 부서", example = "생산관리팀")
    private String department;


    public void recordLoginSuccess(LocalDateTime loginAt) {
        this.loginFailCount = 0;
        this.lockedUntil = null;
        this.lastLoginAt = loginAt;
    }

    public void increaseLoginFailCount() {
        this.loginFailCount++;
    }

    public void lockUntil(LocalDateTime lockedUntil) {
        this.lockedUntil = lockedUntil;
    }

    public void activate() {
        this.status = UserStatus.ACTIVE;
    }

    public void suspend() {
        this.status = UserStatus.SUSPENDED;
    }

    public void ban() {
        this.status = UserStatus.BANNED;
    }

    public void withdraw() {
        this.status = UserStatus.WITHDRAWN;
    }

    public boolean isLocked(LocalDateTime now) {
        return lockedUntil != null && lockedUntil.isAfter(now);
    }

    public boolean isActive() {
        return status == UserStatus.ACTIVE;
    }
}
