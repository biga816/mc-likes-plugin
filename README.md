# Likes Plugin (Minecraft Paper)

A simple Minecraft Paper plugin to enable "likes" and reactions between players.

This plugin is designed to encourage positive interactions in multiplayer servers.

## 🛠 Requirements

- Java 21
- Paper 1.21.x

---

## 📦 Build

```bash
./gradlew build
````

Output:

```
build/libs/Likes-<version>.jar
```

---

## 🚀 Development Setup

### 1. Start local Paper server

```
paper-test/
  paper-1.21.x.jar
  plugins/
```

Run:

```bash
java -Xms1G -Xmx1G -jar paper-1.21.x.jar --nogui
```

---

### 2. Deploy plugin

```bash
./gradlew deployToTestServer
```

This copies the built jar into:

```
./paper-test/plugins/
```

---

### 3. Restart server

Restart the Paper server to load the plugin.

---

## 📄 License

MIT
