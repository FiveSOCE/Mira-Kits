# MiraKits

MiraKits is the GUI-first kit interface and administration layer for the Mira Paper server suite. EssentialsX remains the source of truth for kit contents and cooldowns while MiraKits adds player-friendly browsing, safe claiming, prices, visibility/enabled metadata and in-game administration.

## Download

[**Download MiraKits v0.1.5**](https://github.com/FiveSOCE/Mira-Kits/releases/download/v0.1.5/MiraKits-0.1.5.jar)

## Requirements / Dependencies

- Paper 1.21.11
- Java 21
- MiraCore 0.1.0 or newer
- EssentialsX 2.22.0 or newer

## How MiraKits Works

EssentialsX `kits.yml` remains authoritative for kit existence, item contents and cooldowns. MiraKits reads those kit definitions and presents them through a dynamic GUI. Only enabled, visible kits the player is actually allowed to claim are shown. Left-clicking a kit claims it immediately; right-clicking opens a read-only inspector showing the exact parsed Essentials ItemStacks and preserved non-item kit actions.

MiraKits routes normal Essentials `/kit` access through its own claim checks so players cannot bypass price, cooldown, disabled/event-window or permission rules through the standard command. Mira-specific fields such as price, visibility and enabled state are stored in `plugins/MiraKits/kit-meta.yml`.

Administrators use `/mkits admin` to create and manage kits. Creating a kit or editing price/cooldown uses private chat input: the GUI closes, MiraKits waits for that player's next message, cancels the message so it is not broadcast, then reopens the editor after valid input. Invalid input keeps the prompt active and `cancel` aborts it. Admin tools include copying the administrator's inventory into a kit, safely editing contents, changing cooldown/price, visibility and enabled state, deleting kits and reloading Essentials/Mira metadata. Current source also supports temporary/event availability windows; `mirakits.admin` bypasses inactive event-kit windows.

MiraKits registers `MiraKitsApi` through MiraCore so MiraNPC and other modules can open the kit GUI without duplicating claim logic.

## Commands

| Command | Permission | What it does |
| --- | --- | --- |
| `/mirakits` | `mirakits.use` | Opens the normal player kit GUI. |
| `/mkits` | `mirakits.use` | Alias for `/mirakits`. |
| `/mirakits admin` | `mirakits.admin` | Opens the kit administration GUI. |
| `/mkits admin` | `mirakits.admin` | Short alias for the admin GUI. |
| `/kits` | `mirakits.use` | Routed player-facing kit command that opens/uses the MiraKits flow. |
| `/kit` | `mirakits.use` | Routed player-facing Essentials kit access so MiraKits claim rules cannot be bypassed. |

Most kit creation/editing actions are GUI-driven rather than separate commands.

## Permissions

| Permission | Default | What it does |
| --- | --- | --- |
| `mirakits.use` | Everyone | Allows use of the player kit GUI and routed normal kit flow. |
| `mirakits.admin` | OP | Allows the administration GUI, kit editing and bypass of inactive event-kit windows. |
