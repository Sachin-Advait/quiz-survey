package com.gissoftware.quiz_survey.service;

import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.messaging.SessionConnectEvent;
import org.springframework.web.socket.messaging.SessionConnectedEvent;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Component
public class WebSocketEventListener {

  private static final Logger log = LoggerFactory.getLogger(WebSocketEventListener.class);

  @EventListener
  public void connect(SessionConnectEvent event) {
    log.info("CONNECT: {}", event.getMessage());
  }

  @EventListener
  public void connected(SessionConnectedEvent event) {
    log.info("CONNECTED: {}", event.getMessage());
  }

  @EventListener
  public void disconnect(SessionDisconnectEvent event) {
    log.info("DISCONNECT: sessionId={}", event.getSessionId());
  }
}
