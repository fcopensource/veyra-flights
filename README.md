# Veyra Flights

An Android app to search, compare and book flights, with a small server that fetches live flights from [Duffel](https://duffel.com) (300+ airlines).

```
android/   the app (Kotlin, Jetpack Compose, Material 3)
server/    the server the app talks to (Node.js, no dependencies)
```

The app never holds the Duffel key: it talks to **your** server, and only the server talks to Duffel.

## 1. Run the server

Needs Node.js 20 or newer.

```bash
cd server
npm start          # http://localhost:8787
npm test           # the server's tests
```

With no key it serves **demo flights**: real airports and believable times and prices, clearly labelled "Demo" in the app. Good for trying everything out.

### Use real flights (Duffel)

1. Sign up at [app.duffel.com](https://app.duffel.com) (free).
2. **Developers → Access tokens → Create token.** Start with a **test** token (`duffel_test_…`): real flights and prices, but bookings are tests and nothing is charged.
3. Copy `server/.env.example` to `server/.env` and paste the token after `DUFFEL_TOKEN=`.
   Don't share the token or put it anywhere else. `.env` is never committed.
4. Restart the server. `http://localhost:8787/health` now says `"mode": "test"`.

### Before taking real bookings

A **live** token (`duffel_live_…`) shows real fares, but a real booking is paid from **your Duffel balance**. So live bookings stay switched off (the app explains that booking isn't open yet) until:

1. the app collects payment from the traveller (for example with Razorpay or Stripe), and
2. you set `LIVE_BOOKINGS=on` in `server/.env`.

## 2. Put the server online

The Play Store app needs the server at a public **https** address, for example `https://flights-api.veyraeditor.com`:

- On Hostinger (Node.js hosting), Render, Railway or any VPS: upload `server/`, set the start command to `npm start`, and add the same `DUFFEL_TOKEN` (and `APP_KEY`, if you use one) as environment variables.
- Put that address in `android/gradle.properties` as `veyraApiUrlRelease`.

## 3. Build the app

Needs the Android SDK (Android Studio installs it) and Java 17.

```bash
cd android
./gradlew testDebugUnitTest   # unit tests
./gradlew assembleDebug       # app/build/outputs/apk/debug/app-debug.apk
```

The debug app talks to the server on your computer: start `npm start` in `server/`, then run the app on the Android emulator (it reaches your computer at `10.0.2.2`).

## 4. Publish on Google Play

1. **Create your upload key** (once). You choose the passwords; keep the file and passwords safe and never share them:
   ```bash
   keytool -genkeypair -v -keystore veyra-flights-upload.jks -keyalg RSA -keysize 2048 -validity 10000 -alias upload
   ```
2. Create `android/keystore.properties` (it's never committed):
   ```properties
   storeFile=C:/path/to/veyra-flights-upload.jks
   storePassword=…
   keyAlias=upload
   keyPassword=…
   ```
3. Build the Play Store bundle:
   ```bash
   ./gradlew bundleRelease       # app/build/outputs/bundle/release/app-release.aab
   ```
4. In [Google Play Console](https://play.google.com/console): create the app **Veyra Flights**, upload the `.aab` to **Internal testing** first, fill in the store listing, content rating, and the **Data safety** form:
   - The app collects travellers' names, dates of birth, email and phone **only to book flights**, sends them to the airline through the server, and stores bookings on the phone.
   - It doesn't use ads or analytics.
5. A privacy policy URL is required, for example `https://veyraeditor.com/flights/privacy`.

Every update: raise `versionCode` (and `versionName`) in `android/app/build.gradle.kts`.
