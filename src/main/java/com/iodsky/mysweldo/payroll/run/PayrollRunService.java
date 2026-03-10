package com.iodsky.mysweldo.payroll.run;

import com.iodsky.mysweldo.employee.EmployeeService;
import com.iodsky.mysweldo.attendance.AttendanceService;
import com.iodsky.mysweldo.payroll.core.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;


@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class PayrollRunService {

    private final PayrollRunRepository repository;
    private final PayrollRunMapper mapper;
    private final EmployeeService employeeService;
    private final AttendanceService attendanceService;
    private final PayrollCalculator calculator;
    private final PayrollItemRepository payrollItemRepository;
    private final PayrollBuilder payrollBuilder;

    public PayrollRunDto createPayrollRun(PayrollRunRequest request) {

        // validate period start and end date
        if (request.getPeriodEndDate().isBefore(request.getPeriodStartDate())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid period date");
        }

        PayrollRun payrollRun = PayrollRun.builder()
                .periodStartDate(request.getPeriodStartDate())
                .periodEndDate(request.getPeriodEndDate())
                .type(request.getType())
                .status(PayrollRunStatus.DRAFT)
                .notes(request.getNotes())
                .build();

        repository.save(payrollRun);
        return mapper.toDto(payrollRun);
    }

    public GeneratePayrollResponse generatePayroll(UUID id, GeneratePayrollRequest request) {

        // 1. FETCH & VALIDATE PAYROLL RUN
        PayrollRun run = repository.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Payroll run " + id + " not found"));

        if (!run.getStatus().equals(PayrollRunStatus.DRAFT)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Payroll run " + id + " already processed");
        }

        // 2. RESOLVE EMPLOYEE IDs
        //    - if request.employeeIds is null or empty
        //      → fetch all active employee ids via employeeService.getAllActiveEmployeeIds()
        List<Long> employeeIds = request.getEmployeeIds();
        if (employeeIds == null || employeeIds.isEmpty()) {
            employeeIds = employeeService.getAllActiveEmployeeIds();
        }
        //    - otherwise use request.employeeIds as-is
        //      (client selects from a pre-fetched active employee list, no further validation needed)
        //    - throw 400 if the resolved list is still empty (no active employees in the system)
        if (employeeIds.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "No active employees found");
        }

        // 3. PRELOAD PAYROLL CONFIGURATION (rate tables, tax brackets)
        //    - call payrollCalculator.loadConfiguration(payrollRun.getPeriodEndDate())
        //    - doing this once here avoids N repeated DB lookups inside the loop
        PayrollConfiguration configuration = calculator.loadConfiguration(run.getPeriodEndDate());

        // 4. FOR EACH employeeId in request.employeeIds:
        List<PayrollItem> payrollItems = new ArrayList<>();
        List<Long> skippedIds = new ArrayList<>();
        for (Long employeeId: employeeIds) {
            //
            //    a. SKIP DUPLICATES
            //       - if payrollRepository.existsByPayrollRun_IdAndEmployee_Id(id, employeeId)
            //         → skip (or collect into a "skipped" list to report back)
            if (payrollItemRepository.existsByPayrollRun_IdAndEmployee_Id(run.getId(), employeeId)) {
                log.warn("Payroll exists for employee: {} run: {}", employeeId, run.getId());
                skippedIds.add(employeeId);
                continue;
            }

            //    b. SKIP employees with no attendance in the period
            if (!attendanceService.hasAttendance(employeeId, run.getPeriodStartDate(), run.getPeriodEndDate())) {
                log.warn("No attendance records for employee: {} in period: {} - {}", employeeId, run.getPeriodStartDate(), run.getPeriodEndDate());
                skippedIds.add(employeeId);
                continue;
            }

            //    c. BUILD PAYROLL ITEM
            PayrollItem payrollItem = payrollBuilder.buildPayroll(employeeId, run, configuration);

            //    d. COLLECT into a List<PayrollItem> items
            payrollItems.add(payrollItem);
        }

        // 5. BATCH SAVE ALL ITEMS
        payrollItemRepository.saveAll(payrollItems);

        // 6. AGGREGATE TOTALS BACK ONTO THE RUN
        //    - query ALL items in the run (not just current batch) so totals accumulate
        //      correctly across multiple generatePayroll calls on the same run
        List<PayrollItem> allItems = payrollItemRepository.findAllByPayrollRun_Id(run.getId());
        BigDecimal totalGrossPay = allItems.stream()
                .map(PayrollItem::getGrossPay)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal totalBenefits = allItems.stream()
                .map(PayrollItem::getTotalBenefits)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal totalDeductions = allItems.stream()
                .map(PayrollItem::getTotalDeductions)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal totalNetPay = allItems.stream()
                .map(PayrollItem::getNetPay)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal totalEmployerCost = allItems.stream()
                .map(PayrollItem::getTotalEmployerContributions)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // 7. PERSIST AGGREGATED TOTALS — status stays DRAFT
        //    run remains DRAFT so more employees can be added in subsequent calls
        run.setTotalGrossPay(totalGrossPay);
        run.setTotalBenefits(totalBenefits);
        run.setTotalDeductions(totalDeductions);
        run.setTotalNetPay(totalNetPay);
        run.setTotalEmployerCost(totalEmployerCost);
        run.setStatus(PayrollRunStatus.DRAFT);
        repository.save(run);

        // 8. RETURN
        return GeneratePayrollResponse.builder()
                .payrollRun(mapper.toDto(run))
                .skippedEmployeeIds(skippedIds)
                .build();
    }

}
