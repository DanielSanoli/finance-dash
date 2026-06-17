package com.sanoli.financedash.integration;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class TenantIsolationIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void shouldRejectUnauthenticatedFinancialRequests() throws Exception {
        mockMvc.perform(get("/api/v1/categories"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void shouldPreventUserFromAccessingAnotherUsersCategory() throws Exception {
        String tokenA = registerAndLogin("Alice", "alice@example.com", "password123");
        String tokenB = registerAndLogin("Bob", "bob@example.com", "password123");

        String categoryId = createCategory(tokenA, "Aluguel", "EXPENSE", "#EF4444");

        mockMvc.perform(get("/api/v1/categories/" + categoryId)
                        .header("Authorization", bearer(tokenB)))
                .andExpect(status().isNotFound());

        MvcResult listResult = mockMvc.perform(get("/api/v1/categories")
                        .header("Authorization", bearer(tokenB)))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode categories = objectMapper.readTree(listResult.getResponse().getContentAsString());
        assertThat(categories).isEmpty();
    }

    @Test
    void shouldBlockFinancialApisWhenTrialIsExpired() throws Exception {
        String token = registerAndLogin("Expired", "expired@example.com", "password123");

        mockMvc.perform(get("/api/v1/categories")
                        .header("Authorization", bearer(token)))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/v1/auth/simulate-expired-trial")
                        .header("Authorization", bearer(token)))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/v1/categories")
                        .header("Authorization", bearer(token)))
                .andExpect(status().isPaymentRequired())
                .andExpect(jsonPath("$.error").value("SUBSCRIPTION_REQUIRED"));

        mockMvc.perform(get("/api/v1/auth/me")
                        .header("Authorization", bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessAllowed").value(false));
    }

    private String registerAndLogin(String name, String email, String password) throws Exception {
        Map<String, Object> registerPayload = Map.of(
                "name", name,
                "email", email,
                "password", password
        );

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(registerPayload)))
                .andExpect(status().isCreated());

        Map<String, Object> loginPayload = Map.of(
                "email", email,
                "password", password
        );

        MvcResult loginResult = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginPayload)))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode body = objectMapper.readTree(loginResult.getResponse().getContentAsString());
        return body.get("token").asText();
    }

    private String createCategory(String token, String name, String type, String color) throws Exception {
        Map<String, Object> payload = Map.of(
                "name", name,
                "type", type,
                "color", color
        );

        MvcResult result = mockMvc.perform(post("/api/v1/categories")
                        .header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(payload)))
                .andExpect(status().isCreated())
                .andReturn();

        JsonNode body = objectMapper.readTree(result.getResponse().getContentAsString());
        return body.get("id").asText();
    }

    private String bearer(String token) {
        return "Bearer " + token;
    }
}
