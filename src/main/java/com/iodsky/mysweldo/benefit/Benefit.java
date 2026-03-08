package com.iodsky.mysweldo.benefit;

import com.iodsky.mysweldo.common.BaseModel;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.*;
import org.hibernate.annotations.SQLRestriction;

@Entity
@Table(name = "benefit")
@SQLRestriction("deleted_at IS NULL")
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class Benefit extends BaseModel {

    @Id
    private String code;
    private String description;

}