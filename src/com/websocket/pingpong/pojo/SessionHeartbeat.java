package com.websocket.pingpong.pojo;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import javax.websocket.Session;

import com.websocket.pingpong.config.Properties;

import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = false)
public class SessionHeartbeat {
	private Session userSession;
	private AtomicInteger retry = new AtomicInteger(Properties.MAX_RETRY_COUNT);
	private AtomicReference<Long> lastPingAt = new AtomicReference<>(System.currentTimeMillis());
	private AtomicReference<Long> lastPongReceived = new AtomicReference<>(System.currentTimeMillis());
	private AtomicReference<Long> lastMessageOnInMillis = new AtomicReference<>(System.currentTimeMillis());

	public SessionHeartbeat(Session session) {
		this.userSession = session;
	}
}
