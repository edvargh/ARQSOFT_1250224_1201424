package pt.psoft.g1.psoftg1.newTests.unit.isbn;

import org.junit.jupiter.api.Test;
import pt.psoft.g1.psoftg1.bookmanagement.services.IsbnUtils;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class IsbnUtilsTest {

  @Test
  void normalize_removesNonDigitsAndUppercasesX() {
    assertNull(IsbnUtils.normalize(null));

    assertEquals("9781402894626", IsbnUtils.normalize(" 978-1-4028-9462-6 "));
    assertEquals("0306406152", IsbnUtils.normalize("0-306-40615-2"));
    assertEquals("080442957X", IsbnUtils.normalize("0 8044 2957 x"));
    assertEquals("", IsbnUtils.normalize(" -- / :: "));
  }


  @Test
  void isIsbn13_and_isIsbn10_respectExactFormats() {
    assertTrue(IsbnUtils.isIsbn13("9780306406157"));
    assertFalse(IsbnUtils.isIsbn13("978030640615"));
    assertFalse(IsbnUtils.isIsbn13("978030640615X"));

    assertTrue(IsbnUtils.isIsbn10("0306406152"));
    assertTrue(IsbnUtils.isIsbn10("080442957X"));
    assertFalse(IsbnUtils.isIsbn10("080442957x"));
    assertFalse(IsbnUtils.isIsbn10("080442957"));
    assertFalse(IsbnUtils.isIsbn10("080442957XX"));
  }

  @Test
  void looksValid_acceptsEitherIsbn10OrIsbn13() {
    assertTrue(IsbnUtils.looksValid("9780306406157"));
    assertTrue(IsbnUtils.looksValid("080442957X"));
    assertFalse(IsbnUtils.looksValid("978030640615X"));
    assertFalse(IsbnUtils.looksValid(null));
  }


  @Test
  void normalizeAndOrder_nullYieldsEmptyList() {
    assertEquals(List.of(), IsbnUtils.normalizeAndOrder(null));
  }

  @Test
  void normalizeAndOrder_filtersInvalid_deduplicates_and_putsIsbn13First() {
    List<String> in = List.of(
        "0-306-40615-2",
        "978-0306406157",
        "invalid",
        "0306406152",
        "9780306406157",
        "0 306 40615 2",
        ""
    );

    List<String> out = IsbnUtils.normalizeAndOrder(in);

    assertEquals(List.of("9780306406157", "0306406152"), out);
  }

  @Test
  void atLeastTwoAgree_countsDistinctPerList_and_sortsWithIsbn13First() {
    List<List<String>> lists = List.of(
        List.of("9780306406157", "0-306-40615-2", "foo"),
        List.of("978-0-306-40615-7", "BAR", "0306406152"),
        List.of("9780306406157", "9780306406157"),
        List.of("080442957X")
    );

    List<String> agreed = IsbnUtils.atLeastTwoAgree(lists);

    assertEquals(List.of("9780306406157", "0306406152"), agreed);
  }


  @Test
  void normalizeTitleKey_trims_lowercases_and_collapsesWhitespace() {
    assertEquals("", IsbnUtils.normalizeTitleKey(null));
    assertEquals("", IsbnUtils.normalizeTitleKey("   "));
    assertEquals("the lord of the rings",
        IsbnUtils.normalizeTitleKey("  The  Lord   of  THE  Rings  "));
    assertEquals("clean code",
        IsbnUtils.normalizeTitleKey("Clean   Code"));
  }
}
