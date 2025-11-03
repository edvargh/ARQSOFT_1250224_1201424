package pt.psoft.g1.psoftg1.newTests.domain.shared;

import org.junit.jupiter.api.Test;
import pt.psoft.g1.psoftg1.shared.model.Name;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Functional transparent-box tests for Name.
 */
class NameTest {

  @Test
  void constructor_valid_builds() {
    Name n = new Name("Alice123");
    assertNotNull(n);
    assertEquals("Alice123", n.toString());
  }

  @Test
  void constructor_null_throws() {
    IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> new Name(null));
    assertTrue(ex.getMessage().toLowerCase().contains("cannot be null"));
  }

  @Test
  void constructor_blank_throws() {
    assertThrows(IllegalArgumentException.class, () -> new Name(""));
    assertThrows(IllegalArgumentException.class, () -> new Name("   "));
  }

  @Test
  void constructor_nonAlphanumeric_throws() {
    assertThrows(IllegalArgumentException.class, () -> new Name("John@Doe"));
    assertThrows(IllegalArgumentException.class, () -> new Name("John#Doe"));
    assertThrows(IllegalArgumentException.class, () -> new Name("$John"));
  }

  @Test
  void setName_valid_updates() {
    Name n = new Name("Bob");
    n.setName("Carol42");
    assertEquals("Carol42", n.toString());
  }

  @Test
  void setName_invalid_keepsPreviousValue_andThrows() {
    Name n = new Name("Bob");
    assertThrows(IllegalArgumentException.class, () -> n.setName("Robert C. Martin")); // space + dot
    assertEquals("Bob", n.toString());
  }

  @Test
  void constructor_allowsAccents() {
    Name n = new Name("Álvaro Núñez");
    assertEquals("Álvaro Núñez", n.toString());
  }

  @Test
  void constructor_allowsHyphenAndApostrophe() {
    Name n1 = new Name("Jean-Luc");
    Name n2 = new Name("O'Connor");
    assertEquals("Jean-Luc", n1.toString());
    assertEquals("O'Connor", n2.toString());
  }

  @Test
  void constructor_underscore_disallowed() {
    assertThrows(IllegalArgumentException.class, () -> new Name("John_Doe"));
  }

  @Test
  void setName_valid_reallyUpdates_notNoOp() {
    Name n = new Name("Alice");
    n.setName("Bob");
    assertEquals("Bob", n.toString());
  }
}
