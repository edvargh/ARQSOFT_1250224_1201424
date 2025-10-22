package pt.psoft.g1.psoftg1.usermanagement.infrastructure.repositories.mongo;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Repository;
import pt.psoft.g1.psoftg1.shared.services.Page;
import pt.psoft.g1.psoftg1.usermanagement.model.Role;
import pt.psoft.g1.psoftg1.usermanagement.model.User;
import pt.psoft.g1.psoftg1.usermanagement.repositories.UserRepository;
import pt.psoft.g1.psoftg1.usermanagement.services.SearchUsersQuery;

import java.lang.reflect.Field;
import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

@Repository
@Profile("mongo")
@RequiredArgsConstructor
public class UserMongoRepository implements UserRepository {

  private final SpringMongoUserRepo repo;
  private final MongoTemplate mongo;
  private final UserMongoMapper mapper;

  private static String ciContains(String s) {
    return ".*" + Pattern.quote(s) + ".*";
  }
  private static String ciStartsWith(String s) {
    return "^" + Pattern.quote(s);
  }

  private long ensureUserId(User u, UserDoc docFromUsername) {
    if (u.getId() != null) return u.getId();
    if (docFromUsername != null && docFromUsername.getUserId() != null) return docFromUsername.getUserId();
    return System.currentTimeMillis();
  }

  private void setDomainId(User u, Long id) {
    try {
      Field fid = User.class.getDeclaredField("id");
      fid.setAccessible(true);
      fid.set(u, id);
    } catch (Exception ignored) {}
  }

  @Override
  public <S extends User> List<S> saveAll(Iterable<S> entities) {
    List<S> list = StreamSupport.stream(entities.spliterator(), false).collect(Collectors.toList());
    for (S u : list) {
      save(u);
    }
    return list;
  }

  @Override
  public <S extends User> S save(S entity) {
    var existingByUsername = repo.findByUsername(entity.getUsername()).orElse(null);

    Long userId = ensureUserId(entity, existingByUsername);
    var doc = mapper.toDoc(entity);
    doc.setUserId(userId);
    if (existingByUsername != null) {
      doc.setId(existingByUsername.getId());
    }

    var saved = repo.save(doc);

    setDomainId(entity, saved.getUserId());

    return entity;
  }

  @Override
  public Optional<User> findById(Long objectId) {
    return repo.findByUserId(objectId).map(mapper::toDomain);
  }

  @Override
  public Optional<User> findByUsername(String username) {
    return repo.findByUsername(username).map(mapper::toDomain);
  }

  @Override
  public List<User> searchUsers(Page page, SearchUsersQuery query) {
    if (page == null) page = new Page(1, 10);
    Query q = new Query();

    if (query != null) {
      if (query.getUsername() != null && !query.getUsername().isBlank()) {
        q.addCriteria(Criteria.where("username").is(query.getUsername()));
      }
      if (query.getFullName() != null && !query.getFullName().isBlank()) {
        q.addCriteria(Criteria.where("fullName").regex(ciContains(query.getFullName()), "i"));
      }
    }

    q.with(Sort.by(Sort.Direction.DESC, "createdAt"));
    var pageable = PageRequest.of(Math.max(0, page.getNumber() - 1), Math.max(1, page.getLimit()));
    q.skip((long) pageable.getPageNumber() * pageable.getPageSize()).limit(pageable.getPageSize());

    return mongo.find(q, UserDoc.class).stream().map(mapper::toDomain).toList();
  }

  @Override
  public List<User> findByNameName(String name) {
    if (name == null || name.isBlank()) return List.of();
    return repo.findByFullName(name).stream().map(mapper::toDomain).toList();
  }

  @Override
  public List<User> findByNameNameContains(String name) {
    if (name == null || name.isBlank()) return List.of();
    String regex = ciContains(name);
    return repo.findByFullNameRegex(regex).stream().map(mapper::toDomain).toList();
  }

  @Override
  public void delete(User user) {
    if (user == null) return;
    if (user.getId() != null) {
      repo.findByUserId(user.getId()).ifPresent(d -> repo.deleteById(d.getId()));
      return;
    }
    if (user.getUsername() != null) {
      repo.findByUsername(user.getUsername()).ifPresent(d -> repo.deleteById(d.getId()));
    }
  }
}
