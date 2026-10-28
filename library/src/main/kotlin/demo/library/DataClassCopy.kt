package demo.library

import demo.value.Payload

data class Record(val id: Int, val payload: Payload)

fun sampleRecord(): Record = Record(1, Payload("hello"))
