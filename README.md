# MyDrama Android SDK

Kotlin-first Android client for the MyDrama Worker API, with Java interop and
an OkHttp integration. The SDK registers an installation session, caches the
short-lived session token, and adds the Worker authentication and telemetry
headers to requests.

## Requirements

- Android API 21+
- Java 11 bytecode
- Kotlin or Java
- OkHttp 4.x
- No Google Play Services required — works on Fire TV, Fire Tablet, and AOSP devices

## Install

### JitPack

Add JitPack to dependency resolution:

```kotlin
repositories {
    maven("https://jitpack.io")
}
```

Then add the dependency:

```kotlin
dependencies {
    implementation("com.github.Machrafi:MydramaSDK:v1.0.1")
}
```

## Hiding the API key and base URL

Never put your API key or Worker URL as a plain string in source code — they
will be visible in your git history and any release ZIP downloaded from GitHub.

Instead, store them in `local.properties` (which is excluded from version
control by default) and inject them into your app via `BuildConfig`:

**`local.properties`** (never commit this file):
```properties
MYDRAMA_API_KEY=your-secret-api-key-here
MYDRAMA_BASE_URL=https://your-worker.workers.dev
```

**`app/build.gradle.kts`**:
```kotlin
import java.util.Properties

val localProps = Properties().apply {
    rootProject.file("local.properties").takeIf { it.exists() }?.inputStream()?.use(::load)
}

android {
    defaultConfig {
        buildConfigField("String", "MYDRAMA_API_KEY",
            "\"${localProps["MYDRAMA_API_KEY"] ?: ""}\"")
        buildConfigField("String", "MYDRAMA_BASE_URL",
            "\"${localProps["MYDRAMA_BASE_URL"] ?: ""}\"")
    }
}
```

Then use in code (as shown in the `examples/` folder):

```kotlin
MyDramaSDK.initialize(
    this,
    BuildConfig.MYDRAMA_API_KEY,
    MyDramaConfig.build { baseUrl(BuildConfig.MYDRAMA_BASE_URL) },
)
```

This way the key is never in source, never in the git history, and never in a
GitHub release ZIP.

## Initialize

Initialize once, normally from `Application.onCreate`. Use `BuildConfig` fields
for the API key and URL — see [Hiding the API key and base URL](#hiding-the-api-key-and-base-url):

```kotlin
val myDrama = MyDramaSDK.initialize(
    applicationContext,
    BuildConfig.MYDRAMA_API_KEY,
    MyDramaConfig.build {
        baseUrl(BuildConfig.MYDRAMA_BASE_URL)
    },
)
```

Initialization schedules session registration in the background. It never
logs the API key, session token, or request headers.

Java:

```java
MyDramaConfig config = MyDramaConfig.builder()
        .baseUrl(BuildConfig.MYDRAMA_BASE_URL)
        .build();
MyDramaClient myDrama =
        MyDramaSDK.initialize(getApplicationContext(), BuildConfig.MYDRAMA_API_KEY, config);
```

## Make requests

The bundled client is the simplest integration:

```kotlin
val call = myDrama.newCall("api/home?lang=en")
call.enqueue(callback)
```

To keep an existing OkHttp configuration:

```kotlin
val okHttp = myDrama.installOn(
    existingClient.newBuilder(),
).build()
```

The interceptor only adds credentials when the request origin exactly matches
the configured Worker origin. Requests to other hosts are passed through
without MyDrama headers.

## Custom Worker URL

Set your Worker URL in the config using `BuildConfig` (never hardcode it):

```kotlin
val config = MyDramaConfig.build {
    baseUrl(BuildConfig.MYDRAMA_BASE_URL)
    registerSessionOnInitialize(true)
}

val myDrama = MyDramaSDK.initialize(context, BuildConfig.MYDRAMA_API_KEY, config)
```

Java:

```java
MyDramaConfig config = MyDramaConfig.builder()
        .baseUrl(BuildConfig.MYDRAMA_BASE_URL)
        .registerSessionOnInitialize(true)
        .build();
MyDramaClient client = MyDramaSDK.initialize(context, BuildConfig.MYDRAMA_API_KEY, config);
```

HTTPS is required except for `localhost`, `127.0.0.1`, and `::1`.

## Worker contract

Session registration calls:

```text
POST /api/session/register
X-App-Package-ID: <application package>
X-App-Secure-Token: <SDK API key>
X-App-Instance-ID: <persisted random UUID>
X-App-Elapsed-Realtime-MS: <SystemClock.elapsedRealtime()>
Content-Type: application/json
```

The generated UUID is stored in private `SharedPreferences`. The SDK does not
read or store Android ID, IMEI, serial number, advertising ID, MAC address, or
another raw hardware identifier.

Each Worker request carries:

- `X-App-Package-ID`
- `X-App-Secure-Token`
- `X-App-Instance-ID`
- `X-App-Elapsed-Realtime-MS`
- `X-MyDrama-SDK-Version`
- `X-App-Version`
- `X-Android-API-Level`
- `X-Device-Manufacturer`
- `X-Device-Model`
- `X-App-Locale`
- `User-Agent: MyDrama-Android-SDK/<version> (...)`
- `Authorization: Bearer <sessionToken>` when a valid session is cached

The bootstrap credential remains attached because the current Worker supports
both the secure-token and session-JWT authentication paths. The Worker chooses
the bearer session when present.

## Session lifecycle

`initialize` registers asynchronously by default. To explicitly await a
registration from a background thread:

```kotlin
val registration = myDrama.registerSession()
```

Or use the Java-compatible callback and `Future`:

```kotlin
myDrama.registerSessionAsync { registration, error ->
    if (error == null) println(registration?.mode)
}
```

Tokens are persisted privately and refreshed when fewer than 30 seconds
remain. Registration errors fail the returned `Future`, are supplied to the
callback when present, and never expose the credential in the exception
message.

## Security notes

- Treat the SDK key as an app bootstrap credential, not a user password.
- Do not add a BODY-level HTTP logger to the authenticated client in release
  builds. If logging is necessary in debug builds, redact `Authorization` and
  `X-App-Secure-Token`.
- Credential headers are scoped to the configured Worker scheme, host, and
  port.
- Android application sandboxing protects the generated installation UUID and
  cached session. Clearing app data creates a new installation.

## Fire TV and Fire Tablet

The SDK has no dependency on Google Play Services and works on Amazon Fire OS
devices out of the box:

| Device family | Fire OS | Android API | Supported |
|---------------|---------|-------------|-----------|
| Fire TV Stick (2nd gen+) | Fire OS 5+ | API 22+ | ✓ |
| Fire TV Cube / 4K | Fire OS 6/7 | API 25/28 | ✓ |
| Fire HD / Fire 7 tablet | Fire OS 7 | API 28 | ✓ |

No extra setup is needed — just initialize the SDK the same way as on a
standard Android device. The SDK detects the device manufacturer and model via
`Build.MANUFACTURER` / `Build.MODEL` and includes them in telemetry headers,
so your Worker can distinguish Fire TV requests from phone requests.

## Consumer shrinking

Consumer ProGuard rules are packaged in the AAR. No application rule is
normally required. OkHttp's standard consumer rules are supplied by OkHttp.

## Build and test

```bash
./gradlew clean test lint assembleRelease
```

Publish to the local Maven repository:

```bash
./gradlew :mydrama-sdk:publishReleasePublicationToMavenLocal
```

Optional signing uses `SIGNING_KEY` and `SIGNING_PASSWORD` environment
variables. JitPack can build the publication with the included `jitpack.yml`.

## License

Apache License 2.0.
