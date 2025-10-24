package pt.psoft.g1.psoftg1.lendingmanagement.infrastructure.repositories.mongo;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Repository;
import pt.psoft.g1.psoftg1.lendingmanagement.model.Fine;
import pt.psoft.g1.psoftg1.lendingmanagement.model.Lending;
import pt.psoft.g1.psoftg1.lendingmanagement.repositories.FineRepository;
import pt.psoft.g1.psoftg1.lendingmanagement.repositories.LendingRepository;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Profile("mongo")
@Repository
@RequiredArgsConstructor
public class FineMongoRepository implements FineRepository {

  private final SpringMongoFineRepo fineRepo;
  private final LendingRepository lendingRepository;

  @Override
  public Optional<Fine> findByLendingNumber(String lendingNumber) {
    return fineRepo.findByLendingId(lendingNumber).flatMap(doc -> {
      var lending = lendingRepository.findByLendingNumber(lendingNumber);
      if (lending.isEmpty()) return Optional.empty();
      Fine f = new Fine(lending.get(), doc.getFineValuePerDayInCents(), doc.getCentsValue());
      f.assignId(doc.getId());
      return Optional.of(f);
    });
  }

  @Override
  public Iterable<Fine> findAll() {
    List<Fine> out = new ArrayList<>();
    fineRepo.findAll().forEach(doc -> {
      lendingRepository.findByLendingNumber(doc.getLendingId()).ifPresent(l -> {
        Fine f = new Fine(l, doc.getFineValuePerDayInCents(), doc.getCentsValue());
        f.assignId(doc.getId());
        out.add(f);
      });
    });
    return out;
  }

  @Override
  public Fine save(Fine fine) {
    FineDoc doc = FineDoc.builder()
        .id(fine.getId())
        .lendingId(fine.getLending().getLendingNumber())
        .fineValuePerDayInCents(fine.getFineValuePerDayInCents())
        .centsValue(fine.getCentsValue())
        .build();
    fineRepo.save(doc);
    return fine;
  }
}
