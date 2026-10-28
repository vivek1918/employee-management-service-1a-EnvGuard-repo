package com.employee;

import com.employee.dto.EmployeeRequest;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class EmployeeControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private com.employee.repository.EmployeeRepository employeeRepository;

    @BeforeEach
    void setUp() {
        employeeRepository.deleteAll();
    }

    @Test
    void healthEndpointIsPublic() throws Exception {
        mockMvc.perform(get("/api/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"))
                .andExpect(jsonPath("$.service").value("employee-management-service"));
    }

    @Test
    void createAndGetEmployee() throws Exception {
        EmployeeRequest request = EmployeeRequest.builder()
                .fullName("Ada Lovelace")
                .email("ada@example.com")
                .department("Engineering")
                .designation("Engineer")
                .build();

        MvcResult createResult = mockMvc.perform(post("/api/employees")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.fullName").value("Ada Lovelace"))
                .andExpect(jsonPath("$.email").value("ada@example.com"))
                .andReturn();

        String id = objectMapper.readTree(createResult.getResponse().getContentAsString()).get("id").asText();

        mockMvc.perform(get("/api/employees/" + id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.fullName").value("Ada Lovelace"))
                .andExpect(jsonPath("$.department").value("Engineering"));
    }

    @Test
    void listEmployeesWithSearchAndFilter() throws Exception {
        createEmployee("Grace Hopper", "grace@example.com", "Engineering", "Architect");
        createEmployee("Katherine Johnson", "katherine@example.com", "Research", "Analyst");
        createEmployee("Alan Turing", "alan@example.com", "Engineering", "Scientist");

        mockMvc.perform(get("/api/employees").param("q", "grace"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.content[0].fullName").value("Grace Hopper"));

        mockMvc.perform(get("/api/employees").param("department", "Engineering"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(2)));
    }

    @Test
    void updateEmployee() throws Exception {
        String id = createEmployee("Original Name", "update@example.com", "HR", "Specialist");

        EmployeeRequest update = EmployeeRequest.builder()
                .fullName("Updated Name")
                .email("update@example.com")
                .department("Finance")
                .designation("Manager")
                .build();

        mockMvc.perform(put("/api/employees/" + id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(update)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.fullName").value("Updated Name"))
                .andExpect(jsonPath("$.department").value("Finance"))
                .andExpect(jsonPath("$.designation").value("Manager"));
    }

    @Test
    void deleteEmployee() throws Exception {
        String id = createEmployee("To Delete", "delete@example.com", "Ops", "Coordinator");

        mockMvc.perform(delete("/api/employees/" + id))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/employees/" + id))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").exists())
                .andExpect(jsonPath("$.status").value(404));
    }

    @Test
    void createDuplicateEmailReturnsConflict() throws Exception {
        createEmployee("First", "dup@example.com", "IT", "Dev");

        EmployeeRequest duplicate = EmployeeRequest.builder()
                .fullName("Second")
                .email("dup@example.com")
                .department("IT")
                .designation("Dev")
                .build();

        mockMvc.perform(post("/api/employees")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(duplicate)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("already exists")));
    }

    @Test
    void createInvalidEmployeeReturnsBadRequest() throws Exception {
        EmployeeRequest invalid = EmployeeRequest.builder()
                .fullName("")
                .email("not-an-email")
                .department("")
                .designation("")
                .build();

        mockMvc.perform(post("/api/employees")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalid)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors").exists())
                .andExpect(jsonPath("$.message").value("Validation failed"));
    }

    @Test
    void getMissingEmployeeReturnsNotFoundWithoutStackTrace() throws Exception {
        MvcResult result = mockMvc.perform(get("/api/employees/00000000-0000-0000-0000-000000000099"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.message").exists())
                .andReturn();

        String body = result.getResponse().getContentAsString();
        assertThat(body).doesNotContain("stackTrace");
        assertThat(body).doesNotContain("Exception");
        JsonNode node = objectMapper.readTree(body);
        assertThat(node.has("trace")).isFalse();
    }

    private String createEmployee(String name, String email, String dept, String designation)
            throws Exception {
        EmployeeRequest request = EmployeeRequest.builder()
                .fullName(name)
                .email(email)
                .department(dept)
                .designation(designation)
                .build();

        MvcResult result = mockMvc.perform(post("/api/employees")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andReturn();

        return objectMapper.readTree(result.getResponse().getContentAsString()).get("id").asText();
    }
}
