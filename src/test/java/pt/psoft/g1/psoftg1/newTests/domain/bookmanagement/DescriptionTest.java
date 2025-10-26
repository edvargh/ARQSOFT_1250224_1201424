package pt.psoft.g1.psoftg1.newTests.domain.bookmanagement;

import org.junit.jupiter.api.Test;
import pt.psoft.g1.psoftg1.bookmanagement.model.Description;
import pt.psoft.g1.psoftg1.shared.model.StringUtilsCustom;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Functional transparent-box tests for Description.
 */
class DescriptionTest {

  private static String repeat(char c, int n) {
    return String.valueOf(c).repeat(n);
  }

  @Test
  void constructor_null_setsNull() {
    Description d = new Description(null);
    assertNull(d.toString());
  }

  @Test
  void constructor_blank_setsNull() {
    Description d = new Description("   ");
    assertNull(d.toString());
  }

  @Test
  void constructor_valid_plainText_kept() {
    Description d = new Description("Nice book");
    assertEquals("Nice book", d.toString());
  }

  @Test
  void constructor_valid_html_getsSanitized() {
    String raw = "<script>alert('x')</script><b>Bold</b>";
    String expected = StringUtilsCustom.sanitizeHtml(raw);
    Description d = new Description(raw);
    assertEquals(expected, d.toString());
  }

  @Test
  void constructor_maxLength_ok() {
    String max = repeat('a', 4096);
    Description d = new Description(max);
    assertEquals(max, d.toString());
  }

  @Test
  void constructor_overMax_throws() {
    String over = repeat('a', 4097);
    IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
        () -> new Description(over));
    assertTrue(ex.getMessage().contains("4096"));
  }


  @Test
  void setDescription_toNull_clearsValue() {
    Description d = new Description("something");
    d.setDescription(null);
    assertNull(d.toString());
  }

  @Test
  void setDescription_toBlank_clearsValue() {
    Description d = new Description("something");
    d.setDescription("   ");
    assertNull(d.toString());
  }

  @Test
  void setDescription_updatesWithSanitizedValue() {
    Description d = new Description("first");
    String raw = "<i>ok</i>";
    String expected = StringUtilsCustom.sanitizeHtml(raw);
    d.setDescription(raw);
    assertEquals(expected, d.toString());
  }

  @Test
  void setDescription_overMax_throwsAndLeavesPreviousValueUntouched() {
    Description d = new Description("keep-me");
    String over = repeat('a', 4097);
    assertThrows(IllegalArgumentException.class, () -> d.setDescription(over));
    assertEquals("keep-me", d.toString());
  }
}
