package ec.edu.ups.icc.academic_events.security.repositories;

import ec.edu.ups.icc.academic_events.security.entities.RefreshTokenEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface RefreshTokenRepository extends JpaRepository<RefreshTokenEntity, Long> {

    Optional<RefreshTokenEntity> findByTokenHash(String tokenHash);

    Optional<RefreshTokenEntity> findByTokenId(UUID tokenId);

    List<RefreshTokenEntity> findByUserIdAndRevokedAtIsNull(Long userId);
}