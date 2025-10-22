package pt.psoft.g1.psoftg1.readermanagement.infraestructure.repositories.mongo;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Version;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDate;
import java.util.List;

@Document("readers")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class ReaderDoc {
  @Id
  private String id;

  /** natural key "YYYY/seq" */
  @Indexed(unique = true)
  private String readerNumber;

  /** denormalized user info for quick lookups */
  private Long userId;
  @Indexed
  private String username;
  private String fullName;

  private LocalDate birthDate;
  @Indexed
  private String phoneNumber;

  private boolean gdprConsent;
  private boolean marketingConsent;
  private boolean thirdPartySharingConsent;

  /** list of genre names */
  private List<String> interestGenres;

  /** stored file name (if any) */
  private String photoFile;

  @Version
  private Long version;
}
