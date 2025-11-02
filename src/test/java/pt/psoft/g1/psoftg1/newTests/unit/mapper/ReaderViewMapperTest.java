package pt.psoft.g1.psoftg1.newTests.unit.mapper;

import org.junit.jupiter.api.*;
import org.mapstruct.factory.Mappers;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import pt.psoft.g1.psoftg1.genremanagement.model.Genre;
import pt.psoft.g1.psoftg1.readermanagement.api.ReaderViewMapper;
import pt.psoft.g1.psoftg1.readermanagement.api.ReaderView;
import pt.psoft.g1.psoftg1.readermanagement.model.ReaderDetails;
import pt.psoft.g1.psoftg1.shared.model.Name;
import pt.psoft.g1.psoftg1.usermanagement.model.Reader;
import pt.psoft.g1.psoftg1.readermanagement.services.ReaderBookCountDTO;

import java.lang.reflect.Method;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Functional opaque-box test for ReaderViewMapper.
 */
class ReaderViewMapperTest {

  private ReaderViewMapper mapper;

  @BeforeEach
  void setUp() {
    MockHttpServletRequest req = new MockHttpServletRequest();
    req.setScheme("http");
    req.setServerName("example.org");
    req.setServerPort(8080);
    RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(req));

    mapper = Mappers.getMapper(ReaderViewMapper.class);
  }

  @AfterEach
  void tearDown() {
    RequestContextHolder.resetRequestAttributes();
  }

  @Test
  void toReaderView_maps_all_fields_and_generates_photo_url() {
    Genre g1 = mock(Genre.class); when(g1.getGenre()).thenReturn("Fantasy");
    Genre g2 = mock(Genre.class); when(g2.getGenre()).thenReturn("Sci-Fi");

    Reader reader = mock(Reader.class);
    when(reader.getName()).thenReturn(new Name("John Doe"));
    when(reader.getUsername()).thenReturn("john@example.com");

    var birth = new pt.psoft.g1.psoftg1.readermanagement.model.BirthDate() {
      {
        try {
          var f = pt.psoft.g1.psoftg1.readermanagement.model.BirthDate.class.getDeclaredField("birthDate");
          f.setAccessible(true);
          f.set(this, LocalDate.of(1990, 5, 15));
        } catch (Exception ignored) {}
      }
      @Override public LocalDate getBirthDate() { return LocalDate.of(1990, 5, 15); }
    };

    ReaderDetails details = mock(ReaderDetails.class);
    when(details.getReader()).thenReturn(reader);
    when(details.getReaderNumber()).thenReturn("2024/17");
    when(details.getBirthDate()).thenReturn(birth);
    when(details.getPhoneNumber()).thenReturn("123456789");
    when(details.isGdprConsent()).thenReturn(true);
    when(details.getInterestList()).thenReturn(List.of(g1, g2));

    ReaderView view = mapper.toReaderView(details);

    assertEquals("John Doe", view.getFullName());
    assertEquals("john@example.com", view.getEmail());
    assertEquals("1990-05-15", String.valueOf(view.getBirthDate()));
    assertEquals("123456789", view.getPhoneNumber());
    assertTrue(view.isGdprConsent());
    assertEquals("2024/17", view.getReaderNumber());
    assertEquals(List.of("Fantasy", "Sci-Fi"), view.getInterestList());
    assertTrue(view.getPhoto().endsWith("/api/readers/2024/17/photo"),
        "Photo should end with /api/readers/2024/17/photo but was: " + view.getPhoto());
  }

  @Test
  void toReaderView_null_interestList_maps_to_empty_list() {
    Reader reader = mock(Reader.class);
    when(reader.getName()).thenReturn(new Name("Jane"));
    when(reader.getUsername()).thenReturn("jane@example.com");

    var birth = new pt.psoft.g1.psoftg1.readermanagement.model.BirthDate() {
      { try {
        var f = pt.psoft.g1.psoftg1.readermanagement.model.BirthDate.class.getDeclaredField("birthDate");
        f.setAccessible(true);
        f.set(this, LocalDate.of(1985, 1, 1));
      } catch (Exception ignored) {}
      }
      @Override public LocalDate getBirthDate() { return LocalDate.of(1985, 1, 1); }
    };

    ReaderDetails details = mock(ReaderDetails.class);
    when(details.getReader()).thenReturn(reader);
    when(details.getReaderNumber()).thenReturn("2023/1");
    when(details.getBirthDate()).thenReturn(birth);
    when(details.getPhoneNumber()).thenReturn("912345678");
    when(details.isGdprConsent()).thenReturn(true);
    when(details.getInterestList()).thenReturn(null);

    ReaderView view = mapper.toReaderView(details);

    assertNotNull(view.getInterestList());
    assertTrue(view.getInterestList().isEmpty());
  }

  @Test
  void toReaderCountView_wraps_reader_into_nested_readerView_and_preserves_count() {
    Name name = new Name("Alice");

    Reader reader = mock(Reader.class);
    when(reader.getName()).thenReturn(name);
    when(reader.getUsername()).thenReturn("alice@example.com");

    ReaderDetails details = mock(ReaderDetails.class);
    when(details.getReader()).thenReturn(reader);
    when(details.getReaderNumber()).thenReturn("2022/9");

    ReaderBookCountDTO dto = new ReaderBookCountDTO(details, 7L);

    Object out = mapper.toReaderCountView(dto);

    Object nested = extract(out, "readerView");
    assertNotNull(nested);

    assertEquals("Alice", extract(nested, "fullName"));
    assertEquals("alice@example.com", extract(nested, "email"));
    assertEquals("2022/9", extract(nested, "readerNumber"));

    Object count = tryGet(out, "getBookCount");
    if (count == null) count = extract(out, "bookCount");
    assertEquals(7L, ((Number) count).longValue());
  }


  private static Object extract(Object bean, String prop) {
    if (bean == null) return null;
    String getter = "get" + Character.toUpperCase(prop.charAt(0)) + prop.substring(1);
    try {
      Method m = bean.getClass().getMethod(getter);
      return m.invoke(bean);
    } catch (Exception ignored) {
      getter = "is" + Character.toUpperCase(prop.charAt(0)) + prop.substring(1);
      try {
        Method m = bean.getClass().getMethod(getter);
        return m.invoke(bean);
      } catch (Exception ignored2) {
        try {
          var f = bean.getClass().getDeclaredField(prop);
          f.setAccessible(true);
          return f.get(bean);
        } catch (Exception e) {
          fail("Could not extract '" + prop + "' from " + bean.getClass().getName());
          return null;
        }
      }
    }
  }

  private static Object tryGet(Object bean, String method) {
    try {
      Method m = bean.getClass().getMethod(method);
      return m.invoke(bean);
    } catch (Exception e) {
      return null;
    }
  }
}
