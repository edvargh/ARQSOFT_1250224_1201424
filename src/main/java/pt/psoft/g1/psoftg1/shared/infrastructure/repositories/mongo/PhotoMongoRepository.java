package pt.psoft.g1.psoftg1.shared.infrastructure.repositories.mongo;

import java.nio.file.Path;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Repository;
import pt.psoft.g1.psoftg1.shared.id.IdGenerator;
import pt.psoft.g1.psoftg1.shared.model.Photo;
import pt.psoft.g1.psoftg1.shared.repositories.PhotoRepository;

@Repository
@Profile("mongo")
@RequiredArgsConstructor
public class PhotoMongoRepository implements PhotoRepository {
  private final SpringMongoPhotoRepo repo;
  private final IdGenerator idGenerator;


  @Override
  public Photo save(Photo photo) {
    photo.assignIdIfAbsent(idGenerator.newId("ph_"));
    PhotoDoc doc = PhotoDoc.builder()
        .id(photo.getId())
        .photoFile(photo.getPhotoFile())
        .build();
    repo.save(doc);
    return photo;
  }

  @Override
  public void deleteByPhotoFile(String file) {
    repo.deleteByPhotoFile(file);
  }
}
