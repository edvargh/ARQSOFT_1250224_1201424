package pt.psoft.g1.psoftg1.newTests.system;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import pt.psoft.g1.psoftg1.newTests.testutils.SqlBackedITBase;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles({"it","sql"})
class Nr5SqlSystemTests extends SqlBackedITBase {

  @Autowired MockMvc mvc;

  @Container
  static GenericContainer<?> redis = new GenericContainer<>("redis:7-alpine")
      .withExposedPorts(6379);

  static { redis.start(); }

  @DynamicPropertySource
  static void redisProps(DynamicPropertyRegistry r) {
    r.add("spring.data.redis.host", () -> redis.getHost());
    r.add("spring.data.redis.port", () -> redis.getMappedPort(6379));
  }

  private static org.springframework.test.web.servlet.request.RequestPostProcessor asLibrarian() {
    return jwt()
        .jwt(j -> j.claim("sub", "1,librarian@example.com"))
        .authorities(new SimpleGrantedAuthority("ROLE_LIBRARIAN"));
  }

  @Test
  void lookupIsbnByTitle_authz_ok_returns200_or404() throws Exception {
    String title = "1984";

    MvcResult res = mvc.perform(get("/api/books/isbn")
            .param("title", title)
            .param("mode", "ANY")
            .with(asLibrarian()))
        .andReturn();

    int status = res.getResponse().getStatus();
    assertThat(status).isNotIn(HttpStatus.UNAUTHORIZED.value(), HttpStatus.FORBIDDEN.value());

    if (status == HttpStatus.OK.value()) {
      assertThat(res.getResponse().getContentType()).contains("application/json");
      assertThat(res.getResponse().getContentAsString()).contains("\"allIsbns\"");
      System.out.println("book found");
    } else {
      assertThat(status).isEqualTo(HttpStatus.NOT_FOUND.value());
      System.out.println("book not found");
    }
  }
}
