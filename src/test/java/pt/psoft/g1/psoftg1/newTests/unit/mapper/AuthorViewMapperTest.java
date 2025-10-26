package pt.psoft.g1.psoftg1.newTests.unit.mapper;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import pt.psoft.g1.psoftg1.authormanagement.api.AuthorViewMapper;
import pt.psoft.g1.psoftg1.authormanagement.api.AuthorView;
import pt.psoft.g1.psoftg1.bookmanagement.api.BookShortView;
import pt.psoft.g1.psoftg1.authormanagement.model.Author;
import pt.psoft.g1.psoftg1.bookmanagement.model.Book;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Functional opaque-box tests for AuthorViewMapper.
 */
class AuthorViewMapperTest {

  private AuthorViewMapper mapper;

  @BeforeEach
  void bindRequestContext() {
    MockHttpServletRequest req = new MockHttpServletRequest();
    req.setScheme("http");
    req.setServerName("example.org");
    req.setServerPort(8080);
    req.setContextPath("");
    RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(req));

    mapper = Mappers.getMapper(AuthorViewMapper.class);
    assertNotNull(mapper, "Mapper should be instantiated");
  }

  @Test
  void mapLinks_buildsAuthorAndBooksLinks_andPhotoLink() {
    Author author = mock(Author.class);
    when(author.getId()).thenReturn("A-1");
    when(author.getAuthorNumber()).thenReturn("A-1");

    Map<String, Object> links = mapper.mapLinks(author);

    assertNotNull(links);
    assertEquals("http://example.org:8080/api/authors/A-1", links.get("author"));
    assertEquals("http://example.org:8080/api/authors/A-1/books", links.get("booksByAuthor"));
    assertEquals("http://example.org:8080/api/authors/A-1/photo", links.get("photo"));
  }

  @Test
  void mapShortBookLink_buildsBookLinkFromIsbn() {
    Book b = mock(Book.class);
    when(b.getIsbn()).thenReturn("9780306406157");

    String link = mapper.mapShortBookLink(b);

    assertEquals("http://example.org:8080/api/books/9780306406157", link);
  }

  @Test
  void toAuthorView_populates_links_map() {
    Author author = mock(Author.class);

    when(author.getId()).thenReturn("9");
    when(author.getAuthorNumber()).thenReturn("9");

    AuthorView view = mapper.toAuthorView(author);

    Map<String, Object> links = extractLinks(view);
    assertNotNull(links, "View should contain _links");
    assertEquals("http://example.org:8080/api/authors/9", links.get("author"));
    assertEquals("http://example.org:8080/api/authors/9/books", links.get("booksByAuthor"));
    assertEquals("http://example.org:8080/api/authors/9/photo", links.get("photo"));
  }

  @Test
  void toBookShortView_populates_links_string() {
    Book b = mock(Book.class);
    when(b.getIsbn()).thenReturn("0123456789X");

    BookShortView out = mapper.toBookShortView(b);

    Object links = extractLinksObject(out);
    assertTrue(links instanceof String, "BookShortView _links should be a String");
    assertEquals("http://example.org:8080/api/books/0123456789X", links);
  }

  @SuppressWarnings("unchecked")
  private Map<String, Object> extractLinks(Object dto) {
    Object o = extractLinksObject(dto);
    assertTrue(o instanceof Map, "Expected _links to be a Map");
    return (Map<String, Object>) o;
  }

  private Object extractLinksObject(Object dto) {
    try {
      Method m = dto.getClass().getMethod("_links");
      return m.invoke(dto);
    } catch (NoSuchMethodException ignored) {
      try {
        Method m2 = dto.getClass().getMethod("get_links");
        return m2.invoke(dto);
      } catch (Exception ignored2) {
        try {
          Field f = dto.getClass().getDeclaredField("_links");
          f.setAccessible(true);
          return f.get(dto);
        } catch (Exception e) {
          fail("Could not access _links in " + dto.getClass().getName() + ": " + e.getMessage());
          return null;
        }
      }
    } catch (Exception e) {
      fail("Error invoking _links accessor: " + e.getMessage());
      return null;
    }
  }
}
