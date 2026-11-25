package bubbling.server.handler;

import bubbling.common.codec.JsonCodec;
import bubbling.common.dto.game.GameRankingData;
import bubbling.common.dto.game.GameRankingRequest;
import bubbling.common.dto.game.GameRankingResponse;
import bubbling.common.dto.game.GameResultRequest;
import bubbling.common.protocol.MessageEnvelope;
import bubbling.common.protocol.MessageType;
import bubbling.server.domain.User;
import bubbling.server.exception.BaseException;
import bubbling.server.exception.code.GameExceptionCode;
import bubbling.server.network.ClientConnection;
import bubbling.server.network.MessageSender;
import bubbling.server.service.game.GameResultService;
import bubbling.server.service.game.search.GameResultSearchStrategy;
import bubbling.server.service.game.search.GameResultSearchStrategyResolver;

import java.time.LocalDateTime;
import java.util.List;

public class GameResultHandler {

    private final GameResultService gameResultService;
    private final GameResultSearchStrategyResolver resolver;

    private final MessageSender messageSender;
    private final JsonCodec jsonCodec = new JsonCodec();

    public GameResultHandler(
            GameResultService gameResultService,
            GameResultSearchStrategyResolver resolver,
            MessageSender messageSender
    ) {
        this.gameResultService = gameResultService;
        this.resolver = resolver;
        this.messageSender = messageSender;
    }

    public void ranking(ClientConnection connection, MessageEnvelope message) {
        GameRankingRequest request = jsonCodec.fromPayload(
                message.payload(),
                GameRankingRequest.class
        );

        connection.requireAuthenticatedUser();

        GameResultSearchStrategy strategy = resolver.resolve(request.rankingType());

        if(request.limit() < 1) {
            throw new BaseException(GameExceptionCode.INVALID_SEARCH_CONDITION);
        }

        List<GameRankingData> results = strategy.aggregate(request.gameType());
        List<GameRankingData> rankings = results.subList(0, Math.min(results.size(), request.limit()));

        GameRankingResponse payload = new GameRankingResponse(
                request.rankingType(),
                rankings,
                LocalDateTime.now()
        );
        MessageEnvelope response = new MessageEnvelope(
                MessageType.GAME_RANKING,
                message.requestId(),
                jsonCodec.toPayload(payload)
        );
        messageSender.send(connection, response);
    }

    public void result(ClientConnection clientConnection, MessageEnvelope message) {
        GameResultRequest request = jsonCodec.fromPayload(
                message.payload(),
                GameResultRequest.class
        );

        User user = clientConnection.requireAuthenticatedUser();

        gameResultService.create(user, request.gameType(), request.score());

        MessageEnvelope response = new MessageEnvelope(
                MessageType.GAME_RESULT_SAVED,
                message.requestId(),
                null
        );
        messageSender.send(clientConnection, response);
    }
}
