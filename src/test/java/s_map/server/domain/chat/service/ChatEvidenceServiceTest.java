package s_map.server.domain.chat.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import s_map.server.domain.chat.dto.req.EvidenceLookupFilters;
import s_map.server.domain.chat.dto.req.ChatEvidenceLookupRequest;
import s_map.server.domain.chat.dto.req.EvidenceLookupUser;
import s_map.server.domain.chat.dto.res.ChatEvidenceItemResponse;
import s_map.server.domain.chat.dto.res.ChatEvidenceLookupResponse;

class ChatEvidenceServiceTest {

    @Test
    @DisplayName("질문 의도는 대소문자와 공백을 정규화한 뒤 Provider로 라우팅한다")
    void lookupRoutesToProviderWithNormalizedIntent() {
        ChatEvidenceItemResponse evidenceItem = createEvidenceItem();
        ChatEvidenceService chatEvidenceService = new ChatEvidenceService(List.of(
                new StubEvidenceProvider("material_shortage", List.of(evidenceItem))
        ));

        ChatEvidenceLookupResponse response = chatEvidenceService.lookup(createRequest(" MATERIAL_SHORTAGE "));

        assertEquals("MATERIAL_SHORTAGE", response.intent());
        assertNotNull(response.basisTime());
        assertEquals(1, response.items().size());
        assertSame(evidenceItem, response.items().get(0));
    }

    @Test
    @DisplayName("지원하지 않는 질문 의도는 빈 Evidence 목록을 반환한다")
    void lookupReturnsEmptyItemsForUnsupportedIntent() {
        ChatEvidenceService chatEvidenceService = new ChatEvidenceService(List.of(
                new StubEvidenceProvider("MATERIAL_SHORTAGE", List.of(createEvidenceItem()))
        ));

        ChatEvidenceLookupResponse response = chatEvidenceService.lookup(createRequest("delivery_risk"));

        assertEquals("DELIVERY_RISK", response.intent());
        assertNotNull(response.basisTime());
        assertTrue(response.items().isEmpty());
    }

    private ChatEvidenceLookupRequest createRequest(String intent) {
        return new ChatEvidenceLookupRequest(
                10L,
                24L,
                intent,
                "자재 부족 현황 알려줘",
                new EvidenceLookupUser(1L, "MANUFACTURING_MANAGER", "S-MAP"),
                new EvidenceLookupFilters(5, null, null, null, null)
        );
    }

    private ChatEvidenceItemResponse createEvidenceItem() {
        return new ChatEvidenceItemResponse(
                "MATERIAL",
                "RM-AL-001 알루미늄 원자재 재고 부족",
                "생산계획 1001에서 RM-AL-001 알루미늄 원자재 부족 상태입니다.",
                "/materials/inventory/1?mode=read",
                "production_plan_materials",
                1L,
                Map.of("materialCode", "RM-AL-001"),
                List.of("OPERATOR", "EXECUTIVE", "MANUFACTURING_MANAGER")
        );
    }

    private record StubEvidenceProvider(
            String intent,
            List<ChatEvidenceItemResponse> items
    ) implements EvidenceProvider {

        @Override
        public List<ChatEvidenceItemResponse> getEvidence(ChatEvidenceLookupRequest request) {
            return items;
        }
    }
}
