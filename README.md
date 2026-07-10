# BetterChat

A modern, production-ready chat formatting plugin for **Paper/Purpur 1.21.11**.  
Uses **MiniMessage** (Adventure) for all formatting — no legacy color codes.

## Features

- **Custom chat format** — fully configurable with MiniMessage syntax
- **Join/quit messages** — custom styled messages with first-join support
- **PlaceholderAPI integration** — use any PAPI placeholder in your formats
- **ServerRoles support** — display role prefixes, colors, and names via PAPI
- **Mention highlighting** — `@PlayerName` highlights with optional sound
- **MiniMessage permission** — control who can use formatting tags in chat
- **Localization** — English (en_US) and Portuguese (pt_PT) included
- **Hot reload** — `/betterchat reload` applies changes instantly

## Requirements

- **Paper** or **Purpur** 1.21.11
- **Java 21**
- (Optional) **PlaceholderAPI** — for `%placeholder%` support
- (Optional) **ServerRoles** — for role-based placeholders via PAPI

## Installation

1. Build the plugin:
   ```bash
   ./gradlew build
   ```
2. Copy `build/libs/BetterChat-1.0.jar` to your server's `plugins/` folder.
3. Start or restart the server.
4. Edit `plugins/BetterChat/config.yml` to customize.
5. Run `/betterchat reload` to apply changes.

## Configuration

### config.yml

```yaml
language: en_US

features:
  chat-format: true
  join-quit-messages: true
  first-join-message: true
  allow-message-minimessage-with-permission: true
  mention-highlight:
    enabled: true
    sound: "entity.experience_orb.pickup"
    format: "<yellow>@{mentioned}</yellow>"

formats:
  chat: "%serverroles_prefix_formatted% %serverroles_color%{player}<reset>: <white>{message}</white>"
  join: "<green>+ <gray>{player}</gray></green>"
  quit: "<red>- <gray>{player}</gray></red>"
  first-join: "<gold>★ <gray>{player}</gray> <yellow>joined for the first time!</yellow></gold>"

debug:
  papi: false
  placeholder-flow: false
```

### Placeholder Reference

#### Built-in Placeholders (always available)

| Placeholder      | Description                   |
|------------------|-------------------------------|
| `{player}`       | Player's name                 |
| `{displayname}`  | Player's display name         |
| `{message}`      | Chat message content          |
| `{world}`        | Player's current world        |
| `{online}`       | Number of online players      |

#### ServerRoles Placeholders (requires PlaceholderAPI + ServerRoles)

Use these directly in your format strings with `%placeholder%` syntax:

| Placeholder                         | Description                      |
|-------------------------------------|----------------------------------|
| `%serverroles_role_id%`             | Role ID                          |
| `%serverroles_role_name%`           | Role name                        |
| `%serverroles_prefix%`              | Role prefix (raw)                |
| `%serverroles_prefix_formatted%`    | Role prefix (formatted)          |
| `%serverroles_color%`               | Role color                       |
| `%serverroles_priority%`            | Role priority                    |
| `%serverroles_is_op%`               | Whether the role grants OP       |
| `%serverroles_is_temp%`             | Whether the role is temporary    |
| `%serverroles_expiry%`              | Role expiry date                 |

### Example Formats with ServerRoles

**Chat with role prefix and color:**
```yaml
chat: "%serverroles_prefix_formatted% %serverroles_color%{player}<reset>: <white>{message}</white>"
```
Result: `[Admin] Steve: Hello everyone!` (with colors from ServerRoles)

**Chat with role name:**
```yaml
chat: "<gray>[</gray>%serverroles_role_name%<gray>]</gray> {player}: {message}"
```

**Minimal format:**
```yaml
chat: "%serverroles_color%{player}<reset>: {message}"
```

## Commands

| Command                             | Description                                                   | Permission         |
|-------------------------------------|---------------------------------------------------------------|---------------------|
| `/betterchat reload`                | Reload all config files                                       | `betterchat.admin`  |
| `/betterchat debugpapi <player>`    | Show PAPI resolution stages for chat format                   | `betterchat.admin`  |
| `/betterchat debugformat <player>`  | Show all resolution stages for chat/join/quit formats          | `betterchat.admin`  |

## Permissions

| Permission          | Description                                          | Default |
|---------------------|-----------------------------------------------------|---------|
| `betterchat.admin`  | Access to `/betterchat` admin commands (reload/debug) | OP      |
| `betterchat.format` | Allows **cosmetic** MiniMessage tags (colours/gradients) in chat — interactive tags are always stripped | false   |

## Localization

Language files are stored in `plugins/BetterChat/lang/`.

Included languages:
- `en_US.yml` — English
- `pt_PT.yml` — Portuguese

Change the language in `config.yml`:
```yaml
language: pt_PT
```

To create a new language, copy `en_US.yml`, rename it (e.g., `es_ES.yml`), translate the values, and set `language: es_ES` in config.

## Technical Notes

- Uses Paper's `AsyncChatEvent` (not the deprecated `AsyncPlayerChatEvent`)
- Player message content is **never parsed as full MiniMessage**. It is built as a `Component` and
  injected into the (trusted) format template via a component placeholder, so a player can never
  inject interactive tags (`<click>`, `<hover>`, `<insert>`, `<newline>`, `<selector>`) into chat.
- Players with `betterchat.format` may use **cosmetic** tags only (colours, decorations, gradient,
  rainbow, reset); everyone else has all tags shown as literal text.
- Mentions are capped at **5 pinged players per message** to prevent sound spam / harassment.
- **Placeholder resolution order:**
  1. Built-in placeholders (`{player}`, `{world}`, etc.) are replaced first
  2. PlaceholderAPI `%placeholders%` are resolved (PAPI output may contain MiniMessage tags)
  3. `{message}` becomes a component placeholder inserted **after** PAPI — player message content
     never passes through PAPI or the full MiniMessage parser
  4. MiniMessage parses the trusted template into a Component (player message spliced in as a Component)
- PlaceholderAPI is called **directly** (no reflection) — `compileOnly` + runtime presence check ensures no crash if PAPI is missing
- Use `/betterchat debugpapi <player>` to inspect chat format resolution stages
- Use `/betterchat debugformat <player>` to inspect all format (chat/join/quit) resolution stages
- Enable `debug.placeholder-flow: true` in config to log resolution stages per message (verbose)
- Fully compatible with Paper 1.21.11 and Purpur 1.21.11

## Building

```bash
./gradlew build
```

The compiled JAR will be at `build/libs/BetterChat-1.0.jar`.

