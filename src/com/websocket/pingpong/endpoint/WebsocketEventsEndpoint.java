package com.websocket.pingpong.endpoint;

import javax.websocket.OnClose;
import javax.websocket.OnError;
import javax.websocket.OnMessage;
import javax.websocket.OnOpen;
import javax.websocket.PongMessage;
import javax.websocket.Session;
import javax.websocket.server.ServerEndpoint;

import com.websocket.pingpong.scheduler.WebsocketPingPongSchedulerService;

@ServerEndpoint(value = "/events")
public class WebsocketEventsEndpoint {
	@OnOpen
	public void onOpen(Session session) {
		// Start scheduler on first WS session.
		WebsocketPingPongSchedulerService.getInstance().schedulePingMessages();
		// Authenticate request
		// Register the session
		WebsocketPingPongSchedulerService.getInstance().registerSession(session);
	}

	@OnMessage
	public void onMessage(Session session, String message) {
		// Authenticate request
		// Perform any business logic
	}

	@OnMessage
	public void onPongMessage(Session session, PongMessage message) {
		// Handle ping-pong
		WebsocketPingPongSchedulerService.getInstance().handlePong(session);
	}

	@OnClose
	public void onClose(Session session) {
		// De-register session from ping-pong
		WebsocketPingPongSchedulerService.getInstance().deregisterSession(session);
	}

	@OnError
	public void onError(Session session, Throwable throwable) {
		// Handle errors
	}
}