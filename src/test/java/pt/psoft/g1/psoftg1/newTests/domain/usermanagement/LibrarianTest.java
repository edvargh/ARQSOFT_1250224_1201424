package pt.psoft.g1.psoftg1.newTests.domain.usermanagement;

import org.junit.jupiter.api.Test;
import pt.psoft.g1.psoftg1.usermanagement.model.Librarian;
import pt.psoft.g1.psoftg1.usermanagement.model.Role;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Functional transparent-box tests for Librarian.
 */
class LibrarianTest {

  @Test
  void constructor_basic_inheritsUserFields_noRoleByDefault() {
    Librarian lib = new Librarian("lib@example.com", "pw");
    assertEquals("lib@example.com", lib.getUsername());
    assertEquals("pw", lib.getPassword());
    assertTrue(lib.isEnabled());
    assertTrue(lib.getAuthorities().isEmpty(), "raw constructor should not assign roles");
  }

  @Test
  void factory_newLibrarian_setsName_and_LibrarianRole() {
    Librarian lib = Librarian.newLibrarian("lib@example.com", "pw", "Alice Librarian");

    assertEquals("lib@example.com", lib.getUsername());
    assertEquals("pw", lib.getPassword());
    assertNotNull(lib.getName());
    assertEquals("Alice Librarian", lib.getName().toString());

    assertTrue(
        lib.getAuthorities().contains(new Role(Role.LIBRARIAN)),
        "Factory must add LIBRARIAN authority"
    );
  }

  @Test
  void assignId_setsOnce_andRejectsBlank() {
    Librarian lib = new Librarian("lib@example.com", "pw");

    lib.assignId("lib-1");
    assertEquals("lib-1", lib.getId());

    assertThrows(IllegalArgumentException.class, () -> lib.assignId(" "));
    assertThrows(IllegalArgumentException.class, () -> lib.assignId(null));
  }
}
