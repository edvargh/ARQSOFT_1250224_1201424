package pt.psoft.g1.psoftg1.newTests.domain.authormanagement;

import org.junit.jupiter.api.Test;
import pt.psoft.g1.psoftg1.authormanagement.model.Bio;
import pt.psoft.g1.psoftg1.shared.model.StringUtilsCustom;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Functional transparent-box tests for Bio.
 */
class BioTest {

  private static String repeat(char c, int n) {
    return String.valueOf(c).repeat(n);
  }


  @Test
  void constructor_validPlainText_kept() {
    Bio b = new Bio("Writes books about clean code.");
    assertEquals("Writes books about clean code.", b.toString());
  }

  @Test
  void constructor_html_isSanitized() {
    String raw = "<script>x</script><b>Bold</b>";
    String expected = StringUtilsCustom.sanitizeHtml(raw);
    Bio b = new Bio(raw);
    assertEquals(expected, b.toString());
  }

  @Test
  void constructor_null_throws() {
    IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> new Bio(null));
    assertTrue(ex.getMessage().toLowerCase().contains("null"));
  }

  @Test
  void constructor_blank_throws() {
    IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> new Bio("   "));
    assertTrue(ex.getMessage().toLowerCase().contains("blank"));
  }

  @Test
  void constructor_maxLength_ok() {
    String max = repeat('a', 4096);
    Bio b = new Bio(max);
    assertEquals(max, b.toString());
  }

  @Test
  void constructor_overMax_throws() {
    String over = repeat('a', 4097);
    IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> new Bio(over));
    assertTrue(ex.getMessage().contains("4096"));
  }


  @Test
  void setBio_updatesAndSanitizes() {
    Bio b = new Bio("first");
    String raw = "<i>ok</i>";
    String expected = StringUtilsCustom.sanitizeHtml(raw);
    b.setBio(raw);
    assertEquals(expected, b.toString());
  }

  @Test
  void setBio_null_throws_and_keepsPreviousValue() {
    Bio b = new Bio("keep");
    assertThrows(IllegalArgumentException.class, () -> b.setBio(null));
    assertEquals("keep", b.toString());
  }

  @Test
  void setBio_blank_throws_and_keepsPreviousValue() {
    Bio b = new Bio("keep");
    assertThrows(IllegalArgumentException.class, () -> b.setBio("   "));
    assertEquals("keep", b.toString());
  }

  @Test
  void setBio_overMax_throws_and_keepsPreviousValue() {
    Bio b = new Bio("keep");
    assertThrows(IllegalArgumentException.class, () -> b.setBio(repeat('a', 4097)));
    assertEquals("keep", b.toString());
  }
}
