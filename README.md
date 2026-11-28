# MiraKits

GUI-first kit interface and administration layer for the Mira Paper server suite.

EssentialsX remains authoritative for kit contents and cooldown storage while MiraKits owns player-facing browsing, prices, visibility, permission gating, temporary one-time kits, event windows and safe administration.

## Downloads and compatibility

| Server target | Java | Final JAR name | Download |
| --- | --- | --- | --- |
| **Paper 1.18.2** | 17 | `MiraKits_1.18.2.jar` | [Download 1.18.2 JAR](https://github.com/FiveSOCE/Mira-Kits/releases/download/dev-1.18.2/MiraKits_1.18.2.jar) |
| **Paper 1.21.11–26.2** | Java 21 baseline; Paper 26.2 may require Java 25 | `MiraKits_1.21.11-26.2.jar` | [Download modern JAR](https://github.com/FiveSOCE/Mira-Kits/releases/download/compat-modern-1.21.11-26.2/MiraKits_1.21.11-26.2.jar) |

Install only one versioned JAR. Release assets are published; complete in-game compatibility is not implied by compilation.

[Legacy source](https://github.com/FiveSOCE/Mira-Kits/tree/compat/1.18.2) · [Modern source](https://github.com/FiveSOCE/Mira-Kits) · [All releases](https://github.com/FiveSOCE/Mira-Kits/releases)

## Requirements and integrations

- **Required:** Matching Paper and Java runtime, matching **MiraCore**, and a compatible **EssentialsX** installation (hard dependency `Essentials`).
- **Optional:** MiraCosmetics for configurable successful-claim and error effects.
- **Kit storage:** EssentialsX manages the actual kits and cooldown definitions; MiraKits adds prices, GUI, restrictions, permissions, temporary kits, event windows and grant automation.
- Back up `plugins/MiraKits/` and EssentialsX kit files before switching versions. Do not install both MiraKits builds.

## Commands and permissions

| Command | Permission | Purpose |
| --- | --- | --- |
| `/mirakits` or `/mkits` | `mirakits.use` | Player kit GUI |
| `/mirakits admin` | `mirakits.admin` | Admin kit editor |
| `/mirakits grant <player> <kit>` | `mirakits.admin` (admin/console) | Trusted direct grant |
| `/kit`, `/kits` | `mirakits.use` plus kit node | Routed kit claim and browsing |

| Permission | Default | Purpose |
| --- | --- | --- |
| `mirakits.use` | Everyone | Player claim interface |
| `mirakits.admin` | OP | Configure kits and bypass event windows |
| `Mirakits.<KitName>` | Configured | Access a normal kit |
| `Mirakits.Temp.<KitName>` | Configured | One-time temporary kit permission |

## How MiraKits Works

EssentialsX `kits.yml` remains the source of truth for:

- kit existence
- item/action contents
- normal kit cooldowns

MiraKits adds its own metadata under `plugins/MiraKits/` for:

- price
- enabled/disabled state
- visibility
- Mira-specific permissions
- temporary-kit ownership
- event availability windows

Players browse eligible kits through the GUI. Left-click claims a kit; right-click opens a read-only inspector showing the parsed contents and preserved non-item actions.

Normal `/kit` and `/kits` access is routed through MiraKits so price, cooldown, permission and event-window checks cannot be bypassed through the normal command path.

## Permission Model

### Normal kits

A normal kit requires:

```text
Mirakits.<KitName>
```

Example:

```text
Mirakits.Knight
```

MiraKits does not require the parallel `essentials.kits.<name>` node for GUI claims.

### Temporary one-time kits

Temporary kits use:

```text
Mirakits.Temp.<KitName>
```

After a successful temporary-kit claim:

- the claim is persisted
- the kit disappears from that player's GUI
- re-granting the permission does not reset the claim
- restarting the server does not reset the claim

A kit can be treated as temporary through explicit configuration, event-kit configuration or the temporary permission namespace.

## Event Windows

Kits can optionally have absolute start/end availability windows. Players cannot claim outside the configured window, while administrators can bypass inactive windows for testing and management.

## Admin GUI

`/mirakits admin` provides GUI-first administration for:

- creating kits
- copying the administrator inventory into a kit
- editing contents safely
- setting price
- setting cooldown
- toggling visibility
- toggling enabled state
- temporary/event configuration
- deleting kits
- reloading Essentials/Mira metadata

Price/cooldown editing uses private chat input. The editor closes, captures the administrator's next message without broadcasting it, validates the input and returns to the GUI.

## Direct Grant Path — v0.1.12

MiraKits exposes a console-safe grant command:

```text
/mirakits grant <player> <kit>
```

This is intended for trusted automation and integrations such as vouchers/store delivery. It delivers the Essentials kit directly and deliberately bypasses normal player-facing price, cooldown, permission and event-window checks.

MiraItems uses this direct path for modern kit-voucher redemption so contents appear immediately in the player's inventory.

## Commands

| Command | Permission | Purpose |
| --- | --- | --- |
| `/mirakits` | `mirakits.use` | Opens the player kit GUI. |
| `/mkits` | `mirakits.use` | Alias for `/mirakits`. |
| `/mirakits admin` | `mirakits.admin` | Opens the administration GUI. |
| `/mirakits grant <player> <kit>` | trusted/admin path | Direct console-safe kit delivery. |
| `/kit` | `mirakits.use` | Routed player-facing kit flow. |
| `/kits` | `mirakits.use` | Routed player-facing kit flow. |

## Permissions

| Permission | Default | Purpose |
| --- | --- | --- |
| `mirakits.use` | Everyone | Allows normal player kit use. |
| `mirakits.admin` | OP | Allows administration and inactive-window bypass. |
| `Mirakits.<KitName>` | Configured | Allows a normal kit. |
| `Mirakits.Temp.<KitName>` | Configured | Allows a configured one-time temporary kit. |

## MiraCosmetics Integration

When MiraCosmetics is installed, successful normal, temporary and event claims can trigger centralized Mira visual/audio presentation. Rejected claims can use the configured error audio, and each player's Visual/Audio toggles remain respected.

## API

`MiraKitsApi` is registered through MiraCore so MiraNPC, MiraItems and other Mira modules can open or deliver kits without reimplementing claim logic.


## Dependencies and installation

- **Required:** MiraCore, Essentials; matching Paper version and Java runtime.
- **Optional integrations:** MiraCosmetics.
- Back up plugin data, remove old JARs, and restart after installing the appropriate release.
