package bubbling.server.handler;

import bubbling.common.codec.JsonCodec;
import bubbling.common.dto.auth.*;
import bubbling.common.enums.ProfileCharacter;
import bubbling.common.protocol.*;
import bubbling.server.domain.User;
import bubbling.server.network.ClientConnection;
import bubbling.server.network.MessageSender;
import bubbling.server.service.AuthService;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class AuthHandlerTest {

    private AuthHandler authHandler;
    private AuthService authService;
    private MessageSender messageSender;
    private ClientConnection connection;

    private final JsonCodec jsonCodec = new JsonCodec();

    @BeforeEach
    void setUp() {
        authService = mock(AuthService.class);
        messageSender = mock(MessageSender.class);
        connection = mock(ClientConnection.class);

        authHandler = new AuthHandler(authService, messageSender);
    }

    @Test
    @DisplayName("성공적인 회원가입")
    void registerSuccess() {
        // Given
        User user = User.create(
                "testuser01",
                "hashedPassword",
                "테스터",
                ProfileCharacter.EXAMPLE
        );

        when(authService.register("testuser01", "password123", "테스터")).thenReturn(user);

        MessageEnvelope request = new MessageEnvelope(
                MessageType.REGISTER,
                "req-1",
                jsonCodec.toPayload(new RegisterRequest("testuser01", "password123", "테스터"))
        );

        // When
        authHandler.register(connection, request);

        // Then
        verify(connection).authenticate(user);

        ArgumentCaptor<MessageEnvelope> captor = ArgumentCaptor.forClass(MessageEnvelope.class);

        verify(messageSender).send(eq(connection), captor.capture());

        MessageEnvelope response = captor.getValue();

        assertEquals(MessageType.REGISTER_RESULT, response.type());
        assertEquals("req-1", response.requestId());
    }

    @Test
    @DisplayName("성공적인 로그인")
    void loginSuccess() {
        // Given
        User user = User.create(
                "testuser01",
                "hashedPassword",
                "테스터",
                ProfileCharacter.EXAMPLE
        );

        when(authService.login("testuser01", "password123")).thenReturn(user);

        MessageEnvelope request = new MessageEnvelope(
                MessageType.LOGIN,
                "req-2",
                jsonCodec.toPayload(new LoginRequest("testuser01", "password123"))
        );

        // When
        authHandler.login(connection, request);

        // Then
        verify(connection).authenticate(user);

        ArgumentCaptor<MessageEnvelope> captor = ArgumentCaptor.forClass(MessageEnvelope.class);

        verify(messageSender).send(eq(connection), captor.capture());

        MessageEnvelope response = captor.getValue();

        assertEquals(MessageType.LOGIN_RESULT, response.type());
        assertEquals("req-2", response.requestId());
    }
}