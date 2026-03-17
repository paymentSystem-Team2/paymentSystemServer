package sparta.paymentsystemserver.domain.user.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import sparta.paymentsystemserver.domain.user.entity.User;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {

    boolean existsByEmail(String email);

    Optional<User> findByEmail(String email);
}