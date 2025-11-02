package pt.psoft.g1.psoftg1.newTests.system;

import static org.hamcrest.Matchers.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import pt.psoft.g1.psoftg1.newTests.testutils.MongoBackedITBase;
import pt.psoft.g1.psoftg1.newTests.testutils.SystemTestsSeeds;
import pt.psoft.g1.psoftg1.usermanagement.model.User;
import pt.psoft.g1.psoftg1.usermanagement.repositories.UserRepository;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles({"it","mongo"})
class Nr2MongoSystemTests extends MongoBackedITBase {

  @Autowired MockMvc mvc;

  // Use the Mongo-flavored UserRepository bean exposed under the same interface
  @Autowired UserRepository userRepo;

  private static RequestPostProcessor asReader(User u) {
    return jwt()
        .jwt(j -> j.claim("sub", u.getId() + "," + u.getUsername()))
        .authorities(new SimpleGrantedAuthority("ROLE_READER"));
  }

  @Test
  void journey_reader_signup_and_self_profile() throws Exception {
    // 1) Sign up via fixture
    final String email = "journey.reader.mongo@example.com";
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
