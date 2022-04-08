package kr.dove.redis

import org.junit.jupiter.api.BeforeEach
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.data.redis.connection.ReactiveRedisConnectionFactory
import org.springframework.data.redis.core.ReactiveRedisTemplate


@SpringBootTest
class SpringRedisApplicationTests(
    @Autowired private val lettuceConnectionFactory: ReactiveRedisConnectionFactory,
    @Autowired private val coffeeRedisTemplate: ReactiveRedisTemplate<String, Coffee>,
) {

    @BeforeEach
    fun setup() {
        lettuceConnectionFactory
            .reactiveConnection
            .serverCommands()
            .flushAll()
            .block()
    }
}
