package com.iodsky.mysweldo.employee;

import lombok.*;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DepartmentBasicDto {
    private String id;
    private String title;
}
