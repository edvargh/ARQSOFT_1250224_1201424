package pt.psoft.g1.psoftg1.newTests.testutils;

import com.mongodb.client.MongoDatabase;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestInstance.Lifecycle;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.bson.Document;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.MongoDBContainer;
import org.testcontainers.junit.jupiter.Testcontainers;

@Testcontainers
@TestInstance(Lifecycle.PER_CLASS)
public abstract class MongoBackedITBase {

  static final MongoDBContainer mongo = new MongoDBContainer("mongo:7.0");
  static { mongo.start(); }

  @DynamicPropertySource
  static void mongoProps(DynamicPropertyRegistry r) {
    r.add("spring.data.mongodb.uri", mongo::getConnectionString);
  }

  @Autowired
  private MongoTemplate mongoTemplate;

  @BeforeAll
  void clearMongoForThisClass() {
    MongoDatabase db = mongoTemplate.getDb();
    for (String name : db.listCollectionNames()) {
      if (!name.startsWith("system.")) {
        // delete all docs, keep indexes intact
        db.getCollection(name).deleteMany(new Document());
      }
    }
  }
}
