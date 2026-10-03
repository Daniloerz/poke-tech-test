package com.poketechtest.infrastructure.config;

import com.poketechtest.application.port.out.PokemonCatalogPage;
import com.poketechtest.domain.model.EvolutionNode;
import com.poketechtest.domain.model.Pokemon;
import com.poketechtest.domain.model.PokemonSpecies;
import java.time.Duration;
import org.springframework.cache.annotation.CachingConfigurer;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.interceptor.CacheErrorHandler;
import org.springframework.cache.interceptor.LoggingCacheErrorHandler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.serializer.JacksonJsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext.SerializationPair;

@Configuration
@EnableCaching
public class CacheConfig implements CachingConfigurer {

    // Bump the version when a cached class changes, so entries with the old shape are never read (US02 TDR-004).
    private static final String CACHE_VERSION = "v2";
    private static final String KEY_PREFIX = "poke-tech-test::" + CACHE_VERSION + "::";

    @Bean
    RedisCacheManager cacheManager(RedisConnectionFactory redisConnectionFactory, PokeApiProperties pokeApiProperties) {
        Duration cacheTtl = pokeApiProperties.cacheTtl();
        return RedisCacheManager.builder(redisConnectionFactory)
                .withCacheConfiguration(CacheNames.POKEAPI_PAGES, jsonCacheConfiguration(PokemonCatalogPage.class, cacheTtl))
                .withCacheConfiguration(CacheNames.POKEAPI_POKEMON, jsonCacheConfiguration(Pokemon.class, cacheTtl))
                .withCacheConfiguration(CacheNames.POKEAPI_SPECIES, jsonCacheConfiguration(PokemonSpecies.class, cacheTtl))
                .withCacheConfiguration(CacheNames.POKEAPI_EVOLUTION_CHAINS, jsonCacheConfiguration(EvolutionNode.class, cacheTtl))
                .disableCreateOnMissingCache()
                .build();
    }

    /** Redis is only an optimization: cache errors are logged and the call goes to PokeAPI. */
    @Override
    public CacheErrorHandler errorHandler() {
        return new LoggingCacheErrorHandler();
    }

    private RedisCacheConfiguration jsonCacheConfiguration(Class<?> valueType, Duration cacheTtl) {
        return RedisCacheConfiguration.defaultCacheConfig()
                .entryTtl(cacheTtl)
                .prefixCacheNameWith(KEY_PREFIX)
                .disableCachingNullValues()
                .serializeValuesWith(SerializationPair.fromSerializer(new JacksonJsonRedisSerializer<>(valueType)));
    }
}
