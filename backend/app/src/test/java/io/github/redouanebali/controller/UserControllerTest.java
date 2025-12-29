package io.github.redouanebali.controller;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import io.github.redouanebali.model.User;
import io.github.redouanebali.security.SecurityProps;
import io.github.redouanebali.security.SecurityUtil;
import io.github.redouanebali.service.UserService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

@WebMvcTest(controllers = UserController.class)
@AutoConfigureMockMvc(addFilters = false)
class UserControllerTest {

  @Autowired
  private MockMvc mockMvc;

  @MockitoBean
  private UserService userService;

  @MockitoBean
  private SecurityProps securityProps;

  private MockedStatic<SecurityUtil> secMock;

  @BeforeEach
  public void setUp() {
    secMock = Mockito.mockStatic(SecurityUtil.class);
  }

  @AfterEach
  public void tearDown() {
    if (secMock != null) {
      secMock.close();
    }
  }

  @Test
  void getProfile_returns200_whenUserExists() throws Exception {
    String email = "test@example.com";
    User   user  = new User(email, "Test User", "fr");
    user.setProfileType(User.ProfileType.PLAYER);

    secMock.when(SecurityUtil::currentUserId).thenReturn(email);
    when(userService.getUserIfExists(email)).thenReturn(user);

    mockMvc.perform(MockMvcRequestBuilders.get("/user/profile"))
           .andExpect(status().isOk())
           .andExpect(jsonPath("$.email").value(email))
           .andExpect(jsonPath("$.name").value("Test User"))
           .andExpect(jsonPath("$.profileType").value("PLAYER"));
  }

  @Test
  void getProfile_returns404_whenUserDoesNotExist() throws Exception {
    String email = "unknown@example.com";

    secMock.when(SecurityUtil::currentUserId).thenReturn(email);
    when(userService.getUserIfExists(email)).thenReturn(null);

    mockMvc.perform(MockMvcRequestBuilders.get("/user/profile"))
           .andExpect(status().isNotFound());
  }

  @Test
  void getProfile_returns401_whenNotAuthenticated() throws Exception {
    secMock.when(SecurityUtil::currentUserId).thenReturn(null);

    mockMvc.perform(MockMvcRequestBuilders.get("/user/profile"))
           .andExpect(status().isUnauthorized());
  }

  @ParameterizedTest(name = "PUT /profile creates profile with profileType={0}")
  @CsvSource({"PLAYER", "SPECTATOR", "ORGANIZER"})
  void updateProfile_returns200_whenCreatingNewProfile(String profileType) throws Exception {
    String email   = "new@example.com";
    String name    = "New User";
    User   created = new User(email, name, "fr");
    created.setProfileType(User.ProfileType.valueOf(profileType));

    String requestBody = String.format("""
                                           {
                                             "name": "%s",
                                             "locale": "fr",
                                             "profileType": "%s",
                                             "city": "Paris",
                                             "country": "France"
                                           }
                                           """, name, profileType);

    secMock.when(SecurityUtil::currentUserId).thenReturn(email);
    when(userService.updateOrCreateProfile(eq(email), eq(name), eq("fr"), eq(User.ProfileType.valueOf(profileType)), eq("Paris"), eq("France")))
        .thenReturn(created);

    mockMvc.perform(MockMvcRequestBuilders.put("/user/profile")
                                          .contentType(MediaType.APPLICATION_JSON)
                                          .content(requestBody))
           .andExpect(status().isOk())
           .andExpect(jsonPath("$.email").value(email))
           .andExpect(jsonPath("$.name").value(name))
           .andExpect(jsonPath("$.profileType").value(profileType));
  }

  @Test
  void updateProfile_returns200_whenCreatingProfileWithMinimalInfo() throws Exception {
    String email   = "minimal@example.com";
    String name    = "Minimal User";
    User   created = new User(email, name, null);
    created.setProfileType(User.ProfileType.SPECTATOR);

    String requestBody = """
        {
          "name": "Minimal User",
          "profileType": "SPECTATOR"
        }
        """;

    secMock.when(SecurityUtil::currentUserId).thenReturn(email);
    when(userService.updateOrCreateProfile(eq(email), eq(name), eq(null), eq(User.ProfileType.SPECTATOR), eq(null), eq(null)))
        .thenReturn(created);

    mockMvc.perform(MockMvcRequestBuilders.put("/user/profile")
                                          .contentType(MediaType.APPLICATION_JSON)
                                          .content(requestBody))
           .andExpect(status().isOk())
           .andExpect(jsonPath("$.email").value(email))
           .andExpect(jsonPath("$.name").value(name))
           .andExpect(jsonPath("$.profileType").value("SPECTATOR"));
  }

  @Test
  void updateProfile_returns200_whenUpdatingExistingProfile() throws Exception {
    String email   = "existing@example.com";
    String newName = "Updated Name";
    User   updated = new User(email, newName, "en");
    updated.setProfileType(User.ProfileType.ORGANIZER);
    updated.setCity("London");
    updated.setCountry("UK");

    String requestBody = """
        {
          "name": "Updated Name",
          "locale": "en",
          "profileType": "ORGANIZER",
          "city": "London",
          "country": "UK"
        }
        """;

    secMock.when(SecurityUtil::currentUserId).thenReturn(email);
    when(userService.updateOrCreateProfile(eq(email), eq(newName), eq("en"), eq(User.ProfileType.ORGANIZER), eq("London"), eq("UK")))
        .thenReturn(updated);

    mockMvc.perform(MockMvcRequestBuilders.put("/user/profile")
                                          .contentType(MediaType.APPLICATION_JSON)
                                          .content(requestBody))
           .andExpect(status().isOk())
           .andExpect(jsonPath("$.email").value(email))
           .andExpect(jsonPath("$.name").value(newName))
           .andExpect(jsonPath("$.profileType").value("ORGANIZER"))
           .andExpect(jsonPath("$.city").value("London"))
           .andExpect(jsonPath("$.country").value("UK"));
  }

  @Test
  void updateProfile_returns400_whenProfileTypeInvalid() throws Exception {
    String requestBody = """
        {
          "name": "Test",
          "locale": "fr",
          "profileType": "INVALID_TYPE",
          "city": "Paris",
          "country": "France"
        }
        """;

    secMock.when(SecurityUtil::currentUserId).thenReturn("test@example.com");

    mockMvc.perform(MockMvcRequestBuilders.put("/user/profile")
                                          .contentType(MediaType.APPLICATION_JSON)
                                          .content(requestBody))
           .andExpect(status().isBadRequest());
  }

  @Test
  void updateProfile_returns400_whenProfileTypeNull() throws Exception {
    String requestBody = """
        {
          "name": "Test",
          "locale": "fr",
          "city": "Paris",
          "country": "France"
        }
        """;

    secMock.when(SecurityUtil::currentUserId).thenReturn("test@example.com");

    mockMvc.perform(MockMvcRequestBuilders.put("/user/profile")
                                          .contentType(MediaType.APPLICATION_JSON)
                                          .content(requestBody))
           .andExpect(status().isBadRequest());
  }

  @Test
  void updateProfile_returns400_whenNameBlank() throws Exception {
    String requestBody = """
        {
          "name": "",
          "locale": "fr",
          "profileType": "PLAYER",
          "city": "Paris",
          "country": "France"
        }
        """;

    secMock.when(SecurityUtil::currentUserId).thenReturn("test@example.com");

    mockMvc.perform(MockMvcRequestBuilders.put("/user/profile")
                                          .contentType(MediaType.APPLICATION_JSON)
                                          .content(requestBody))
           .andExpect(status().isBadRequest());
  }

  @Test
  void updateProfile_returns401_whenNotAuthenticated() throws Exception {
    String requestBody = """
        {
          "name": "Test",
          "locale": "fr",
          "profileType": "PLAYER",
          "city": "Paris",
          "country": "France"
        }
        """;

    secMock.when(SecurityUtil::currentUserId).thenReturn(null);

    mockMvc.perform(MockMvcRequestBuilders.put("/user/profile")
                                          .contentType(MediaType.APPLICATION_JSON)
                                          .content(requestBody))
           .andExpect(status().isUnauthorized());
  }

  @Test
  void getUserName_returns200_whenUserExists() throws Exception {
    String email = "test@example.com";
    String name  = "Test User";

    when(userService.getUserNameByEmail(email)).thenReturn(name);

    mockMvc.perform(MockMvcRequestBuilders.get("/user/{email}/name", email))
           .andExpect(status().isOk())
           .andExpect(jsonPath("$").value(name));
  }

  @Test
  void getUserName_returns404_whenUserNotFound() throws Exception {
    String email = "unknown@example.com";

    when(userService.getUserNameByEmail(email)).thenReturn(null);

    mockMvc.perform(MockMvcRequestBuilders.get("/user/{email}/name", email))
           .andExpect(status().isNotFound());
  }
}

