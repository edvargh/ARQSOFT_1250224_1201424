package pt.psoft.g1.psoftg1.shared.infrastructure.repositories.mongo;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Repository;
import pt.psoft.g1.psoftg1.shared.repositories.PhotoRepository;

@Repository
@Profile("mongo")
public class PhotoMongoRepository implements PhotoRepository {

  /**
   * In the Mongo model there is no separate Photo collection.
   * The photo path is embedded in the owning document, so there’s nothing to delete here.
   * File deletion (on disk) is handled by FileStorageService, not by this repository.
   */
  @Override
  public void deleteByPhotoFile(String photoFile) {
    // no-op under Mongo
  }
}
