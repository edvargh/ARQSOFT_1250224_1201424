package pt.psoft.g1.psoftg1.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.data.mongodb.repository.config.EnableMongoRepositories;

@Profile("mongo")
@Configuration
@EnableMongoRepositories(basePackages = {
    "pt.psoft.g1.psoftg1.readermanagement.infraestructure.repositories.mongo",
    "pt.psoft.g1.psoftg1.bookmanagement.infrastructure.repositories.mongo",
    "pt.psoft.g1.psoftg1.authormanagement.infrastructure.repositories.mongo",
    "pt.psoft.g1.psoftg1.genremanagement.infrastructure.repositories.mongo",
    "pt.psoft.g1.psoftg1.lendingmanagement.infrastructure.repositories.mongo",
    "pt.psoft.g1.psoftg1.shared.infrastructure.repositories.mongo",
    "pt.psoft.g1.psoftg1.usermanagement.infrastructure.repositories.mongo"
})
public class MongoReposConfig {}

