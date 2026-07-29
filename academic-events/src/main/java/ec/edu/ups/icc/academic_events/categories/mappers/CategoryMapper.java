package ec.edu.ups.icc.academic_events.categories.mappers;

import ec.edu.ups.icc.academic_events.categories.dtos.CategoryResponseDTO;
import ec.edu.ups.icc.academic_events.categories.entities.CategoryEntity;
import org.springframework.stereotype.Component;

@Component
public class CategoryMapper {

    public CategoryResponseDTO toResponse(CategoryEntity category) {
        return new CategoryResponseDTO(
                category.getId(),
                category.getName(),
                category.getDescription(),
                category.getActive(),
                category.getCreatedAt(),
                category.getUpdatedAt()
        );
    }
}