package com.mqtt.edge_server.config;

import com.mqtt.edge_server.websocket.WebSocketHandler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

@Configuration
@EnableWebSocket
public class WebSocketConfig implements WebSocketConfigurer {

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(mqttDataHandler(), "/mqtt-data")
                .setAllowedOrigins("*");
        registry.addHandler(androidLocationHandler(), "/android-location")
                .setAllowedOrigins("*");
    }

    @Bean
    public WebSocketHandler mqttDataHandler() {
        return new WebSocketHandler();
    }

    @Bean
    public WebSocketHandler androidLocationHandler() {
        return new WebSocketHandler();
    }
}
