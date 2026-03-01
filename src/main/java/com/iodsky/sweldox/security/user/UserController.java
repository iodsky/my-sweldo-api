package com.iodsky.sweldox.security.user;

import com.iodsky.sweldox.common.response.ApiResponse;
import com.iodsky.sweldox.common.response.PaginationMeta;
import com.iodsky.sweldox.common.response.ResponseFactory;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/users")
@PreAuthorize("hasAnyRole('SUPERUSER','IT')")
@RequiredArgsConstructor
@Tag(name = "Users", description = "User account management endpoints")
public class UserController {

    private final UserService userService;
    private final UserMapper userMapper;

    @PostMapping
    @Operation(
            summary = "Create user",
            description = "Create a new user account. Requires IT role.",
            operationId = "createUser"
    )
    public ResponseEntity<ApiResponse<UserDto>> createUser(@Valid @RequestBody UserRequest userRequest) {
        UserDto user = userMapper.toDto(userService.createUser(userRequest));
        return ResponseFactory.created("User created successfully", user);
    }

    @GetMapping
    @Operation(summary = "Get all users", description = "Retrieve a paginated list of user accounts with optional role filtering. Requires IT role.")
    public ResponseEntity<ApiResponse<List<UserDto>>> getUsers(
            @Parameter(description = "Page number (0-indexed)") @RequestParam(defaultValue = "0") @Min(0) int pageNo,
            @Parameter(description = "Number of items per page (1-100)") @RequestParam(defaultValue = "10") @Min(1) @Max(100) int limit,
            @Parameter(description = "Filter by role") @RequestParam(required = false) String roleName
    ) {
        Page<User> page  = userService.getAllUsers(pageNo, limit, roleName);
        List<UserDto> data = page.getContent().stream().map(userMapper::toDto).toList();

        return ResponseFactory.ok("Users retrieved successfully", data, PaginationMeta.of(page));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<UserDto>> getUserById(@PathVariable UUID id) {
        User user = userService.getUserById(id);
        UserDto res = userMapper.toDto(user);
        return ResponseFactory.ok("User retrieved successfully successfully", res);
    }

    @PatchMapping("/{id}/role")
    public ResponseEntity<ApiResponse<UserDto>> updateUserRole(@PathVariable UUID id, @RequestParam String role) {
        User user = userService.updateUserRole(id, role);
        UserDto res = userMapper.toDto(user);
        return ResponseFactory.ok("User's role updated successfully", res);
    }

}
