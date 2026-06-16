package com.sanoli.financedash.config;

import com.sanoli.financedash.domain.AppUser;
import com.sanoli.financedash.repository.UserRepository;
import com.sanoli.financedash.service.DefaultCategoryService;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.List;

@Configuration
public class DataSeeder {

    @Bean
    CommandLineRunner seedDemoAccount(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            DefaultCategoryService defaultCategoryService,
            JdbcTemplate jdbcTemplate
    ) {
        return args -> {
            removeGlobalCategoryUniqueConstraints(jdbcTemplate);

            AppUser demoUser = userRepository.findByEmailIgnoreCase("demo@financedash.com")
                    .orElseGet(() -> {
                        AppUser user = new AppUser();
                        user.setName("Demo FinanceDash");
                        user.setEmail("demo@financedash.com");
                        user.setPasswordHash(passwordEncoder.encode("demo12345"));
                        return userRepository.save(user);
                    });
            defaultCategoryService.seedForUser(demoUser);
        };
    }

    private void removeGlobalCategoryUniqueConstraints(JdbcTemplate jdbcTemplate) {
        try {
            List<String> constraintNames = jdbcTemplate.queryForList("""
                    select conname
                    from pg_constraint
                    where conrelid = 'categories'::regclass
                      and contype = 'u'
                    """, String.class);

            for (String constraintName : constraintNames) {
                jdbcTemplate.execute("alter table categories drop constraint if exists " + constraintName);
            }
        } catch (Exception ignored) {
            // Startup must not fail if the database is not PostgreSQL or the constraint was already removed.
        }
    }
}

