package s_map.server.domain.order.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import s_map.server.domain.order.entity.OrderNoSequence;

import java.time.LocalDate;

public interface OrderNoSequenceRepository extends JpaRepository<OrderNoSequence, LocalDate> {

    @Query(
            value = """
                    INSERT INTO order_no_sequences (
                        sequence_date,
                        last_sequence,
                        created_at,
                        updated_at
                    )
                    VALUES (
                        :sequenceDate,
                        1,
                        CURRENT_TIMESTAMP,
                        CURRENT_TIMESTAMP
                    )
                    ON CONFLICT (sequence_date)
                    DO UPDATE SET
                        last_sequence = order_no_sequences.last_sequence + 1,
                        updated_at = CURRENT_TIMESTAMP
                    RETURNING last_sequence
                    """,
            nativeQuery = true
    )
    int nextSequence(@Param("sequenceDate") LocalDate sequenceDate);
}
