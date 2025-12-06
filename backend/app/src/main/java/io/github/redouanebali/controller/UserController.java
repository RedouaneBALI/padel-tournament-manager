package io.github.redouanebali.controller;

import io.github.redouanebali.dto.request.UpdateProfileRequest;
import io.github.redouanebali.model.User;
import io.github.redouanebali.security.SecurityUtil;
import io.github.redouanebali.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/user")
@RequiredArgsConstructor
@PreAuthorize("isAuthenticated()")
public class UserController {

  private final UserService userService;

  @GetMapping("/profile")
  public ResponseEntity<User> getProfile() {
    User user = SecurityUtil.getExistingUser();
    if (user == null) {
      return ResponseEntity.notFound().build();
    }
    return ResponseEntity.ok(user);
  }

  @PutMapping("/profile")
  public ResponseEntity<User> updateProfile(@RequestBody @Valid UpdateProfileRequest request) {
    String email   = SecurityUtil.currentUserId();
    User   updated = userService.updateProfile(email, request.name(), request.locale(), request.profileType(), request.city(), request.country());
    return ResponseEntity.ok(updated);
  }
}
