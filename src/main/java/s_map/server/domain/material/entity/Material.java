package s_map.server.domain.material.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import s_map.server.global.common.LastModifiedEntity;

@Getter
@Entity
@Builder
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "materials")
public class Material extends LastModifiedEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "material_id")
    private Long materialId;

    @Column(name = "material_code", nullable = false, unique = true, length = 50)
    private String materialCode;

    @Column(name = "material_name", nullable = false, length = 100)
    private String materialName;

    @Column(name = "material_type", nullable = false, length = 50)
    private String materialType;

    @Column(name = "unit", nullable = false, length = 20)
    private String unit;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    public void update(String materialName, String materialType, String unit, String description) {
        this.materialName = materialName;
        this.materialType = materialType;
        this.unit = unit;
        this.description = description;
    }
}
