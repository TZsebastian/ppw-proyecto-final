package ec.edu.ups.icc.academic_events.registrations.repositories;

import ec.edu.ups.icc.academic_events.registrations.entities.RegistrationEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface RegistrationRepository
        extends JpaRepository<RegistrationEntity, Long> {

    Optional<RegistrationEntity> findByRegistrationCode(
            UUID registrationCode
    );

    boolean existsByEventIdAndParticipantId(
            Long eventId,
            Long participantId
    );

    boolean existsByEventIdAndParticipantIdAndStatusIn(
            Long eventId,
            Long participantId,
            List<String> statuses
    );

    Optional<RegistrationEntity>
    findByEventIdAndParticipantId(
            Long eventId,
            Long participantId
    );

    Optional<RegistrationEntity>
    findByIdAndParticipantId(
            Long id,
            Long participantId
    );

    Page<RegistrationEntity> findAllByEventId(
            Long eventId,
            Pageable pageable
    );

    List<RegistrationEntity> findAllByEventId(
            Long eventId
    );

    Page<RegistrationEntity> findAllByParticipantId(
            Long participantId,
            Pageable pageable
    );

    long countByEventIdAndStatus(
            Long eventId,
            String status
    );
}