package com.sanoli.financedash.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sanoli.financedash.domain.AppUser;
import com.sanoli.financedash.exception.ErrorResponse;
import com.sanoli.financedash.repository.UserRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.LocalDateTime;

@Component
public class SubscriptionAccessFilter extends OncePerRequestFilter {

    private final UserRepository userRepository;
    private final ObjectMapper objectMapper;

    public SubscriptionAccessFilter(UserRepository userRepository, ObjectMapper objectMapper) {
        this.userRepository = userRepository;
        this.objectMapper = objectMapper;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        if (!requiresActiveSubscription(request)) {
            filterChain.doFilter(request, response);
            return;
        }

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof UserPrincipal principal)) {
            filterChain.doFilter(request, response);
            return;
        }

        AppUser user = userRepository.findById(principal.getId()).orElse(null);
        if (user == null || user.hasActiveAccess()) {
            filterChain.doFilter(request, response);
            return;
        }

        writePaymentRequired(response, request.getRequestURI(), user.getAccessMessage());
    }

    private boolean requiresActiveSubscription(HttpServletRequest request) {
        String path = request.getRequestURI();
        return path.startsWith("/api/v1/transactions")
                || path.startsWith("/api/v1/categories")
                || path.startsWith("/api/v1/goals")
                || path.startsWith("/api/v1/dashboard");
    }

    private void writePaymentRequired(HttpServletResponse response, String path, String message) throws IOException {
        ErrorResponse error = new ErrorResponse(
                LocalDateTime.now(),
                HttpStatus.PAYMENT_REQUIRED.value(),
                "SUBSCRIPTION_REQUIRED",
                message,
                path
        );

        response.setStatus(HttpStatus.PAYMENT_REQUIRED.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        objectMapper.writeValue(response.getWriter(), error);
    }
}
