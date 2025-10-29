package pt.psoft.g1.psoftg1.newTests.testutils;

import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Testcontainers;

@Testcontainers
public abstract class SqlBackedITBase {

  static final MySQLContainer<?> mysql = new MySQLContainer<>("mysql:8.4")
      .withDatabaseName("app")
      .withUsername("test")
      .withPassword("test");

  static {
    mysql.start();
  }

  @DynamicPropertySource
  static void sqlProps(DynamicPropertyRegistry r) {
    r.add("spring.datasource.url", mysql::getJdbcUrl);
    r.add("spring.datasource.username", mysql::getUsername);
    r.add("spring.datasource.password", mysql::getPassword);

    r.add("spring.jpa.database-platform", () -> "org.hibernate.dialect.MySQLDialect");
  }
}
