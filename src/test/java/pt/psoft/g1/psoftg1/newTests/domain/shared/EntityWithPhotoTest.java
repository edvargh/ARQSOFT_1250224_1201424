package pt.psoft.g1.psoftg1.newTests.domain.shared;

import java.nio.file.Paths;
import org.junit.jupiter.api.Test;
import pt.psoft.g1.psoftg1.shared.model.EntityWithPhoto;
import pt.psoft.g1.psoftg1.shared.model.Photo;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Functional transparent-box tests for EntityWithPhoto.
 */
class EntityWithPhotoTest {

  /** Minimal concrete type for testing the abstract base. */
  static class DummyEntity extends EntityWithPhoto {}

  @Test
  void setPhoto_null_clearsPhoto() {
    DummyEntity e = new DummyEntity();
    e.setPhoto(null);
    assertNull(e.getPhoto());
  }

  @Test
  void setPhoto_validPath_setsPhotoWithMatchingFile() {
    DummyEntity e = new DummyEntity();
    String path = "images/cover.jpg";

    e.setPhoto(path);

    Photo p = e.getPhoto();
    assertNotNull(p, "Photo should be created for a valid path");
    assertEquals(Paths.get(path).toString(), p.getPhotoFile());
  }

  @Test
  void setPhoto_invalidPath_setsPhotoNull() {
    DummyEntity e = new DummyEntity();
    String invalid = "\u0000bad";

    e.setPhoto(invalid);

    assertNull(e.getPhoto(), "Invalid path should result in a null photo");
  }

  @Test
  void setPhoto_canBeOverwritten_fromValidToNull() {
    DummyEntity e = new DummyEntity();
    e.setPhoto("pics/a.png");
    assertNotNull(e.getPhoto());

    e.setPhoto(null);
    assertNull(e.getPhoto(), "Setting null later should clear previous photo");
  }

  @Test
  void setPhoto_validThenDifferentValid_overwritesWithNewPhoto() {
    EntityWithPhotoTest.DummyEntity e = new EntityWithPhotoTest.DummyEntity();
    e.setPhoto("pics/a.png");
    String second = "pics/b.png";

    e.setPhoto(second);

    assertNotNull(e.getPhoto());
    assertEquals(java.nio.file.Paths.get(second).toString(), e.getPhoto().getPhotoFile());
  }

  @Test
  void setPhoto_validThenInvalid_overwritesToNull() {
    EntityWithPhotoTest.DummyEntity e = new EntityWithPhotoTest.DummyEntity();
    e.setPhoto("pics/a.png");
    assertNotNull(e.getPhoto());

    e.setPhoto("\u0000bad");

    assertNull(e.getPhoto());
  }

}
