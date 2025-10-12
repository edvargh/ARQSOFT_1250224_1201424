package pt.psoft.g1.psoftg1.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.data.mongodb.repository.config.EnableMongoRepositories;

@Configuration
@Profile("mongo")
@EnableMongoRepositories(basePackages = {
    "pt.psoft.g1.psoftg1.**.infrastructure.repositories.mongo"
})
public class MongoReposConfig {}

