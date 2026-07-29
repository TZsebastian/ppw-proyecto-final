package ec.edu.ups.icc.academic_events.events.repositories;

import ec.edu.ups.icc.academic_events.events.entities.EventEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface EventRepository extends JpaRepository<EventEntity, Long> {

    Optional<EventEntity> findByIdAndDeletedFalse(Long id);

    Page<EventEntity> findAllByDeletedFalse(Pageable pageable);

    Page<EventEntity> findAllByStatusAndDeletedFalse(String status, Pageable pageable);

    List<EventEntity> findAllByOrganizerIdAndDeletedFalse(Long organizerId);

    Page<EventEntity> findAllByOrganizerIdAndDeletedFalse(Long organizerId, Pageable pageable);

    Page<EventEntity> findAllByCategoryIdAndDeletedFalse(Long categoryId, Pageable pageable);

    boolean existsByIdAndOrganizerId(Long id, Long organizerId);
}