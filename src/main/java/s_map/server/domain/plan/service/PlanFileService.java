package s_map.server.domain.plan.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import s_map.server.domain.order.entity.CustomerOrder;
import s_map.server.domain.order.entity.PlanStatus;
import s_map.server.domain.order.entity.ProductionPlan;
import s_map.server.domain.order.repository.CustomerOrderRepository;
import s_map.server.domain.order.repository.LineCapabilityProjection;
import s_map.server.domain.order.repository.ProductionPlanRepository;
import s_map.server.domain.plan.dto.req.PlanFileApplyMode;
import s_map.server.domain.plan.dto.res.PlanFileApplyResponse;
import s_map.server.domain.plan.dto.res.PlanFileDownload;
import s_map.server.domain.plan.dto.res.PlanFileValidationErrorResponse;
import s_map.server.domain.plan.dto.res.PlanFileValidationResponse;
import s_map.server.domain.plan.repository.PlanQueryRepository;
import s_map.server.domain.plan.repository.PlanRow;
import s_map.server.domain.user.entity.Role;
import s_map.server.domain.user.entity.UserStatus;
import s_map.server.domain.user.repository.UserRepository;
import s_map.server.global.error.CustomException;
import s_map.server.global.error.ErrorCode;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.TreeMap;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipOutputStream;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PlanFileService {

    private static final ZoneId DEFAULT_PRODUCTION_ZONE = ZoneId.of("Asia/Seoul");
    private static final List<PlanStatus> OPERATING_STATUSES = List.of(
            PlanStatus.SCHEDULED,
            PlanStatus.IN_PROGRESS,
            PlanStatus.DELAYED
    );
    private static final List<String> EXPORT_HEADERS = List.of(
            "plan_id",
            "order_id",
            "product_id",
            "product_code",
            "product_name",
            "line_id",
            "line_code",
            "line_name",
            "operator_id",
            "operator_name",
            "planned_start_at",
            "planned_end_at",
            "estimated_duration_hr",
            "planned_quantity",
            "plan_sequence",
            "plan_status"
    );
    private static final Set<String> REQUIRED_UPLOAD_HEADERS = Set.of(
            "order_id",
            "product_id",
            "line_id",
            "planned_start_at",
            "planned_end_at",
            "planned_quantity",
            "plan_sequence"
    );
    private static final List<String> IGNORED_UPLOAD_COLUMNS = List.of(
            "plan_id",
            "product_code",
            "product_name",
            "line_code",
            "line_name",
            "operator_name",
            "estimated_duration_hr",
            "plan_status"
    );

    private final PlanQueryRepository planQueryRepository;
    private final ProductionPlanRepository productionPlanRepository;
    private final CustomerOrderRepository customerOrderRepository;
    private final UserRepository userRepository;

    /**
     * [기능]
     * 현재 생산계획 목록을 CSV 또는 XLSX 파일로 내보낸다.
     *
     * [Input]
     * - format: csv 또는 xlsx
     *
     * [Process]
     * - production_plans, products, production_lines, users를 조인 조회한다.
     * - CSV 요청이면 UTF-8 BOM이 포함된 CSV 바이트를 생성한다.
     * - XLSX 요청이면 단일 시트 워크북 바이트를 생성한다.
     *
     * [Output]
     * - PlanFileDownload
     * - 다운로드 파일명, Content-Type, 파일 바이트를 반환한다.
     */
    public PlanFileDownload exportPlans(String format) {
        String normalizedFormat = normalizeFormat(format);
        List<List<String>> table = createExportTable(planQueryRepository.findPlans(null, null, null, null));

        if ("csv".equals(normalizedFormat)) {
            return new PlanFileDownload(
                    "production-plans.csv",
                    "text/csv; charset=UTF-8",
                    writeCsv(table)
            );
        }

        if ("xlsx".equals(normalizedFormat)) {
            return new PlanFileDownload(
                    "production-plans.xlsx",
                    "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                    writeXlsx(table)
            );
        }

        throw new CustomException(ErrorCode.INVALID_PLAN_FILE);
    }

    /**
     * [기능]
     * 업로드한 생산계획 파일을 검증한다.
     *
     * [Input]
     * - file: CSV 또는 XLSX 파일
     * - mode: 최초 등록 또는 전체 교체
     *
     * [Process]
     * - 파일 형식과 필수 컬럼을 검증한다.
     * - 주문, 제품, 라인, 담당자, 기간, 수량, 라인 내 순서를 검증한다.
     * - 파일 내부의 라인 내 순서 중복과 일정 충돌을 검증한다.
     * - 예상 소요 시간과 상태는 파일 값을 반영하지 않고 시스템 기준으로 계산/설정한다.
     *
     * [Output]
     * - PlanFileValidationResponse
     * - 정상 행, 오류 행, 반영 제외 행, 오류 상세 목록을 반환한다.
     */
    public PlanFileValidationResponse validatePlanFile(MultipartFile file, PlanFileApplyMode mode) {
        return validate(file, mode).response();
    }

    /**
     * [기능]
     * 업로드한 생산계획 파일을 검증 후 production_plans에 반영한다.
     *
     * [Input]
     * - file: CSV 또는 XLSX 파일
     * - mode: 최초 등록 또는 전체 교체
     *
     * [Process]
     * - 파일 검증 결과 오류가 있으면 DB에 반영하지 않는다.
     * - 최초 등록은 현재 운영 계획이 없을 때만 신규 생산계획을 저장한다.
     * - 전체 교체는 기존 운영 계획을 CANCELLED로 변경한 뒤 업로드 파일 기준 신규 생산계획을 저장한다.
     * - 예상 소요 시간은 product_line_capabilities 기준으로 계산한다.
     * - 계획 상태는 사용자 입력값을 무시하고 SCHEDULED로 등록한다.
     *
     * [Output]
     * - PlanFileApplyResponse
     * - 반영 여부, 반영 행 수, 검증 요약을 반환한다.
     */
    @Transactional
    public PlanFileApplyResponse applyPlanFile(MultipartFile file, PlanFileApplyMode mode) {
        ValidationResult validation = validate(file, mode);
        if (!validation.response().isCanApply()) {
            return PlanFileApplyResponse.notApplied(mode, validation.response());
        }

        if (mode == PlanFileApplyMode.FULL_REPLACE) {
            productionPlanRepository.findByPlanStatusIn(OPERATING_STATUSES)
                    .forEach(ProductionPlan::cancel);
        }

        List<ProductionPlan> plans = validation.rows()
                .stream()
                .map(this::toProductionPlan)
                .toList();

        productionPlanRepository.saveAll(plans);

        return PlanFileApplyResponse.applied(mode, validation.response(), plans.size());
    }

    private ValidationResult validate(MultipartFile file, PlanFileApplyMode mode) {
        PlanFileApplyMode resolvedMode = mode == null ? PlanFileApplyMode.FULL_REPLACE : mode;
        List<ParsedRow> parsedRows = parse(file);
        List<PlanFileValidationErrorResponse> errors = new ArrayList<>();
        Map<Integer, Integer> rowErrorCounts = new HashMap<>();

        validateApplyMode(resolvedMode, errors);

        List<ValidatedRow> candidates = new ArrayList<>();
        for (ParsedRow parsedRow : parsedRows) {
            RowValidation rowValidation = validateRow(parsedRow);
            if (rowValidation.row().isPresent()) {
                candidates.add(rowValidation.row().get());
            }

            rowValidation.errors().forEach(error -> addError(errors, rowErrorCounts, error));
        }

        validateDuplicateSequences(candidates, errors, rowErrorCounts);
        validateInternalScheduleConflicts(candidates, errors, rowErrorCounts);

        Set<Integer> errorRowNumbers = rowErrorCounts.keySet();
        List<ValidatedRow> validRows = candidates.stream()
                .filter(row -> !errorRowNumbers.contains(row.rowNumber()))
                .toList();

        int totalRows = parsedRows.size();
        int errorRows = errorRowNumbers.size();
        PlanFileValidationResponse response = PlanFileValidationResponse.builder()
                .mode(resolvedMode)
                .canApply(errors.isEmpty())
                .totalRows(totalRows)
                .validRows(totalRows - errorRows)
                .errorRows(errorRows)
                .excludedRows(0)
                .ignoredColumns(IGNORED_UPLOAD_COLUMNS)
                .errors(errors)
                .message("생산계획 파일 검증이 완료되었습니다.")
                .build();

        return new ValidationResult(response, validRows);
    }

    private void validateApplyMode(
            PlanFileApplyMode mode,
            List<PlanFileValidationErrorResponse> errors
    ) {
        long operatingPlanCount = productionPlanRepository.countByPlanStatusIn(OPERATING_STATUSES);

        if (mode == PlanFileApplyMode.INITIAL_REGISTER && operatingPlanCount > 0) {
            errors.add(createError(
                    0,
                    "mode",
                    "최초 등록은 현재 운영 생산계획이 없을 때만 가능합니다."
            ));
        }

        if (mode == PlanFileApplyMode.FULL_REPLACE && operatingPlanCount == 0) {
            errors.add(createError(
                    0,
                    "mode",
                    "전체 교체는 현재 운영 생산계획이 있을 때만 가능합니다."
            ));
        }
    }

    private RowValidation validateRow(ParsedRow parsedRow) {
        List<PlanFileValidationErrorResponse> errors = new ArrayList<>();

        Long orderId = parseLong(parsedRow, "order_id", true, errors);
        Long productId = parseLong(parsedRow, "product_id", true, errors);
        Long lineId = parseLong(parsedRow, "line_id", true, errors);
        Long operatorId = parseLong(parsedRow, "operator_id", false, errors);
        OffsetDateTime plannedStartAt = parseOffsetDateTime(parsedRow, "planned_start_at", true, errors);
        OffsetDateTime plannedEndAt = parseOffsetDateTime(parsedRow, "planned_end_at", true, errors);
        Integer plannedQuantity = parseInteger(parsedRow, "planned_quantity", true, errors);
        Integer planSequence = parseInteger(parsedRow, "plan_sequence", true, errors);

        if (plannedStartAt != null && plannedEndAt != null && !plannedStartAt.isBefore(plannedEndAt)) {
            errors.add(createError(parsedRow.rowNumber(), "planned_end_at", "계획 종료일시는 계획 시작일시보다 이후여야 합니다."));
        }

        if (plannedQuantity != null && plannedQuantity <= 0) {
            errors.add(createError(parsedRow.rowNumber(), "planned_quantity", "계획 수량은 0보다 커야 합니다."));
        }

        if (planSequence != null && planSequence <= 0) {
            errors.add(createError(parsedRow.rowNumber(), "plan_sequence", "라인 내 순서는 1 이상이어야 합니다."));
        }

        CustomerOrder order = null;
        if (orderId != null) {
            order = customerOrderRepository.findById(orderId).orElse(null);
            if (order == null) {
                errors.add(createError(parsedRow.rowNumber(), "order_id", "주문 정보를 찾을 수 없습니다."));
            }
        }

        if (order != null && productId != null && !Objects.equals(order.getProductId(), productId)) {
            errors.add(createError(parsedRow.rowNumber(), "product_id", "주문에 등록된 제품과 업로드 제품이 일치하지 않습니다."));
        }

        if (operatorId != null
                && !userRepository.existsByIdAndStatusAndRole(operatorId, UserStatus.ACTIVE, Role.OPERATOR)) {
            errors.add(createError(parsedRow.rowNumber(), "operator_id", "활성 작업자 정보를 찾을 수 없습니다."));
        }

        BigDecimal estimatedDurationHr = null;
        if (productId != null && lineId != null && plannedQuantity != null && plannedQuantity > 0) {
            Optional<LineCapabilityProjection> capability =
                    productionPlanRepository.findActiveLineCapabilityDetail(lineId, productId);

            if (capability.isEmpty()) {
                errors.add(createError(parsedRow.rowNumber(), "line_id", "해당 제품을 생산할 수 있는 활성 라인이 아닙니다."));
            } else {
                estimatedDurationHr = calculateEstimatedDurationHr(plannedQuantity, capability.get());
            }
        }

        if (!errors.isEmpty()
                || orderId == null
                || productId == null
                || lineId == null
                || plannedStartAt == null
                || plannedEndAt == null
                || plannedQuantity == null
                || planSequence == null
                || estimatedDurationHr == null) {
            return new RowValidation(Optional.empty(), errors);
        }

        return new RowValidation(
                Optional.of(new ValidatedRow(
                        parsedRow.rowNumber(),
                        orderId,
                        productId,
                        lineId,
                        operatorId,
                        plannedStartAt,
                        plannedEndAt,
                        estimatedDurationHr,
                        plannedQuantity,
                        planSequence
                )),
                errors
        );
    }

    private ProductionPlan toProductionPlan(ValidatedRow row) {
        return ProductionPlan.create(
                row.orderId(),
                row.productId(),
                row.lineId(),
                row.operatorId(),
                row.plannedStartAt(),
                row.plannedEndAt(),
                row.estimatedDurationHr(),
                row.plannedQuantity(),
                row.planSequence()
        );
    }

    private void validateDuplicateSequences(
            List<ValidatedRow> rows,
            List<PlanFileValidationErrorResponse> errors,
            Map<Integer, Integer> rowErrorCounts
    ) {
        Map<String, ValidatedRow> firstRows = new HashMap<>();

        for (ValidatedRow row : rows) {
            String key = row.lineId() + ":" + row.planSequence();
            ValidatedRow firstRow = firstRows.putIfAbsent(key, row);
            if (firstRow != null) {
                addError(errors, rowErrorCounts, createError(
                        row.rowNumber(),
                        "plan_sequence",
                        "파일 내 동일 라인에 같은 라인 내 순서가 중복되었습니다."
                ));
            }
        }
    }

    private void validateInternalScheduleConflicts(
            List<ValidatedRow> rows,
            List<PlanFileValidationErrorResponse> errors,
            Map<Integer, Integer> rowErrorCounts
    ) {
        List<ValidatedRow> sortedRows = rows.stream()
                .sorted(Comparator.comparing(ValidatedRow::lineId)
                        .thenComparing(ValidatedRow::plannedStartAt))
                .toList();

        for (int i = 0; i < sortedRows.size(); i++) {
            ValidatedRow current = sortedRows.get(i);
            for (int j = i + 1; j < sortedRows.size(); j++) {
                ValidatedRow next = sortedRows.get(j);
                if (!Objects.equals(current.lineId(), next.lineId())) {
                    break;
                }

                if (!next.plannedStartAt().isBefore(current.plannedEndAt())) {
                    break;
                }

                if (next.plannedEndAt().isAfter(current.plannedStartAt())) {
                    addError(errors, rowErrorCounts, createError(
                            next.rowNumber(),
                            "planned_start_at",
                            "파일 내 동일 라인 일정이 다른 행과 중복됩니다."
                    ));
                }
            }
        }
    }

    private List<ParsedRow> parse(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new CustomException(ErrorCode.INVALID_PLAN_FILE);
        }

        String filename = file.getOriginalFilename();
        String extension = resolveExtension(filename);

        try {
            if ("csv".equals(extension)) {
                return toParsedRows(parseCsv(new String(file.getBytes(), StandardCharsets.UTF_8)));
            }

            if ("xlsx".equals(extension)) {
                return toParsedRows(readXlsx(file.getBytes()));
            }
        } catch (IOException exception) {
            throw new CustomException(ErrorCode.INVALID_PLAN_FILE);
        }

        throw new CustomException(ErrorCode.INVALID_PLAN_FILE);
    }

    private List<ParsedRow> toParsedRows(List<List<String>> table) {
        if (table.isEmpty()) {
            throw new CustomException(ErrorCode.INVALID_PLAN_FILE);
        }

        List<String> headers = table.get(0).stream()
                .map(this::normalizeHeader)
                .toList();

        if (!new HashSet<>(headers).containsAll(REQUIRED_UPLOAD_HEADERS)) {
            throw new CustomException(ErrorCode.INVALID_PLAN_FILE);
        }

        List<ParsedRow> rows = new ArrayList<>();
        for (int rowIndex = 1; rowIndex < table.size(); rowIndex++) {
            List<String> cells = table.get(rowIndex);
            if (isEmptyRow(cells)) {
                continue;
            }

            Map<String, String> values = new LinkedHashMap<>();
            for (int columnIndex = 0; columnIndex < headers.size(); columnIndex++) {
                String header = headers.get(columnIndex);
                if (header.isBlank()) {
                    continue;
                }

                String value = columnIndex < cells.size() ? cells.get(columnIndex).trim() : "";
                values.put(header, value);
            }

            rows.add(new ParsedRow(rowIndex + 1, values));
        }

        return rows;
    }

    private List<List<String>> createExportTable(List<PlanRow> plans) {
        List<List<String>> rows = new ArrayList<>();
        rows.add(EXPORT_HEADERS);

        for (PlanRow plan : plans) {
            rows.add(List.of(
                    toStringValue(plan.planId()),
                    toStringValue(plan.orderId()),
                    toStringValue(plan.productId()),
                    toStringValue(plan.productCode()),
                    toStringValue(plan.productName()),
                    toStringValue(plan.lineId()),
                    toStringValue(plan.lineCode()),
                    toStringValue(plan.lineName()),
                    toStringValue(plan.operatorId()),
                    toStringValue(plan.operatorName()),
                    toStringValue(plan.plannedStartAt()),
                    toStringValue(plan.plannedEndAt()),
                    toStringValue(plan.estimatedDurationHr()),
                    toStringValue(plan.plannedQuantity()),
                    toStringValue(plan.planSequence()),
                    toStringValue(plan.planStatus())
            ));
        }

        return rows;
    }

    private byte[] writeCsv(List<List<String>> table) {
        StringBuilder builder = new StringBuilder("\uFEFF");

        for (List<String> row : table) {
            for (int i = 0; i < row.size(); i++) {
                if (i > 0) {
                    builder.append(',');
                }

                builder.append(escapeCsv(row.get(i)));
            }

            builder.append("\r\n");
        }

        return builder.toString().getBytes(StandardCharsets.UTF_8);
    }

    private List<List<String>> parseCsv(String csv) {
        String normalizedCsv = csv.startsWith("\uFEFF") ? csv.substring(1) : csv;
        List<List<String>> rows = new ArrayList<>();
        List<String> row = new ArrayList<>();
        StringBuilder cell = new StringBuilder();
        boolean inQuotes = false;

        for (int i = 0; i < normalizedCsv.length(); i++) {
            char current = normalizedCsv.charAt(i);

            if (current == '"') {
                if (inQuotes && i + 1 < normalizedCsv.length() && normalizedCsv.charAt(i + 1) == '"') {
                    cell.append('"');
                    i++;
                } else {
                    inQuotes = !inQuotes;
                }
                continue;
            }

            if (current == ',' && !inQuotes) {
                row.add(cell.toString());
                cell.setLength(0);
                continue;
            }

            if ((current == '\n' || current == '\r') && !inQuotes) {
                if (current == '\r' && i + 1 < normalizedCsv.length() && normalizedCsv.charAt(i + 1) == '\n') {
                    i++;
                }
                row.add(cell.toString());
                rows.add(row);
                row = new ArrayList<>();
                cell.setLength(0);
                continue;
            }

            cell.append(current);
        }

        if (!row.isEmpty() || !cell.isEmpty()) {
            row.add(cell.toString());
            rows.add(row);
        }

        return rows;
    }

    private byte[] writeXlsx(List<List<String>> table) {
        try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
             ZipOutputStream zipOutputStream = new ZipOutputStream(outputStream, StandardCharsets.UTF_8)) {
            addZipEntry(zipOutputStream, "[Content_Types].xml", contentTypesXml());
            addZipEntry(zipOutputStream, "_rels/.rels", rootRelationshipsXml());
            addZipEntry(zipOutputStream, "docProps/app.xml", appXml());
            addZipEntry(zipOutputStream, "docProps/core.xml", coreXml());
            addZipEntry(zipOutputStream, "xl/workbook.xml", workbookXml());
            addZipEntry(zipOutputStream, "xl/_rels/workbook.xml.rels", workbookRelationshipsXml());
            addZipEntry(zipOutputStream, "xl/worksheets/sheet1.xml", sheetXml(table));
            zipOutputStream.finish();
            return outputStream.toByteArray();
        } catch (IOException exception) {
            throw new CustomException(ErrorCode.INTERNAL_SERVER_ERROR);
        }
    }

    private List<List<String>> readXlsx(byte[] bytes) {
        try {
            java.nio.file.Path tempFile = java.nio.file.Files.createTempFile("plan-upload-", ".xlsx");
            try {
                java.nio.file.Files.write(tempFile, bytes);
                try (ZipFile zipFile = new ZipFile(tempFile.toFile())) {
                    List<String> sharedStrings = readSharedStrings(zipFile);
                    ZipEntry sheetEntry = zipFile.getEntry("xl/worksheets/sheet1.xml");
                    if (sheetEntry == null) {
                        throw new CustomException(ErrorCode.INVALID_PLAN_FILE);
                    }

                    Document sheetDocument = parseXml(zipFile.getInputStream(sheetEntry).readAllBytes());
                    NodeList rowNodes = sheetDocument.getElementsByTagName("row");
                    List<List<String>> rows = new ArrayList<>();

                    for (int rowIndex = 0; rowIndex < rowNodes.getLength(); rowIndex++) {
                        Element rowElement = (Element) rowNodes.item(rowIndex);
                        NodeList cellNodes = rowElement.getElementsByTagName("c");
                        Map<Integer, String> cells = new TreeMap<>();

                        for (int cellIndex = 0; cellIndex < cellNodes.getLength(); cellIndex++) {
                            Element cellElement = (Element) cellNodes.item(cellIndex);
                            int columnIndex = columnIndex(cellElement.getAttribute("r"));
                            cells.put(columnIndex, readCellValue(cellElement, sharedStrings));
                        }

                        int lastColumn = cells.keySet().stream().mapToInt(Integer::intValue).max().orElse(-1);
                        List<String> row = new ArrayList<>();
                        for (int columnIndex = 0; columnIndex <= lastColumn; columnIndex++) {
                            row.add(cells.getOrDefault(columnIndex, ""));
                        }
                        rows.add(row);
                    }

                    return rows;
                }
            } finally {
                java.nio.file.Files.deleteIfExists(tempFile);
            }
        } catch (IOException exception) {
            throw new CustomException(ErrorCode.INVALID_PLAN_FILE);
        }
    }

    private List<String> readSharedStrings(ZipFile zipFile) throws IOException {
        ZipEntry sharedStringsEntry = zipFile.getEntry("xl/sharedStrings.xml");
        if (sharedStringsEntry == null) {
            return List.of();
        }

        Document document = parseXml(zipFile.getInputStream(sharedStringsEntry).readAllBytes());
        NodeList textNodes = document.getElementsByTagName("t");
        List<String> sharedStrings = new ArrayList<>();

        for (int i = 0; i < textNodes.getLength(); i++) {
            sharedStrings.add(textNodes.item(i).getTextContent());
        }

        return sharedStrings;
    }

    private Document parseXml(byte[] bytes) {
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
            factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
            return factory.newDocumentBuilder().parse(new ByteArrayInputStream(bytes));
        } catch (Exception exception) {
            throw new CustomException(ErrorCode.INVALID_PLAN_FILE);
        }
    }

    private String readCellValue(Element cellElement, List<String> sharedStrings) {
        String type = cellElement.getAttribute("t");
        if ("inlineStr".equals(type)) {
            NodeList textNodes = cellElement.getElementsByTagName("t");
            return textNodes.getLength() == 0 ? "" : textNodes.item(0).getTextContent();
        }

        NodeList values = cellElement.getElementsByTagName("v");
        if (values.getLength() == 0) {
            return "";
        }

        String value = values.item(0).getTextContent();
        if ("s".equals(type)) {
            int sharedStringIndex = Integer.parseInt(value);
            return sharedStringIndex < sharedStrings.size() ? sharedStrings.get(sharedStringIndex) : "";
        }

        return value;
    }

    private void addZipEntry(ZipOutputStream zipOutputStream, String name, String content) throws IOException {
        zipOutputStream.putNextEntry(new ZipEntry(name));
        zipOutputStream.write(content.getBytes(StandardCharsets.UTF_8));
        zipOutputStream.closeEntry();
    }

    private String sheetXml(List<List<String>> table) {
        StringBuilder builder = new StringBuilder();
        builder.append("""
                <?xml version="1.0" encoding="UTF-8" standalone="yes"?>
                <worksheet xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main">
                <sheetData>
                """);

        for (int rowIndex = 0; rowIndex < table.size(); rowIndex++) {
            int rowNumber = rowIndex + 1;
            builder.append("<row r=\"").append(rowNumber).append("\">");
            List<String> row = table.get(rowIndex);
            for (int columnIndex = 0; columnIndex < row.size(); columnIndex++) {
                String cellReference = columnName(columnIndex) + rowNumber;
                builder.append("<c r=\"").append(cellReference).append("\" t=\"inlineStr\"><is><t>")
                        .append(escapeXml(row.get(columnIndex)))
                        .append("</t></is></c>");
            }
            builder.append("</row>");
        }

        builder.append("</sheetData></worksheet>");
        return builder.toString();
    }

    private String contentTypesXml() {
        return """
                <?xml version="1.0" encoding="UTF-8" standalone="yes"?>
                <Types xmlns="http://schemas.openxmlformats.org/package/2006/content-types">
                  <Default Extension="rels" ContentType="application/vnd.openxmlformats-package.relationships+xml"/>
                  <Default Extension="xml" ContentType="application/xml"/>
                  <Override PartName="/xl/workbook.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.sheet.main+xml"/>
                  <Override PartName="/xl/worksheets/sheet1.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.worksheet+xml"/>
                  <Override PartName="/docProps/core.xml" ContentType="application/vnd.openxmlformats-package.core-properties+xml"/>
                  <Override PartName="/docProps/app.xml" ContentType="application/vnd.openxmlformats-officedocument.extended-properties+xml"/>
                </Types>
                """;
    }

    private String rootRelationshipsXml() {
        return """
                <?xml version="1.0" encoding="UTF-8" standalone="yes"?>
                <Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">
                  <Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/officeDocument" Target="xl/workbook.xml"/>
                  <Relationship Id="rId2" Type="http://schemas.openxmlformats.org/package/2006/relationships/metadata/core-properties" Target="docProps/core.xml"/>
                  <Relationship Id="rId3" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/extended-properties" Target="docProps/app.xml"/>
                </Relationships>
                """;
    }

    private String workbookXml() {
        return """
                <?xml version="1.0" encoding="UTF-8" standalone="yes"?>
                <workbook xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main"
                          xmlns:r="http://schemas.openxmlformats.org/officeDocument/2006/relationships">
                  <sheets>
                    <sheet name="production_plans" sheetId="1" r:id="rId1"/>
                  </sheets>
                </workbook>
                """;
    }

    private String workbookRelationshipsXml() {
        return """
                <?xml version="1.0" encoding="UTF-8" standalone="yes"?>
                <Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">
                  <Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/worksheet" Target="worksheets/sheet1.xml"/>
                </Relationships>
                """;
    }

    private String appXml() {
        return """
                <?xml version="1.0" encoding="UTF-8" standalone="yes"?>
                <Properties xmlns="http://schemas.openxmlformats.org/officeDocument/2006/extended-properties"
                            xmlns:vt="http://schemas.openxmlformats.org/officeDocument/2006/docPropsVTypes">
                  <Application>S-MAP</Application>
                </Properties>
                """;
    }

    private String coreXml() {
        String now = OffsetDateTime.now(DEFAULT_PRODUCTION_ZONE)
                .format(DateTimeFormatter.ISO_OFFSET_DATE_TIME);
        return """
                <?xml version="1.0" encoding="UTF-8" standalone="yes"?>
                <cp:coreProperties xmlns:cp="http://schemas.openxmlformats.org/package/2006/metadata/core-properties"
                    xmlns:dc="http://purl.org/dc/elements/1.1/"
                    xmlns:dcterms="http://purl.org/dc/terms/"
                    xmlns:dcmitype="http://purl.org/dc/dcmitype/"
                    xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance">
                  <dc:creator>S-MAP</dc:creator>
                  <dcterms:created xsi:type="dcterms:W3CDTF">%s</dcterms:created>
                  <dcterms:modified xsi:type="dcterms:W3CDTF">%s</dcterms:modified>
                </cp:coreProperties>
                """.formatted(now, now);
    }

    private Long parseLong(
            ParsedRow row,
            String fieldName,
            boolean required,
            List<PlanFileValidationErrorResponse> errors
    ) {
        String value = row.value(fieldName);
        if (value.isBlank()) {
            if (required) {
                errors.add(createError(row.rowNumber(), fieldName, "필수 값입니다."));
            }
            return null;
        }

        try {
            return Long.parseLong(value);
        } catch (NumberFormatException exception) {
            errors.add(createError(row.rowNumber(), fieldName, "숫자 형식이어야 합니다."));
            return null;
        }
    }

    private Integer parseInteger(
            ParsedRow row,
            String fieldName,
            boolean required,
            List<PlanFileValidationErrorResponse> errors
    ) {
        String value = row.value(fieldName);
        if (value.isBlank()) {
            if (required) {
                errors.add(createError(row.rowNumber(), fieldName, "필수 값입니다."));
            }
            return null;
        }

        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException exception) {
            errors.add(createError(row.rowNumber(), fieldName, "정수 형식이어야 합니다."));
            return null;
        }
    }

    private OffsetDateTime parseOffsetDateTime(
            ParsedRow row,
            String fieldName,
            boolean required,
            List<PlanFileValidationErrorResponse> errors
    ) {
        String value = row.value(fieldName);
        if (value.isBlank()) {
            if (required) {
                errors.add(createError(row.rowNumber(), fieldName, "필수 값입니다."));
            }
            return null;
        }

        try {
            return OffsetDateTime.parse(value);
        } catch (DateTimeParseException ignored) {
            try {
                return LocalDateTime.parse(value.replace(' ', 'T'))
                        .atZone(DEFAULT_PRODUCTION_ZONE)
                        .toOffsetDateTime();
            } catch (DateTimeParseException exception) {
                errors.add(createError(row.rowNumber(), fieldName, "ISO 날짜/시간 형식이어야 합니다."));
                return null;
            }
        }
    }

    private BigDecimal calculateEstimatedDurationHr(
            Integer plannedQuantity,
            LineCapabilityProjection capability
    ) {
        Integer capacityPerDay = capability.getCapacityPerDay();
        BigDecimal standardProductionTimeHr = capability.getStandardProductionTimeHr();

        if (capacityPerDay != null && capacityPerDay > 0) {
            long requiredDays = (plannedQuantity + capacityPerDay - 1L) / capacityPerDay;
            return BigDecimal.valueOf(requiredDays)
                    .multiply(BigDecimal.valueOf(24))
                    .setScale(2, RoundingMode.HALF_UP);
        }

        if (standardProductionTimeHr != null && standardProductionTimeHr.compareTo(BigDecimal.ZERO) > 0) {
            return standardProductionTimeHr
                    .multiply(BigDecimal.valueOf(plannedQuantity))
                    .setScale(2, RoundingMode.CEILING);
        }

        throw new CustomException(ErrorCode.AVAILABLE_PRODUCTION_LINE_NOT_FOUND);
    }

    private void addError(
            List<PlanFileValidationErrorResponse> errors,
            Map<Integer, Integer> rowErrorCounts,
            PlanFileValidationErrorResponse error
    ) {
        errors.add(error);
        if (error.getRowNumber() > 0) {
            rowErrorCounts.merge(error.getRowNumber(), 1, Integer::sum);
        }
    }

    private PlanFileValidationErrorResponse createError(
            int rowNumber,
            String fieldName,
            String message
    ) {
        return PlanFileValidationErrorResponse.builder()
                .rowNumber(rowNumber)
                .fieldName(fieldName)
                .message(message)
                .build();
    }

    private String normalizeFormat(String format) {
        if (format == null || format.isBlank()) {
            return "csv";
        }

        return format.trim().toLowerCase(Locale.ROOT);
    }

    private String resolveExtension(String filename) {
        if (filename == null || !filename.contains(".")) {
            throw new CustomException(ErrorCode.INVALID_PLAN_FILE);
        }

        return filename.substring(filename.lastIndexOf('.') + 1)
                .toLowerCase(Locale.ROOT);
    }

    private String normalizeHeader(String header) {
        return header == null ? "" : header.trim().toLowerCase(Locale.ROOT);
    }

    private boolean isEmptyRow(List<String> cells) {
        return cells.stream().allMatch(cell -> cell == null || cell.isBlank());
    }

    private String escapeCsv(String value) {
        String safeValue = value == null ? "" : value;
        if (safeValue.contains(",") || safeValue.contains("\"") || safeValue.contains("\n") || safeValue.contains("\r")) {
            return "\"" + safeValue.replace("\"", "\"\"") + "\"";
        }

        return safeValue;
    }

    private String escapeXml(String value) {
        String safeValue = value == null ? "" : value;
        return safeValue
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&apos;");
    }

    private String columnName(int columnIndex) {
        StringBuilder builder = new StringBuilder();
        int index = columnIndex;
        do {
            builder.insert(0, (char) ('A' + (index % 26)));
            index = index / 26 - 1;
        } while (index >= 0);
        return builder.toString();
    }

    private int columnIndex(String cellReference) {
        int index = 0;
        for (int i = 0; i < cellReference.length(); i++) {
            char current = cellReference.charAt(i);
            if (!Character.isLetter(current)) {
                break;
            }
            index = index * 26 + Character.toUpperCase(current) - 'A' + 1;
        }
        return Math.max(index - 1, 0);
    }

    private String toStringValue(Object value) {
        return value == null ? "" : value.toString();
    }

    private record ParsedRow(
            int rowNumber,
            Map<String, String> values
    ) {
        private String value(String fieldName) {
            return values.getOrDefault(fieldName, "");
        }
    }

    private record ValidatedRow(
            int rowNumber,
            Long orderId,
            Long productId,
            Long lineId,
            Long operatorId,
            OffsetDateTime plannedStartAt,
            OffsetDateTime plannedEndAt,
            BigDecimal estimatedDurationHr,
            Integer plannedQuantity,
            Integer planSequence
    ) {
    }

    private record RowValidation(
            Optional<ValidatedRow> row,
            List<PlanFileValidationErrorResponse> errors
    ) {
    }

    private record ValidationResult(
            PlanFileValidationResponse response,
            List<ValidatedRow> rows
    ) {
    }
}
