package s_map.server.domain.chat.service;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import s_map.server.domain.chat.dto.req.EvidenceLookupRequest;
import s_map.server.domain.chat.dto.res.EvidenceItemResponse;
import s_map.server.domain.chat.dto.res.EvidenceLookupResponse;

@Service
@Slf4j
@Transactional(readOnly = true)
public class ChatEvidenceService {

    private final Map<String, EvidenceProvider> evidenceProviders;

    public ChatEvidenceService(List<EvidenceProvider> evidenceProviders) {
        this.evidenceProviders = evidenceProviders.stream()
                .collect(Collectors.toUnmodifiableMap(
                        provider -> normalizeIntent(provider.intent()),
                        Function.identity()
                ));
    }

    /**
     * 기능: FastAPI 챗봇이 요청한 질문 의도에 맞는 RDB Evidence를 조회한다.
     *
     * Input:
     * - request / EvidenceLookupRequest / 챗봇 Evidence 조회 요청 값
     * - request.intent / String / 질문 의도. 예: MATERIAL_SHORTAGE
     * - request.question / String / 사용자 원문 질문
     * - request.user / EvidenceLookupUser / 사용자 ID, Role, 회사명 메타데이터
     * - request.filters / EvidenceLookupFilters / 질문에서 추출한 조회 힌트
     *
     * Output:
     * - result / EvidenceLookupResponse / FastAPI EvidenceResult와 호환되는 RDB 근거 응답
     * - result.intent / String / 조회에 사용한 질문 의도
     * - result.basisTime / OffsetDateTime / RDB 근거 조회 기준 시각
     * - result.items / List<EvidenceItemResponse> / 답변 생성에 사용할 근거 목록
     */
    public EvidenceLookupResponse lookup(EvidenceLookupRequest request) {
        String intent = normalizeIntent(request.intent());
        EvidenceProvider evidenceProvider = evidenceProviders.get(intent);
        log.info(
                "[ChatEvidenceService] Evidence 조회 요청 intent={}, sessionId={}, messageId={}, userId={}, role={}",
                intent,
                request.sessionId(),
                request.messageId(),
                request.user().userId(),
                request.user().role()
        );

        List<EvidenceItemResponse> items = evidenceProvider == null
                ? handleUnsupportedIntent(intent, request)
                : evidenceProvider.getEvidence(request);

        log.info(
                "[ChatEvidenceService] Evidence 조회 완료 intent={}, sessionId={}, messageId={}, itemCount={}",
                intent,
                request.sessionId(),
                request.messageId(),
                items.size()
        );
        return new EvidenceLookupResponse(intent, OffsetDateTime.now(), items);
    }

    private List<EvidenceItemResponse> handleUnsupportedIntent(
            String intent,
            EvidenceLookupRequest request
    ) {
        log.warn(
                "[ChatEvidenceService] Evidence 조회 스킵 reason=unsupported_intent intent={}, sessionId={}, messageId={}, userId={}",
                intent,
                request.sessionId(),
                request.messageId(),
                request.user().userId()
        );
        return List.of();
    }

    private String normalizeIntent(String intent) {
        return intent.trim().toUpperCase(Locale.ROOT);
    }
}
