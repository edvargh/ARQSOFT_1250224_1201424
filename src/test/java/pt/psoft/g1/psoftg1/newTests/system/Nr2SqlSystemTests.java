package pt.psoft.g1.psoftg1.newTests.system;

import static org.hamcrest.Matchers.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import jakarta.transaction.Transactional;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import pt.psoft.g1.psoftg1.newTests.testutils.SqlBackedITBase;
import pt.psoft.g1.psoftg1.newTests.testutils.SystemTestsSeeds;
import pt.psoft.g1.psoftg1.usermanagement.model.User;
import pt.psoft.g1.psoftg1.usermanagement.repositories.UserRepository;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles({"it","sql"})
@Transactional
class Nr2SqlSystemTests extends SqlBackedITBase {

  @Autowired MockMvc mvc;
  @Autowired UserRepository userRepo;

  private static RequestPostProcessor asReader(User u) {
    return jwt()
        .jwt(j -> j.claim("sub", u.getId() + "," + u.getUsername()))
        .authorities(new SimpleGrantedAuthority("ROLE_READER"));
  }

  @Test
  void journey_reader_signup_and_self_profile() throws Exception {
    // 1) Sign up
    final String email = "journey.reader@example.com";
    SystemTestsSeeds.createReader(mvc, email, "Journey Reader", "923456789");

    // 2) Authenticate as the created reader
    var created = userRepo.findByUsername(email).orElseThrow();

    // 3) Self profile view
    mvc.perform(get("/api/readers").with(asReader(created)))
        .andExpect(status().isOk())
        .andExpect(header().string("ETag", not(isEmptyOrNullString())))
        .andExpect(jsonPath("$.email").value(email))
        .andExpect(jsonPath("$.readerNumber", not(emptyOrNullString())))
        .andExpect(jsonPath("$.fullName").value("Journey Reader"));
  }
}
