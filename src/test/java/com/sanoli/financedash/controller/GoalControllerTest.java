package com.sanoli.financedash.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sanoli.financedash.exception.GlobalExceptionHandler;
import com.sanoli.financedash.repository.UserRepository;
import com.sanoli.financedash.security.JwtAuthenticationFilter;
import com.sanoli.financedash.security.JwtService;
import com.sanoli.financedash.security.SubscriptionAccessFilter;
import com.sanoli.financedash.service.GoalService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.Map;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(GoalController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(GlobalExceptionHandler.class)
class GoalControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private GoalService goalService;

    @MockBean
    private JwtService jwtService;

    @MockBean
    private UserRepository userRepository;

    @MockBean
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    @MockBean
    private SubscriptionAccessFilter subscriptionAccessFilter;

    @Test
    void shouldRejectGoalWithoutTitle() throws Exception {
        Map<String, Object> payload = Map.of(
                "month", 7,
                "year", 2026,
                "targetAmount", new BigDecimal("3000.00"),
                "type", "SAVINGS_TARGET"
        );

        mockMvc.perform(post("/api/v1/goals")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(payload)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.message").value(containsString("title é obrigatório")))
                .andExpect(jsonPath("$.path").value("/api/v1/goals"));
    }
}

