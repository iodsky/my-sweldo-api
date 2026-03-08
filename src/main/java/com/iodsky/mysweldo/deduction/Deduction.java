package com.iodsky.mysweldo.deduction;

import com.iodsky.mysweldo.common.BaseModel;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.*;

@Entity
@Table(name = "deduction")
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class Deduction extends BaseModel {
    @Id
    private String code;
    private String description;
}