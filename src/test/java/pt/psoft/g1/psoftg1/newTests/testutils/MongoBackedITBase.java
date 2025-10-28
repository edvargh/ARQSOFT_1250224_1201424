package pt.psoft.g1.psoftg1.newTests.testutils;

import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.MongoDBContainer;
import org.testcontainers.junit.jupiter.Testcontainers;

@Testcontainers
public abstract class MongoBackedITBase {

  static final MongoDBContainer mongo = new MongoDBContainer("mongo:7.0");
  static { mongo.start(); }

  @DynamicPropertySource
  static void mongoProps(DynamicPropertyRegistry r) {
    r.add("spring.data.mongodb.uri", mongo::getConnectionString);
  }
}
