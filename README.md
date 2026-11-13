# Lem0nka Tools

A small web app that helps our dubbing team work with subtitles.

**What it does**

- **Splits subtitles by actor.** Upload an `.ass` file, tell it who voices which character, and get a separate file for every actor (or every character) in `.ass` or `.srt`.
- **Shows actors who they play.** Each project has a sheet with character pictures. You can send an actor a link and they can open it without an account. It also exports to Google Sheets.
- **Counts words.** Drop in `.srt` / `.ass` files, pick characters, and it counts lines, words and numbers.

Plus the usual: a profile page, invite-only registration and a small admin panel.

## Running it

You need JDK 21 or newer.

```bash
./gradlew run          # starts on http://localhost:8000
./gradlew buildFatJar  # makes build/libs/lemonka-tools.jar
./gradlew test         # runs the tests
```

### On a server (Docker)

Build the jar on your machine, then put `lemonka-tools.jar` next to the `Dockerfile` on the server:

```dockerfile
FROM amazoncorretto:25-headless
WORKDIR /opt/app
COPY lemonka-tools.jar .
EXPOSE 8000
CMD ["java", "-Djava.awt.headless=true", "-jar", "/opt/app/lemonka-tools.jar"]
```

```yaml
services:
    lemonka:
        build: .
        restart: unless-stopped
        ports:
            - "8052:8000"
        environment:
            - DB_PATH=/opt/app/data/cache.db
            # - PUBLIC_URL=https://tools.example.com
        volumes:
            - ./data:/opt/app/data
```

### Settings

| Variable     | Default    | What it's for |
|--------------|------------|---------------|
| `PORT`       | `8000`     | Port the app listens on |
| `DB_PATH`    | `cache.db` | Where the database lives |
| `PUBLIC_URL` | (guessed)  | The site's real address. Set it if the app is behind a domain or reverse proxy, otherwise invite and share links may point to the wrong place |

## Your data

Everything lives in **one file**: `cache.db`. That covers accounts, projects, uploaded subtitles and character pictures. To back up, copy that file.

When a new version changes the database, the app makes a copy first (`cache.db.backup-…`) and then upgrades it by itself on startup.

## First start

- **New install:** register, and the first account becomes the admin. Nobody else can sign up without an invite code, which you create in **Адміністрування → Запрошення**.
- **Coming from the old version:** just point it at your old `cache.db`. Everything carries over, people stay logged in, and the oldest account becomes admin.

## Working on the code

It's Kotlin all the way: [Ktor](https://ktor.io) for the server, [kotlinx.html](https://github.com/Kotlin/kotlinx.html) and [kotlin-css](https://github.com/JetBrains/kotlin-wrappers) for pages and styles, [HTMX](https://htmx.org) for interactivity, [Exposed](https://github.com/JetBrains/Exposed) + SQLite for data. The only JavaScript is one small generic file for drag and drop (`static/js/dnd.js`), and you won't need to touch it.

```
src/main/kotlin/com/magnariuk/lemonka/
├── features/      each page or tool in its own folder (routes + UI)
├── ui/            reusable components (Button, Card, Dialog…) and the theme
├── subs/          reading, counting and writing subtitles (no web stuff)
├── data/db/       database tables, migrations, queries
├── auth/          login, sessions, roles, invites
└── web/           glue: the Feature interface and HTML/HTMX helpers
```

UI is written like Compose:

```kotlin
Card("Мої актори") {
    Row(gap = 8) {
        TextField(null, "name", placeholder = "Ім'я")
        Button("Додати", ButtonVariant.Primary, icon = Icon.Plus) {
            hxPost("/profile/actors"); hxTarget("#actors-card"); hxSwap("outerHTML")
        }
    }
}
```

**To add a new page:** make a `Feature` in `features/<name>/`, add it to the list in `web/Features.kt`, and if it has its own styles, register them in `ui/theme/Stylesheet.kt`.

**To change the database:** edit `data/db/Tables.kt`, then add a new step at the end of `data/db/Migrations.kt`. Don't edit old steps.