package s_map.server.domain.user.repository;

import s_map.server.domain.user.entity.User;
import s_map.server.domain.user.entity.Role;
import s_map.server.domain.user.entity.UserStatus;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByEmail(String email);

    boolean existsByEmail(String email);

    boolean existsByIdAndStatusAndRole(Long id, UserStatus status, Role role);

    Optional<User> findFirstByNameAndStatusAndRoleOrderByIdAsc(String name, UserStatus status, Role role);

    Page<User> findByStatusNot(UserStatus status, Pageable pageable);
}
