package pt.psoft.g1.psoftg1.newTests.integration;

import static org.hamcrest.Matchers.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.transaction.Transactional;
import java.nio.charset.StandardCharsets;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import org.springframework.test.web.servlet.MvcResult;
import pt.psoft.g1.psoftg1.authormanagement.model.Author;
import pt.psoft.g1.psoftg1.authormanagement.repositories.AuthorRepository;
import pt.psoft.g1.psoftg1.bookmanagement.model.Book;
import pt.psoft.g1.psoftg1.bookmanagement.repositories.BookRepository;
import pt.psoft.g1.psoftg1.genremanagement.model.Genre;
import pt.psoft.g1.psoftg1.genremanagement.repositories.GenreRepository;
import pt.psoft.g1.psoftg1.lendingmanagement.repositories.LendingRepository;
import pt.psoft.g1.psoftg1.newTests.testutils.SqlBackedITBase;
import pt.psoft.g1.psoftg1.readermanagement.model.ReaderDetails;
import pt.psoft.g1.psoftg1.readermanagement.repositories.ReaderRepository;
import pt.psoft.g1.psoftg1.shared.id.IdGenerator;
import pt.psoft.g1.psoftg1.usermanagement.model.Librarian;
import pt.psoft.g1.psoftg1.usermanagement.model.Reader;
import pt.psoft.g1.psoftg1.usermanagement.repositories.UserRepository;

/**
 * Opaque-box integration tests for LendingController against a real SQL backend.
 * Controller → Service → Repos → Domain are all real.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles({"it","sql"})
@Transactional
class LendingControllerSqlIT extends SqlBackedITBase {

  @Autowired MockMvc mvc;

  @Autowired GenreRepository genreRepo;
  @Autowired AuthorRepository authorRepo;
  @Autowired BookRepository bookRepo;
  @Autowired UserRepository userRepo;
  @Autowired ReaderRepository readerRepo;
  @Autowired LendingRepository lendingRepo;
  @Autowired IdGenerator idGen;

  private final ObjectMapper om = new ObjectMapper();

  private Genre tech;

  @BeforeEach
  void setUp() {
    tech = new Genre("Tech");
    tech.assignPk(idGen.newId());
    tech = genreRepo.save(tech);
  }

  private Author author(String name) {
    var a = new Author(name, "bio", null);
    a.assignId(idGen.newId());
    return authorRepo.save(a);
  }

  private Book book(String isbn, String title, String desc, List<Author> authors) {
    var b = new Book(isbn, title, desc, tech, authors, null);
    b.assignPk(idGen.newId());
    return bookRepo.save(b);
  }

  private Reader persistReaderUser(String username, String fullName) {
    var r = Reader.newReader(username, "pwd", fullName);
    r.assignId(idGen.newId());
    return userRepo.save(r);
  }

  private Librarian persistLibrarianUser(String username, String fullName) {
    var l = Librarian.newLibrarian(username, "pwd", fullName);
    l.assignId(idGen.newId());
    return userRepo.save(l);
  }

  private ReaderDetails persistReaderDetails(Reader readerUser, int seqNumber) {
    var rd = new ReaderDetails(
        seqNumber,
        readerUser,
        "2000-01-01",
        "912345678",
        true,
        false,
        false,
        null,
        List.of()
    );
    rd.assignId(idGen.newId());
    return readerRepo.save(rd);
  }

  // ---------- tests ---------------------------------------------------------

  @Test
  void findByLendingNumber_404_whenMissing() throws Exception {
    mvc.perform(get("/api/lendings/{year}/{seq}", 2099, 999))
        .andExpect(status().isNotFound());
  }

  @Test
  void setLendingReturned_200_happyPath() throws Exception {
    var a = author("Ron Jeffries");
    var savedBook = book("9780135974445", "XP Explained", "D", List.of(a));

    var owner = persistReaderUser("xp@example.com", "XP Owner");
    var rd = persistReaderDetails(owner, 88888);

    String createBody = """
  { "isbn": "%s", "readerNumber": "%s" }
  """.formatted(savedBook.getIsbn(), rd.getReaderNumber());

    MvcResult created = mvc.perform(post("/api/lendings")
            .contentType(MediaType.APPLICATION_JSON)
            .content(createBody))
        .andExpect(status().isCreated())
        .andExpect(header().string("ETag", not(isEmptyOrNullString())))
        .andReturn();

    String etag = created.getResponse().getHeader("ETag");
    JsonNode json = om.readTree(created.getResponse().getContentAsString());
    String ln = json.get("lendingNumber").asText();
    String[] parts = ln.split("/");
    String year = parts[0];
    String seq = parts[1];

    String sub = owner.getId() + "," + owner.getUsername();

    String today = java.time.LocalDate.now(java.time.ZoneId.of("Europe/Lisbon")).toString();

    String patchBody = """
  { "returnDate": "%s", "condition": "GOOD" }
  """.formatted(today);

    mvc.perform(patch("/api/lendings/{year}/{seq}", year, seq)
            .with(jwt().jwt(j -> j.claim("sub", sub)))
            .header("If-Match", etag)
            .contentType(MediaType.APPLICATION_JSON)
            .content(patchBody))
        .andExpect(status().isOk())
        .andExpect(content().contentTypeCompatibleWith("application/hal+json"))
        .andExpect(header().string("ETag", not(isEmptyOrNullString())))
        .andExpect(jsonPath("$.lendingNumber").value(ln))
        .andExpect(jsonPath("$.returnedDate").value(today));
  }



  @Test
  void setLendingReturned_400_withoutIfMatch() throws Exception {
    String body = """
      { "returnDate": "2025-01-01", "condition": "GOOD" }
      """;

    mvc.perform(patch("/api/lendings/{year}/{seq}", 2025, 1)
            .contentType(MediaType.APPLICATION_JSON)
            .content(body))
        .andExpect(status().isBadRequest())
        .andExpect(content().contentTypeCompatibleWith("application/problem+json"))
        .andExpect(jsonPath("$.detail",
            containsString("You must issue a conditional PATCH")));
  }

  @Test
  void avgDuration_200_happyPath() throws Exception {
    var a1 = author("Eric Evans");
    var b1 = book("9780321125217", "Domain-Driven Design", "D", List.of(a1));
    var u1 = persistReaderUser("avg1@example.com", "Avg One");
    var rd1 = persistReaderDetails(u1, 90001);

    var a2 = author("Greg Young");
    var b2 = book("9781617292392", "Event Sourcing", "D", List.of(a2));
    var u2 = persistReaderUser("avg2@example.com", "Avg Two");
    var rd2 = persistReaderDetails(u2, 90002);

    String body1 = """
  { "isbn": "%s", "readerNumber": "%s" }
  """.formatted(b1.getIsbn(), rd1.getReaderNumber());
    String body2 = """
  { "isbn": "%s", "readerNumber": "%s" }
  """.formatted(b2.getIsbn(), rd2.getReaderNumber());

    MvcResult c1 = mvc.perform(post("/api/lendings")
            .contentType(MediaType.APPLICATION_JSON).content(body1))
        .andExpect(status().isCreated()).andReturn();
    MvcResult c2 = mvc.perform(post("/api/lendings")
            .contentType(MediaType.APPLICATION_JSON).content(body2))
        .andExpect(status().isCreated()).andReturn();

    String etag1 = c1.getResponse().getHeader("ETag");
    String ln1 = om.readTree(c1.getResponse().getContentAsString())
        .get("lendingNumber").asText();
    String[] p1 = ln1.split("/"); String y1 = p1[0], s1 = p1[1];

    String etag2 = c2.getResponse().getHeader("ETag");
    String ln2 = om.readTree(c2.getResponse().getContentAsString())
        .get("lendingNumber").asText();
    String[] p2 = ln2.split("/"); String y2 = p2[0], s2 = p2[1];

    String sub1 = u1.getId() + "," + u1.getUsername();
    String sub2 = u2.getId() + "," + u2.getUsername();

    String today = java.time.LocalDate.now(java.time.ZoneId.of("Europe/Lisbon")).toString();
    String patch = """
  { "returnDate": "%s", "condition": "GOOD" }
  """.formatted(today);

    mvc.perform(patch("/api/lendings/{y}/{s}", y1, s1)
            .with(org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt()
                .jwt(j -> j.claim("sub", sub1)))
            .header("If-Match", etag1)
            .contentType(MediaType.APPLICATION_JSON).content(patch))
        .andExpect(status().isOk());

    mvc.perform(patch("/api/lendings/{y}/{s}", y2, s2)
            .with(org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt()
                .jwt(j -> j.claim("sub", sub2)))
            .header("If-Match", etag2)
            .contentType(MediaType.APPLICATION_JSON).content(patch))
        .andExpect(status().isOk());

    MvcResult avg = mvc.perform(get("/api/lendings/avgDuration"))
        .andExpect(status().isOk())
        .andReturn();

    JsonNode avgJson = om.readTree(avg.getResponse().getContentAsString());
    boolean hasNumber = false;
    var fields = avgJson.fields();
    while (fields.hasNext()) {
      var f = fields.next();
      if (f.getValue().isNumber()) { hasNumber = true; break; }
    }
    org.junit.jupiter.api.Assertions.assertTrue(hasNumber, "avgDuration response should include a numeric field");
  }


  @Test
  void avgDuration_400_whenNoData() throws Exception {
    mvc.perform(get("/api/lendings/avgDuration"))
        .andExpect(status().isBadRequest())
        .andExpect(content().contentTypeCompatibleWith("application/json"))
        .andExpect(jsonPath("$.message").value("Bad Request"))
        .andExpect(jsonPath("$.details[0]", containsString("For input string")));
  }


  @Test
  void overdue_404_whenNoLendings() throws Exception {
    String pageBody = """
      { "number": 1, "limit": 10 }
      """;

    mvc.perform(get("/api/lendings/overdue")
            .contentType(MediaType.APPLICATION_JSON)
            .content(pageBody))
        .andExpect(status().isNotFound())
        .andExpect(content().contentTypeCompatibleWith("application/json"))
        .andExpect(jsonPath("$.message").value("Not found"))
        .andExpect(jsonPath("$.details[0]", containsString("No lendings to show")));
  }

  @Test
  void search_200_happyPath_returnsItems() throws Exception {
    var a = author("Kent Beck");
    var savedBook = book("9780321146533", "Test-Driven Development", "D", List.of(a));

    var user = persistReaderUser("search@example.com", "Searcher");
    var rd = persistReaderDetails(user, 77777);

    String createBody = """
    { "isbn": "%s", "readerNumber": "%s" }
    """.formatted(savedBook.getIsbn(), rd.getReaderNumber());

    mvc.perform(post("/api/lendings")
            .contentType(MediaType.APPLICATION_JSON)
            .content(createBody))
        .andExpect(status().isCreated());

    String searchBody = """
    { "page": { "number": 1, "limit": 10 }, "query": { } }
  """;

    mvc.perform(post("/api/lendings/search")
            .contentType(MediaType.APPLICATION_JSON)
            .content(searchBody))
        .andExpect(status().isOk())
        .andExpect(content().contentTypeCompatibleWith("application/json"))
        .andExpect(jsonPath("$.items", not(empty())))
        .andExpect(jsonPath("$.items[*].bookTitle", hasItem("Test-Driven Development")));
  }


  @Test
  void search_200_emptyOk() throws Exception {
    String body = """
    {
      "page": { "number": 1, "limit": 10 },
      "query": { }
    }
    """;

    mvc.perform(post("/api/lendings/search")
            .contentType(MediaType.APPLICATION_JSON)
            .content(body))
        .andExpect(status().isOk())
        .andExpect(content().contentTypeCompatibleWith("application/json"))
        .andExpect(jsonPath("$.items").exists());
  }

  @Test
  void create_201_returnsLocationETag_andBodyMapped() throws Exception {
    var a = author("Robert Martin");
    var savedBook = book("9780306406157", "Clean Code", "D", List.of(a));

    var readerUser = persistReaderUser("reader1@example.com", "Reader One");
    var rd = persistReaderDetails(readerUser, 12345);
    var readerNumber = rd.getReaderNumber();

    String body = """
      {
        "isbn": "%s",
        "readerNumber": "%s"
      }
      """.formatted(savedBook.getIsbn(), readerNumber);

    mvc.perform(post("/api/lendings")
            .contentType(MediaType.APPLICATION_JSON)
            .content(body.getBytes(StandardCharsets.UTF_8)))
        .andExpect(status().isCreated())
        .andExpect(header().string("ETag", not(isEmptyOrNullString())))
        .andExpect(header().string("Location", containsString("/api/lendings/")))
        .andExpect(content().contentTypeCompatibleWith("application/hal+json"))
        .andExpect(jsonPath("$.lendingNumber", not(isEmptyOrNullString())))
        .andExpect(jsonPath("$.bookTitle").value("Clean Code"))
        .andExpect(jsonPath("$.startDate", notNullValue()))
        .andExpect(jsonPath("$.limitDate", notNullValue()));
  }

  @Test
  void findByLendingNumber_200_asLibrarian_skipsReaderCheck() throws Exception {
    var a = author("Eric Ries");
    var savedBook = book("9781861972712", "The Lean Startup", "D", List.of(a));

    var readerUser = persistReaderUser("reader2@example.com", "Reader Two");
    var rd = persistReaderDetails(readerUser, 22222);

    String createBody = """
      { "isbn": "%s", "readerNumber": "%s" }
      """.formatted(savedBook.getIsbn(), rd.getReaderNumber());

    MvcResult created = mvc.perform(post("/api/lendings")
            .contentType(MediaType.APPLICATION_JSON)
            .content(createBody))
        .andExpect(status().isCreated())
        .andReturn();

    JsonNode json = om.readTree(created.getResponse().getContentAsString());
    String lendingNumber = json.get("lendingNumber").asText();
    String[] parts = lendingNumber.split("/");
    String year = parts[0];
    String seq = parts[1];

    var librarian = persistLibrarianUser("lib@example.com", "Lib Rarian");
    String sub = librarian.getId() + "," + librarian.getUsername();

    mvc.perform(get("/api/lendings/{year}/{seq}", year, seq)
            .with(jwt().jwt(j -> j.claim("sub", sub))))
        .andExpect(status().isOk())
        .andExpect(header().string("ETag", not(isEmptyOrNullString())))
        .andExpect(content().contentTypeCompatibleWith("application/hal+json"))
        .andExpect(jsonPath("$.lendingNumber").value(lendingNumber))
        .andExpect(jsonPath("$.bookTitle").value("The Lean Startup"));
  }

  @Test
  void findByLendingNumber_200_asOwningReader_allowed() throws Exception {
    var a = author("Kent Beck");
    var savedBook = book("9780321146533", "Test-Driven Development", "D", List.of(a));

    var readerUser = persistReaderUser("owner@example.com", "Owner Reader");
    var rd = persistReaderDetails(readerUser, 33333);

    String body = """
      { "isbn": "%s", "readerNumber": "%s" }
      """.formatted(savedBook.getIsbn(), rd.getReaderNumber());

    MvcResult created = mvc.perform(post("/api/lendings")
            .contentType(MediaType.APPLICATION_JSON)
            .content(body))
        .andExpect(status().isCreated())
        .andReturn();

    JsonNode json = om.readTree(created.getResponse().getContentAsString());
    String ln = json.get("lendingNumber").asText();
    String[] parts = ln.split("/");
    String year = parts[0];
    String seq = parts[1];

    String sub = readerUser.getId() + "," + readerUser.getUsername();

    mvc.perform(get("/api/lendings/{year}/{seq}", year, seq)
            .with(jwt().jwt(j -> j.claim("sub", sub))))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.lendingNumber").value(ln))
        .andExpect(jsonPath("$.bookTitle").value("Test-Driven Development"));
  }

  @Test
  void findByLendingNumber_403_asDifferentReader_forbidden() throws Exception {
    var a = author("Martin Fowler");
    var savedBook = book("9780321127426", "Refactoring", "D", List.of(a));

    var ownerUser = persistReaderUser("owner2@example.com", "Owner Two");
    var ownerDetails = persistReaderDetails(ownerUser, 44444);

    String body = """
      { "isbn": "%s", "readerNumber": "%s" }
      """.formatted(savedBook.getIsbn(), ownerDetails.getReaderNumber());

    MvcResult created = mvc.perform(post("/api/lendings")
            .contentType(MediaType.APPLICATION_JSON)
            .content(body))
        .andExpect(status().isCreated())
        .andReturn();

    JsonNode json = om.readTree(created.getResponse().getContentAsString());
    String ln = json.get("lendingNumber").asText();
    String[] parts = ln.split("/");
    String year = parts[0];
    String seq = parts[1];

    var otherUser = persistReaderUser("other@example.com", "Other Reader");
    persistReaderDetails(otherUser, 55555);
    String sub = otherUser.getId() + "," + otherUser.getUsername();

    mvc.perform(get("/api/lendings/{year}/{seq}", year, seq)
            .with(jwt().jwt(j -> j.claim("sub", sub))))
        .andExpect(status().isForbidden());
  }
}
