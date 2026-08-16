# Fabric 1.21.x placeholder

This module reserves the future Minecraft 1.21.x implementation boundary and
depends on the same `:common` Java library as `:fabric-1.20.1`.

It is intentionally not a runnable or distributable Minecraft mod yet. It has:

- no selected Minecraft 1.21 patch version;
- no Fabric Loader, Fabric API, Loom, or mapping versions;
- no entrypoints or `fabric.mod.json`;
- no mixins, accessors, networking codecs, renderers, or copied resources;
- no duplicated 1.20.1 implementation classes.

When the port begins, select an exact Minecraft 1.21.x version first, then add
its Fabric/Yarn toolchain and implement thin adapters against `:common`.
