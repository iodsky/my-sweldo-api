package com.iodsky.mysweldo.benefit;

import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class BenefitRequest {

    @NotBlank(message = "Code is required")
    @Size(max = 50, message = "Code must not exceed 50 characters")
    private String code;

    @NotBlank(message = "Description is required")
    @Size(max = 100, message = "Type must not exceed 100 characters")
    private String description;

    private boolean taxable;

    @Digits(integer = 17, fraction = 2)
    @PositiveOrZero(message = "Non-taxable limit must be zero or positive")
    private BigDecimal nonTaxableLimit;
}
