package com.websocket.pingpong.scheduler;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import javax.websocket.CloseReason;
import javax.websocket.CloseReason.CloseCodes;
import javax.websocket.Session;

import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import com.websocket.pingpong.config.Properties;
import com.websocket.pingpong.pojo.SessionHeartbeat;

public final class WebsocketPingPongSchedulerService {
	private static ScheduledExecutorService websocketHeartbeatMainExecutorService;
	private static ExecutorService webSocketHeartbeatWorkerExecutorService;
	private final Map<Session, SessionHeartbeat> sessionHeartbeats = new ConcurrentHashMap<>();

	private WebsocketPingPongSchedulerService() {
	}

	private static final class SchedulerService {
		private static final WebsocketPingPongSchedulerService instance = new WebsocketPingPongSchedulerService();
	}

	public static WebsocketPingPongSchedulerService getInstance() {
		return SchedulerService.instance;
	}

	/**
	 * This method needs to be invoked during application startup, for the scheduler
	 * to be initiated.
	 */
	public void schedulePingMessages() {
		websocketHeartbeatMainExecutorService.scheduleAtFixedRate(this::pingClients,
				Properties.WEBSOCKET_PING_SCHEDULED_TIME_IN_SECONDS,
				Properties.WEBSOCKET_PING_SCHEDULED_TIME_IN_SECONDS, TimeUnit.SECONDS);
	}

	/**
	 * Add session to the registry.
	 * 
	 * @param heartbeat
	 */
	public void registerSession(Session session) {
		sessionHeartbeats.put(session, new SessionHeartbeat(session));
	}

	/**
	 * Remove the from the registry.
	 * 
	 * @param heartbeat
	 */
	public void deregisterSession(Session session) {
		sessionHeartbeats.remove(session);
	}

	public void handlePong(Session session) {
		SessionHeartbeat heartbeat = sessionHeartbeats.getOrDefault(session, new SessionHeartbeat(session));
		heartbeat.getLastPongReceived().set(System.currentTimeMillis());
		heartbeat.getRetry().set(Properties.MAX_RETRY_COUNT);
	}

	private void pingClients() {
		sessionHeartbeats.values().parallelStream().forEach(heartbeat -> {
			submitToWorker(heartbeat);
		});
	}

	private void submitToWorker(SessionHeartbeat heartbeat) {
		webSocketHeartbeatWorkerExecutorService.submit(() -> {
			if (heartbeat.getRetry().get() == 0 || hasIdleTimeExpired(heartbeat)) {
				closeSession(heartbeat);
			} else {
				pingToClient(heartbeat);
			}
		});
	}

	private void pingToClient(SessionHeartbeat heartbeat) {
		Session session = heartbeat.getUserSession();
		try {
			if (session.isOpen()) {
				JsonObject payloadJson = createPingPayload(session); // �Maximum allowed payload of 125 bytes only
				ByteBuffer payload = ByteBuffer.wrap(payloadJson.toString().getBytes());

				session.getBasicRemote().sendPing(payload);
				heartbeat.getLastPingAt().set(System.currentTimeMillis());
				heartbeat.getRetry().set(Properties.MAX_RETRY_COUNT);
			} else {
				heartbeat.getRetry().decrementAndGet();
			}
		} catch (Exception e) {
			heartbeat.getRetry().decrementAndGet();
		}
	}

	private JsonObject createPingPayload(Session session) {
		JsonObject payload = new JsonObject();
		payload.add("sessionId", new JsonPrimitive(session.getId()));
		payload.add("pingedAt", new JsonPrimitive(LocalDateTime.now().toString()));
		return payload;
	}

	private boolean hasIdleTimeExpired(SessionHeartbeat heartbeat) {
		Long lastWsSessionPingTimeInMillis = heartbeat.getLastMessageOnInMillis().get();
		return (System.currentTimeMillis() - lastWsSessionPingTimeInMillis) > TimeUnit.MINUTES
				.toMillis(Properties.WEBSOCKET_SESSION_IDLE_TIME_IN_MINUTES);
	}

	private void closeSession(SessionHeartbeat heartbeat) {
		Session session = heartbeat.getUserSession();
		try {
			session.close(new CloseReason(CloseCodes.NORMAL_CLOSURE,
					"SessionId: " + session.getId() + ", Client does not respond"));
		} catch (IOException e) {
		}
	}
}
