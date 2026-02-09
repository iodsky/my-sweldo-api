package com.iodsky.sweldox.payroll.contribution.philhealth;

import com.iodsky.sweldox.common.response.ApiResponse;
import com.iodsky.sweldox.common.response.DeleteResponse;
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
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/payroll-config/philhealth")
@PreAuthorize("hasRole('PAYROLL')")
@Validated
@RequiredArgsConstructor
@Tag(name = "Payroll Configuration - PhilHealth", description = "Manage PhilHealth contribution configurations")
public class PhilhealthContributionController {

    private final PhilhealthContributionService philhealthContributionService;
    private final PhilhealthContributionMapper philhealthContributionMapper;

    @PostMapping
    @Operation(summary = "Create PhilHealth configuration", description = "Create a new PhilHealth contribution configuration. Requires PAYROLL role.")
    public ResponseEntity<ApiResponse<PhilhealthContributionDto>> createPhilhealthContribution(
            @Valid @RequestBody PhilhealthContributionRequest request) {
        PhilhealthContribution contribution = philhealthContributionService.createPhilhealthContribution(request);
        return ResponseFactory.created(
                "PhilHealth contribution configuration created successfully",
                philhealthContributionMapper.toDto(contribution)
        );
    }

    @GetMapping
    @Operation(summary = "Get all PhilHealth configurations", description = "Retrieve all PhilHealth contribution configurations with pagination. Requires PAYROLL role.")
    public ResponseEntity<ApiResponse<List<PhilhealthContributionDto>>> getAllPhilhealthContributions(
            @Parameter(description = "Page number (0-indexed)") @RequestParam(defaultValue = "0") @Min(0) int pageNo,
            @Parameter(description = "Number of items per page (1-100)") @RequestParam(defaultValue = "10") @Min(1) @Max(100) int limit,
            @Parameter(description = "Filter by effective date (on or before)") @RequestParam(required = false) LocalDate effectiveDate
    ) {
        Page<PhilhealthContribution> page = philhealthContributionService.getAllPhilhealthContributions(pageNo, limit, effectiveDate);
        List<PhilhealthContributionDto> contributions = page.getContent().stream()
                .map(philhealthContributionMapper::toDto)
                .toList();

        return ResponseFactory.ok(
                "PhilHealth contribution configurations retrieved successfully",
                contributions,
                PaginationMeta.of(page)
        );
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get PhilHealth configuration by ID", description = "Retrieve a specific PhilHealth contribution configuration. Requires PAYROLL role.")
    public ResponseEntity<ApiResponse<PhilhealthContributionDto>> getPhilhealthContributionById(
            @Parameter(description = "Configuration ID") @PathVariable UUID id) {
        PhilhealthContribution contribution = philhealthContributionService.getPhilhealthContributionById(id);
        return ResponseFactory.ok(
                "PhilHealth contribution configuration retrieved successfully",
                philhealthContributionMapper.toDto(contribution)
        );
    }

    @GetMapping("/latest")
    @Operation(summary = "Get latest PhilHealth configuration", description = "Retrieve the latest PhilHealth contribution configuration for a given date. Requires PAYROLL role.")
    public ResponseEntity<ApiResponse<PhilhealthContributionDto>> getLatestPhilhealthContribution(
            @Parameter(description = "Date to check (defaults to today)") @RequestParam(required = false) LocalDate date
    ) {
        LocalDate effectiveDate = date != null ? date : LocalDate.now();
        PhilhealthContribution contribution = philhealthContributionService.getLatestPhilhealthContribution(effectiveDate);
        return ResponseFactory.ok(
                "Latest PhilHealth contribution configuration retrieved successfully",
                philhealthContributionMapper.toDto(contribution)
        );
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update PhilHealth configuration", description = "Update an existing PhilHealth contribution configuration. Requires PAYROLL role.")
    public ResponseEntity<ApiResponse<PhilhealthContributionDto>> updatePhilhealthContribution(
            @Parameter(description = "Configuration ID") @PathVariable UUID id,
            @Valid @RequestBody PhilhealthContributionRequest request) {
        PhilhealthContribution contribution = philhealthContributionService.updatePhilhealthContribution(id, request);
        return ResponseFactory.ok(
                "PhilHealth contribution configuration updated successfully",
                philhealthContributionMapper.toDto(contribution)
        );
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete PhilHealth configuration", description = "Soft delete a PhilHealth contribution configuration. Requires PAYROLL role.")
    public ResponseEntity<ApiResponse<DeleteResponse>> deletePhilhealthContribution(
            @Parameter(description = "Configuration ID") @PathVariable UUID id) {
        philhealthContributionService.deletePhilhealthContribution(id);
        return ResponseFactory.ok(
                "PhilHealth contribution configuration deleted successfully",
                new DeleteResponse("PhilHealthContribution", id)
        );
    }
}
