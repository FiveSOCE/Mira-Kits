# MiraKits

GUI-first kit claiming and administration for the Mira Paper ecosystem.

## Download

**Current release: MiraKits v0.1.4**

- [Download MiraKits-0.1.4.jar](https://github.com/FiveSOCE/Mira-Kits/releases/download/v0.1.4/MiraKits-0.1.4.jar)
- [View MiraKits v0.1.4 release](https://github.com/FiveSOCE/Mira-Kits/releases/tag/v0.1.4)
- [View all releases](https://github.com/FiveSOCE/Mira-Kits/releases)

SHA-256: `881e47c3de7080c7368f696049bf06d6fdbd4314ebd460522a427b58495f242c`

MiraKits does **not** maintain a second kit database. EssentialsX `kits.yml` remains the source of truth for kit existence, item contents and cooldowns. MiraKits adds the player GUI, Essentials-backed economy charging, visibility/enabled metadata, and GUI administration.

## Player GUI

`/kits`, `/kit`, `/mirakits` and `/mkits` route players into the MiraKits GUI.

The v0.1.4 player list is compact and dynamic:

- Up to 7 available kits: 9x3
- 8-14 available kits: 9x4
- Additional rows are added as needed, with pagination retained for very large kit sets
- Only visible, enabled kits the player is actually permitted to claim are shown
- Kit icons are Ender Chests without enchant glint
- Empty/dead spaces use blank grey stained glass panes with forced enchant glint
- The glass creates the visual glow while kit icons remain clean
- The Close button is centered on the bottom row

For 8 kits the layout is:

```text
XXXXXXXXX
XOXOXOXOX
XOXOXOXOX
XXXXMXXXX
```

`X` = glowing grey glass, `O` = kit, `M` = Close.

Kit lore is generated from the actual Essentials kit contents and includes item enchantments using Roman numerals.

Opening a kit shows a deliberately minimal player-facing screen with only a centered **Claim Kit** button. Price and cooldown remain enforced during the claim but are not exposed in the player GUI.

## Admin GUI

`/mkits admin`

Admins with `mirakits.admin` can:

- Create kits through a private chat naming flow
- Copy their current inventory into a kit
- Edit existing kit contents safely
- Set cooldown through private chat input
- Set price through private chat input
- Toggle visible/hidden
- Toggle enabled/disabled
- Delete a kit with confirmation
- Reload Essentials kits and Mira metadata

Valid chat input updates the current draft and reopens the editor. Invalid input keeps waiting without broadcasting the message. Type `cancel` to abort the prompt.

## Metadata

Mira-specific fields are stored in `plugins/MiraKits/kit-meta.yml` while Essentials remains authoritative for actual kit contents and cooldowns.

## Claim integrity

Essentials `/kit` access is routed through MiraKits so prices, cooldowns, disabled state and permission rules cannot be bypassed through the normal player command flow.

## MiraNPC integration

MiraKits registers `MiraKitsApi` in MiraCore so MiraNPC can open the main kits GUI or a specific kit directly without duplicating kit logic.

## Requirements

- Paper 1.21.11
- Java 21
- MiraCore 0.1.0
- EssentialsX 2.22.0+
