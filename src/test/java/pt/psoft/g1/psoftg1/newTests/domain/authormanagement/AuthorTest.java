package pt.psoft.g1.psoftg1.newTests.domain.authormanagement;

import org.hibernate.StaleObjectStateException;
import org.junit.jupiter.api.Test;
import pt.psoft.g1.psoftg1.authormanagement.model.Author;
import pt.psoft.g1.psoftg1.authormanagement.services.UpdateAuthorRequest;
import pt.psoft.g1.psoftg1.exceptions.ConflictException;

import java.lang.reflect.Field;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Functional transparent-box tests for Author.
 */
class AuthorTest {

  private static void setVersion(Author a, long version) {
    try {
      Field f = Author.class.getDeclaredField("version");
      f.setAccessible(true);
      f.set(a, version);
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }


  @Test
  void constructor_valid_buildsAggregate() {
    Author a = new Author("Robert Martin", "Author of Clean Code", null);
    assertNotNull(a);
    assertEquals("Robert Martin", a.getName());
    assertEquals("Author of Clean Code", a.getBio());
    assertNull(a.getPhoto());
  }

  @Test
  void assignId_setsAndValidates() {
    Author a = new Author("Name", "Bio", null);
    a.assignId("auth-123");
    assertEquals("auth-123", a.getId());
  }

  @Test
  void assignId_blankOrNull_throws() {
    Author a = new Author("Name", "Bio", null);
    assertThrows(IllegalArgumentException.class, () -> a.assignId("  "));
    assertThrows(IllegalArgumentException.class, () -> a.assignId(null));
  }


  @Test
  void removePhoto_versionMismatch_throwsConflict() {
    Author a = new Author("Name", "Bio", "photo://x");
    setVersion(a, 3L);

    assertThrows(ConflictException.class, () -> a.removePhoto(2L));
  }


  @Test
  void applyPatch_versionMismatch_throwsStale() {
    Author a = new Author("Name", "Bio", null);
    setVersion(a, 5L);

    UpdateAuthorRequest req = mock(UpdateAuthorRequest.class);
    when(req.getName()).thenReturn("New Name");

    assertThrows(StaleObjectStateException.class, () -> a.applyPatch(4L, req));
  }

  @Test
  void applyPatch_updatesOnlyProvidedFields() {
    Author a = new Author("Old Name", "Old Bio", null);
    setVersion(a, 7L);

    UpdateAuthorRequest req = mock(UpdateAuthorRequest.class);
    when(req.getName()).thenReturn("New Name");
    when(req.getBio()).thenReturn("New Bio");
    when(req.getPhotoURI()).thenReturn(null);

    a.applyPatch(7L, req);

    assertEquals("New Name", a.getName());
    assertEquals("New Bio", a.getBio());
    assertNull(a.getPhoto());
  }

  @Test
  void applyPatch_withNulls_changesNothingForThoseFields() {
    Author a = new Author("Name", "Bio", null);
    setVersion(a, 1L);

    UpdateAuthorRequest req = mock(UpdateAuthorRequest.class);
    when(req.getName()).thenReturn(null);
    when(req.getBio()).thenReturn(null);
    when(req.getPhotoURI()).thenReturn(null);

    a.applyPatch(1L, req);

    assertEquals("Name", a.getName());
    assertEquals("Bio", a.getBio());
    assertNull(a.getPhoto());
  }

  @Test
  void constructor_withPhoto_buildsWithPhoto() {
    Author a = new Author("Robert Martin", "Author of Clean Code", "images/cover.jpg");
    assertNotNull(a.getPhoto(), "photo should be set when a valid URI is given");
    assertTrue(a.getPhoto().getPhotoFile().contains("images"), "photo file should reflect the URI");
  }

  @Test
  void applyPatch_photoUriProvided_setsPhoto() {
    Author a = new Author("Old Name", "Old Bio", null);
    setVersion(a, 10L);

    UpdateAuthorRequest req = mock(UpdateAuthorRequest.class);
    when(req.getName()).thenReturn(null);
    when(req.getBio()).thenReturn(null);
    when(req.getPhotoURI()).thenReturn("pics/a.png");

    a.applyPatch(10L, req);

    assertNotNull(a.getPhoto(), "photo should be created from patch");
    assertTrue(a.getPhoto().getPhotoFile().contains("pics"));
  }

  @Test
  void applyPatch_invalidPhotoUri_clearsPhoto() {
    Author a = new Author("Name", "Bio", "valid/one.png");
    assertNotNull(a.getPhoto());
    setVersion(a, 2L);

    UpdateAuthorRequest req = mock(UpdateAuthorRequest.class);
    when(req.getName()).thenReturn(null);
    when(req.getBio()).thenReturn(null);
    when(req.getPhotoURI()).thenReturn("\u0000bad");

    a.applyPatch(2L, req);

    assertNull(a.getPhoto(), "invalid path in patch should result in null photo");
  }

  @Test
  void removePhoto_versionMatches_clearsPhoto() {
    Author a = new Author("Name", "Bio", "folder/pic.jpg");
    assertNotNull(a.getPhoto());
    setVersion(a, 3L);

    a.removePhoto(3L);

    assertNull(a.getPhoto(), "photo should be cleared when version matches");
  }

  @Test
  void applyPatch_photoUriNull_keepsExistingPhoto() {
    Author a = new Author("Name", "Bio", "existing/pic.jpg");
    assertNotNull(a.getPhoto(), "precondition: photo should exist");
    String before = a.getPhoto().getPhotoFile();
    setVersion(a, 11L);

    UpdateAuthorRequest req = mock(UpdateAuthorRequest.class);
    when(req.getName()).thenReturn("New Name");
    when(req.getBio()).thenReturn("New Bio");
    when(req.getPhotoURI()).thenReturn(null);

    a.applyPatch(11L, req);

    assertNotNull(a.getPhoto(), "photo should be preserved when patch photoURI is null");
    assertEquals(before, a.getPhoto().getPhotoFile(), "existing photo should not be cleared or modified");
    assertEquals("New Name", a.getName());
    assertEquals("New Bio", a.getBio());
  }

}
