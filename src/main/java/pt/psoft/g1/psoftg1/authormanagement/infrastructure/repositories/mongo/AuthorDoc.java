package pt.psoft.g1.psoftg1.authormanagement.infrastructure.repositories.mongo;

import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Version;
import org.springframework.data.mongodb.core.mapping.Document;

@Document("authors")
public class AuthorDoc {

  @Id
  private String id;

  private String name;
  private String bio;

  private Long photoId;

  @Version
  private Long version;

  public String getId() { return id; }
  public void setId(String id) { this.id = id; }
  public String getName() { return name; }
  public void setName(String name) { this.name = name; }
  public String getBio() { return bio; }
  public void setBio(String bio) { this.bio = bio; }
  public Long getPhotoId() { return photoId; }
  public void setPhotoId(Long photoId) { this.photoId = photoId; }
  public Long getVersion() { return version; }
  public void setVersion(Long version) { this.version = version; }
}
