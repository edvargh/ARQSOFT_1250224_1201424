package pt.psoft.g1.psoftg1.newTests.domain.usermanagement;

import org.junit.jupiter.api.Test;
import pt.psoft.g1.psoftg1.usermanagement.model.Role;
import pt.psoft.g1.psoftg1.usermanagement.model.User;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Functional transparent-box tests for User.
 */
class UserTest {

  @Test
  void constructor_setsUsernameAndPassword() {
    var u = new User("alice@example.com", "encPwd");
    assertEquals("alice@example.com", u.getUsername());
    assertEquals("encPwd", u.getPassword());
    assertTrue(u.isEnabled());
  }

  @Test
  void newUser_withName_setsName() {
    var u = User.newUser("bob@example.com", "pwd", "Bob Jones");
    assertEquals("Bob Jones", u.getName().toString());
  }

  @Test
  void newUser_withRole_addsAuthority_once() {
    var u = User.newUser("lib@example.com", "pwd", "Lib Rarian", "ADMIN");

    assertEquals(1, u.getAuthorities().size(), "should have exactly one role from factory");

    u.addAuthority(new Role("ADMIN"));
    assertEquals(1, u.getAuthorities().size());
  }

  @Test
  void userDetails_flags_followEnabled() {
    var u = new User("u@x.com", "p");
    assertTrue(u.isAccountNonExpired());
    assertTrue(u.isAccountNonLocked());
    assertTrue(u.isCredentialsNonExpired());
    assertTrue(u.isEnabled());

    u.setEnabled(false);
    assertFalse(u.isAccountNonExpired());
    assertFalse(u.isAccountNonLocked());
    assertFalse(u.isCredentialsNonExpired());
    assertFalse(u.isEnabled());
  }

  @Test
  void setName_valid_allowsAlphanumericSpaceHyphenApostrophe() {
    var u = new User("n@x.com", "p");
    u.setName("Ana-Maria d'Almeida 2");
    assertEquals("Ana-Maria d'Almeida 2", u.getName().toString());
  }

  @Test
  void setName_invalid_throws() {
    var u = new User("n@x.com", "p");
    assertThrows(IllegalArgumentException.class, () -> u.setName(null));
    assertThrows(IllegalArgumentException.class, () -> u.setName("   "));
    assertThrows(IllegalArgumentException.class, () -> u.setName("John_Doe"));
    assertThrows(IllegalArgumentException.class, () -> u.setName("Jane@Doe"));
  }

  @Test
  void addAuthority_allowsDifferentRoles() {
    var u = new User("multi@x.com", "p");
    u.addAuthority(new Role("ADMIN"));
    u.addAuthority(new Role("LIBRARIAN"));
    assertEquals(2, u.getAuthorities().size());
  }

  @Test
  void assignId_sets_whenValid_andRejectsBlankOrNull() {
    var u = new User("id@x.com", "p");
    u.assignId("user-123");
    assertEquals("user-123", u.getId());

    assertThrows(IllegalArgumentException.class, () -> new User("x@x.com","p").assignId(null));
    assertThrows(IllegalArgumentException.class, () -> new User("y@y.com","p").assignId("  "));
  }

  @Test
  void newUser_invalidName_usesSetterValidation_andThrows() {
    assertThrows(IllegalArgumentException.class,
        () -> User.newUser("bad@x.com", "pwd", "   "));
  }

  @Test
  void newUser_validName_setsEmbeddedName_nonNull() throws Exception {
    var u = User.newUser("ok@x.com", "pwd", "Alice Smith");

    assertNotNull(u.getName(), "factory must initialize embedded Name");
    assertEquals("Alice Smith", u.getName().toString());

    var f = User.class.getDeclaredField("name");
    f.setAccessible(true);
    assertNotNull(f.get(u), "embedded Name field should be set by factory");
  }

  @Test
  void newUser_withoutSetName_wouldLeaveNameNull_butFactoryMustNot() throws Exception {
    var u = User.newUser("x@y.com", "pwd", "Valid Name");

    var f = User.class.getDeclaredField("name");
    f.setAccessible(true);
    Object value = f.get(u);

    assertNotNull(value, "User.newUser must call setName to initialize embedded Name");
    assertEquals("Valid Name", u.getName().toString());
  }
}
