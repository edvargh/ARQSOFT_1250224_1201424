package pt.psoft.g1.psoftg1.lendingmanagement.infrastructure.repositories.mongo;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Document(collection = "fines")
public class FineDoc {
  @Id
  private String id;

  @Field("lendingId")
  private String lendingId;

  private int fineValuePerDayInCents;
  private int centsValue;
}
