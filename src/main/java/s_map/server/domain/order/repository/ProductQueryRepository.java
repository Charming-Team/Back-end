package s_map.server.domain.order.repository;

import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class ProductQueryRepository {

    private final JdbcTemplate jdbcTemplate;

    public Optional<String> findProductNameById(Long productId) {
        List<String> productNames = jdbcTemplate.query(
                """
                        SELECT p.product_name
                        FROM products p
                        WHERE p.product_id = ?
                        """,
                (resultSet, rowNumber) -> resultSet.getString("product_name"),
                productId
        );

        return productNames.stream().findFirst();
    }
}
