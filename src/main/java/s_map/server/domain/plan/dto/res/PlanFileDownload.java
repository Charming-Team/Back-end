package s_map.server.domain.plan.dto.res;

public record PlanFileDownload(
        String fileName,
        String contentType,
        byte[] content
) {
}
