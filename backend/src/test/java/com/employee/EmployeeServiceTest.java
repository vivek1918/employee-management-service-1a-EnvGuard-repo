package com.employee;

import com.employee.dto.EmployeeRequest;
import com.employee.dto.EmployeeResponse;
import com.employee.exception.DuplicateResourceException;
import com.employee.exception.ResourceNotFoundException;
import com.employee.model.Employee;
import com.employee.repository.EmployeeRepository;
import com.employee.service.EmployeeService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EmployeeServiceTest {

    @Mock
    private EmployeeRepository employeeRepository;

    @InjectMocks
    private EmployeeService employeeService;

    @Test
    void createSavesNormalizedEmployee() {
        EmployeeRequest request = EmployeeRequest.builder()
                .fullName("  Ada Lovelace  ")
                .email("  Ada@Example.COM  ")
                .department(" Engineering ")
                .designation(" Engineer ")
                .build();

        when(employeeRepository.existsByEmailIgnoreCase("  Ada@Example.COM  ")).thenReturn(false);
        when(employeeRepository.save(any(Employee.class))).thenAnswer(invocation -> {
            Employee e = invocation.getArgument(0);
            e.setId(UUID.randomUUID());
            e.setCreatedAt(Instant.now());
            e.setUpdatedAt(Instant.now());
            return e;
        });

        EmployeeResponse response = employeeService.create(request);

        ArgumentCaptor<Employee> captor = ArgumentCaptor.forClass(Employee.class);
        verify(employeeRepository).save(captor.capture());
        Employee saved = captor.getValue();
        assertThat(saved.getFullName()).isEqualTo("Ada Lovelace");
        assertThat(saved.getEmail()).isEqualTo("ada@example.com");
        assertThat(saved.getDepartment()).isEqualTo("Engineering");
        assertThat(response.getFullName()).isEqualTo("Ada Lovelace");
    }

    @Test
    void createThrowsOnDuplicateEmail() {
        EmployeeRequest request = EmployeeRequest.builder()
                .fullName("Ada")
                .email("ada@example.com")
                .department("Eng")
                .designation("Dev")
                .build();
        when(employeeRepository.existsByEmailIgnoreCase("ada@example.com")).thenReturn(true);

        assertThatThrownBy(() -> employeeService.create(request))
                .isInstanceOf(DuplicateResourceException.class)
                .hasMessageContaining("already exists");
    }

    @Test
    void getByIdThrowsWhenMissing() {
        UUID id = UUID.randomUUID();
        when(employeeRepository.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> employeeService.getById(id))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining(id.toString());
    }

    @Test
    void listDelegatesToRepository() {
        Employee employee = Employee.builder()
                .id(UUID.randomUUID())
                .fullName("Ada")
                .email("ada@example.com")
                .department("Eng")
                .designation("Dev")
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();
        PageRequest pageable = PageRequest.of(0, 10);
        when(employeeRepository.search(eq("ada"), eq("Eng"), eq(pageable)))
                .thenReturn(new PageImpl<>(List.of(employee)));

        Page<EmployeeResponse> page = employeeService.list("ada", "Eng", pageable);

        assertThat(page.getContent()).hasSize(1);
        assertThat(page.getContent().getFirst().getEmail()).isEqualTo("ada@example.com");
    }

    @Test
    void deleteThrowsWhenMissing() {
        UUID id = UUID.randomUUID();
        when(employeeRepository.existsById(id)).thenReturn(false);

        assertThatThrownBy(() -> employeeService.delete(id))
                .isInstanceOf(ResourceNotFoundException.class);
    }
}
