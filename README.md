# Likes

A Minecraft Paper plugin that encourages positive interactions by allowing players to send direct Likes, like ordinary chat messages, and react to items in a unified feed.

Likes provides a lightweight social system with feeds, rankings, personal statistics, and multilingual support, helping communities recognize and celebrate positive moments.

---

# Features

* ❤️ Send Likes to other players with a custom message
* 💬 Like ordinary chat messages using a per-viewer clickable `[♡]` control
* ⬆️ Promote a chat message to the feed only when it receives its first Like; unliked messages are not persisted
* ❤️ React to feed items via display code (e.g. `/like #ABCD`), including clients that cannot click chat controls
* 📖 Browse the unified DIRECT/CHAT feed (in-game book UI, up to 40 entries)
* 🏆 View player rankings: top receivers, top direct givers, and most-reacted feed items (book UI)
* 👤 View your personal Like statistics with received, sent, and reacted counts (book UI)
* 🧾 Quick chat log of the 5 most recent feed items (`/like log`)
* 🔔 Server-wide announcement for direct Likes with a clickable reaction control
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
| `/like #<code>`                | Like a pending chat or react to a feed item    |
| `/like feed`                   | Open the recent unified feed (book UI)         |
| `/like ranking`                | Open the ranking screen (book UI)              |
| `/like mine`                   | View your personal Like statistics (book UI)   |
| `/like log`                    | Show the 5 most recent feed items in chat      |

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
| `item.prefix`                | `[LIKE]`  | Prefix shown at the start of direct Like announcements in chat.                                                                 |
| `effects.enabled`            | `true`    | Enable or disable particle effects (heart + firework) on Like and reaction success.                                             |
| `chat.enabled`               | `true`    | Enable or disable chat Likes. When disabled, the plugin does not modify `AsyncChatEvent` renderers.                              |
| `chat.minLength`             | `4`       | Minimum plain-text message length eligible for a chat Like control. Whitespace-only and shorter messages are ignored.            |
| `chat.pendingBufferSize`     | `30`      | Number of unpromoted chat messages retained in memory. Old entries are discarded and their display codes become reusable.        |
| `chat.maxStoredLength`       | `100`     | Maximum plain-text length persisted when a chat message is promoted. Truncated messages end with `…`.                           |

**Language** is not a config option. The plugin automatically uses each player's Minecraft client locale. Supported locales: English (`en_US`) and Japanese (`ja_JP`). English is the fallback for all other locales.

## Chat Likes

When chat Likes are enabled, eligible public chat messages receive a clickable `[♡]` control for every viewer except the author. The control runs `/like #<code>`, so the same code can be entered manually by Bedrock/Geyser players or other clients that cannot use chat click events.

The plugin wraps the `ChatRenderer` already installed on `AsyncChatEvent`; it does not rebuild the existing format. Prefixes, nicknames, chat colors, channel decorations, and other component events supplied by preceding chat plugins are therefore preserved. The listener runs at `HIGHEST` priority and ignores cancelled chat events.

Paper does not expose a universal way to distinguish global chat from staff, party, guild, or local channels. Chat Likes are intended for public normal chat. If another plugin routes chat through private channels or replaces the renderer later in the event pipeline, verify compatibility on your server and disable `chat.enabled` if necessary.

Pending chat messages exist only in memory. The first reaction atomically creates a `CHAT` feed item and its initial reaction in SQLite. Messages evicted from the pending buffer without a reaction are never stored.

## Statistics

The unified feed uses these count definitions:

* **Received** — all reactions on feed items authored by the player, across DIRECT and CHAT items.
* **Sent** — DIRECT feed items initiated by the player.
* **Reacted** — reactions made by the player, including the initial reaction created with a DIRECT item.

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
