package com.sanoli.financedash.radar.rules;

import com.sanoli.financedash.domain.AppUser;
import com.sanoli.financedash.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Avalia as regras do Radar para todos os usuários de forma agendada (diária por padrão).
 * Alertas CRITICAL também podem ser disparados na hora via {@link RadarRuleEngine#evaluate}.
 */
@Component
public class RadarAlertScheduler {

    private static final Logger log = LoggerFactory.getLogger(RadarAlertScheduler.class);

    private final RadarRuleEngine radarRuleEngine;
    private final UserRepository userRepository;

    public RadarAlertScheduler(RadarRuleEngine radarRuleEngine, UserRepository userRepository) {
        this.radarRuleEngine = radarRuleEngine;
        this.userRepository = userRepository;
    }

    @Scheduled(cron = "${app.radar.alerts-cron:0 0 8 * * *}", zone = "${app.radar.timezone:America/Sao_Paulo}")
    public void evaluateAllUsers() {
        for (AppUser user : userRepository.findAll()) {
            try {
                radarRuleEngine.evaluate(user.getId());
            } catch (RuntimeException exception) {
                log.warn("Falha ao avaliar regras do Radar para o usuário {}", user.getId());
            }
        }
    }
}
