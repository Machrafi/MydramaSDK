# Official DramaSDK Android SDK

Build short drama apps with **DramaSDK**, the official Android client for the
[DramaSDK API](https://sdk.dramasdk.com/docs). Load series, chapters and current
video URLs through one API, with automatic application identity, Secure Token
headers and short-lived installation sessions.

Supports Kotlin and Java, Android 5.0+ (API 21), Fire TV, Fire Tablet and AOSP.
Google Play Services are not required. Java 11 bytecode; OkHttp 4.x.

## Install

Add JitPack to your project's repositories:

```kotlin
maven { url = uri("https://jitpack.io") }
```

Add the official SDK:

```kotlin
implementation("com.github.Machrafi:MydramaSDK:v1.1.0")
```

## Connect your app

1. Register your actual Android `applicationId` at [DramaSDK](https://dramasdk.com).
2. Copy the app's active **Secure Token**. This is your SDK API key.
3. Store it outside source control and initialize the SDK once in `Application.onCreate`.

In your private `local.properties`:

```properties
DRAMASDK_API_KEY=YOUR_APP_SECURE_TOKEN
```

In `app/build.gradle.kts`, inject it into BuildConfig. JSON encoding safely
escapes the string instead of interpolating an unescaped key into generated code:

```kotlin
import java.util.Properties
import groovy.json.JsonOutput

val localProps = Properties().apply {
    rootProject.file("local.properties").takeIf { it.exists() }
        ?.inputStream()?.use { load(it) }
}
val dramaKey = providers.environmentVariable("DRAMASDK_API_KEY").orNull
    ?: localProps.getProperty("DRAMASDK_API_KEY", "")

android {
    buildFeatures { buildConfig = true }
    defaultConfig {
        buildConfigField("String", "DRAMASDK_API_KEY", JsonOutput.toJson(dramaKey))
    }
}
```

Initialize using the official API by default:

```kotlin
val drama = MyDramaSDK.initialize(applicationContext, BuildConfig.DRAMASDK_API_KEY)
```

Java:

```java
MyDramaClient drama = MyDramaSDK.initialize(this, BuildConfig.DRAMASDK_API_KEY);
```

The SDK supplies `X-App-Package-ID` from the installed application, your
`X-App-Secure-Token`, installation metadata and a valid session bearer token when
available. Register the exact package name—changing headers cannot provision a
new application.

## Make requests

Each method returns an OkHttp `Call`. Use `enqueue()` or run `execute()` on a
background thread; always close the response.

```kotlin
drama.home(lang = "en", pageSize = 10).enqueue(object : okhttp3.Callback {
    override fun onFailure(call: okhttp3.Call, error: java.io.IOException) {
        // Show a retry message. Do not log credentials or authorization headers.
    }
    override fun onResponse(call: okhttp3.Call, response: okhttp3.Response) {
        response.use {
            if (!it.isSuccessful) {
                // 401: authentication/session; 403: access; 429: quota/rate limit.
                return
            }
            val json = org.json.JSONObject(it.body?.string().orEmpty())
            val categories = json.getJSONObject("data").getJSONArray("categories")
            // Read stories and keep the exact story_id returned by DramaSDK.
        }
    }
})
```

```kotlin
drama.detail(storyId)
drama.chapters(storyId, index = 1, episodePageSize = 100)
drama.stream(storyId, episode = 1)
// Or use the documented API contract directly:
drama.newCall("api/search?keyword=love&lang=en")
```

| Request | Endpoint |
|---|---|
| Home series | `GET /api/home` |
| Search | `GET /api/search?keyword=...` |
| Series detail | `GET /api/detail/{story_id}/v2` |
| Chapters | `GET /api/chapters/{story_id}` |
| Current playback URL | `GET /api/stream?bookId=...&episode=1` |
| Installation session | `POST /api/session/register` (handled by SDK) |

Responses contain catalog data under `data.categories[].stories[]`. Read the
pagination fields and request subsequent chapter pages when indicated. Locked or
unavailable chapters are hidden: build your episode list from returned episodes,
not from total_episodes. Original episode numbers can have gaps. Series counts
describe visible catalog entries, not fully unlocked series or verified videos. Resolve
playback through `stream()` when the user opens an episode; signed video URLs may
expire. Pass the returned `video_url` to your player with a separate media client.
Do not attach API keys to third-party video hosts or store signed URLs permanently.
A catalog response alone does not prove that every video plays.

## Sessions and security

The SDK starts session registration in the background. You can wait explicitly
from a background thread using `drama.registerSession()` or use
`drama.registerSessionAsync(callback)`. Session failures are reported through the
callback; normal API requests still use the app-bound Secure Token.

Version 1.1.0 keeps session tokens in memory, clears the matching old plaintext
session preferences and blocks redirects on SDK-managed clients. When installed
on an existing OkHttp client, a network interceptor strips SDK credentials and
installation headers before a redirect reaches a different origin. `newCall()`
rejects external origins. Normal platform TLS verification remains enabled.

Use a separate token per app, enforce access and quotas on the server, and revoke
or replace compromised tokens from the dashboard. Keep keys out of Git, screenshots,
analytics, crash reports and HTTP logs. If adding an HTTP logger, redact
`X-App-Secure-Token` and `Authorization`.

**An APK cannot hold an unextractable API key.** Package headers and device metadata
are not cryptographic proof of app identity. These protections reduce credential
exposure; server authentication, revocation and quotas remain necessary. No SDK
can promise to prevent all scraping or reverse engineering.

## Custom deployment

The default is `https://sdk.dramasdk.com/`. For your own compatible deployment:

```kotlin
val config = MyDramaConfig.build { baseUrl("https://your-api.example/") }
val drama = MyDramaSDK.initialize(applicationContext, BuildConfig.DRAMASDK_API_KEY, config)
```

Remote API URLs must use HTTPS and cannot contain credentials, query parameters
or fragments. HTTP is permitted only for loopback development. SDK-managed clients
do not follow redirects; configure the final API origin directly.

## Upgrade from 1.0.x

Use version `v1.1.0`, replace custom legacy API configuration with the default
DramaSDK origin, and keep your registered package and current token. Public
`com.mydrama.sdk` class names remain compatible. Session tokens are registered
again after process restarts. Review redirect assumptions if reusing the SDK client.

## Build

```sh
./gradlew :mydrama-sdk:testDebugUnitTest :mydrama-sdk:assembleRelease
```

API reference: [sdk.dramasdk.com/docs](https://sdk.dramasdk.com/docs).
Developer dashboard: [dramasdk.com](https://dramasdk.com).
Source and releases: [Machrafi/MydramaSDK](https://github.com/Machrafi/MydramaSDK).
