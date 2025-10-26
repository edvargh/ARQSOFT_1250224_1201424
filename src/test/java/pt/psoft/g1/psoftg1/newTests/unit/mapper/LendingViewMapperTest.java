package pt.psoft.g1.psoftg1.newTests.unit.mapper;

import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import pt.psoft.g1.psoftg1.bookmanagement.model.Title;
import pt.psoft.g1.psoftg1.lendingmanagement.api.LendingViewMapper;
import pt.psoft.g1.psoftg1.lendingmanagement.model.Lending;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Functional opaque-box test for LendingViewMapper.
 */
class LendingViewMapperTest {

  private LendingViewMapper mapper;

  @BeforeEach
  void bindRequestContext() {
    MockHttpServletRequest req = new MockHttpServletRequest();
    req.setScheme("http");
    req.setServerName("example.org");
    req.setServerPort(8080);
    RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(req));

    mapper = Mappers.getMapper(LendingViewMapper.class);
    assertNotNull(mapper);
  }

  @Test
  void toLendingView_maps_fields_and_builds_links() {
    var book = mock(pt.psoft.g1.psoftg1.bookmanagement.model.Book.class);
    when(book.getTitle()).thenReturn(new Title("Clean Code"));
    when(book.getIsbn()).thenReturn("9780132350884");

    var reader = mock(pt.psoft.g1.psoftg1.readermanagement.model.ReaderDetails.class);
    when(reader.getReaderNumber()).thenReturn("R-7");

    var lending = mock(Lending.class, RETURNS_DEEP_STUBS);
    when(lending.getLendingNumber()).thenReturn("L-123");
    when(lending.getBook()).thenReturn(book);
    when(lending.getReaderDetails()).thenReturn(reader);
    when(lending.getFineValueInCents()).thenReturn(Optional.of(250));
    LocalDate returned = LocalDate.of(2024, 5, 10);
    when(lending.getReturnedDate()).thenReturn(returned);

    Object view = mapper.toLendingView(lending);

    assertEquals("L-123", extract(view, "lendingNumber"));
    assertEquals("Clean Code", String.valueOf(extract(view, "bookTitle")));
    assertEquals(250, extract(view, "fineValueInCents"));
    assertEquals(returned, extract(view, "returnedDate"));

    Object links = extract(view, "_links");
    assertNotNull(links, "_links should be present");

    assertEquals("http://example.org:8080/api/lendings/L-123", hrefOf(extract(links, "self")));
    assertEquals("http://example.org:8080/api/books/9780132350884", hrefOf(extract(links, "book")));
    assertEquals("http://example.org:8080/api/readers/R-7", hrefOf(extract(links, "reader")));
  }

  @SuppressWarnings("unchecked")
  private String hrefOf(Object link) {
    if (link == null) return null;
    if (link instanceof Map<?,?> m) {
      Object href = m.get("href");
      return href == null ? null : String.valueOf(href);
    }
    return String.valueOf(link);
  }

  @Test
  void toLendingView_sets_fineValue_null_when_optional_empty() {
    var b = mock(pt.psoft.g1.psoftg1.bookmanagement.model.Book.class);
    when(b.getTitle()).thenReturn(new Title("Some Book"));
    when(b.getIsbn()).thenReturn("0123456789X");

    var lending = mock(Lending.class, RETURNS_DEEP_STUBS);
    when(lending.getLendingNumber()).thenReturn("L-9");
    when(lending.getBook()).thenReturn(b);
    when(lending.getReaderDetails().getReaderNumber()).thenReturn("R-1");
    when(lending.getFineValueInCents()).thenReturn(Optional.empty());
    when(lending.getReturnedDate()).thenReturn(null);

    Object view = mapper.toLendingView(lending);

    assertNull(extract(view, "fineValueInCents"));
  }

  @Test
  void toLendingView_list_maps_each_item() {
    var b1 = mock(pt.psoft.g1.psoftg1.bookmanagement.model.Book.class);
    when(b1.getTitle()).thenReturn(new Title("B1"));
    when(b1.getIsbn()).thenReturn("1111111111111");
    var l1 = mock(Lending.class, RETURNS_DEEP_STUBS);
    when(l1.getLendingNumber()).thenReturn("L1");
    when(l1.getBook()).thenReturn(b1);
    when(l1.getReaderDetails().getReaderNumber()).thenReturn("R1");
    when(l1.getFineValueInCents()).thenReturn(Optional.of(10));

    var b2 = mock(pt.psoft.g1.psoftg1.bookmanagement.model.Book.class);
    when(b2.getTitle()).thenReturn(new Title("B2"));
    when(b2.getIsbn()).thenReturn("2222222222222");
    var l2 = mock(Lending.class, RETURNS_DEEP_STUBS);
    when(l2.getLendingNumber()).thenReturn("L2");
    when(l2.getBook()).thenReturn(b2);
    when(l2.getReaderDetails().getReaderNumber()).thenReturn("R2");
    when(l2.getFineValueInCents()).thenReturn(Optional.empty());

    List<Lending> in = List.of(l1, l2);
    List<?> out = mapper.toLendingView(in);

    assertEquals(2, out.size());
    assertEquals("L1", extract(out.get(0), "lendingNumber"));
    assertEquals("L2", extract(out.get(1), "lendingNumber"));
    assertEquals("B1", String.valueOf(extract(out.get(0), "bookTitle")));
    assertEquals("B2", String.valueOf(extract(out.get(1), "bookTitle")));
  }

  @Test
  void toLendingsAverageDurationView_maps_single_value() {
    Object view = mapper.toLendingsAverageDurationView(3.5);

    Object val = firstNonNull(
        extractOrNull(view, "lendingsAverageDuration"),
        extractOrNull(view, "averageDuration"),
        extractOrNull(view, "value")
    );

    assertNotNull(val);
    assertEquals(3.5, (val instanceof Number n) ? n.doubleValue() : val);
  }


  private Object firstNonNull(Object... vals) {
    for (Object v : vals) if (v != null) return v;
    return null;
  }

  private Object extractOrNull(Object dto, String prop) {
    try { return extract(dto, prop); } catch (AssertionError e) { return null; }
  }

  private Object extract(Object dto, String prop) {
    String accessor = prop;
    String getter = "get" + capitalize(prop);
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

  private String capitalize(String prop) {
    if (prop == null || prop.isEmpty()) return prop;
    if (prop.startsWith("_")) return "_" + capitalize(prop.substring(1));
    return Character.toUpperCase(prop.charAt(0)) + prop.substring(1);
  }
}
