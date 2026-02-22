package com.iodsky.sweldox.leave.request;

import com.iodsky.sweldox.common.RequestStatus;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class UpdateLeaveStatusDto {
    @NotNull(message = "Status is required")
    private RequestStatus status;
}
