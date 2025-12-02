package io.github.redouanebali.websocket;

import static org.mockito.Mockito.verify;

import io.github.redouanebali.dto.response.UpdateScoreDTO;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.Mockito;
import org.springframework.messaging.simp.SimpMessagingTemplate;

class GameScoreWebSocketControllerTest {

  @ParameterizedTest
  @CsvSource({
      "1,true,TEAM_A",
      "2,false,TEAM_B",
      "3,true,''"
  })
  void broadcastScoreUpdate_sendsMessageToCorrectTopic(Long gameId, boolean tournamentUpdated, String winner) {
    SimpMessagingTemplate        messagingTemplate = Mockito.mock(SimpMessagingTemplate.class);
    GameScoreWebSocketController controller        = new GameScoreWebSocketController(messagingTemplate);
    UpdateScoreDTO               dto               = new UpdateScoreDTO();
    dto.setTournamentUpdated(tournamentUpdated);
    dto.setWinner(winner.isEmpty() ? null : io.github.redouanebali.model.TeamSide.valueOf(winner));
    controller.broadcastScoreUpdate(gameId, dto);
    verify(messagingTemplate).convertAndSend("/topic/game/" + gameId, dto);
  }
}

