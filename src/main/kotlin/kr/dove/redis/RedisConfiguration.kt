package kr.dove.redis

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Primary
import org.springframework.data.redis.connection.ReactiveRedisConnectionFactory
import org.springframework.data.redis.connection.RedisStandaloneConfiguration
import org.springframework.data.redis.connection.lettuce.LettuceClientConfiguration
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory
import org.springframework.data.redis.core.ReactiveRedisTemplate
import org.springframework.data.redis.serializer.*
import java.time.Duration

@ConfigurationProperties(prefix = "spring.data.redis")
@Configuration
class RedisConfiguration {
    lateinit var host: String
    lateinit var port: String

    //  Lettuce: Asynchronous
    //  Jedis: Synchronous
    @Primary
    @Bean
    fun lettuceConnectionFactory(): ReactiveRedisConnectionFactory {
        val config = LettuceClientConfiguration
            .builder()
            .commandTimeout(Duration.ofSeconds(1))
            .shutdownTimeout(Duration.ZERO)
            .build()
        return LettuceConnectionFactory(RedisStandaloneConfiguration(host, port.toInt()), config)
    }

    @Bean
    fun coffeeRedisTemplate(lettuceConnectionFactory: ReactiveRedisConnectionFactory): ReactiveRedisTemplate<String, Coffee> {
        val serializationContext = RedisSerializationContext
            .newSerializationContext<String, Coffee>(StringRedisSerializer())
            //  for string operations
            //  .key(StringRedisSerializer())
            //  .value(Jackson2JsonRedisSerializer(Coffee::class.java))

            //  hash operations
            //  format: <Main Key, Field, Value>
            .hashKey(StringRedisSerializer())
            .hashValue(StringRedisSerializer())
            .build()
        return ReactiveRedisTemplate(lettuceConnectionFactory, serializationContext)
    }
}