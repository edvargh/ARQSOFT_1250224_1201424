package pt.psoft.g1.psoftg1.usermanagement.infrastructure.repositories.mongo;

import org.springframework.stereotype.Component;
import pt.psoft.g1.psoftg1.shared.model.Name;
import pt.psoft.g1.psoftg1.usermanagement.model.Librarian;
import pt.psoft.g1.psoftg1.usermanagement.model.Reader;
import pt.psoft.g1.psoftg1.usermanagement.model.Role;
import pt.psoft.g1.psoftg1.usermanagement.model.User;

import java.lang.reflect.Field;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

@Component
public class UserMongoMapper {

  public UserDoc toDoc(User u) {
    if (u == null) return null;

    Set<String> roles = new HashSet<>();
    u.getAuthorities().forEach(r -> roles.add(r.getAuthority()));

    return UserDoc.builder()
        .id(u.getId())
        .username(u.getUsername())
        .password(extractPasswordUnsafe(u))
        .fullName(u.getName() == null ? null : u.getName().toString())
        .enabled(u.isEnabled())
        .roles(roles)
        .createdAt(LocalDateTime.now())
        .modifiedAt(LocalDateTime.now())
        .build();
  }

  public User toDomain(UserDoc d) {
    if (d == null) return null;

    final String uname = d.getUsername();
    final String pwdHash = d.getPassword();
    final String fullName = d.getFullName();
    final Set<String> roles = d.getRoles() == null ? Set.of() : d.getRoles();

    User u;
    if (roles.contains(Role.LIBRARIAN)) {
      u = new Librarian(uname, "Dummy#123");
    } else if (roles.contains(Role.READER)) {
      u = new Reader(uname, "Dummy#123");
    } else {
      u = new User(uname, "Dummy#123");
    }

    if (fullName != null) {
      u.setName(fullName);
    }

    u.setEnabled(d.isEnabled());

    roles.forEach(r -> u.addAuthority(new Role(r)));

    try {
      Field fp = User.class.getDeclaredField("password");
      fp.setAccessible(true);
      fp.set(u, pwdHash);
    } catch (Exception ignored) {}

    try {
      Field fid = User.class.getDeclaredField("id");
      fid.setAccessible(true);
      fid.set(u, d.getId());
    } catch (Exception ignored) {}

    return u;
  }

  private String extractPasswordUnsafe(User u) {
    try {
      var f = User.class.getDeclaredField("password");
      f.setAccessible(true);
      return (String) f.get(u);
    } catch (Exception e) {
      return null;
    }
  }
}
