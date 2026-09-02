# MiraKits

GUI-first kit claiming and administration for the Mira Paper ecosystem.

## Download

**Current release: MiraKits v0.1.2**

- [Download MiraKits-0.1.2.jar](https://github.com/FiveSOCE/Mira-Kits/releases/download/v0.1.2/MiraKits-0.1.2.jar)
- [View MiraKits v0.1.2 release](https://github.com/FiveSOCE/Mira-Kits/releases/tag/v0.1.2)

SHA-256: `59074cdde7e9c2dffcc57be1b87b06f16149f95a121fb72fa7f5d957d7e6445f`

MiraKits does **not** maintain a second kit database. EssentialsX `kits.yml` is the source of truth for kit existence, item contents and cooldowns. MiraKits adds a player GUI, Essentials-backed economy charging, visible/hidden and enabled/disabled metadata, and a GUI admin editor.

## Player flow

`/kits`, `/kit`, `/mirakits` and `/mkits` route players into the GUI. Visible + enabled kits are always listed. Essentials permission `essentials.kits.<kit>` is checked when a player attempts to claim the kit.

Every kit is represented in the main list by an enchanted Ender Chest named after the kit. Its lore is generated from the actual Essentials kit contents. Enchantments are shown on indented green lines using Roman numerals.

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

Opening a kit now shows a deliberately minimal player-facing screen with only a centered **Claim Kit** button. Price and cooldown remain enforced during the claim but are not exposed in the player GUI.

## Admin GUI

`/mkits admin`

Admins with `mirakits.admin` can:

- Create a kit with a private chat naming flow
- Copy their current inventory into a kit
- Open an existing kit and remove/copy items without risking their real inventory
- Set cooldown in minutes through private chat input
- Set price through private chat input
- Toggle visible/hidden
- Toggle enabled/disabled
- Delete an Essentials kit with confirmation
- Reload Essentials kits + Mira metadata

### Price and cooldown input

Clicking **Set Price** or **Set Cooldown** closes the editor after the inventory click transaction finishes, then arms a private chat prompt.

- The next chat message is captured privately and is not broadcast.
- Invalid input leaves the GUI closed and keeps waiting for another value.
- Valid input updates the current kit draft and reopens that kit's admin editor.
- Type `cancel` to abort and return to the editor.

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

## Requirements

- Paper 1.21.11
- Java 21
- MiraCore 0.1.0
- EssentialsX 2.22.0+
