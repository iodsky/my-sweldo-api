package com.iodsky.mysweldo.employee;

import lombok.*;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PositionBasicDto {
    private String id;
    private String title;
}
