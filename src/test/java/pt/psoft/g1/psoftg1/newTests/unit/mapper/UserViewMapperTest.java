package pt.psoft.g1.psoftg1.newTests.unit.mapper;

import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;
import pt.psoft.g1.psoftg1.usermanagement.api.UserView;
import pt.psoft.g1.psoftg1.usermanagement.api.UserViewMapper;
import pt.psoft.g1.psoftg1.usermanagement.model.User;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Functional opaque-box test for UserViewMapper.
 */
class UserViewMapperTest {

  private final UserViewMapper mapper = Mappers.getMapper(UserViewMapper.class);

  @Test
  void toUserView_maps_user_fields_correctly() {
    User user = User.newUser("alice@example.com", "secret123", "Alice Wonderland");
    user.assignId(UUID.randomUUID().toString());

    UserView view = mapper.toUserView(user);

    assertNotNull(view, "Mapped UserView should not be null");
    assertEquals(user.getId(), view.getId(), "ID should match");
    assertEquals("alice@example.com", view.getUsername(), "Username should match");
    assertEquals("Alice Wonderland", view.getFullName(), "Full name should be mapped correctly");
  }

  @Test
  void toUserViewList_maps_multiple_users_correctly() {
    User u1 = User.newUser("bob@example.com", "pw", "Bob Builder");
    User u2 = User.newUser("eve@example.com", "pw", "Eve Hacker");
    u1.assignId(UUID.randomUUID().toString());
    u2.assignId(UUID.randomUUID().toString());

    List<UserView> views = mapper.toUserView(List.of(u1, u2));

    assertNotNull(views, "List of UserViews should not be null");
    assertEquals(2, views.size(), "Should map exactly 2 users");

    assertEquals("Bob Builder", views.get(0).getFullName());
    assertEquals("bob@example.com", views.get(0).getUsername());

    assertEquals("Eve Hacker", views.get(1).getFullName());
    assertEquals("eve@example.com", views.get(1).getUsername());
  }

  @Test
  void toUserView_returns_null_when_input_is_null() {
    UserView result = mapper.toUserView((User) null);

    assertNull(result, "Mapper should return null when input is null");
  }
}
