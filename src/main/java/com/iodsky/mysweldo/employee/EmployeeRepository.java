package com.iodsky.mysweldo.employee;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface EmployeeRepository extends JpaRepository<Employee, Long> {

    @Query("""
            SELECT new com.iodsky.mysweldo.employee.EmployeeBasicDto(
            e.id,
            e.firstName,
            e.lastName,
            new com.iodsky.mysweldo.employee.DepartmentBasicDto(d.id, d.title),
            new com.iodsky.mysweldo.employee.PositionBasicDto(p.id, p.title),
            e.status,
            e.type
            )
            FROM Employee e
            JOIN e.position p
            JOIN e.department d
            WHERE e.status = :status
            """)
    Page<EmployeeBasicDto> findAllByStatus(@Param("status") EmploymentStatus status, Pageable pageable);

    @Query("""
            SELECT new com.iodsky.mysweldo.employee.EmployeeBasicDto(
            e.id,
            e.firstName,
            e.lastName,
            new com.iodsky.mysweldo.employee.DepartmentBasicDto(d.id, d.title),
            new com.iodsky.mysweldo.employee.PositionBasicDto(p.id, p.title),
            e.status,
            e.type
            )
            FROM Employee e
            JOIN e.position p
            JOIN e.department d
            WHERE d.id = :departmentId
            """)
    Page<EmployeeBasicDto> findAllByDepartment_Id(@Param("departmentId") String departmentId, Pageable pageable);

    @Query("""
            SELECT new com.iodsky.mysweldo.employee.EmployeeBasicDto(
            e.id,
            e.firstName,
            e.lastName,
            new com.iodsky.mysweldo.employee.DepartmentBasicDto(d.id, d.title),
            new com.iodsky.mysweldo.employee.PositionBasicDto(p.id, p.title),
            e.status,
            e.type
            )
            FROM Employee e
            JOIN e.position p
            JOIN e.department d
            WHERE e.supervisor.id = :supervisorId
            """)
    Page<EmployeeBasicDto> findAllBySupervisor_Id(@Param("supervisorId") Long supervisorId, Pageable pageable);

    @Query("""
            SELECT new com.iodsky.mysweldo.employee.EmployeeBasicDto(
            e.id,
            e.firstName,
            e.lastName,
            new com.iodsky.mysweldo.employee.DepartmentBasicDto(d.id, d.title),
            new com.iodsky.mysweldo.employee.PositionBasicDto(p.id, p.title),
            e.status,
            e.type
            )
            FROM Employee e
            JOIN e.position p
            JOIN e.department d
            """)
    Page<EmployeeBasicDto> findAllBasicEmployees(Pageable pageable);

    @Query("""
        SELECT e.id
        FROM Employee e
        WHERE e.status NOT IN (
        com.iodsky.mysweldo.employee.EmploymentStatus.RESIGNED,
         com.iodsky.mysweldo.employee.EmploymentStatus.TERMINATED
         )
       """)
    List<Long> findAllActiveEmployeeIds();

    List<Employee> findAllBySupervisor_Id(Long supervisorId);

}
