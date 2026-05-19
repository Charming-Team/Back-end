package s_map.server.domain.chat.service;

import java.util.List;
import s_map.server.domain.chat.dto.req.EvidenceLookupRequest;
import s_map.server.domain.chat.dto.res.EvidenceItemResponse;

public interface EvidenceProvider {

    /**
     * 기능: 이 Provider가 처리하는 질문 의도 값을 반환한다.
     *
     * Output:
     * - result / String / 질문 의도. 예: MATERIAL_SHORTAGE
     */
    String intent();

    /**
     * 기능: 요청 사용자와 필터 기준에 맞는 RDB Evidence 목록을 조회한다.
     *
     * Input:
     * - request / EvidenceLookupRequest / 사용자, 질문, 필터를 포함한 Evidence 요청
     *
     * Output:
     * - result / List<EvidenceItemResponse> / 답변 생성에 사용할 근거 목록
     */
    List<EvidenceItemResponse> getEvidence(EvidenceLookupRequest request);
}
