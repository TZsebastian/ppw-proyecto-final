package ec.edu.ups.icc.academic_events.users.controllers;

import ec.edu.ups.icc.academic_events.users.dtos.UserCreateRequestDTO;
import ec.edu.ups.icc.academic_events.users.dtos.UserResponseDTO;
import ec.edu.ups.icc.academic_events.users.dtos.UserRolesRequestDTO;
import ec.edu.ups.icc.academic_events.users.dtos.UserStatusRequestDTO;
import ec.edu.ups.icc.academic_events.users.dtos.UserUpdateRequestDTO;
import ec.edu.ups.icc.academic_events.users.services.UserService;
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
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class UserController {

    private final UserService userService;

    @GetMapping
    public ResponseEntity<Page<UserResponseDTO>> findAll(
            Pageable pageable
    ) {
        return ResponseEntity.ok(userService.findAll(pageable));
    }

    @GetMapping("/{id}")
    public ResponseEntity<UserResponseDTO> findById(
            @PathVariable Long id
    ) {
        return ResponseEntity.ok(userService.findById(id));
    }

    @PostMapping
    public ResponseEntity<UserResponseDTO> create(
            @Valid @RequestBody UserCreateRequestDTO request
    ) {
        UserResponseDTO response = userService.create(request);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(response);
    }

    @PutMapping("/{id}")
    public ResponseEntity<UserResponseDTO> update(
            @PathVariable Long id,
            @Valid @RequestBody UserUpdateRequestDTO request
    ) {
        return ResponseEntity.ok(userService.update(id, request));
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<UserResponseDTO> updateStatus(
            @PathVariable Long id,
            @Valid @RequestBody UserStatusRequestDTO request
    ) {
        return ResponseEntity.ok(
                userService.updateStatus(id, request)
        );
    }

    @PutMapping("/{id}/roles")
    public ResponseEntity<UserResponseDTO> updateRoles(
            @PathVariable Long id,
            @Valid @RequestBody UserRolesRequestDTO request
    ) {
        return ResponseEntity.ok(
                userService.updateRoles(id, request)
        );
    }
}