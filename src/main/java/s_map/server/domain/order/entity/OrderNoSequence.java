package s_map.server.domain.order.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import s_map.server.global.common.BaseEntity;

import java.time.LocalDate;

@Getter
@Entity
@Table(name = "order_no_sequences")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class OrderNoSequence extends BaseEntity {

    @Id
    @Column(name = "sequence_date", nullable = false)
    private LocalDate sequenceDate;

    @Column(name = "last_sequence", nullable = false)
    private Integer lastSequence;
}
