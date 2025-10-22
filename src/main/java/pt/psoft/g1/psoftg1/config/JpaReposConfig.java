package pt.psoft.g1.psoftg1.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.transaction.annotation.EnableTransactionManagement;

@Configuration
@Profile("sql")
@EnableJpaRepositories(basePackages = {
    "pt.psoft.g1.psoftg1.**.infraestructure.repositories.impl",
    "pt.psoft.g1.psoftg1.**.infrastructure.repositories.impl"
})
@EntityScan(basePackages = {
    "pt.psoft.g1.psoftg1.**.model"
})
@EnableTransactionManagement
public class JpaReposConfig {}
