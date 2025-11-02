package pt.psoft.g1.psoftg1.shared.infrastructure.repositories.mongo;

import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Repository;

import lombok.RequiredArgsConstructor;
import pt.psoft.g1.psoftg1.shared.model.ForbiddenName;
import pt.psoft.g1.psoftg1.shared.repositories.ForbiddenNameRepository;

@Repository
@Profile("mongo")
@RequiredArgsConstructor
public class ForbiddenNameMongoRepository implements ForbiddenNameRepository {

  private final SpringMongoForbiddenNameRepo repo;

  private static ForbiddenName toDomain(ForbiddenNameDoc d) {
    return d == null ? null : new ForbiddenName(d.getId(), d.getForbiddenName());
  }

  private static ForbiddenNameDoc toDoc(ForbiddenName f) {
    return f == null ? null : new ForbiddenNameDoc(f.getForbiddenName());
  }

  @Override
  public Iterable<ForbiddenName> findAll() {
    return repo.findAll()
        .stream()
        .map(ForbiddenNameMongoRepository::toDomain)
        .collect(Collectors.toList());
  }

  @Override
  public List<ForbiddenName> findByForbiddenNameIsContained(String pat) {
    if (pat == null) return List.of();
    final String haystack = pat.toLowerCase(Locale.ROOT);
    return repo.findAll().stream()
        .filter(d -> d.getForbiddenName() != null &&
            haystack.contains(d.getForbiddenName().toLowerCase(Locale.ROOT)))
        .map(ForbiddenNameMongoRepository::toDomain)
        .collect(Collectors.toList());
  }

  @Override
  public ForbiddenName save(ForbiddenName forbiddenName) {
    ForbiddenNameDoc saved = repo.save(toDoc(forbiddenName));
    return toDomain(saved);
  }

  @Override
  public Optional<ForbiddenName> findByForbiddenName(String forbiddenName) {
    return repo.findByForbiddenName(forbiddenName).map(ForbiddenNameMongoRepository::toDomain);
  }

  @Override
  public int deleteForbiddenName(String forbiddenName) {
    return (int) repo.deleteByForbiddenName(forbiddenName);
  }
}
