package io.github.redouanebali.security;

import io.github.redouanebali.model.Game;
import io.github.redouanebali.model.Tournament;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Component;

/**
 * Centralized authorization service for tournament and game access control. Eliminates duplication of authorization logic across controllers and
 * services.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class AuthorizationService {

  private final SecurityProps securityProps;

  /**
   * Check if the current user can edit the given tournament.
   *
   * @param tournament the tournament to check
   * @param userId the current user ID
   * @return true if user is super-admin, owner, or editor
   */
  public boolean canEditTournament(Tournament tournament, String userId) {
    if (userId == null) {
      return false;
    }
    Set<String> superAdmins = securityProps.getSuperAdmins();
    return superAdmins.contains(userId) || tournament.isEditableBy(userId);
  }

  /**
   * Check if the current user can edit the given game.
   *
   * @param game the game to check
   * @param userId the current user ID
   * @return true if user is super-admin or owner
   */
  public boolean canEditGame(Game game, String userId) {
    if (userId == null) {
      return false;
    }
    Set<String> superAdmins = securityProps.getSuperAdmins();
    if (superAdmins.contains(userId)) {
      return true;
    }
    String owner = game.getCreatedBy();
    return owner != null && owner.equals(userId);
  }

  /**
   * Check if the current user has super-admin privileges.
   *
   * @param userId the current user ID
   * @return true if user is super-admin
   */
  public boolean isSuperAdmin(String userId) {
    if (userId == null) {
      return false;
    }
    return securityProps.getSuperAdmins().contains(userId);
  }

  /**
   * Require that the current user can edit the tournament. Throws AccessDeniedException if user lacks permission.
   *
   * @param tournament the tournament to check
   * @param userId the current user ID
   * @throws AccessDeniedException if user cannot edit
   */
  public void requireTournamentEditPermission(Tournament tournament, String userId) {
    if (!canEditTournament(tournament, userId)) {
      log.warn("Access denied for user '{}' on tournament {} (owner={}, editors={})",
               userId, tournament.getId(), tournament.getOwnerId(), tournament.getEditorIds());
      throw new AccessDeniedException("You are not allowed to modify this tournament");
    }
  }

  /**
   * Require that the current user can edit the game. Throws AccessDeniedException if user lacks permission.
   *
   * @param game the game to check
   * @param userId the current user ID
   * @throws AccessDeniedException if user cannot edit
   */
  public void requireGameEditPermission(Game game, String userId) {
    if (!canEditGame(game, userId)) {
      log.warn("Access denied for user '{}' on game {} (owner={})",
               userId, game.getId(), game.getCreatedBy());
      throw new AccessDeniedException("You are not allowed to modify this game");
    }
  }
}

