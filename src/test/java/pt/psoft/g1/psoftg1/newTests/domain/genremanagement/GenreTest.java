package pt.psoft.g1.psoftg1.newTests.domain.genremanagement;

import org.junit.jupiter.api.Test;
import pt.psoft.g1.psoftg1.genremanagement.model.Genre;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Functional transparent-box tests for Genre.
 */
class GenreTest {

  private static String repeat(char c, int n) {
    return String.valueOf(c).repeat(n);
  }


  @Test
  void constructor_valid_builds() {
    Genre g = new Genre("Fantasy");
    assertEquals("Fantasy", g.toString());
    assertEquals("Fantasy", g.getGenre());
  }

  @Test
  void constructor_null_throws() {
    IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> new Genre(null));
    assertTrue(ex.getMessage().toLowerCase().contains("null"));
  }

  @Test
  void constructor_blank_throws() {
    IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> new Genre("   "));
    assertTrue(ex.getMessage().toLowerCase().contains("blank"));
  }

  @Test
  void constructor_maxLength_ok() {
    String max = repeat('a', 100);
    Genre g = new Genre(max);
    assertEquals(max, g.getGenre());
  }

  @Test
  void constructor_overMax_throws() {
    String over = repeat('a', 101);
    IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> new Genre(over));
    assertTrue(ex.getMessage().toLowerCase().contains("maximum"));
  }


  @Test
  void assignPk_setsPk() {
    Genre g = new Genre("SciFi");
    g.assignPk("gen-001");
    assertEquals("gen-001", g.getPk());
  }

  @Test
  void assignPk_blankOrNull_throws() {
    Genre g = new Genre("Horror");
    assertThrows(IllegalArgumentException.class, () -> g.assignPk("  "));
    assertThrows(IllegalArgumentException.class, () -> g.assignPk(null));
  }


  @Test
  void toString_returnsGenreName() {
    Genre g = new Genre("Mystery");
    assertEquals("Mystery", g.toString());
  }
}
