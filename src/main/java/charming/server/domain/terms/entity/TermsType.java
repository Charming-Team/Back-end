package charming.server.domain.terms.entity;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "약관 종류")
public enum TermsType {
    @Schema(description = "이용약관")
    TERMS_OF_SERVICE,

    @Schema(description = "개인정보 처리방침")
    PRIVACY_POLICY,

    @Schema(description = "보안 서약")
    SECURITY_PLEDGE
}
