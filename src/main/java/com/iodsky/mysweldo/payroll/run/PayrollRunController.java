package com.iodsky.mysweldo.payroll.run;

import com.iodsky.mysweldo.common.response.ApiResponse;
import com.iodsky.mysweldo.common.response.ResponseFactory;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/payroll-runs")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('PAYROLL', 'SUPERUSER')")
@Validated
public class PayrollRunController {

    private final PayrollRunService service;

    @PostMapping
    public ResponseEntity<ApiResponse<PayrollRunDto>> createPayrollRun(@Valid @RequestBody PayrollRunRequest request) {
        PayrollRunDto dto = service.createPayrollRun(request);
        return ResponseFactory.created("PayrollRun successfully created", dto);
    }

    @PostMapping("/{id}/generate")
    public ResponseEntity<ApiResponse<GeneratePayrollResponse>> generatePayroll(@PathVariable UUID id, @Valid @RequestBody GeneratePayrollRequest request) {
        GeneratePayrollResponse response = service.generatePayroll(id, request);
        return ResponseFactory.ok("Payroll generated successfully", response);
    }

}
