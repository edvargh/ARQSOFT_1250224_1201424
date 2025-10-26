package pt.psoft.g1.psoftg1.newTests.domain.readermanagement;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InOrder;
import pt.psoft.g1.psoftg1.exceptions.ConflictException;
import pt.psoft.g1.psoftg1.genremanagement.model.Genre;
import pt.psoft.g1.psoftg1.readermanagement.model.ReaderDetails;
import pt.psoft.g1.psoftg1.readermanagement.services.UpdateReaderRequest;
import pt.psoft.g1.psoftg1.usermanagement.model.Reader;

import java.lang.reflect.Field;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Functional transparent-box tests for ReaderDetails.
 */
class ReaderDetailsTest {

  private Reader reader;


  private static void setVersion(ReaderDetails d, long v) {
    try {
      Field f = ReaderDetails.class.getDeclaredField("version");
      f.setAccessible(true);
      f.set(d, v);
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  @BeforeEach
  void setup() {
    reader = mock(Reader.class);
  }


  @Test
  void constructor_valid_builds() {
    ReaderDetails d = new ReaderDetails(
        42,
        reader,
        "2000-01-01",
        "910000000",
        true,
        true,
        false,
        null,
        List.of()
    );

    assertNotNull(d.getReader());
    assertTrue(d.isGdprConsent());
    assertTrue(d.isMarketingConsent());
    assertFalse(d.isThirdPartySharingConsent());
    assertEquals("910000000", d.getPhoneNumber());
    assertEquals("2000-1-1", d.getBirthDate().toString());
    assertEquals(List.of(), d.getInterestList());
    assertNull(d.getPhoto());
    assertNotNull(d.getReaderNumber());
  }

  @Test
  void constructor_nullReaderOrPhone_gdprFalse_throw() {
    assertThrows(IllegalArgumentException.class, () ->
        new ReaderDetails(1, null, "2000-01-01", "910000000",
            true, false, false, null, null));

    assertThrows(IllegalArgumentException.class, () ->
        new ReaderDetails(1, reader, "2000-01-01", null,
            true, false, false, null, null));

    assertThrows(IllegalArgumentException.class, () ->
        new ReaderDetails(1, reader, "2000-01-01", "910000000",
            false, false, false, null, null));
  }


  @Test
  void assignId_sets_once_only_and_validates() {
    ReaderDetails d = new ReaderDetails(7, reader, "1999-12-31", "910000000",
        true, false, false, null, null);

    d.assignId("reader-1");
    assertEquals("reader-1", d.getId());

    assertThrows(IllegalStateException.class, () -> d.assignId("reader-2"));
    ReaderDetails d2 = new ReaderDetails(8, reader, "1990-01-01", "910000001",
        true, false, false, null, null);
    assertThrows(IllegalArgumentException.class, () -> d2.assignId("  "));
    assertThrows(IllegalArgumentException.class, () -> d2.assignId(null));
  }


  @Test
  void removePhoto_versionMismatch_throwsConflict() {
    ReaderDetails d = new ReaderDetails(1, reader, "2000-01-01", "910000000",
        true, false, false, "x", null);
    setVersion(d, 5L);

    assertThrows(ConflictException.class, () -> d.removePhoto(4L));
  }

  @Test
  void removePhoto_ok_setsNull() {
    ReaderDetails d = new ReaderDetails(1, reader, "2000-01-01", "910000000",
        true, false, false, "x", null);
    setVersion(d, 2L);

    d.removePhoto(2L);
    assertNull(d.getPhoto());
  }


  @Test
  void applyPatch_versionMismatch_throwsConflict() {
    ReaderDetails d = new ReaderDetails(1, reader, "2000-01-01", "910000000",
        true, true, false, null, null);
    setVersion(d, 10L);

    UpdateReaderRequest req = mock(UpdateReaderRequest.class);
    assertThrows(ConflictException.class, () -> d.applyPatch(9L, req, null, null));
  }

  @Test
  void applyPatch_updatesOnlyProvidedFields() {
    ReaderDetails d = new ReaderDetails(1, reader, "2000-01-01", "910000000",
        true, false, false, null, List.of());
    setVersion(d, 3L);

    UpdateReaderRequest req = mock(UpdateReaderRequest.class);
    when(req.getBirthDate()).thenReturn("1995-05-05");
    when(req.getPhoneNumber()).thenReturn("930000000");
    when(req.getMarketing()).thenReturn(true);
    when(req.getThirdParty()).thenReturn(true);
    when(req.getFullName()).thenReturn("AliceReader");
    when(req.getUsername()).thenReturn("alice");
    when(req.getPassword()).thenReturn("secret");

    List<Genre> interests = List.of(mock(Genre.class));
    String photoUri = null;

    d.applyPatch(3L, req, photoUri, interests);

    assertEquals("1995-5-5", d.getBirthDate().toString());
    assertEquals("930000000", d.getPhoneNumber());

    assertTrue(d.isMarketingConsent());
    assertTrue(d.isThirdPartySharingConsent());

    InOrder inOrder = inOrder(reader);
    inOrder.verify(reader).setUsername("alice");
    inOrder.verify(reader).setPassword("secret");
    inOrder.verify(reader).setName("AliceReader");

    assertEquals(1, d.getInterestList().size());
  }

  @Test
  void applyPatch_allNulls_keepExistingValues() {
    ReaderDetails d = new ReaderDetails(1, reader, "2000-01-01", "910000000",
        true, true, false, null, List.of());
    setVersion(d, 1L);

    UpdateReaderRequest req = mock(UpdateReaderRequest.class);
    when(req.getBirthDate()).thenReturn(null);
    when(req.getPhoneNumber()).thenReturn(null);
    when(req.getMarketing()).thenReturn(true);
    when(req.getThirdParty()).thenReturn(false);
    when(req.getFullName()).thenReturn(null);
    when(req.getUsername()).thenReturn(null);
    when(req.getPassword()).thenReturn(null);

    d.applyPatch(1L, req, null, null);

    assertEquals("2000-1-1", d.getBirthDate().toString());
    assertEquals("910000000", d.getPhoneNumber());
    assertTrue(d.isMarketingConsent());
    assertFalse(d.isThirdPartySharingConsent());
    verifyNoMoreInteractions(reader);
  }
}
