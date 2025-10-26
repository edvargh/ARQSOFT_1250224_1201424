package pt.psoft.g1.psoftg1.newTests.unit.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jwt.Jwt;
import pt.psoft.g1.psoftg1.shared.id.IdGenerator;
import pt.psoft.g1.psoftg1.shared.repositories.ForbiddenNameRepository;
import pt.psoft.g1.psoftg1.usermanagement.model.Role;
import pt.psoft.g1.psoftg1.usermanagement.model.User;
import pt.psoft.g1.psoftg1.usermanagement.repositories.UserRepository;
import pt.psoft.g1.psoftg1.usermanagement.services.*;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests (opaque-box) for UserService.
 */
@ExtendWith(MockitoExtension.class)
class UserServiceTest {

  @Mock UserRepository userRepo;
  @Mock EditUserMapper userEditMapper;
  @Mock ForbiddenNameRepository forbiddenNameRepository;
  @Mock PasswordEncoder passwordEncoder;
  @Mock IdGenerator idGenerator;

  UserService service;

  @BeforeEach
  void setUp() {
    service = new UserService(userRepo, userEditMapper, forbiddenNameRepository, passwordEncoder, idGenerator);
  }

  @Test
  void findByName_delegates() {
    when(userRepo.findByNameName("Ana")).thenReturn(List.of());
    assertNotNull(service.findByName("Ana"));
    verify(userRepo).findByNameName("Ana");
  }

  @Test
  void findByNameLike_delegates() {
    when(userRepo.findByNameNameContains("na")).thenReturn(List.of());
    assertNotNull(service.findByNameLike("na"));
    verify(userRepo).findByNameNameContains("na");
  }

  @Test
  void create_whenUsernameExists_throwsConflict() {
    CreateUserRequest req = new CreateUserRequest();
    req.setUsername("alice");
    req.setPassword("p");
    req.setName("Alice Smith");
    req.setRole(Role.READER);

    when(userRepo.findByUsername("alice")).thenReturn(Optional.of(mock(User.class)));

    assertThrows(pt.psoft.g1.psoftg1.exceptions.ConflictException.class, () -> service.create(req));
    verify(userRepo, never()).save(any());
  }

  @Test
  void create_whenNameContainsForbiddenWord_throwsIllegalArgument() {
    CreateUserRequest req = new CreateUserRequest();
    req.setUsername("bob");
    req.setPassword("pw");
    req.setName("Evil Bob");
    req.setRole(Role.READER);

    when(userRepo.findByUsername("bob")).thenReturn(Optional.empty());
    when(forbiddenNameRepository.findByForbiddenNameIsContained("Evil"))
        .thenReturn(List.of(mock(pt.psoft.g1.psoftg1.shared.model.ForbiddenName.class)));

    assertThrows(IllegalArgumentException.class, () -> service.create(req));
    verify(userRepo, never()).save(any());
  }

  @Test
  void create_whenRoleReader_encodesPassword_assignsId_andSaves() {
    CreateUserRequest req = new CreateUserRequest();
    req.setUsername("reader1");
    req.setPassword("Secret1!");
    req.setName("Rick Deckard");
    req.setRole(Role.READER);

    when(userRepo.findByUsername("reader1")).thenReturn(Optional.empty());
    when(forbiddenNameRepository.findByForbiddenNameIsContained(anyString())).thenReturn(List.of());
    when(passwordEncoder.encode("Secret1!")).thenReturn("ENC(Secret1!)");
    when(idGenerator.newId()).thenReturn("U-1");
    when(userRepo.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

    User out = service.create(req);

    assertNotNull(out);
    verify(passwordEncoder).encode("Secret1!");
    verify(idGenerator).newId();

    ArgumentCaptor<User> cap = ArgumentCaptor.forClass(User.class);
    verify(userRepo).save(cap.capture());
    assertEquals("ENC(Secret1!)", cap.getValue().getPassword());
  }

  @Test
  void create_whenRoleLibrarian_encodesPassword_assignsId_andSaves() {
    CreateUserRequest req = new CreateUserRequest();
    req.setUsername("lib1");
    req.setPassword("Pw123456!");
    req.setName("Libby Fox");
    req.setRole(Role.LIBRARIAN);

    when(userRepo.findByUsername("lib1")).thenReturn(Optional.empty());
    when(forbiddenNameRepository.findByForbiddenNameIsContained(anyString())).thenReturn(List.of());
    when(passwordEncoder.encode("Pw123456!")).thenReturn("ENC(Pw123456!)");
    when(idGenerator.newId()).thenReturn("U-2");
    when(userRepo.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

    User out = service.create(req);

    assertNotNull(out);
    verify(passwordEncoder).encode("Pw123456!");
    verify(idGenerator).newId();
    verify(userRepo).save(any(User.class));
  }

  @Test
  void update_fetchesEntity_appliesPatch_andSaves() {
    EditUserRequest patch = new EditUserRequest();
    User entity = mock(User.class);
    when(userRepo.getById("ID-1")).thenReturn(entity);
    when(userRepo.save(entity)).thenReturn(entity);

    User out = service.update("ID-1", patch);

    assertSame(entity, out);
    verify(userEditMapper).update(patch, entity);
    verify(userRepo).save(entity);
  }

  @Test
  void delete_fetchesEntity_disables_andSaves() {
    User entity = mock(User.class);
    when(userRepo.getById("ID-2")).thenReturn(entity);
    when(userRepo.save(entity)).thenReturn(entity);

    User out = service.delete("ID-2");

    assertSame(entity, out);
    verify(entity).setEnabled(false);
    verify(userRepo).save(entity);
  }

  @Test
  void loadUserByUsername_whenFound_returnsUserDetails() {
    User u = mock(User.class, withSettings().extraInterfaces(UserDetails.class));
    when(userRepo.findByUsername("ana")).thenReturn(Optional.of(u));

    UserDetails ud = service.loadUserByUsername("ana");

    assertNotNull(ud);
    verify(userRepo).findByUsername("ana");
  }

  @Test
  void loadUserByUsername_whenMissing_throwsUsernameNotFound() {
    when(userRepo.findByUsername("miss")).thenReturn(Optional.empty());
    assertThrows(UsernameNotFoundException.class, () -> service.loadUserByUsername("miss"));
  }

  @Test
  void usernameExists_trueWhenRepoFinds() {
    when(userRepo.findByUsername("x")).thenReturn(Optional.of(mock(User.class)));
    assertTrue(service.usernameExists("x"));
  }

  @Test
  void getUser_delegates() {
    User u = mock(User.class);
    when(userRepo.getById("ID-3")).thenReturn(u);
    assertSame(u, service.getUser("ID-3"));
  }

  @Test
  void findByUsername_delegates() {
    when(userRepo.findByUsername("bob")).thenReturn(Optional.empty());
    assertTrue(service.findByUsername("bob").isEmpty());
    verify(userRepo).findByUsername("bob");
  }

  @Test
  void searchUsers_whenPageNull_defaultsPage() {
    when(userRepo.searchUsers(any(), any())).thenReturn(List.of());

    service.searchUsers(null, new SearchUsersQuery("u", "n"));

    ArgumentCaptor<pt.psoft.g1.psoftg1.shared.services.Page> pageCap =
        ArgumentCaptor.forClass(pt.psoft.g1.psoftg1.shared.services.Page.class);
    verify(userRepo).searchUsers(pageCap.capture(), any(SearchUsersQuery.class));
    assertNotNull(pageCap.getValue(), "Default Page should be created when null is passed");
  }

  @Test
  void searchUsers_whenQueryNull_buildsDefaultAndDelegates() {
    pt.psoft.g1.psoftg1.shared.services.Page page =
        new pt.psoft.g1.psoftg1.shared.services.Page(1, 10);
    when(userRepo.searchUsers(eq(page), any())).thenReturn(List.of());

    List<User> out = service.searchUsers(page, null);

    assertNotNull(out);
    verify(userRepo).searchUsers(eq(page), any(SearchUsersQuery.class));
  }

  @Test
  void getAuthenticatedUser_whenAuthNull_throwsAccessDenied() {
    assertThrows(AccessDeniedException.class, () -> service.getAuthenticatedUser(null));
  }

  @Test
  void getAuthenticatedUser_whenPrincipalNotJwt_throwsAccessDenied() {
    Authentication auth = mock(Authentication.class);
    when(auth.getPrincipal()).thenReturn("not-a-jwt");
    assertThrows(AccessDeniedException.class, () -> service.getAuthenticatedUser(auth));
  }

  @Test
  void getAuthenticatedUser_whenJwtButUserMissing_throwsAccessDenied() {
    Jwt jwt = Jwt.withTokenValue("t")
        .header("alg", "none")
        .claim("sub", "UID123,ghost") // id,username
        .build();
    Authentication auth = mock(Authentication.class);
    when(auth.getPrincipal()).thenReturn(jwt);
    when(userRepo.findByUsername("ghost")).thenReturn(Optional.empty());

    assertThrows(AccessDeniedException.class, () -> service.getAuthenticatedUser(auth));
  }

  @Test
  void getAuthenticatedUser_whenJwtAndUserFound_returnsUser() {
    Jwt jwt = Jwt.withTokenValue("t")
        .header("alg", "none")
        .claim("sub", "UID123,alice")
        .build();
    Authentication auth = mock(Authentication.class);
    when(auth.getPrincipal()).thenReturn(jwt);

    User u = mock(User.class);
    when(userRepo.findByUsername("alice")).thenReturn(Optional.of(u));

    User out = service.getAuthenticatedUser(auth);

    assertSame(u, out);
    verify(userRepo).findByUsername("alice");
  }
}
