package demo.app

import demo.library.Envelope
import demo.library.Store
import demo.library.describe
import demo.library.putSample
import demo.library.sampleRecord
import demo.library.sampleStore
import kotlin.system.exitProcess

class ForwardingStore(delegate: Store) : Store by delegate

private val cases: List<Pair<String, () -> Any>> = listOf(
    "1a. function call omitting a defaulted value-class parameter" to { describe(1) },
    "1b. constructor call omitting a defaulted value-class parameter" to { Envelope(1).id },
    "2.  copy() on a data class with a value-class property" to { sampleRecord().copy(id = 2).id },
    "3.  'by' delegation of an interface with a value-class member" to { putSample(ForwardingStore(sampleStore())) },
)

fun main() {
    val failures = cases.count { (name, case) ->
        runCatching(case).fold(
            onSuccess = { result ->
                println("OK      $name -> $result")
                false
            },
            onFailure = { error ->
                println("FAILED  $name")
                println("        $error")
                true
            },
        )
    }
    println()
    println("$failures of ${cases.size} cases compiled cleanly and failed at runtime.")
    if (failures > 0) exitProcess(1)
}
