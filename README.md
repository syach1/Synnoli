# Karipap

Karipap is an Android retro-gaming frontend forked from Cannoli.

This is a hard fork with different frontend opinions. It keeps Cannoli's Android foundation, built-in libretro direction, and external-emulator launching model, but changes the library scanner, BIOS handling, in-game menu behavior, and several UX defaults to better fit this build.

Upstream Cannoli:

- GitHub: https://github.com/CannoliHQ/cannoli
- Website: https://cannoli.dev

## Current Focus

- Android frontend for ROM libraries, external emulators, and bundled libretro cores.
- Less opinionated ROM folder handling.
- No automatic creation of platform/game/BIOS subfolders.
- Explicit ROM and BIOS directory settings.
- First-run setup asks for storage permission, ROM directory, and BIOS directory.
- Practical built-in libretro improvements for handheld/frontend use.

## Changes In This Fork

- Added a separate `BIOS Directory` setting under Library.
- First-run setup now lets the user choose ROM and BIOS directories instead of silently assuming pre-made folders.
- Built-in libretro BIOS lookup now scans the selected BIOS folder and its subfolders.
- Firmware is staged into the app cache using core-declared BIOS names so cores can find files even when the user's folder layout or file casing differs.
- Built-in libretro JNI exports now match the Karipap package namespace.
- Disabled automatic creation of ROM, BIOS, and per-game subfolders.
- Scanner now detects games by file/content where possible instead of requiring exact folder names.
- Scanner is limited to the selected ROM directory and subfolders, not the whole storage root.
- Fixed case/alias duplicate issues such as `GB` vs `gb`, `psx` vs `PlayStation`, `sfc` vs `SNES`, `megadrive` vs `Genesis`, and other common frontend folder names.
- Platform menus strip known platform folder aliases so games show directly instead of being hidden behind an extra alias folder.
- Fixed PlayStation games under a `psx` folder showing as a nested `psx` folder in the PlayStation menu.
- Added and fixed built-in libretro analog handling, including PlayStation BIOS detection for PCSX-ReARMed.
- Built-in libretro core options are applied before core init so PCSX-ReARMed can honor BIOS boot-logo settings at startup.
- PCSX-ReARMed built-in launches default to real BIOS auto-detection with the PlayStation startup logo enabled; SwanStation fast boot is disabled by default.
- The native core-option cache reflects startup overrides so the in-game menu shows the active values.
- Upstream Cannoli auto-update endpoints are disabled by default so fork builds do not offer upstream APK updates.
- Added more in-game video scaling options for built-in libretro cores.
- Added `Show FPS` to in-game shortcut settings and fixed fast-forward FPS reporting.
- Added artwork scraping from the selected ROM directory only, with local cover caching.
- Moved `Refresh Library` to `Settings > Advanced`.
- Tuned launcher analog-stick menu navigation to reduce accidental double-scrolls.
- Set JetBrainsMono Nerd Font as the default bundled UI font.

## Artwork Scraper

Artwork scraping is ROM-driven: Karipap only offers scraper entries for platforms that have ROMs in the selected ROM directory. Sources are attempted as local-cache downloads rather than permanent hotlinks where allowed:

- Libretro thumbnails
- TheGamesDB API
- DSESS-style HTML selector scraper

Respect upstream source terms, robots.txt, rate limits, and attribution requirements.

## Storage Compatibility

Fresh setups use a Karipap root by default and ask for explicit ROM and BIOS folders. Legacy `Synnoli` and `Cannoli` root names remain in code only as compatibility fallbacks for existing user data and migrations; they are not presented as the new frontend identity.

## Release Updates

Karipap does not point at Cannoli's update feed. To publish update checks for your own GitHub releases, configure `UPDATE_FEED_URL` and `UPDATE_DOWNLOAD_BASE_URL` in `app/build.gradle.kts` and host a compatible `versions.json` feed.

## Credits

Karipap is based on Cannoli by CannoliHQ. Third-party libraries, bundled cores, fonts, and shaders retain their original licenses and credit entries in the app.

## License

This fork follows the upstream license terms in `LICENSE` and preserves third-party license obligations for bundled dependencies, cores, shaders, and assets.
