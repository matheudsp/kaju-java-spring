package com.valedosol.kaju.config;

import com.valedosol.kaju.model.SubscriptionPlan;
import com.valedosol.kaju.model.Target;
import com.valedosol.kaju.repository.SubscriptionPlanRepository;
import com.valedosol.kaju.repository.TargetRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class DataInitializer {

    @Bean
    public CommandLineRunner initData(SubscriptionPlanRepository subscriptionPlanRepository,
                                      TargetRepository targetRepository) {
        return args -> {
            // Initialize subscription plans if they don't exist
            if (subscriptionPlanRepository.count() == 0) {
                SubscriptionPlan beginner = new SubscriptionPlan("Iniciante", 1, 25.00);
                SubscriptionPlan professional = new SubscriptionPlan("Profissional", 3, 50.00);

                subscriptionPlanRepository.save(beginner);
                subscriptionPlanRepository.save(professional);
            }

            // Initialize default targets if they don't exist
            if (targetRepository.count() == 0) {
                Target newsletterTarget = new Target(
                        "Canal Newsletter do Kaju",
                        "120363417811722085@newsletter",
                        "newsletter",
                        "Canal oficial de newsletter do Kaju para promoções e novidades"
                );

                Target groupTarget = new Target(
                        "Grupo do Kaju",
                        "120363397285478228@g.us",
                        "channel",
                        "Grupo oficial do Kaju para discussões e promoções"
                );

                targetRepository.save(newsletterTarget);
                targetRepository.save(groupTarget);
            }
        };
    }
}