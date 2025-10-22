package pt.psoft.g1.psoftg1.genremanagement.infrastructure.repositories.mongo;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

@Document("genres")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class GenreDoc {
  @Id
  private String id;

  @Indexed(unique = true)
  private String genre;
}
