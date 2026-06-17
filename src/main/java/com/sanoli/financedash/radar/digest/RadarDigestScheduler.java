package com.sanoli.financedash.radar.digest;

import com.sanoli.financedash.config.RadarDigestProperties;
import com.sanoli.financedash.domain.AppUser;
import com.sanoli.financedash.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class RadarDigestScheduler {

    private static final Logger log = LoggerFactory.getLogger(RadarDigestScheduler.class);

    private final RadarDigestService radarDigestService;
    private final UserRepository userRepository;
    private final RadarDigestProperties radarDigestProperties;

    public RadarDigestScheduler(
            RadarDigestService radarDigestService,
            UserRepository userRepository,
            RadarDigestProperties radarDigestProperties
    ) {
        this.radarDigestService = radarDigestService;
        this.userRepository = userRepository;
        this.radarDigestProperties = radarDigestProperties;
    }

    @Scheduled(cron = "${app.radar.digest-cron:0 0 9 * * MON}", zone = "${app.radar.timezone:America/Sao_Paulo}")
    public void sendScheduledDigests() {
        if (!radarDigestProperties.isEnabled()) {
            return;
        }

        for (AppUser user : userRepository.findAll()) {
            try {
                radarDigestService.sendDigestIfDue(user);
            } catch (RuntimeException exception) {
                log.warn("Falha ao enviar digest do Radar para o usuário {}", user.getId());
            }
        }
    }
}
