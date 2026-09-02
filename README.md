# MiraKits

GUI-first kit claiming and administration for the Mira Paper ecosystem.

## Download

**Current release: MiraKits v0.1.1**

- [Download MiraKits-0.1.1.jar](https://github.com/FiveSOCE/Mira-Kits/releases/download/v0.1.1/MiraKits-0.1.1.jar)
- [View MiraKits v0.1.1 release](https://github.com/FiveSOCE/Mira-Kits/releases/tag/v0.1.1)

SHA-256: `bd5a7d7e82455443d718da6c18653246f01ce0c1b79e1ed153518374e9ec60c1`

MiraKits does **not** maintain a second kit database. EssentialsX `kits.yml` is the source of truth for kit existence, item contents and cooldowns. MiraKits adds a player GUI, Essentials-backed economy charging, visible/hidden and enabled/disabled metadata, and a GUI admin editor.

## Requirements

- Paper 1.21.11
- Java 21
- MiraCore 0.1.0
- EssentialsX 2.22.0+

## Player flow

`/kits`, `/kit`, `/mirakits` and `/mkits` route players into the GUI. Visible + enabled kits are always listed. Essentials permission `essentials.kits.<kit>` is checked when a player attempts to claim the kit.

Every kit is represented by an enchanted Ender Chest named after the kit. Its lore is generated from the actual Essentials kit contents. Enchantments are shown on indented green lines using Roman numerals.

Example:

```text
Starter Kit
Iron Helmet
 - Protection V
Iron Chestplate
 - Protection V
 - Unbreaking III
Iron Leggings
Iron Boots
Iron Sword
64x Apples
5x Ender Pearls
```

The detail GUI shows the kit contents, MiraKits price, Essentials cooldown state and claim button.

## Admin GUI

`/mkits admin`

Admins can:

- Create a kit with a private chat naming flow
- Copy their current inventory into a kit
- Open an existing kit and remove/copy items without risking their real inventory
- Set cooldown in minutes through private chat input
- Set price through private chat input
- Toggle visible/hidden
- Toggle enabled/disabled
- Delete an Essentials kit with confirmation
- Reload Essentials kits + Mira metadata

Saving writes the item list and cooldown directly through EssentialsX's `Kits` API. Kits loaded from Essentials' optional `kits/*.yml` directory are migrated safely into the main `kits.yml` when edited.

## Metadata

Only Mira-specific fields are stored in `plugins/MiraKits/kit-meta.yml`:

```yaml
kits:
  starter_kit:
    display-name: Starter Kit
    price: '2500'
    visible: true
    enabled: true
```

No item list or cooldown is duplicated there.

## Claim integrity

By default, Essentials `/kit` commands are routed to MiraKits and outside `KitClaimEvent` attempts are cancelled. This prevents bypassing MiraKits prices, disabled state or GUI policy. MiraKits itself opens a short internal claim window when it calls the real Essentials `Kit#expandItems` flow.

## MiraNPC integration

MiraKits registers `MiraKitsApi` in MiraCore's service registry:

```java
MiraKitsApi.openKits(Player player)
MiraKitsApi.openKit(Player player, String kitId)
MiraKitsApi.kitIds()
```

MiraNPC can therefore open the main kit GUI or a specific kit GUI without dispatching commands or duplicating kit logic.
