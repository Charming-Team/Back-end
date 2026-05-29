package s_map.server.domain.user.repository;

import s_map.server.domain.user.entity.User;
import s_map.server.domain.user.entity.Role;
import s_map.server.domain.user.entity.UserStatus;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByEmail(String email);

    boolean existsByEmail(String email);

    boolean existsByIdAndStatusAndRole(Long id, UserStatus status, Role role);

    Optional<User> findFirstByNameAndStatusAndRoleOrderByIdAsc(String name, UserStatus status, Role role);

    Page<User> findByStatusNot(UserStatus status, Pageable pageable);

    @Query("""
            select u
            from User u
            where u.status <> :status
              and (
                    lower(u.name) like lower(concat('%', :keyword, '%'))
                 or lower(u.email) like lower(concat('%', :keyword, '%'))
                 or lower(u.department) like lower(concat('%', :keyword, '%'))
                 or lower(u.companyName) like lower(concat('%', :keyword, '%'))
              )
            """)
    Page<User> findByKeywordAndStatusNot(
            @Param("keyword") String keyword,
            @Param("status") UserStatus status,
            Pageable pageable
    );
}
