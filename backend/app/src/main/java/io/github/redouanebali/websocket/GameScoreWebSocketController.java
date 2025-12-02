package io.github.redouanebali.websocket;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;

/**
 * WebSocket controller for broadcasting game score updates.
 */
@Controller
public class GameScoreWebSocketController {

  private final SimpMessagingTemplate messagingTemplate;

  @Autowired
  public GameScoreWebSocketController(SimpMessagingTemplate messagingTemplate) {
    this.messagingTemplate = messagingTemplate;
  }

  /**
   * Broadcasts score update to all subscribers for a specific game.
   *
   * @param gameId the game identifier
   * @param scoreDTO the score data transfer object
   */
  public void broadcastScoreUpdate(Long gameId, Object scoreDTO) {
    messagingTemplate.convertAndSend("/topic/game/" + gameId, scoreDTO);
  }
}

