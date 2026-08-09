![Title](.github/title.png)

<div align="center">

<a href="">![Java 17](https://img.shields.io/badge/Java%2017-ee9258?logo=coffeescript&logoColor=ffffff&labelColor=606060&style=flat-square)</a>
<a href="">![Environment: Client & Server](https://img.shields.io/badge/environment-Client%20&%20Server-1976d2?style=flat-square)</a>
<a href="">![Minecraft 1.20.1](https://img.shields.io/badge/Minecraft-1.20.1-62b47a?style=flat-square)</a>
<a href="">![Fabric](https://img.shields.io/badge/Loader-Fabric-d7c49e?style=flat-square)</a>

</div>

Time Lord is a Fabric mod for Minecraft 1.20.1 that gives the player several time and space manipulation abilities, including slowing nearby entities, stopping time, extreme movement speed, and cutting through space.

## Features

### ![Slow Time](.github/domain_32x32.png) Slow Time

Create a field around yourself that slows nearby entities.

Slow Time supports multiple modes that can be switched using a separate keybind.

- **First Shift** — moderate slowdown
- **Second Shift** — stronger slowdown with a larger radius
- **Third Shift** — extreme slowdown across an even larger area

The currently selected mode is shared between Slow Time activations.

**Default controls**

- `Z` — Activate Slow Time
- `R` — Switch Slow Time mode

---

### ![The World](.github/hourglass_32x32.png) The World

Stop entity time completely.

While The World is active:

- Mobs stop moving
- Projectiles freeze
- Dropped items freeze
- Other players are frozen
- The player who activated The World can continue moving

If another player also activates The World while time is stopped, they become able to move inside stopped time as well.

Attacks against frozen entities are stored during stopped time and are applied when time resumes.

**Default control**

- `X` — Toggle The World

---

### ![Dimension Cut](.github/cuts_32x32.png) Dimension Cut

A space-cutting offensive ability.

**Default control**

- `C` — Dimension Cut

---

### ![Time Shift](.github/dash_32x32.png) Time Shift

Increase your movement speed without using vanilla potion effects.

Time Shift cycles through multiple speed modes:

- `2x`
- `3x`
- `5x`
- `10x`
- `OFF`

Pressing the key repeatedly cycles through the available modes.

[WIP] While Time Shift is active, the player also has automatic obstacle jumping to make high-speed movement easier.

**Default control**

- `V` — Cycle Time Shift speed

---
## Configuration

### Client side:

Keybindings can be changed through:

`Options → Controls → Key Binds → Time Lord`

Default controls:

| Ability | Key |
|---|---|
| Slow Time | `Z` |
| Switch Slow Time Mode | `R` |
| The World | `X` |
| Dimension Cut | `C` |
| Time Shift | `V` |

### Server side:

Ability execution, cooldowns, time manipulation, entity freezing, and damage handling are controlled server-side.

The mod must be installed on both the client and server.

## Installation

### Requirements

- Minecraft `1.20.1`
- Java `17`
- Fabric Loader
- Fabric API

### Installing

1. Install Fabric Loader for Minecraft 1.20.1.
2. Install Fabric API.
3. Place the Time Lord `.jar` file inside your Minecraft `mods` folder.
4. Launch Minecraft using the Fabric profile.

For multiplayer, install the mod and Fabric API on both the server and connecting clients.

## Building from Source

Clone the repository:

```bash
git clone <repository-url>
cd time-lord
```

## Bug Reports

Found a bug? Please report it through [GitHub Issues](https://github.com/metebalc/TimeLord/issues).

When reporting an issue, include:
- Minecraft version
- Time Lord version
- Fabric Loader version
- Fabric API version
- Steps to reproduce the issue
- Relevant logs or crash reports

## Compatibility

So far, no official compatibility with other mods has been confirmed or added.

Time Lord may work alongside many Fabric mods, but conflicts can occur with mods that modify entity movement, player speed, damage handling, projectiles, networking, or game tick behavior.

If you discover a compatibility issue, please report it through GitHub Issues and include the conflicting mod name, version, and any relevant logs.

Compatibility patches are not guaranteed. There are a large number of Fabric mods, and it may not be practical to support every possible mod combination. Compatibility fixes may be considered depending on the severity of the issue, the mods involved, and the amount of work required.

## Disclaimers

> [!WARNING]
> **Early Development Warning**
>
> Time Lord is currently in early development.
>
> Features, balancing, controls, visuals, networking behavior, and internal systems may change significantly between versions. Bugs, crashes, unexpected interactions, and compatibility issues with other mods may occur.
>
> If you use this mod in an important world or server, make regular backups before updating or testing new versions.
>
> Please report reproducible issues with logs and relevant mod/version information when possible.

> [!NOTE]
> **AI-Generated Image Disclaimer**
>
> Some visual assets used by this project, including promotional artwork, icons, textures, or other images, may be created or assisted with generative AI tools.
>
> AI-generated assets may be replaced, edited, or redesigned as development continues.
>
> These images are used only as part of the mod's presentation and visual design and are not intended to imitate, impersonate, or claim ownership of any specific artist's work.

> [!NOTE]
> **Fan Project & Non-Commercial Disclaimer**
>
> Time Lord is an unofficial, non-commercial, open-source fan project created for entertainment purposes.
>
> This project is not affiliated with, endorsed by, or sponsored by any companies.
>
> This project is not monetized and does not accept donations. References to existing works, including names and concepts, belong to their respective owners.
