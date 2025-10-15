package pt.psoft.g1.psoftg1.lendingmanagement.infrastructure.repositories.mongo;

import pt.psoft.g1.psoftg1.lendingmanagement.model.Lending;

public class LendingMongoMapper {
  public static LendingDoc toDoc(Lending lending) {
    return LendingDoc.builder()
        .id(lending.getLendingNumber())
        .lendingNumber(lending.getLendingNumber())
        .bookIsbn(lending.getBook().getIsbn().toString())
        .readerNumber(lending.getReaderDetails().getReaderNumber().toString())
        .startDate(lending.getStartDate())
        .limitDate(lending.getLimitDate())
        .returnedDate(lending.getReturnedDate())
        .fineValuePerDayInCents(lending.getFineValuePerDayInCents())
        .build();
  }
}
