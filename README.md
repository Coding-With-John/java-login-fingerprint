# Java login with a Fingerprint check

A simple Spring Boot username/password login system with a protected page,
built as a starter for a Coding With John video. The point of the video is
the login flow, and the Fingerprint integration is one step inside it: a
60-90 second beat where a credential-stuffing attempt shows up and
Fingerprint is how the app spots it.

This is a thin starter, not polished video-ready code. It runs end to end
with no Fingerprint account, using simulated signals (see below).

## Run it

```bash
./mvnw spring-boot:run
```

On Windows: `.\mvnw.cmd spring-boot:run`

Then open http://localhost:8080

Demo users: `demo` / `password123`, `alice` / `correct-horse`

## How the Fingerprint check works

1. The login page loads the Fingerprint JavaScript agent, which identifies
   the visitor's browser/device and returns an event id. That id is sent
   with the login POST as `requestId`.
2. `POST /api/login` runs the Fingerprint gate **before** the password
   check (`LoginGate`). The backend calls Fingerprint's Server API with the
   event id and gets back the trusted visitor id plus Smart Signals (bot,
   VPN, incognito, proxy, suspect score).
3. The gate decides, in order: bot detected -> reject (403); this device
   has already failed 5 logins in 10 minutes -> reject (429); the user has
   logged in before but never from this device -> allow but flag step-up.
4. Only then does the password get checked. A failed password is recorded
   against the visitor id, so repeated failures from one device trip the
   velocity rule. A successful login remembers the device.

Fingerprint only supplies signals. Every allow / step-up / block decision
is our own code. We never send usernames or passwords to Fingerprint, only
the event id.

## Demo the stuffing block

Start the app, then run the demo script. It fires 5 failed logins from one
simulated device, shows the 6th blocked with 429, shows a different device
logging in fine, and shows a bot visit rejected with 403.

```bash
bash demo/stuffing-demo.sh
```

On Windows: `powershell -File demo/stuffing-demo.ps1`

## What is placeholder

- **No real Fingerprint account yet.** With no keys configured the app
  uses `StubFingerprintService`, which simulates signals deterministically
  from the request id (see its Javadoc). Set `FINGERPRINT_PUBLIC_KEY` and
  `FINGERPRINT_SECRET_KEY` to switch on the real service.
- **Real service call shape verified, live behaviour not.** The SDK calls
  in `RealFingerprintService` were checked against the published Java
  Server SDK v8.8.0 jar, but which Smart Signals are active on our plan,
  the fail-open vs fail-closed choice on lookup failure, and the data
  region still need confirming at the kick-off call. Each is marked TODO
  in the code.
- **JavaScript agent snippet.** `static/app.js` uses the documented v4 CDN
  import (`https://fpjscdn.net/v4/<PUBLIC_API_KEY>`). Confirm the exact
  snippet with Fingerprint's tech team before filming.
- **Step-up is flagged, not enforced.** An unrecognized device returns
  `stepUp: true`, but the login still completes. A real version would ask
  for a one-time code first.
- **In-memory attempt tracking.** `LoginAttemptTracker` resets on restart.
  A real deployment would use Redis or the database.
- **Demo users and dev database.** H2 in-memory, two seeded users with
  demo passwords. Not production data handling.

## Project layout

- `auth/` - login/logout/me endpoints; the gate runs before the password check
- `fingerprint/` - the Fingerprint pattern (interface, stub, real service,
  config, attempt tracker, login gate). This package is identical in the
  sibling repo `java-jwt-auth-fingerprint`.
- `user/` - the demo user entity and repository
- `config/` - password hashing (BCrypt) and demo-user seeding
- `static/` - plain HTML/JS login page
- `demo/` - credential-stuffing demo scripts

## Tech

Java 21, Spring Boot 3.5.4, Maven, Spring Web + Spring Data JPA + H2.
No Spring Security web filters: password hashing uses only the
`spring-security-crypto` module, so the login flow stays plain and visible.
