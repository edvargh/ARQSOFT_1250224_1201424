package pt.psoft.g1.psoftg1.shared.infrastructure.repositories.mongo;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Repository;
import pt.psoft.g1.psoftg1.shared.repositories.PhotoRepository;

@Repository
@Profile("mongo")
public class PhotoMongoRepository implements PhotoRepository {


  @Override
  public void deleteByPhotoFile(String photoFile) {
    // no-op under Mongo
  }
}
