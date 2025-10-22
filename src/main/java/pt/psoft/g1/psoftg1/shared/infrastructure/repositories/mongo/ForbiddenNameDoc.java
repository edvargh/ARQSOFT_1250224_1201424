package pt.psoft.g1.psoftg1.shared.infrastructure.repositories.mongo;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

@Document("forbidden_names")
public class ForbiddenNameDoc {
  @Id
  private String id;

  @Indexed(unique = true)
  private String forbiddenName;

  public ForbiddenNameDoc() {}

  public ForbiddenNameDoc(String forbiddenName) {
    this.forbiddenName = forbiddenName;
  }

  public String getId() { return id; }
  public void setId(String id) { this.id = id; }

  public String getForbiddenName() { return forbiddenName; }
  public void setForbiddenName(String forbiddenName) { this.forbiddenName = forbiddenName; }
}
