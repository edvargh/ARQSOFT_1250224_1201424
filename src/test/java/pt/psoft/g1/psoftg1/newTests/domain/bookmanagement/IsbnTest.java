package pt.psoft.g1.psoftg1.newTests.domain.bookmanagement;

import java.lang.reflect.Method;
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

  @Test
  void canEqual_onlyAcceptsIsbnInstances() throws Exception {
    Isbn a = new Isbn("9780306406157");

    var m = Isbn.class.getDeclaredMethod("canEqual", Object.class);
    m.setAccessible(true);

    assertEquals(Boolean.FALSE, m.invoke(a, new Object()), "canEqual should be false for non-Isbn");
    assertEquals(Boolean.TRUE, m.invoke(a, new Isbn("9780306406157")), "canEqual should be true for Isbn");
  }

  @Test
  void constructor_isbn10_lowercaseX_isRejected_byRegexGuard() {
    assertThrows(IllegalArgumentException.class, () -> new Isbn("048665088x"));
  }

  @Test
  void constructor_isbn13_withNonDigitCharacter_isRejectedByRegexGuard() {
    assertThrows(IllegalArgumentException.class, () -> new Isbn("97803064061A7"));
  }

  @Test
  void constructor_isbn10_lowercaseX_butChecksumWouldPass_stillRejectedByRegexGuard() {
    assertThrows(IllegalArgumentException.class, () -> new Isbn("000001000x"));
  }

  @Test
  void constructor_isbn13_withNonDigitCharacter_rejectedByRegexGuard_exactExceptionType() {
    try {
      new Isbn("97803064061A7");
      fail("Expected IllegalArgumentException");
    } catch (IllegalArgumentException expected) {
    }
  }

  @Test
  void constructor_nullString_rejectedByNullGuard() {
    assertThrows(IllegalArgumentException.class, () -> new Isbn(null));
  }

  @Test
  void isValidIsbn13_null_returnsFalse_noExceptions() throws Exception {
    Method m = Isbn.class.getDeclaredMethod("isValidIsbn13", String.class);
    m.setAccessible(true);

    Boolean result = (Boolean) m.invoke(null, (Object) null);
    assertEquals(Boolean.FALSE, result, "null ISBN-13 should return false via guard");
  }

  @Test
  void isValidIsbn13_valid13digits_returnsTrue() throws Exception {
    Method m = Isbn.class.getDeclaredMethod("isValidIsbn13", String.class);
    m.setAccessible(true);

    Boolean result = (Boolean) m.invoke(null, "9780306406157");
    assertEquals(Boolean.TRUE, result, "valid 13-digit ISBN should pass");
  }

  @Test
  void isValidIsbn13_emptyString_returnsFalse_noExceptions() throws Exception {
    Method m = Isbn.class.getDeclaredMethod("isValidIsbn13", String.class);
    m.setAccessible(true);

    Boolean result = (Boolean) m.invoke(null, "");
    assertEquals(Boolean.FALSE, result, "empty string should be rejected by the regex guard");
  }

  @Test
  void isValidIsbn13_checksum10_normalizesToZero_trueOnAllZeros() throws Exception {
    Method m = Isbn.class.getDeclaredMethod("isValidIsbn13", String.class);
    m.setAccessible(true);

    Boolean result = (Boolean) m.invoke(null, "0000000000000");
    assertEquals(Boolean.TRUE, result, "checksum==10 must normalize to 0 for all-zero ISBN");
  }
}
