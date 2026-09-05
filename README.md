# MiraKits

MiraKits is the GUI-first kit interface and administration layer for the Mira Paper server suite. EssentialsX remains the source of truth for kit contents and cooldowns while MiraKits adds player-friendly browsing, safe claiming, prices, visibility/enabled metadata and in-game administration.

## Download

[**Download MiraKits v0.1.8**](https://github.com/FiveSOCE/Mira-Kits/releases/download/v0.1.8/MiraKits-0.1.8.jar)

## Requirements / Dependencies

- Paper 1.21.11
- Java 21
- MiraCore 0.2.0 or newer
- EssentialsX 2.22.0 or newer

## How MiraKits Works

EssentialsX `kits.yml` remains authoritative for kit existence, item contents and cooldowns. MiraKits v0.1.8 is now authoritative for player kit permissions: players do not need `essentials.kits.<name>` when claiming through MiraKits. MiraKits reads those kit definitions and presents them through a dynamic GUI. Only enabled, visible kits the player is actually allowed to claim are shown. Left-clicking a kit claims it immediately; right-clicking opens a read-only inspector showing the exact parsed Essentials ItemStacks and preserved non-item kit actions.

MiraKits routes normal Essentials `/kit` access through its own claim checks so players cannot bypass price, cooldown, disabled/event-window or permission rules through the standard command. Mira-specific fields such as price, visibility and enabled state are stored in `plugins/MiraKits/kit-meta.yml`.

Administrators use `/mkits admin` to create and manage kits. Creating a kit or editing price/cooldown uses private chat input: the GUI closes, MiraKits waits for that player's next message, cancels the message so it is not broadcast, then reopens the editor after valid input. Invalid input keeps the prompt active and `cancel` aborts it. Admin tools include copying the administrator's inventory into a kit, safely editing contents, changing cooldown/price, visibility and enabled state, deleting kits and reloading Essentials/Mira metadata. v0.1.8 adds temporary/event availability windows directly to the kit claim pipeline. Each Essentials kit can optionally have an absolute ISO-8601 start/end window in MiraKits config; players cannot claim it outside that window, while `mirakits.admin` can bypass inactive windows for testing/administration. The release also fixes EssentialsX 2.22 item serialization when saving edited kit contents.

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


## Kit Permission Model

MiraKits uses two permission namespaces.

### Normal kits

A normal kit requires:

`Mirakits.<KitName>`

Example:

Essentials kit ID:

`Knight`

Required permission:

`Mirakits.Knight`

Without that permission the kit is not shown in the player GUI and cannot be claimed through MiraKits.

Normal kits continue to obey their configured Essentials cooldown and MiraKits price.

### Temporary one-time kits

A temporary kit requires:

`Mirakits.Temp.<KitName>`

Example:

`Mirakits.Temp.Summer`

Temporary kits are one-time per player.

After a successful claim:

- the claim is written to `plugins/MiraKits/temporary-claims.yml`
- the kit immediately disappears from that player's GUI
- another claim is denied
- removing and re-granting the LuckPerms permission does not reset the claim
- restarting the server does not reset the claim

A kit is treated as temporary when:

1. it is configured as an enabled `event-kits.<id>` entry, or
2. its ID is listed in `temporary-kits`, or
3. the player has the `Mirakits.Temp.<KitName>` permission instead of the normal permission.

If a kit is explicitly configured as temporary, players must use the temporary permission namespace.

## Examples

Normal reusable/cooldown kit:

```
Kit: Knight
LuckPerms:
Mirakits.Knight
```

Temporary one-time kit:

```
Kit: Summer
LuckPerms:
Mirakits.Temp.Summer
```

After Summer is claimed once, it is permanently consumed for that player unless its claim record is administratively removed from the MiraKits data file.

## Permission Authority

MiraKits no longer requires the parallel Essentials node `essentials.kits.<name>` for GUI claims.

EssentialsX still supplies:

- kit contents
- item/action expansion
- cooldown storage for normal kits
- economy hooks used by existing kit pricing

MiraKits supplies:

- visibility
- permission checks
- temporary one-time ownership
- event windows
- GUI claiming


## MiraCosmetics Integration (0.1.8)

Adds MiraCosmetics visuals for successful normal and one-time temporary kit claims while preserving MiraKits permission and claim authority.
