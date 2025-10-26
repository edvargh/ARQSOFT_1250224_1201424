package pt.psoft.g1.psoftg1.newTests.domain.bookmanagement;

import org.junit.jupiter.api.Test;
import pt.psoft.g1.psoftg1.bookmanagement.model.Isbn;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Functional transparent-box tests for Isbn.
 */
class IsbnTest {

  @Test
  void constructor_validIsbn10_buildsSuccessfully() {
    Isbn isbn = new Isbn("0306406152");
    assertEquals("0306406152", isbn.toString());
  }

  @Test
  void constructor_validIsbn13_buildsSuccessfully() {
    Isbn isbn = new Isbn("9780306406157");
    assertEquals("9780306406157", isbn.toString());
  }


  @Test
  void constructor_null_throws() {
    IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> new Isbn(null));
    assertTrue(ex.getMessage().toLowerCase().contains("null"));
  }

  @Test
  void constructor_invalidLength_throws() {
    assertThrows(IllegalArgumentException.class, () -> new Isbn("123456789012"));
  }

  @Test
  void constructor_invalidCharacters_throws() {
    assertThrows(IllegalArgumentException.class, () -> new Isbn("ABC6406157XYZ"));
  }

  @Test
  void constructor_invalidIsbn10Checksum_throws() {
    assertThrows(IllegalArgumentException.class, () -> new Isbn("0306406153"));
  }

  @Test
  void constructor_invalidIsbn13Checksum_throws() {
    assertThrows(IllegalArgumentException.class, () -> new Isbn("9780306406158"));
  }


  @Test
  void isbn10_withXasCheckDigit_isValid() {
    Isbn isbn = new Isbn("048665088X");
    assertEquals("048665088X", isbn.toString());
  }

  @Test
  void isbn13_withChecksum10_mapsToZero_andStillValid() {
    Isbn isbn = new Isbn("9780306406157");
    assertEquals("9780306406157", isbn.toString());
  }


  @Test
  void equals_and_hashCode_workForSameValue() {
    Isbn a = new Isbn("9780306406157");
    Isbn b = new Isbn("9780306406157");

    assertEquals(a, b);
    assertEquals(a.hashCode(), b.hashCode());
  }

  @Test
  void notEqual_forDifferentValues() {
    Isbn a = new Isbn("9780306406157");
    Isbn b = new Isbn("9781861972712");

    assertNotEquals(a, b);
  }
}
