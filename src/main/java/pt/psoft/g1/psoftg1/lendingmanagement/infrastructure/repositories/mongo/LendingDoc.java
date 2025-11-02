package pt.psoft.g1.psoftg1.lendingmanagement.infrastructure.repositories.mongo;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

import java.time.LocalDate;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Document(collection = "lendings")
public class LendingDoc {

  @Id
  private String id;

  @Field("lendingNumber")
  private String lendingNumber;

  @Field("bookIsbn")
  private String bookIsbn;

  @Field("readerNumber")
  private String readerNumber;

  private LocalDate startDate;
  private LocalDate limitDate;
  private LocalDate returnedDate;

  private int fineValuePerDayInCents;
}
