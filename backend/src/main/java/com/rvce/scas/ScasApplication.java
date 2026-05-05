package com.rvce.scas;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.data.redis.RedisProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.core.env.Environment;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;

@SpringBootApplication
public class ScasApplication {

	private static final Logger log = LoggerFactory.getLogger(ScasApplication.class);

	public static void main(String[] args) {
		SpringApplication.run(ScasApplication.class, args);
	}

	@Bean
    public ApplicationRunner logRedisConfiguration(Environment environment,
                                                   RedisProperties redisProperties,
                                                   RedisConnectionFactory redisConnectionFactory) {
        return args -> {
            log.info("Active profiles: {}", String.join(", ", environment.getActiveProfiles()));
            log.info("spring.redis.host => {}", environment.getProperty("spring.redis.host"));
            log.info("spring.redis.port => {}", environment.getProperty("spring.redis.port"));
            log.info("spring.data.redis.host => {}", environment.getProperty("spring.data.redis.host"));
            log.info("spring.data.redis.port => {}", environment.getProperty("spring.data.redis.port"));
            log.info("REDIS_HOST => {}", environment.getProperty("REDIS_HOST"));
            log.info("SPRING_REDIS_HOST => {}", environment.getProperty("SPRING_REDIS_HOST"));
            log.info("SPRING_DATA_REDIS_HOST => {}", environment.getProperty("SPRING_DATA_REDIS_HOST"));
            log.info("SPRING_DATA_REDIS_PORT => {}", environment.getProperty("SPRING_DATA_REDIS_PORT"));
            log.info("RedisProperties.host => {}", redisProperties.getHost());
            log.info("RedisProperties.port => {}", redisProperties.getPort());
            if (redisConnectionFactory instanceof LettuceConnectionFactory) {
                LettuceConnectionFactory lettuce = (LettuceConnectionFactory) redisConnectionFactory;
                log.info("Lettuce standalone host => {}", lettuce.getStandaloneConfiguration().getHostName());
                log.info("Lettuce standalone port => {}", lettuce.getStandaloneConfiguration().getPort());
            }
        };
    }

}
