package com.ecommerce.order_service.config;

import io.netty.channel.ChannelOption;
import io.netty.handler.timeout.ReadTimeoutHandler;
import io.netty.handler.timeout.WriteTimeoutHandler;
import org.springframework.cloud.client.loadbalancer.LoadBalanced;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.reactive.function.client.ClientRequest;
import org.springframework.web.reactive.function.client.ExchangeFilterFunction;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.netty.http.client.HttpClient;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

@Configuration
public class WebClientConfig {

    @Bean
    @LoadBalanced
    public WebClient.Builder webClientBuilder() {
        HttpClient httpClient = HttpClient.create()
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 3000)
                .responseTimeout(Duration.ofSeconds(3))
                .doOnConnected(conn -> conn
                        .addHandlerLast(new ReadTimeoutHandler(3, TimeUnit.SECONDS))
                        .addHandlerLast(new WriteTimeoutHandler(3, TimeUnit.SECONDS))
                );
        return WebClient.builder()
                .clientConnector(new ReactorClientHttpConnector(httpClient));
    }

    //Filter to propagate JWT token to downstream services
    private ExchangeFilterFunction addJwtToken(){
        return ExchangeFilterFunction.ofRequestProcessor(clientRequest -> {
            var authentication = SecurityContextHolder.getContext().getAuthentication();
            if (authentication instanceof JwtAuthenticationToken jwtAuth){
                String token = jwtAuth.getToken().getTokenValue();

                ClientRequest newRequest = ClientRequest.from(clientRequest)
                        .header("Authorization", "Bearer " + token).build();

                return reactor.core.publisher.Mono.just(newRequest);
            }
            return reactor.core.publisher.Mono.just(clientRequest);
        });
    }
}
