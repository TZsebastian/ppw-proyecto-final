package ec.edu.ups.icc.academic_events.categories.controllers;

import ec.edu.ups.icc.academic_events.categories.dtos.CategoryCreateRequestDTO;
import ec.edu.ups.icc.academic_events.categories.dtos.CategoryResponseDTO;
import ec.edu.ups.icc.academic_events.categories.dtos.CategoryStatusRequestDTO;
import ec.edu.ups.icc.academic_events.categories.dtos.CategoryUpdateRequestDTO;
import ec.edu.ups.icc.academic_events.categories.services.CategoryService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/categories")
@RequiredArgsConstructor
public class CategoryController {

    private final CategoryService categoryService;

    @GetMapping
    public ResponseEntity<Page<CategoryResponseDTO>> findAll(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) Boolean active,
            Pageable pageable
    ) {
        return ResponseEntity.ok(
                categoryService.findAll(
                        search,
                        active,
                        pageable
                )
        );
    }

    @GetMapping("/{id}")
    public ResponseEntity<CategoryResponseDTO> findById(
            @PathVariable Long id
    ) {
        return ResponseEntity.ok(
                categoryService.findById(id)
        );
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<CategoryResponseDTO> create(
            @Valid @RequestBody
            CategoryCreateRequestDTO request
    ) {
        CategoryResponseDTO response =
                categoryService.create(request);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(response);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<CategoryResponseDTO> update(
            @PathVariable Long id,
            @Valid @RequestBody
            CategoryUpdateRequestDTO request
    ) {
        return ResponseEntity.ok(
                categoryService.update(id, request)
        );
    }

    @PatchMapping("/{id}/status")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<CategoryResponseDTO> updateStatus(
            @PathVariable Long id,
            @Valid @RequestBody
            CategoryStatusRequestDTO request
    ) {
        return ResponseEntity.ok(
                categoryService.updateStatus(id, request)
        );
    }
}