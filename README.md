# MiraKits

GUI-first kit claiming and administration for the Mira Paper ecosystem.

## Download

**Current release: MiraKits v0.1.5**

- [Download MiraKits-0.1.5.jar](https://github.com/FiveSOCE/Mira-Kits/releases/download/v0.1.5/MiraKits-0.1.5.jar)
- [View MiraKits v0.1.5 release](https://github.com/FiveSOCE/Mira-Kits/releases/tag/v0.1.5)
- [View all releases](https://github.com/FiveSOCE/Mira-Kits/releases)

MiraKits does **not** maintain a second kit database. EssentialsX `kits.yml` remains the source of truth for kit existence, item contents and cooldowns. MiraKits adds the player GUI, Essentials-backed economy charging, visibility/enabled metadata, and GUI administration.

## Player GUI

`/kits`, `/kit`, `/mirakits` and `/mkits` route players into the MiraKits GUI.

The player list is compact and dynamic:

- Up to 7 available kits: 9x3
- 8-14 available kits: 9x4
- Additional rows are added as needed, with pagination retained for very large kit sets
- Only visible, enabled kits the player is actually permitted to claim are shown
- Kit icons are clean Ender Chests named only with the kit display name
- Empty/dead spaces use blank grey stained glass panes with forced enchant glint
- The Close button is centered on the bottom row

For 8 kits the layout is:

```text
XXXXXXXXX
XOXOXOXOX
XOXOXOXOX
XXXXMXXXX
```

`X` = glowing grey glass, `O` = kit, `M` = Close.

### Claim and inspect controls

- **Left-click a kit** to claim it immediately. There is no second Claim Kit confirmation menu.
- **Right-click a kit** to inspect it.

The inspection GUI is read-only and shows the exact parsed Essentials kit ItemStacks in their configured kit slots. This preserves the real quantity, renamed/custom display name, lore, enchantments, durability, custom metadata and PDC data that the player will receive.

The Kit Details book also shows:

- kit price
- cooldown / one-time status
- any preserved non-item Essentials kit lines such as currency rewards or commands/actions

This makes the inspector reflect the complete Essentials kit definition instead of generating an approximate text-only item list.

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

Build output: `build/libs/MiraKits-0.1.5.jar`
