package s_map.server.domain.order.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import s_map.server.domain.order.entity.OrderNoSequence;

import java.time.LocalDate;
import java.util.Optional;

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
                        :initialSequence,
                        CURRENT_TIMESTAMP,
                        CURRENT_TIMESTAMP
                    )
                    ON CONFLICT (sequence_date)
                    DO UPDATE SET
                        last_sequence = GREATEST(order_no_sequences.last_sequence + 1, :initialSequence),
                        updated_at = CURRENT_TIMESTAMP
                    RETURNING last_sequence
                    """,
            nativeQuery = true
    )
    int nextSequence(
            @Param("sequenceDate") LocalDate sequenceDate,
            @Param("initialSequence") int initialSequence
    );

    @Query(
            value = """
                    SELECT ons.last_sequence
                    FROM order_no_sequences ons
                    WHERE ons.sequence_date = :sequenceDate
                    """,
            nativeQuery = true
    )
    Optional<Integer> findLastSequence(@Param("sequenceDate") LocalDate sequenceDate);
}
