package ec.edu.ups.icc.academic_events.audit.repositories;

import ec.edu.ups.icc.academic_events.audit.entities.AuditLogEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface AuditLogRepository extends JpaRepository<AuditLogEntity, Long> {

    Page<AuditLogEntity> findAllByActorId(Long actorId, Pageable pageable);

    Page<AuditLogEntity> findAllByResourceTypeAndResourceId(
            String resourceType, Long resourceId, Pageable pageable);

    Page<AuditLogEntity> findAllByAction(String action, Pageable pageable);
}