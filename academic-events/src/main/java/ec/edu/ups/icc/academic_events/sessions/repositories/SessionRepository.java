package ec.edu.ups.icc.academic_events.sessions.repositories;

import ec.edu.ups.icc.academic_events.sessions.entities.SessionEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface SessionRepository extends JpaRepository<SessionEntity, Long> {

    List<SessionEntity> findAllByEventId(Long eventId);

    Optional<SessionEntity> findByIdAndEventId(Long id, Long eventId);

    List<SessionEntity> findAllByEventIdAndStartAtLessThanAndEndAtGreaterThan(
            Long eventId, LocalDateTime endAt, LocalDateTime startAt);

    long countByEventId(Long eventId);
}