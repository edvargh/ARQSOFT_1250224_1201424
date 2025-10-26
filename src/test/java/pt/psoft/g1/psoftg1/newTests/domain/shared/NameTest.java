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
}
