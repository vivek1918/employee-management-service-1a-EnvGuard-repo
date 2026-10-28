package com.employee.repository;

import com.employee.model.Employee;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface EmployeeRepository extends JpaRepository<Employee, UUID> {

    boolean existsByEmailIgnoreCase(String email);

    boolean existsByEmailIgnoreCaseAndIdNot(String email, UUID id);

    Optional<Employee> findByEmailIgnoreCase(String email);

    @Query("""
            SELECT e FROM Employee e
            WHERE (:q IS NULL OR :q = '' OR LOWER(e.fullName) LIKE LOWER(CONCAT('%', :q, '%'))
               OR LOWER(e.email) LIKE LOWER(CONCAT('%', :q, '%'))
               OR LOWER(e.designation) LIKE LOWER(CONCAT('%', :q, '%')))
              AND (:department IS NULL OR :department = '' OR LOWER(e.department) = LOWER(:department))
            """)
    Page<Employee> search(
            @Param("q") String q,
            @Param("department") String department,
            Pageable pageable);
}
