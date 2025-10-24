package pt.psoft.g1.psoftg1.lendingmanagement.infrastructure.repositories.mongo;

import pt.psoft.g1.psoftg1.lendingmanagement.model.Lending;

public class LendingMongoMapper {

  public static LendingDoc toDoc(Lending lending) {
    if (lending == null) return null;

    return LendingDoc.builder()
        .id(lending.getId())
        .lendingNumber(lending.getLendingNumber())
        .bookIsbn(lending.getBook().getIsbn())
        .readerNumber(lending.getReaderDetails().getReaderNumber())
        .startDate(lending.getStartDate())
        .limitDate(lending.getLimitDate())
        .returnedDate(lending.getReturnedDate())
        .fineValuePerDayInCents(lending.getFineValuePerDayInCents())
        .build();
  }
}
