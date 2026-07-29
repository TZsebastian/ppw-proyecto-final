package ec.edu.ups.icc.academic_events.users.repositories;

import ec.edu.ups.icc.academic_events.users.entities.UserEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<UserEntity, Long> {

    Optional<UserEntity> findByEmail(String email);

    Optional<UserEntity> findByEmailAndStatus(String email, String status);

    boolean existsByEmail(String email);

    boolean existsByEmailAndIdNot(String email, Long id);
}