package demo.library

import demo.value.Payload

fun describe(id: Int, payload: Payload? = null): String = "describe($id, ${payload?.text})"

class Envelope(val id: Int, val payload: Payload? = null)
