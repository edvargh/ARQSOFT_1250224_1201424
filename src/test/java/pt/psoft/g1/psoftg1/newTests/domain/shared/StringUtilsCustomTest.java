package pt.psoft.g1.psoftg1.newTests.domain.shared;

import org.junit.jupiter.api.Test;
import pt.psoft.g1.psoftg1.shared.model.StringUtilsCustom;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Functional transparent-box tests for StringUtilsCustom.
 */
class StringUtilsCustomTest {

  @Test
  void isAlphanumeric_allowsLettersDigitsSpaceHyphenApostrophe_andAccents() {
    assertTrue(StringUtilsCustom.isAlphanumeric("John Doe"));
    assertTrue(StringUtilsCustom.isAlphanumeric("O'Connor"));
    assertTrue(StringUtilsCustom.isAlphanumeric("Mary-Jane 2"));
    assertTrue(StringUtilsCustom.isAlphanumeric("Álvaro Núñez 123"));
  }

  @Test
  void isAlphanumeric_rejectsOtherPunctuation() {
    assertFalse(StringUtilsCustom.isAlphanumeric("John_Doe"));
    assertFalse(StringUtilsCustom.isAlphanumeric("John.Doe"));
    assertFalse(StringUtilsCustom.isAlphanumeric("Jane@Home"));
    assertFalse(StringUtilsCustom.isAlphanumeric("Hello#World"));
  }

  @Test
  void startsOrEndsInWhiteSpace_trueWhenNoLeadingOrTrailingSpace() {
    assertTrue(StringUtilsCustom.startsOrEndsInWhiteSpace("hello"));
    assertTrue(StringUtilsCustom.startsOrEndsInWhiteSpace("a b"));
  }

  @Test
  void startsOrEndsInWhiteSpace_falseWhenLeadingOrTrailingSpace() {
    assertFalse(StringUtilsCustom.startsOrEndsInWhiteSpace(" hello"));
    assertFalse(StringUtilsCustom.startsOrEndsInWhiteSpace("hello "));
    assertFalse(StringUtilsCustom.startsOrEndsInWhiteSpace(" hello "));
  }

  @Test
  void sanitizeHtml_removesScripts_keepsBasicFormattingAndLinks() {
    String unsafe = "<b>Hi</b> <script>alert('x')</script> <a href=\"http://x\">link</a>";
    String sanitized = StringUtilsCustom.sanitizeHtml(unsafe);

    assertTrue(sanitized.contains("<b>Hi</b>"));
    assertTrue(sanitized.contains("<a"));
    assertFalse(sanitized.contains("<script"));
  }

  @Test
  void sanitizeHtml_plainTextUnchanged() {
    String s = "just text 123";
    assertEquals(s, StringUtilsCustom.sanitizeHtml(s));
  }

  @Test
  void isAlphanumeric_emptyString_isAllowedByPattern() {
    assertTrue(StringUtilsCustom.isAlphanumeric(""));
  }

  @Test
  void isAlphanumeric_rejectsEmojiAndSymbols() {
    assertFalse(StringUtilsCustom.isAlphanumeric("Alice 😀"));
    assertFalse(StringUtilsCustom.isAlphanumeric("§ection"));
  }

  @Test
  void startsOrEndsInWhiteSpace_tabsNewlinesAreNotTreatedAsSpaces_andEmptyStringIsOk() {
    assertTrue(StringUtilsCustom.startsOrEndsInWhiteSpace("\tindented"));
    assertTrue(StringUtilsCustom.startsOrEndsInWhiteSpace("line\n"));
    assertTrue(StringUtilsCustom.startsOrEndsInWhiteSpace(""));
  }
}
