package com.iodsky.sweldox.common.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.iodsky.sweldox.common.DuplicateField;
import com.iodsky.sweldox.common.ValidationError;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

import java.time.Instant;
import java.util.List;

@Data
@Builder
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ErrorResponse {

    private Instant timestamp;
    private int status;
    private String message;
    private List<ValidationError> validationErrors;
    private String path;
    private DuplicateField duplicateField;

}
