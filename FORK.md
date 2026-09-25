# Wakes Reforged early-config fix (community fork)

This fork is based on [Wakes Reforged by Project8gbDeRam](https://www.curseforge.com/minecraft/mc-mods/wakes-reforged), for Minecraft 1.21.1 NeoForge. It addresses the startup crash in [upstream issue #40](https://github.com/Leclowndu93150/Wakes/issues/40). It is not an official release.

## Change

`WakeHandler` used to read `WakesConfig.APPEARANCE.wakeResolution.get()` in its static initializer. When the first client tick loads this class before NeoForge finishes loading the client config, the read throws `IllegalStateException: Cannot get config value before config is loaded`. The fork initializes the field to the config's documented default, `Resolution.SIXTEEN`. The existing `tick()` logic applies the actual configured resolution when a world is active.

The changed source is `versions/1.21.1/src/main/java/com/leclowndu93150/wakes/simulation/WakeHandler.java`. The distributed 1.4.1 JAR was generated from the official [CurseForge file 8657118](https://www.curseforge.com/minecraft/mc-mods/wakes-reforged/files/8657118) with `fork-patch/PatchWakes.java`. The patcher changes only `WakeHandler.class` in executable code. It also updates the license metadata and includes the GPL text and a fork notice. Its output replaces the original JAR; installing both will duplicate the mod ID.

## Reproduce the binary patch

Use Java 21 or newer and ASM 9.10.1 (`org.ow2.asm:asm:9.10.1`). From this repo's root:

```powershell
javac -cp path\to\asm-9.10.1.jar fork-patch\PatchWakes.java
java -cp "fork-patch;path\to\asm-9.10.1.jar" PatchWakes path\to\wakes-1.21.1-NeoForge-1.4.1.jar build\wakes-1.21.1-NeoForge-1.4.1-early-config-fix.jar LICENSE
```

The public upstream source currently labels its 1.21.1 build `1.3.0`, whereas the latest 1.21.1 CurseForge binary is `1.4.1`. For that reason, the published binary's exact provenance is the official 1.4.1 JAR plus the documented patcher, not a claim that this repo builds the upstream 1.4.1 binary from source. The local patched client started successfully after the reported crash; this has not been tested with every mod configuration.

Original code and assets: Project8gbDeRam, Goby56 and contributors. Fork modification: florianfinn. The original CurseForge project lists GPL-3.0; the upstream JAR metadata and public build script say MIT. Both notices are retained here to make that discrepancy visible. The fork is distributed under GPL-3.0, following the original CurseForge listing.
