package com.wildbeyond.config;

import org.flywaydb.core.Flyway;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import jakarta.persistence.EntityManagerFactory;

import javax.sql.DataSource;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * Explicit Flyway bootstrap to guarantee schema migrations run at startup.
 *
 * This keeps database evolution deterministic even if framework autoconfiguration
 * behavior changes between Spring Boot releases.
 */
@Configuration
@ConditionalOnProperty(prefix = "spring.flyway", name = "enabled", havingValue = "true", matchIfMissing = true)
public class FlywayMigrationConfig {

    @Bean(initMethod = "migrate")
    public Flyway flyway(DataSource dataSource) {
        return Flyway.configure()
                .dataSource(dataSource)
                .locations("classpath:db/migration")
                .baselineOnMigrate(true)
                .load();
    }

    @Bean
    public static BeanFactoryPostProcessor entityManagerFactoryDependsOnFlyway() {
        return (ConfigurableListableBeanFactory beanFactory) -> {
            String[] emfBeanNames = beanFactory.getBeanNamesForType(EntityManagerFactory.class, true, false);

            for (String emfBeanName : emfBeanNames) {
                var beanDefinition = beanFactory.getBeanDefinition(emfBeanName);
                Set<String> dependencies = new LinkedHashSet<>();

                if (beanDefinition.getDependsOn() != null) {
                    dependencies.addAll(Arrays.asList(beanDefinition.getDependsOn()));
                }

                dependencies.add("flyway");
                beanDefinition.setDependsOn(dependencies.toArray(String[]::new));
            }
        };
    }
}
