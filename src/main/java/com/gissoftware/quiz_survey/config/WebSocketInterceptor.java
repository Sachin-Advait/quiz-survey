package com.gissoftware.quiz_survey.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

@Component
public class WebSocketInterceptor implements WebSocketMessageBrokerConfigurer {

  private static final Logger log = LoggerFactory.getLogger(WebSocketInterceptor.class);

  @Override
  public void configureClientInboundChannel(ChannelRegistration registration) {
    registration.interceptors(
        new ChannelInterceptor() {

          @Override
          public Message<?> preSend(Message<?> message, MessageChannel channel) {
            StompHeaderAccessor accessor = StompHeaderAccessor.wrap(message);
            log.info("STOMP COMMAND: {}", accessor.getCommand());
            return message;
          }
        });
  }
}
