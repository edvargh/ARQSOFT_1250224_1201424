package pt.psoft.g1.psoftg1.newTests.domain.bookmanagement;

import org.junit.jupiter.api.Test;
import pt.psoft.g1.psoftg1.bookmanagement.model.Title;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Functional transparent-box tests for Title.
 */
class TitleTest {

  private static String repeat(char c, int n) {
    return String.valueOf(c).repeat(n);
  }


  @Test
  void constructor_validTitle_buildsSuccessfully() {
    Title t = new Title("Clean Code");
    assertEquals("Clean Code", t.toString());
  }

  @Test
  void constructor_null_throws() {
    IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> new Title(null));
    assertTrue(ex.getMessage().contains("null"));
  }

  @Test
  void constructor_blank_throws() {
    IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> new Title("   "));
    assertTrue(ex.getMessage().toLowerCase().contains("blank"));
  }

  @Test
  void constructor_overMaxLength_throws() {
    String tooLong = repeat('a', 129);
    IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> new Title(tooLong));
    assertTrue(ex.getMessage().contains("128"));
  }

  @Test
  void constructor_withLeadingAndTrailingSpaces_stripsWhitespace() {
    Title t = new Title("   The Pragmatic Programmer   ");
    assertEquals("The Pragmatic Programmer", t.toString());
  }


  @Test
  void setTitle_valid_updatesValue() {
    Title t = new Title("Old Title");
    t.setTitle("New Title");
    assertEquals("New Title", t.toString());
  }

  @Test
  void setTitle_null_throwsAndKeepsOldValue() {
    Title t = new Title("Original");
    assertThrows(IllegalArgumentException.class, () -> t.setTitle(null));
    assertEquals("Original", t.toString());
  }

  @Test
  void setTitle_blank_throwsAndKeepsOldValue() {
    Title t = new Title("Original");
    assertThrows(IllegalArgumentException.class, () -> t.setTitle("  "));
    assertEquals("Original", t.toString());
  }

  @Test
  void setTitle_overMax_throwsAndKeepsOldValue() {
    Title t = new Title("Original");
    String over = repeat('a', 129);
    assertThrows(IllegalArgumentException.class, () -> t.setTitle(over));
    assertEquals("Original", t.toString());
  }


  @Test
  void toString_returnsRawTitleValue() {
    Title t = new Title("Refactoring");
    assertEquals("Refactoring", t.toString());
  }
}
