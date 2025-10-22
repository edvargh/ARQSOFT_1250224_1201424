package pt.psoft.g1.psoftg1.usermanagement.infrastructure.repositories.mongo;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;
import java.util.Set;

@Document("users")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class UserDoc {
  @Id
  private String id;

  @Indexed(unique = true)
  private Long userId;

  @Indexed(unique = true)
  private String username;

  private String password;
  private String fullName;
  private boolean enabled;

  private Set<String> roles;

  private LocalDateTime createdAt;
  private LocalDateTime modifiedAt;
}
