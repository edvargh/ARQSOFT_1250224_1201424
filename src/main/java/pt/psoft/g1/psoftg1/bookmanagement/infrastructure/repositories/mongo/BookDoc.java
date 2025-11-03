package pt.psoft.g1.psoftg1.bookmanagement.infrastructure.repositories.mongo;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Version;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.List;

@Document("books")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class BookDoc {
  @Id
  private String id;
  @Version
  private Long version;

  @Indexed(unique = true)
  private String isbn;

  private String title;
  private String description;

  private String genre;

  private List<String> authorIds;

  private List<String> authorNames;

  private String photoFile;
}
