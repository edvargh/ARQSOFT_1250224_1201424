package pt.psoft.g1.psoftg1.newTests.domain.usermanagement;

import org.junit.jupiter.api.Test;
import pt.psoft.g1.psoftg1.usermanagement.model.Reader;
import pt.psoft.g1.psoftg1.usermanagement.model.Role;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Functional transparent-box tests for Reader.
 */
class ReaderTest {

  @Test
  void ctor_addsReaderRole_andIsEnabled() {
    Reader r = new Reader("alice@example.com", "secret");
    assertTrue(r.isEnabled());
    assertTrue(r.getAuthorities().contains(new Role(Role.READER)));
    long readerCount = r.getAuthorities().stream().filter(a -> a.getAuthority().equals(Role.READER)).count();
    assertEquals(1, readerCount);
  }

  @Test
  void newReader_setsName_andAddsReaderRole() {
    Reader r = Reader.newReader("bob@example.com", "pw", "Bob Example");
    assertEquals("Bob Example", r.getName().toString());
    assertTrue(r.getAuthorities().contains(new Role(Role.READER)));
  }

  @Test
  void userDetailsFlags_inheritFromUserEnabled() {
    Reader r = new Reader("carol@example.com", "pw");
    assertTrue(r.isAccountNonExpired());
    assertTrue(r.isAccountNonLocked());
    assertTrue(r.isCredentialsNonExpired());
  }
}
