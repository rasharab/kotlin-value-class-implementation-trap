# Kotlin value classes + `implementation` = runtime crash, no compile error

If a library exposes a Kotlin `@JvmInline value class` from another module in its public API, but depends on that module
with `implementation` instead of `api`, consumers of the library can **compile cleanly and then crash at runtime** with
`NoSuchMethodError` or `AbstractMethodError`. The compiler gives no error and no warning.

## Layout

| Module        | Role                                                                                                   |
|---------------|--------------------------------------------------------------------------------------------------------|
| `value-class` | Defines `@JvmInline value class Payload(val text: String)`.                                            |
| `library`     | Uses `Payload` in its public API. Depends on `value-class` with **`implementation`**.                  |
| `app`         | Depends only on `library`. Never names `Payload`, so it never sees `value-class` at compile time.       |

`library/build.gradle.kts` switches the dependency to `api` when you pass `-PuseApi=true`. Nothing else changes.

## Run it

Requires JDK 17 or newer. Gradle 9.8.0 and Kotlin 2.4.10 come from the wrapper.

```bash
make
```

This builds `app` with Gradle, then runs it on a plain JVM through the generated start script, so Gradle is not involved
at runtime. `./gradlew :app:run` shows the same thing.

```text
FAILED  1a. function call omitting a defaulted value-class parameter
        java.lang.NoSuchMethodError: 'java.lang.String demo.library.DefaultArgumentsKt.describe$default(int, demo.value.Payload, int, java.lang.Object)'
FAILED  1b. constructor call omitting a defaulted value-class parameter
        java.lang.NoSuchMethodError: 'void demo.library.Envelope.<init>(int, demo.value.Payload, int, kotlin.jvm.internal.DefaultConstructorMarker)'
FAILED  2.  copy() on a data class with a value-class property
        java.lang.NoSuchMethodError: 'demo.library.Record demo.library.Record.copy$default(demo.library.Record, int, demo.value.Payload, int, java.lang.Object)'
FAILED  3.  'by' delegation of an interface with a value-class member
        java.lang.AbstractMethodError: Receiver class demo.app.ForwardingStore does not define or inherit an implementation of the resolved method 'abstract java.lang.String put-aaer_b0(java.lang.String)' of interface demo.library.Store.

4 of 4 cases compiled cleanly and failed at runtime.
```

The app exits with status 1, so `make` ends with `make: *** [repro] Error 1`.

## The fix: `api`

```bash
make fixed
```

This rebuilds with `library` depending on `value-class` through `api` (`-PuseApi=true`) and runs the same app.

```text
OK      1a. function call omitting a defaulted value-class parameter -> describe(1, null)
OK      1b. constructor call omitting a defaulted value-class parameter -> 1
OK      2.  copy() on a data class with a value-class property -> 2
OK      3.  'by' delegation of an interface with a value-class member -> stored hello

0 of 4 cases compiled cleanly and failed at runtime.
```

## Why it compiles

A value class is erased to its underlying type on the JVM, and any function with a value-class parameter gets a hashed
name. `javap` on `library` shows what actually exists:

```text
public static java.lang.String describe-ngilFFk$default(int, java.lang.String, int, java.lang.Object);
public demo.library.Envelope(int, java.lang.String, int, kotlin.jvm.internal.DefaultConstructorMarker);
public static demo.library.Record copy-0eKfRhA$default(demo.library.Record, int, java.lang.String, int, java.lang.Object);
public abstract java.lang.String put-aaer_b0(java.lang.String);
```

When `app` compiles, `Payload.class` is not on its classpath, so the compiler cannot see `@JvmInline`. It assumes `Payload`
is an ordinary class and emits the unmangled, boxed signature: `describe$default(int, Payload, ...)` instead of
`describe-ngilFFk$default(int, String, ...)`.

Normally that still fails to compile with `Cannot access class 'demo.value.Payload'`, because the call site has to type an
expression as `Payload`. These cases slip through because the compiler fills in the `Payload` slot itself:

| Case | Code in `app`                                  | Why `app` never types a `Payload` expression                                                 |
|------|------------------------------------------------|----------------------------------------------------------------------------------------------|
| 1a   | `describe(1)`                                  | The omitted default is supplied by `describe$default`.                                       |
| 1b   | `Envelope(1)`                                  | Same, through the synthetic defaults constructor.                                            |
| 2    | `sampleRecord().copy(id = 2)`                  | Every `copy` parameter defaults to the current value, even with no declared defaults.        |
| 3    | `class ForwardingStore(d: Store) : Store by d` | The compiler generates `put(Payload)` forwarders, leaving the real `put-aaer_b0` unimplemented. |

All of these, by contrast, fail to compile in `app` with `Cannot access class 'demo.value.Payload'`:

```kotlin
describe(1, null)
sampleRecord().payload
describe(1, sampleRecord().payload)
record.copy(payload = sampleRecord().payload)
listOf(sampleRecord()).map { it.payload }
val (_, payload) = sampleRecord()
```

## Stale builds hide the fix

`gradle.properties` sets `kotlin.incremental=false` so that flipping `-PuseApi` takes effect immediately. With incremental
compilation on, switching `implementation` to `api` changes `app`'s classpath but none of its sources, so the broken
class files are kept and `:app:run -PuseApi=true` still fails until you run `./gradlew clean`.
