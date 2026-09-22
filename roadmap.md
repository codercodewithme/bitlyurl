# Shortly — rebuild this project by hand

Use this file as a **build order**, not a copy-paste dump. Recreate the same Bitly-style app in a **new empty folder**. Keep this repo open as a reference when you get stuck.

Target stack: **Java 17**, **Spring Boot 3.3**, **Thymeleaf**, **H2**, **vanilla JS**, **ZXing**.

---

## How to use this roadmap

1. Finish one phase before starting the next.
2. After each phase, hit the **Checkpoint**. Do not skip it.
3. When a step says “look at the existing file”, open that path in this repo and rewrite it yourself.
4. Do not jump to the UI until the API and redirect work in the browser or with curl/Postman.

Suggested new folder: `D:\Documents\SOHAM\Shortly-Manual`

---

## What you are building

```text
Browser
  ├─ GET  /              home (shorten form)
  ├─ GET  /links         all short links
  ├─ GET  /stats/{code}  analytics page
  ├─ GET  /{code}        302 redirect + click tracking
  └─ /api/...            JSON + QR image

Spring controllers → services → JPA repositories → H2 file DB
```

A visitor pastes a long URL, gets `http://localhost:8080/abc1234`, opens it, and lands on the original site. You store the mapping and count every click.

---

## Phase 0 — Tools (30 min)

- [ ] Install **JDK 17** (Temurin is fine).
- [ ] Set `JAVA_HOME` to the JDK **folder**, not `bin`, and **no trailing `\`**.
  - Good: `C:\Program Files\Eclipse Adoptium\jdk-17.0.20.101-hotspot`
  - Bad: `...\hotspot\` or `...\hotspot\bin`
- [ ] Confirm: `java -version` prints 17.
- [ ] Maven Wrapper later, so a global `mvn` install is optional.

**Windows trap:** a trailing `\` on `JAVA_HOME` plus `Program Files` makes Maven say *JAVA_HOME is not defined correctly*.

---

## Phase 1 — Empty Spring Boot app (45 min)

### 1.1 Create the Maven project

Create `pom.xml` with parent `spring-boot-starter-parent` **3.3.4** and:

| Dependency | Why |
| --- | --- |
| `spring-boot-starter-web` | REST + Tomcat |
| `spring-boot-starter-data-jpa` | entities + repositories |
| `spring-boot-starter-thymeleaf` | HTML pages |
| `spring-boot-starter-validation` | `@Valid` on requests |
| `h2` (runtime) | file database |
| `spring-boot-starter-test` | JUnit + Mockito |

Do **not** add ZXing yet. Add it in Phase 7.

### 1.2 First Java class

```
src/main/java/com/soham/bitly/BitlyApplication.java
```

`@SpringBootApplication` + `main` that calls `SpringApplication.run`.

### 1.3 Config

```
src/main/resources/application.properties
```

Set at least:

```properties
server.port=8080
app.base-url=http://localhost:8080
spring.datasource.url=jdbc:h2:file:./data/bitlydb;AUTO_SERVER=TRUE
spring.datasource.username=sa
spring.datasource.password=
spring.jpa.hibernate.ddl-auto=update
spring.h2.console.enabled=true
spring.h2.console.path=/h2-console
```

### 1.4 Prove it boots

```bat
.\mvnw.cmd spring-boot:run
```

Or generate the wrapper after you have Maven once: `mvn -N wrapper:wrapper`.

**Checkpoint:** [http://localhost:8080](http://localhost:8080) is not a crash. H2 console opens at `/h2-console` (JDBC URL `jdbc:h2:file:./data/bitlydb`, user `sa`, empty password).

---

## Phase 2 — Data model (1–2 hr)

Build persistence first. No UI.

### 2.1 Entity `ShortUrl`

Path: `entity/ShortUrl.java`

| Field | Notes |
| --- | --- |
| `id` | `@Id` + `IDENTITY` |
| `shortCode` | unique, max 30 |
| `originalUrl` | max 2048 |
| `title` | optional |
| `customAlias` | boolean |
| `clickCount` | long, default 0 |
| `expiresAt` | `LocalDateTime`, nullable |
| `createdAt` | set in `@PrePersist` |
| `clicks` | `@OneToMany` → `ClickEvent`, cascade all, orphanRemoval |

Add `isExpired()`: `expiresAt != null && now.isAfter(expiresAt)`.

### 2.2 Entity `ClickEvent`

Path: `entity/ClickEvent.java`

| Field | Notes |
| --- | --- |
| `id` | identity |
| `shortUrl` | `@ManyToOne(optional = false)` |
| `clickedAt` | set in `@PrePersist` |
| `ipAddress` | max 45 |
| `userAgent` | max 512 |
| `referrer` | max 512 |
| `deviceType` | Desktop / Mobile / Tablet / Unknown |
| `browser` | Chrome / Edge / … |

### 2.3 Repositories

```
repository/ShortUrlRepository.java
repository/ClickEventRepository.java
```

`ShortUrlRepository` needs:

- `findByShortCode`
- `existsByShortCode`
- `findAllByOrderByCreatedAtDesc`
- `countByCreatedAtAfter`
- `sumClickCount` (JPQL `coalesce(sum(s.clickCount), 0)`)
- `countActive` (null expiry or expiry in the future)

`ClickEventRepository` needs:

- `findByShortUrlOrderByClickedAtDesc`
- `findByShortUrlAndClickedAtAfterOrderByClickedAtAsc`
- `countByClickedAtAfter`
- `findTop20ByShortUrlOrderByClickedAtDesc`

**Checkpoint:** app starts with no mapping errors. After first run, H2 shows tables `SHORT_URLS` and `CLICK_EVENTS`.

---

## Phase 3 — Core shorten + redirect (2–3 hr)

This is the product. Everything else hangs off it.

### 3.1 Exceptions

Create one class each in `exception/`:

- `ResourceNotFoundException`
- `InvalidUrlException`
- `AliasTakenException`
- `ExpiredUrlException`

### 3.2 DTOs

```
dto/ShortenRequest.java
dto/ShortenResponse.java
```

Request fields: `originalUrl` (required), `title`, `customAlias`, `expiresAt`.

Validate:

- `@NotBlank` + `@Size(max = 2048)` on URL
- alias `@Pattern`: empty **or** `^[A-Za-z0-9_-]{3,30}$`

Response fields: `id`, `shortCode`, `shortUrl`, `originalUrl`, `title`, `customAlias`, `clickCount`, `expiresAt`, `createdAt`.

### 3.3 `ShortCodeGenerator`

Path: `service/ShortCodeGenerator.java`

- Alphabet: `0-9a-zA-Z` (Base62)
- Length 7
- `SecureRandom`
- Loop until `existsByShortCode` is false (cap attempts so it cannot spin forever)

### 3.4 `UserAgentParser`

Static helpers:

- device: tablet / mobile / desktop
- browser: Edge before Chrome (Edge’s UA contains `Chrome` and `Edg/`)

### 3.5 `UrlService` (the brain)

Path: `service/UrlService.java`

Implement in this order:

1. **`normalizeAndValidateUrl`**
   - trim
   - if no `scheme://`, prepend `https://`
   - allow only `http` / `https`
   - require a host
   - reject `javascript:`, `data:`, `file:`
2. **`shorten`**
   - reserved aliases: `api`, `links`, `stats`, `h2-console`, `error`, `css`, `js`, `images`
   - custom alias → use it; else generate
   - throw if alias taken
3. **`findAll` / `findByCode` / `delete`**
4. **`resolveAndTrack`**
   - load by code or 404
   - expired → `ExpiredUrlException`
   - save a `ClickEvent` (IP from `X-Forwarded-For` or `remoteAddr`)
   - increment `clickCount`
   - return `originalUrl`
5. **`toShortUrl`** → `{app.base-url}/{code}` (`@Value("${app.base-url}")`, strip trailing `/`)

### 3.6 Controllers

**`controller/api/UrlApiController`** — `@RestController` + `/api`

| Method | Path | Action |
| --- | --- | --- |
| POST | `/urls` | shorten |
| GET | `/urls` | list |
| GET | `/urls/{code}` | one |
| DELETE | `/urls/{code}` | delete |

**`controller/RedirectController`** — `@Controller`

```text
GET /{shortCode:[A-Za-z0-9_-]{3,30}}
```

- success → `return "redirect:" + destination`
- missing → status 404 + `error` view (you can use a plain message until Phase 6)
- expired → status 410

Spring will still prefer more specific mappings (`/links`, `/stats/{code}`, `/api/**`) over `/{shortCode}`.

### 3.7 API errors

`exception/GlobalExceptionHandler` with `@RestControllerAdvice(basePackages = "com.soham.bitly.controller.api")`:

| Exception | HTTP |
| --- | --- |
| `ResourceNotFoundException` | 404 |
| `InvalidUrlException` | 400 |
| `AliasTakenException` | 409 |
| `ExpiredUrlException` | 410 |
| `MethodArgumentNotValidException` | 400 |

Return `dto/ErrorResponse`: `status`, `message`, `path`, `timestamp`.

**Checkpoint (must pass before UI):**

```http
POST /api/urls
{ "originalUrl": "https://spring.io", "customAlias": "spring" }

GET  /spring          → 302 Location: https://spring.io
GET  /api/urls/spring → clickCount is 1
POST /api/urls        { "originalUrl": "javascript:alert(1)" } → 400
POST /api/urls        { "originalUrl": "https://x.com", "customAlias": "spring" } → 409
GET  /nope-nope       → 404
```

---

## Phase 4 — Analytics API (1–2 hr)

### 4.1 Extra DTOs

- `DailyClickDto` — `date`, `clicks`
- `ClickEventDto` — when, ip, referrer, device, browser
- `DashboardStatsResponse` — totalLinks, totalClicks, linksToday, clicksToday, activeLinks
- `AnalyticsResponse` — link info + `clicksByDay` + maps for device / browser / referrer + last 20 clicks

### 4.2 `AnalyticsService`

- **Dashboard:** counts from repositories; “today” = `LocalDate.now().atStartOfDay()`.
- **Per code:** last **14 days**, fill missing days with `0`.
- Empty referrer → `"Direct"`. Otherwise use the referrer host.

### 4.3 Endpoints

```http
GET /api/stats/summary
GET /api/urls/{code}/analytics
```

**Checkpoint:** shorten a link, open it twice, analytics `totalClicks` is 2 and today has a bar.

---

## Phase 5 — Tests (1 hr)

Write these **before** polishing the UI so you do not break the core.

`src/test/java/com/soham/bitly/service/UrlServiceTest.java` (Mockito):

- [ ] missing scheme becomes `https://`
- [ ] reserved alias `api` rejected
- [ ] taken alias → `AliasTakenException`
- [ ] `javascript:` rejected
- [ ] track click increments count
- [ ] expired link throws
- [ ] unknown code throws not found
- [ ] custom alias is stored as the code

`UserAgentParserTest`: iPhone → Mobile/Safari, Windows Edge UA → Desktop/Edge.

```bat
.\mvnw.cmd test
```

**Checkpoint:** all tests green.

---

## Phase 6 — Pages (2–3 hr)

Do HTML structure first, then CSS. Keep JS last.

### 6.1 Page controller

`controller/PageController.java`

| Path | Template |
| --- | --- |
| `/` | `index` |
| `/links` | `links` |
| `/stats/{code}` | `analytics` (add `code` to the model) |

### 6.2 Templates

```
templates/fragments.html   nav + footer
templates/index.html       shorten form + dashboard cards + result box
templates/links.html       table + search
templates/analytics.html   stats, chart canvas, QR img, breakdowns
templates/error.html       404 / expired
```

Shared chrome: brand **Shortly**, links to `/`, `/links`.

Home form ids (JS will use them):

- `shorten-form`, `originalUrl`, `title`, `customAlias`, `expiresAt`, `shorten-btn`, `result`

### 6.3 Static files

```
static/css/style.css
static/js/app.js         toast, api(), copy, home form, dashboard stats
static/js/links.js       list, search, delete
static/js/analytics.js   Chart.js + breakdowns + QR
static/favicon.svg
```

Rules for JS:

- `fetch` `/api/...` with `Content-Type: application/json`
- show server `message` on error
- escape HTML when you inject strings
- datetime-local: if length is 16, append `:00` before POST
- Chart.js from CDN only on the analytics page

**Checkpoint:** you can create a link in the UI, copy it, open it, see it on `/links`, open `/stats/{code}`.

---

## Phase 7 — QR codes + polish (1–2 hr)

### 7.1 ZXing

In `pom.xml`:

```xml
<dependency>
  <groupId>com.google.zxing</groupId>
  <artifactId>core</artifactId>
  <version>3.5.3</version>
</dependency>
<dependency>
  <groupId>com.google.zxing</groupId>
  <artifactId>javase</artifactId>
  <version>3.5.3</version>
</dependency>
```

`service/QrCodeService.java` → PNG bytes for the short URL.

```http
GET /api/urls/{code}/qr   → image/png
```

### 7.2 Config extras

- `config/WebConfig` — CORS on `/api/**` (handy if you later split the frontend)
- `config/JacksonConfig` — `LocalDateTime` as `yyyy-MM-dd'T'HH:mm:ss`
- `config/DataSeeder` — `CommandLineRunner`, seed only when `shortUrlRepository.count() == 0`
  - demo codes: `spring`, `wiki`, plus a random-style one
  - a few fake clicks so charts are not empty

### 7.3 `.gitignore`

Ignore `target/`, `data/`, `.idea/`, `*.log`.

**Checkpoint:** analytics page shows a QR you can download. Restart the app; seeded + created links are still in `./data/bitlydb`.

---

## Phase 8 — README and run scripts (30 min)

Write a short README in the **new** repo:

- features
- how to run (`.\mvnw.cmd spring-boot:run`)
- `JAVA_HOME` note
- API table
- H2 console login

Optional Windows helper `run.cmd` that sets `JAVA_HOME` then calls `mvnw.cmd spring-boot:run`.

---

## File checklist (same as this repo)

Tick these off as you create them.

```text
pom.xml
.gitignore
src/main/java/com/soham/bitly/BitlyApplication.java
src/main/java/com/soham/bitly/config/WebConfig.java
src/main/java/com/soham/bitly/config/JacksonConfig.java
src/main/java/com/soham/bitly/config/DataSeeder.java
src/main/java/com/soham/bitly/controller/PageController.java
src/main/java/com/soham/bitly/controller/RedirectController.java
src/main/java/com/soham/bitly/controller/api/UrlApiController.java
src/main/java/com/soham/bitly/dto/...
src/main/java/com/soham/bitly/entity/ShortUrl.java
src/main/java/com/soham/bitly/entity/ClickEvent.java
src/main/java/com/soham/bitly/exception/...
src/main/java/com/soham/bitly/repository/...
src/main/java/com/soham/bitly/service/UrlService.java
src/main/java/com/soham/bitly/service/AnalyticsService.java
src/main/java/com/soham/bitly/service/ShortCodeGenerator.java
src/main/java/com/soham/bitly/service/QrCodeService.java
src/main/java/com/soham/bitly/service/UserAgentParser.java
src/main/resources/application.properties
src/main/resources/templates/*.html
src/main/resources/static/css/style.css
src/main/resources/static/js/*.js
src/test/java/com/soham/bitly/service/UrlServiceTest.java
src/test/java/com/soham/bitly/service/UserAgentParserTest.java
```

---

## Suggested calendar

| Day | Phase | Outcome |
| --- | --- | --- |
| 1 | 0–1 | App boots, H2 console works |
| 2 | 2–3 | Shorten + redirect + click count via API |
| 3 | 4–5 | Analytics API + tests |
| 4 | 6 | Working UI |
| 5 | 7–8 | QR, seed data, README |

If you only have a weekend: Day 1 = Phases 0–3, Day 2 = Phases 4–8.

---

## Do this, not that

| Do | Don’t |
| --- | --- |
| Validate http/https only | Store whatever the user typed |
| Keep `/api`, `/links`, `/stats` reserved | Allow alias `api` (it would hide your API) |
| Track clicks inside a `@Transactional` method | Redirect without saving the click |
| Use file H2 (`./data/bitlydb`) while learning | In-memory H2 (data dies on restart) |
| Test `UrlService` with mocks | Only “test” by clicking the UI |
| Escape HTML in JS | Drop API strings into `innerHTML` raw |

---

## When you are stuck

1. Re-read the matching class in **this** repo.
2. Hit the phase checkpoint again with curl/Postman.
3. Check the terminal: port `8080` already in use means an old Java process is still running.

Done when a stranger can: paste a URL → copy a short link → open it → see the click on `/stats/{code}`.
