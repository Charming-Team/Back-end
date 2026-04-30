package charming.server.domain.terms.entity;

import charming.server.global.common.BaseEntity;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Lob;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "terms")
@Schema(description = "약관 엔티티")
public class Terms extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Schema(description = "약관 ID", example = "1")
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    @Schema(description = "약관 종류", example = "TERMS_OF_SERVICE")
    private TermsType type;

    @Lob
    @Column(nullable = false, columnDefinition = "TEXT")
    @Schema(description = "약관 내용")
    private String content;

    @Column(nullable = false, length = 20)
    @Schema(description = "약관 버전", example = "1.0")
    private String version;

    @Builder
    public Terms(TermsType type, String content, String version) {
        this.type = type;
        this.content = content;
        this.version = version;
    }

    public void updateContent(String content, String version) {
        this.content = content;
        this.version = version;
    }
}
