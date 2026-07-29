package ec.edu.ups.icc.academic_events.categories.repositories;

import ec.edu.ups.icc.academic_events.categories.entities.CategoryEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CategoryRepository
        extends JpaRepository<CategoryEntity, Long> {

    List<CategoryEntity> findAllByActiveTrue();

    Page<CategoryEntity> findAllByActive(
            Boolean active,
            Pageable pageable
    );

    Page<CategoryEntity> findAllByNameContainingIgnoreCase(
            String name,
            Pageable pageable
    );

    Page<CategoryEntity> findAllByNameContainingIgnoreCaseAndActive(
            String name,
            Boolean active,
            Pageable pageable
    );

    Optional<CategoryEntity> findByNameIgnoreCase(String name);

    boolean existsByNameIgnoreCase(String name);

    boolean existsByNameIgnoreCaseAndIdNot(
            String name,
            Long id
    );
}