package com.poketechtest.infrastructure.config;

import java.net.http.HttpClient;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

@Configuration
@EnableConfigurationProperties(PokeApiProperties.class)
public class PokeApiConfig {

    @Bean(destroyMethod = "close")
    HttpClient pokeApiHttpClient(PokeApiProperties pokeApiProperties) {
        return HttpClient.newBuilder()
                .connectTimeout(pokeApiProperties.connectTimeout())
                .build();
    }

    @Bean
    RestClient pokeApiRestClient(HttpClient pokeApiHttpClient, PokeApiProperties pokeApiProperties) {
        JdkClientHttpRequestFactory requestFactory = new JdkClientHttpRequestFactory(pokeApiHttpClient);
        requestFactory.setReadTimeout(pokeApiProperties.readTimeout());
        return RestClient.builder()
                .baseUrl(pokeApiProperties.baseUrl())
                .requestFactory(requestFactory)
                .build();
    }

    /** Executor for the parallel PokeAPI calls: one cheap virtual thread per call. */
    @Bean(destroyMethod = "close")
    ExecutorService virtualThreadExecutor() {
        return Executors.newVirtualThreadPerTaskExecutor();
    }
}
