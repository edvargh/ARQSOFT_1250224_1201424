package pt.psoft.g1.psoftg1.newTests.unit.mapper;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import pt.psoft.g1.psoftg1.authormanagement.model.Author;
import pt.psoft.g1.psoftg1.bookmanagement.api.BookCountView;
import pt.psoft.g1.psoftg1.bookmanagement.api.BookView;
import pt.psoft.g1.psoftg1.bookmanagement.api.BookViewMapper;
import pt.psoft.g1.psoftg1.bookmanagement.model.Book;
import pt.psoft.g1.psoftg1.bookmanagement.services.BookCountDTO;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Functional opaque-box tests for BookViewMapper.
 */
class BookViewMapperTest {

  private BookViewMapper mapper;

  @BeforeEach
  void bindRequestContext() {
    MockHttpServletRequest req = new MockHttpServletRequest();
    req.setScheme("http");
    req.setServerName("example.org");
    req.setServerPort(8080);
    req.setContextPath("");
    RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(req));

    mapper = Mappers.getMapper(BookViewMapper.class);
    assertNotNull(mapper, "Mapper should be instantiated");
  }

  @Test
  void mapLinks_builds_self_photo_and_author_links() {
    Author a1 = mock(Author.class);
    when(a1.getAuthorNumber()).thenReturn("42");

    Author a2 = mock(Author.class);
    when(a2.getAuthorNumber()).thenReturn("7");

    Book book = mock(Book.class);
    when(book.getIsbn()).thenReturn("9780306406157");
    when(book.getAuthors()).thenReturn(List.of(a1, a2));

    Map<String, Object> links = mapper.mapLinks(book);

    assertEquals("http://example.org:8080/api/books/9780306406157", links.get("self"));
    assertEquals("http://example.org:8080/api/books/9780306406157/photo", links.get("photo"));

    Object authorsObj = links.get("authors");
    assertTrue(authorsObj instanceof List, "authors link should be a list");
    @SuppressWarnings("unchecked")
    List<Map<String, String>> authorLinks = (List<Map<String, String>>) authorsObj;
    assertEquals(2, authorLinks.size());
    assertEquals("http://example.org:8080/api/authors/42", authorLinks.get(0).get("href"));
    assertEquals("http://example.org:8080/api/authors/7", authorLinks.get(1).get("href"));
  }

  @Test
  void toBookView_populates_links_and_author_names() {
    Author a1 = mock(Author.class);
    when(a1.getAuthorNumber()).thenReturn("101");
    when(a1.getName()).thenReturn("Alice");

    Author a2 = mock(Author.class);
    when(a2.getAuthorNumber()).thenReturn("202");
    when(a2.getName()).thenReturn("Bob");

    Book book = mock(Book.class);
    when(book.getIsbn()).thenReturn("0123456789X");
    when(book.getAuthors()).thenReturn(List.of(a1, a2));

    BookView view = mapper.toBookView(book);

    Map<String, Object> links = extractMapProperty(view, "_links");
    assertEquals("http://example.org:8080/api/books/0123456789X", links.get("self"));
    assertEquals("http://example.org:8080/api/books/0123456789X/photo", links.get("photo"));

    @SuppressWarnings("unchecked")
    List<Map<String, String>> authorLinks = (List<Map<String, String>>) links.get("authors");
    assertEquals(2, authorLinks.size());
    assertEquals("http://example.org:8080/api/authors/101", authorLinks.get(0).get("href"));
    assertEquals("http://example.org:8080/api/authors/202", authorLinks.get(1).get("href"));

    @SuppressWarnings("unchecked")
    List<String> names = (List<String>) extractProperty(view, "authors");
    assertEquals(List.of("Alice", "Bob"), names);
  }

  @Test
  void toBookCountView_wraps_bookView_and_keeps_links_in_nested_view() {
    Author a = mock(Author.class);
    when(a.getAuthorNumber()).thenReturn("9");
    when(a.getName()).thenReturn("Jane");

    Book book = mock(Book.class);
    when(book.getIsbn()).thenReturn("9789999999999");
    when(book.getAuthors()).thenReturn(List.of(a));

    BookCountDTO dto = mock(BookCountDTO.class);
    when(dto.getBook()).thenReturn(book);
    when(dto.getLendingCount()).thenReturn(5L);

    BookCountView out = mapper.toBookCountView(dto);

    Object nested = extractProperty(out, "bookView");
    assertNotNull(nested, "BookCountView should contain a nested bookView");

    Map<String, Object> links = extractMapProperty(nested, "_links");
    assertEquals("http://example.org:8080/api/books/9789999999999", links.get("self"));
    assertEquals("http://example.org:8080/api/books/9789999999999/photo", links.get("photo"));

    @SuppressWarnings("unchecked")
    List<Map<String, String>> authorLinks = (List<Map<String, String>>) links.get("authors");
    assertEquals(1, authorLinks.size());
    assertEquals("http://example.org:8080/api/authors/9", authorLinks.get(0).get("href"));

    @SuppressWarnings("unchecked")
    List<String> names = (List<String>) extractProperty(nested, "authors");
    assertEquals(List.of("Jane"), names);
  }

  /** Extracts a property that is expected to be a Map (e.g., "_links") from a DTO via accessor/field. */
  @SuppressWarnings("unchecked")
  private Map<String, Object> extractMapProperty(Object dto, String prop) {
    Object o = extractProperty(dto, prop);
    assertTrue(o instanceof Map, "Expected " + prop + " to be a Map");
    return (Map<String, Object>) o;
  }

  /** Extracts a property from a DTO trying: record accessor, bean getter, then field access. */
  private Object extractProperty(Object dto, String prop) {
    String accessor = prop;
    String getter = "get" + capitalizeForBean(prop);
    try {
      Method m = dto.getClass().getMethod(accessor);
      return m.invoke(dto);
    } catch (NoSuchMethodException ignored) {
      try {
        Method m2 = dto.getClass().getMethod(getter);
        return m2.invoke(dto);
      } catch (NoSuchMethodException ignored2) {
        try {
          Field f = dto.getClass().getDeclaredField(prop);
          f.setAccessible(true);
          return f.get(dto);
        } catch (Exception e) {
          fail("Could not access property '" + prop + "' on " + dto.getClass().getName() + ": " + e.getMessage());
          return null;
        }
      } catch (Exception e2) {
        fail("Error invoking getter '" + getter + "': " + e2.getMessage());
        return null;
      }
    } catch (Exception e) {
      fail("Error invoking accessor '" + accessor + "': " + e.getMessage());
      return null;
    }
  }

  private String capitalizeForBean(String prop) {
    if (prop == null || prop.isEmpty()) return prop;
    if (prop.startsWith("_")) return "_" + capitalizeForBean(prop.substring(1));
    return Character.toUpperCase(prop.charAt(0)) + prop.substring(1);
  }
}
