# Likes

A Minecraft Paper plugin that encourages positive interactions by allowing players to send "Likes" to each other and react to existing Likes.

Likes provides a lightweight social system with feeds, rankings, personal statistics, and multilingual support, helping communities recognize and celebrate positive moments.

---

# Features

* ❤️ Send Likes to other players with a custom message
* ❤️ React to existing Likes via display code (e.g. `/like #ABCD`)
* 📖 Browse the recent Like feed (in-game book UI, up to 40 entries)
* 🏆 View player rankings: top receivers, top givers, and most-reacted Likes (book UI)
* 👤 View your personal Like statistics with received, sent, and reacted counts (book UI)
* 💬 Quick chat log of the 5 most recent Likes (`/like log`)
* 🔔 Server-wide broadcast on Like and reaction with a clickable react button
* ✨ Particle effects (heart + firework) on Like and reaction success
* 🛡️ Daily Like limit and per-pair cooldown to prevent spam
* 🌐 Multi-language support — locale auto-detected from each player's client (English and Japanese)
* 🖥️ Per-server data isolation via `server-id` for multi-server networks
* 💾 SQLite-based persistent storage
* ⚡ Lightweight and optimized for Paper

---

# Commands

All commands require the `likes.use` permission (granted to all players by default).

| Command                        | Description                                    |
| ------------------------------ | ---------------------------------------------- |
| `/like <player> <reason...>`   | Send a Like to another player                  |
| `/like #<code>`                | React to an existing Like by display code      |
| `/like feed`                   | Open the recent Like feed (book UI)            |
| `/like ranking`                | Open the ranking screen (book UI)              |
| `/like mine`                   | View your personal Like statistics (book UI)   |
| `/like log`                    | Show the 5 most recent Likes in chat           |

## Permissions

| Permission           | Default    | Description                        |
| -------------------- | ---------- | ---------------------------------- |
| `likes.use`          | Everyone   | Use all `/like` commands           |
| `likes.admin`        | Operators  | Admin access                       |
| `likes.limit.bypass` | Operators  | Bypass the daily Like limit        |

## Tab Completion

`/like` supports tab completion for:

* Subcommands: `feed`, `log`, `ranking`, `mine`
* Recent display codes (prefixed with `#`)
* Online player names

---

# Installation

## Requirements

* Java 21 or later
* Paper 1.21.x

## Steps

1. Download the latest release JAR.
2. Copy it into your server's `plugins/` directory.
3. Start or restart the Paper server.
4. Edit `plugins/Likes/config.yml` to configure the plugin (optional).
5. Reload or restart the server if you changed the config.

---

# Configuration

The plugin generates `plugins/Likes/config.yml` automatically on first run with the following defaults.

| Key                          | Default   | Description                                                                                                                     |
| ---------------------------- | --------- | ------------------------------------------------------------------------------------------------------------------------------- |
| `server-id`                  | `default` | Unique identifier to scope all Like data per server. Set a different value on each server in a multi-server network.            |
| `limits.dailyDirectLikeLimit`| `20`      | Maximum number of Likes a player can send per day (UTC). Bypassed by the `likes.limit.bypass` permission.                      |
| `limits.pairCooldownSeconds` | `60`      | Cooldown in seconds before the same player can Like the same target again. Resets on server restart.                            |
| `recent.bufferSize`          | `100`     | Size of the in-memory recent Like buffer loaded from the database on startup.                                                   |
| `reason.maxLength`           | `48`      | Maximum character length of the Like reason text.                                                                               |
| `broadcast.prefix`           | `[LIKE]`  | Prefix shown at the start of server-wide Like broadcast messages in chat.                                                       |
| `effects.enabled`            | `true`    | Enable or disable particle effects (heart + firework) on Like and reaction success.                                             |

**Language** is not a config option. The plugin automatically uses each player's Minecraft client locale. Supported locales: English (`en_US`) and Japanese (`ja_JP`). English is the fallback for all other locales.

---

# Development

## Requirements

* Java 21
* Paper 1.21.x
* Gradle

## Build

```bash
./gradlew build
```

Output:

```text
build/libs/Likes-<version>.jar
```

The build uses the Shadow plugin to bundle the SQLite JDBC driver into the JAR.

## Deploy to Local Test Server

```bash
./gradlew deployToTestServer
```

This copies the plugin JAR and a fresh `config.yml` into:

```text
paper-test/plugins/
paper-test/plugins/Likes/config.yml
```

## Start the Test Server

```bash
cd paper-test

java -Xms1G -Xmx1G -jar paper-1.21.x.jar --nogui
```

## Testing

Recommended local development environment:

* Paper test server
* Prism Launcher
* Two Minecraft accounts for multiplayer testing

---

# License

MIT
