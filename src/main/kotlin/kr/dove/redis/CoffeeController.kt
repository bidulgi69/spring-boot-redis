package kr.dove.redis

import org.springframework.beans.factory.InitializingBean
import org.springframework.data.redis.connection.ReactiveRedisConnectionFactory
import org.springframework.data.redis.core.*
import org.springframework.http.MediaType
import org.springframework.web.bind.annotation.*
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import java.nio.ByteBuffer
import java.util.*


@RestController
class CoffeeController(
    private val lettuceConnectionFactory: ReactiveRedisConnectionFactory,
    private val coffeeRedisTemplate: ReactiveRedisTemplate<String, Coffee>,
) : InitializingBean {

    //  Using scan() is more beneficial to redis performance than using keys().
    //  Because Redis acts as a single thread, the keys() command which blocks the thread is not recommended.
    @GetMapping(
        value = ["/coffees/{pattern}"],
        produces = [MediaType.APPLICATION_NDJSON_VALUE]
    )
    fun coffees(@PathVariable(name = "pattern") pattern: String): Flux<Coffee> {
        return coffeeRedisTemplate
            .scan(
                ScanOptions
                    .scanOptions()
                    .match("$pattern*")
                    .count(10L)
                    .build()
            )
            .flatMap { key ->
                coffeeRedisTemplate
                    .opsForHash<String, String>()
                    .multiGet(key, listOf("name", "taste"))
                    .flatMap { values ->
                        Mono.just(
                            Coffee(
                                key,
                                values[0],
                                Taste.valueOf(values[1])
                            )
                        )
                    }
            }
    }

    @GetMapping(
        value = ["/coffee/{id}"],
        produces = [MediaType.APPLICATION_JSON_VALUE]
    )
    fun get(@PathVariable(name = "id") id: String): Mono<Coffee> {
        return coffeeRedisTemplate
            .opsForHash<String, String>()
            .multiGet(id, listOf("name", "taste"))
            .flatMap { values ->
                Mono.just(
                    Coffee(
                        id,
                        values[0],
                        Taste.valueOf(values[1])
                    )
                )
            }
    }

    @PostMapping(
        value = ["/coffee"],
        consumes = [MediaType.APPLICATION_JSON_VALUE],
        produces = [MediaType.APPLICATION_JSON_VALUE]
    )
    fun post(@RequestBody name: String): Mono<Coffee> {
        return Mono.defer {
            val uuid = UUID.randomUUID().toString()
            coffeeRedisTemplate
                .opsForHash<String, String>()
                .putAll(
                    uuid,
                    mutableMapOf<String, String>().apply {
                        set("name", name)
                        set("taste", Taste.BLAND.toString())
                    }
                )
                .then(
                    coffeeRedisTemplate
                        .opsForHash<String, String>()
                        .multiGet(uuid, listOf("name", "taste"))
                        .flatMap { values ->
                            //  emit instantly
                            Mono.just(
                                Coffee(
                                    uuid,
                                    values[0],
                                    Taste.valueOf(values[1])
                                )
                            )
                        }
                )
        }
    }

    @PatchMapping(
        value = ["/coffee/{id}"],
        consumes = [MediaType.APPLICATION_JSON_VALUE],
        produces = [MediaType.APPLICATION_JSON_VALUE]
    )
    fun patch(
        @PathVariable(name = "id") id: String,
        @RequestBody taste: Taste
    ): Mono<Coffee> {
        return coffeeRedisTemplate
            .opsForHash<String, String>()
            .put(id, "taste", taste.toString())
            .then(
                coffeeRedisTemplate
                    .opsForHash<String, String>()
                    .multiGet(id, listOf("name", "taste"))
                    .flatMap { values ->
                        Mono.just(
                            Coffee(
                                id,
                                values[0],
                                Taste.valueOf(values[1])
                            )
                        )
                    }
            )
    }

    @DeleteMapping(
        value = ["/coffee/{id}"],
        produces = [MediaType.APPLICATION_JSON_VALUE]
    )
    fun delete(
        @PathVariable(name = "id") id: String
    ): Mono<Long> {
        return coffeeRedisTemplate
            //  .delete(id)
            .unlink(id) //  The actual memory reclaiming here happens asynchronously. (unlike delete())
    }

    private fun init() {
        val nameFieldAsByteBuffer: ByteBuffer = "name".wrap()
        val tasteFieldAsByteBuffer: ByteBuffer = "taste".wrap()
        lettuceConnectionFactory
            .reactiveConnection
            .serverCommands()
            .flushAll() //  remove all data
            .thenMany(Flux.defer {
                coffeeRedisTemplate
                    .execute { conn ->
                        Flux.just("Jet Black Redis", "Darth Redis", "Black Alert Redis")
                            .flatMap { name ->
                                val key = UUID.randomUUID().toString().wrap()
                                conn
                                    .hashCommands()
                                    .hMSet(
                                        key,
                                        mutableMapOf<ByteBuffer, ByteBuffer>().apply {
                                            set(nameFieldAsByteBuffer, name.wrap())
                                            set(tasteFieldAsByteBuffer, Taste.BLAND.toString().wrap())
                                        }
                                    )
                            }
                    }
            })
            .subscribe()
    }

    override fun afterPropertiesSet() {
        //  initialize data
        init()
    }

    fun String.wrap(): ByteBuffer {
        return ByteBuffer.wrap(this.toByteArray())
    }
}