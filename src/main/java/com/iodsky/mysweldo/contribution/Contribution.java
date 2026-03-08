package com.iodsky.mysweldo.contribution;

import com.iodsky.mysweldo.common.BaseModel;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.*;
import org.hibernate.annotations.SQLRestriction;

@Entity
@Table(name = "contribution")
@SQLRestriction("deleted_at IS NULL")
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class Contribution extends BaseModel {

    @Id
    @Column(unique = true)
    private String code;

    private String description;

}
