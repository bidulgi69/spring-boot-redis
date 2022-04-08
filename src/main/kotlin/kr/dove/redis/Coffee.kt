package kr.dove.redis

import java.util.*

data class Coffee(
    val id: String = UUID.randomUUID().toString(),
    val name: String = "Randomly generated ${(Math.random() * 10).toInt()}",
    var taste: Taste = Taste.BLAND
)

enum class Taste { BITTER, SWEET, SOUR, TART, OILY, BLAND }