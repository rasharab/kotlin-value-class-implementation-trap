package demo.library

import demo.value.Payload

interface Store {
    fun put(payload: Payload): String
}

fun sampleStore(): Store = object : Store {
    override fun put(payload: Payload): String = "stored ${payload.text}"
}

fun putSample(store: Store): String = store.put(Payload("hello"))
