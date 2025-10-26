package pt.psoft.g1.psoftg1.newTests.domain.shared;

import org.junit.jupiter.api.Test;
import pt.psoft.g1.psoftg1.shared.model.Photo;

import java.nio.file.Path;
import java.nio.file.Paths;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Functional transparent-box tests for Photo.
 */
class PhotoTest {

  @Test
  void constructor_validPath_setsPhotoFile_andLeavesIdNull() {
    Path p = Paths.get("images", "cover.jpg");
    Photo photo = new Photo(p);

    assertNull(photo.getId(), "pk should be null until assigned");
    assertEquals(p.toString(), photo.getPhotoFile());
  }

  @Test
  void constructor_nullPath_throwsNPE() {
    assertThrows(NullPointerException.class, () -> new Photo(null));
  }

  @Test
  void assignIdIfAbsent_setsIdOnce() {
    Path p = Paths.get("x", "y.png");
    Photo photo = new Photo(p);

    photo.assignIdIfAbsent("ph-1");
    assertEquals("ph-1", photo.getId());

    photo.assignIdIfAbsent("ph-2");
    assertEquals("ph-1", photo.getId(), "id must not be overwritten");
  }

  @Test
  void assignIdIfAbsent_doesNothing_whenIdAlreadyPresent() throws Exception {
    Path p = Paths.get("a", "b.png");
    Photo photo = new Photo(p);

    photo.assignIdIfAbsent("first");
    photo.assignIdIfAbsent("second");

    assertEquals("first", photo.getId());
    assertEquals(p.toString(), photo.getPhotoFile());
  }
}
