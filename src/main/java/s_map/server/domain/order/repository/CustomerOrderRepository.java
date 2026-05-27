package s_map.server.domain.order.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import s_map.server.domain.order.entity.CustomerOrder;

import java.util.Optional;

public interface CustomerOrderRepository extends JpaRepository<CustomerOrder, Long> {

    @Query(
            value = """
                    SELECT co.order_no
                    FROM customer_orders co
                    WHERE co.order_no LIKE CONCAT(:prefix, '%')
                    ORDER BY CAST(SUBSTRING(co.order_no FROM LENGTH(:prefix) + 1) AS integer) DESC
                    LIMIT 1
                    """,
            nativeQuery = true
    )
    Optional<String> findLatestOrderNoByPrefix(@Param("prefix") String prefix);
}
