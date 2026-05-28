package s_map.server.domain.user.entity;

import s_map.server.global.common.BaseEntity;
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
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.ColumnTransformer;

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

    @Email
    @Column(nullable = false, unique = true, length = 100)
    @Schema(description = "이메일", example = "operator01@sk.com")
    private String email;

    @Column(nullable = false)
    @Schema(description = "암호화된 비밀번호", example = "$2a$10$encryptedPassword")
    private String password;

    @Enumerated(EnumType.STRING)
    @ColumnTransformer(write = "?::role_enum")
    @Column(nullable = false, columnDefinition = "role_enum")
    @Schema(description = "사용자 권한", example = "OPERATOR")
    private Role role;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @ColumnTransformer(write = "?::user_status_enum")
    @Column(nullable = false, columnDefinition = "user_status_enum")
    @Schema(description = "계정 상태", example = "ACTIVE")
    private UserStatus status = UserStatus.ACTIVE;

    @Column(length = 100)
    @Schema(description = "소속 부서", example = "생산관리팀")
    private String department;

    @Column(length = 100, nullable = false)
    @Schema(description = "회사명", example = "s_map")
    private String companyName;

    @Column(length = 20, nullable = false)
    @Schema(description = "연락처", example = "010-1234-5678")
    private String phoneNumber;


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

    public boolean isActive() {
        return status == UserStatus.ACTIVE;
    }
}
