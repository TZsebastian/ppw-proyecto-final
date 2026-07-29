package ec.edu.ups.icc.academic_events.categories.services;

import ec.edu.ups.icc.academic_events.categories.dtos.CategoryCreateRequestDTO;
import ec.edu.ups.icc.academic_events.categories.dtos.CategoryResponseDTO;
import ec.edu.ups.icc.academic_events.categories.dtos.CategoryStatusRequestDTO;
import ec.edu.ups.icc.academic_events.categories.dtos.CategoryUpdateRequestDTO;
import ec.edu.ups.icc.academic_events.categories.entities.CategoryEntity;
import ec.edu.ups.icc.academic_events.categories.mappers.CategoryMapper;
import ec.edu.ups.icc.academic_events.categories.repositories.CategoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class CategoryService {

    private final CategoryRepository categoryRepository;
    private final CategoryMapper categoryMapper;

    @Transactional(readOnly = true)
    public Page<CategoryResponseDTO> findAll(
            String search,
            Boolean active,
            Pageable pageable
    ) {
        String normalizedSearch = normalizeOptionalText(search);

        Page<CategoryEntity> categories;

        if (normalizedSearch != null && active != null) {
            categories =
                    categoryRepository
                            .findAllByNameContainingIgnoreCaseAndActive(
                                    normalizedSearch,
                                    active,
                                    pageable
                            );
        } else if (normalizedSearch != null) {
            categories =
                    categoryRepository
                            .findAllByNameContainingIgnoreCase(
                                    normalizedSearch,
                                    pageable
                            );
        } else if (active != null) {
            categories =
                    categoryRepository.findAllByActive(
                            active,
                            pageable
                    );
        } else {
            categories = categoryRepository.findAll(pageable);
        }

        return categories.map(categoryMapper::toResponse);
    }

    @Transactional(readOnly = true)
    public CategoryResponseDTO findById(Long id) {
        return categoryMapper.toResponse(findEntityById(id));
    }

    @Transactional
    public CategoryResponseDTO create(
            CategoryCreateRequestDTO request
    ) {
        String normalizedName = normalizeRequiredText(
                request.name()
        );

        if (categoryRepository.existsByNameIgnoreCase(
                normalizedName
        )) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Ya existe una categoría con ese nombre"
            );
        }

        CategoryEntity category = new CategoryEntity();
        category.setName(normalizedName);
        category.setDescription(
                normalizeOptionalText(request.description())
        );
        category.setActive(true);

        LocalDateTime now = LocalDateTime.now();
        category.setCreatedAt(now);
        category.setUpdatedAt(now);

        CategoryEntity saved =
                categoryRepository.save(category);

        return categoryMapper.toResponse(saved);
    }

    @Transactional
    public CategoryResponseDTO update(
            Long id,
            CategoryUpdateRequestDTO request
    ) {
        CategoryEntity category = findEntityById(id);
        String normalizedName = normalizeRequiredText(
                request.name()
        );

        if (categoryRepository
                .existsByNameIgnoreCaseAndIdNot(
                        normalizedName,
                        id
                )) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Ya existe otra categoría con ese nombre"
            );
        }

        category.setName(normalizedName);
        category.setDescription(
                normalizeOptionalText(request.description())
        );
        category.setUpdatedAt(LocalDateTime.now());

        CategoryEntity saved =
                categoryRepository.save(category);

        return categoryMapper.toResponse(saved);
    }

    @Transactional
    public CategoryResponseDTO updateStatus(
            Long id,
            CategoryStatusRequestDTO request
    ) {
        CategoryEntity category = findEntityById(id);

        category.setActive(request.active());
        category.setUpdatedAt(LocalDateTime.now());

        CategoryEntity saved =
                categoryRepository.save(category);

        return categoryMapper.toResponse(saved);
    }

    private CategoryEntity findEntityById(Long id) {
        return categoryRepository.findById(id)
                .orElseThrow(() ->
                        new ResponseStatusException(
                                HttpStatus.NOT_FOUND,
                                "Categoría no encontrada"
                        )
                );
    }

    private String normalizeRequiredText(String value) {
        if (value == null || value.isBlank()) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "El nombre de la categoría es obligatorio"
            );
        }

        return value.trim().replaceAll("\\s+", " ");
    }

    private String normalizeOptionalText(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }

        return value.trim().replaceAll("\\s+", " ");
    }
}