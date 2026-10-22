# Accord Garage: project brief

Personal Android app for one car: James's red 2008 Honda Accord EX-L coupe. This brief carries the decisions from the first build so new conversations can continue without re-explaining.

## The car

| Item | Value |
| --- | --- |
| VIN | 1HGCS12838A014367 (decoded clean with NHTSA vPIC) |
| Model | 2008 Honda Accord EX-L coupe, 2 doors, red |
| Engine | 2.4L K24Z3 I4, DOHC, 190 hp, timing chain (no timing belt, no VCM) |
| Transmission | 5-speed automatic |
| TPMS | Direct, a sensor in each wheel (originals about 18 years old) |
| Built | Marysville, Ohio |
| Mileage | 102,000 as of Oct 2026 |
| Factory radio | 6-disc changer, no factory navigation |

Known weak spots the app watches: early rear brake pad wear, oil consumption (piston rings on the 2.4L), VTC actuator cold-start rattle, Takata and 2008–2009 airbag recalls.

## Decisions so far

- Personal use only, installed by APK; never published. Uses Honda's official owner's manual (owner downloads the PDF from owners.honda.com and imports it; searched offline).
- VIN and specs are built into the app; no setup step and no content for other Accord versions.
- 100,000-mile catch-up list: spark plugs, valve clearance, coolant, ATF, rear brake pads.
- Home screen callouts appear only on a part of the car with something due, overdue or flagged, and disappear once logged.
- Design: dark "instrument panel" look (graphite, one red accent, Inter + JetBrains Mono, numbers as hero), no neon or sci-fi effects. Sunlight (light, high-contrast) mode in More.
- Car image: a red coupe illustration for now (kept generic, not a copy of Honda's design); replace with a photo of James's own car in v1.1.
- App icon: white gauge arc around a red coupe on graphite; monochrome themed version.

## Head unit install (in the app as the featured guide)

- Unit: AINAVITO 10-inch wireless CarPlay / Android Auto, model 10-Accord-8th-2008-2013-L (Amazon B0GRY6CSP6). Fits 2008–2012 Accords without factory nav. Likely no GPS antenna; phone GPS is fine.
- Factory 6-disc radio is kept, moved lower behind the screen, and acts as the amplifier: new unit's RCA OUT jacks to the breakout cable on the factory radio.
- Videos: Iowa Buckeye stereo install https://www.youtube.com/watch?v=crPFee1Pvgc (steering-wheel connector at 13:35 and 17:33) and backup camera https://www.youtube.com/watch?v=UUR9t--j1Mc (coupe route: grommet between plate and left taillight, cable behind the rear seat pivot).
- Radio anti-theft code needed after the battery is disconnected.

## Links

- PRD (Claude doc): https://claude.ai/artifact/BtkFn1qeLWyQnAf2WzAVPv
- Design mockups (home, tutorial step, icon): https://claude.ai/artifact/6gAWfCAUV3qihpL24T8bEt
- Code: https://github.com/JamesSallee/accord-garage (private)
- APK downloads: https://github.com/JamesSallee/accord-garage/releases

## How the code is built

- Kotlin, Jetpack Compose, Material 3 with a custom theme; Room (SQLite, FTS4 for manual search); WorkManager for daily reminders; pdfbox-android for manual text; ML Kit text recognition (Play services) for receipts; no accounts, no analytics.
- Package `app.accordgarage`. Main folders: `data/` (entities, DAOs, recalls, manual import, backup), `domain/` (job catalog, Minder codes, due-date engine), `content/` (tutorials, specs, roadside), `ui/` (theme, components, screens).
- Every push to `main` runs GitHub Actions, builds a signed release APK and publishes it as a GitHub release `build-N`. Compiler errors show as annotations on the failed run.
- Signing key `app/keystore/garage.jks` lives in the private repo so every build installs over the last. Keep the repo private.
- Android tooling can't be downloaded in Claude's sandbox, so builds and testing happen in CI and on the phone.

## Status and next steps

- v1.0 (build 5): home, service/fuel/oil logs with receipt OCR, Minder decoder, reminders, manual import and search, 15 guides, recalls, quick specs, documents wallet, roadside help, encrypted backup file. Not yet tested on the phone.
- v1.1 ideas: photo of the car on the home screen, OBD-II Bluetooth scanner, auto-dimming mirror + dash cam guide (shared fuse tap and headliner run; products not yet chosen), hands-free voice "next"/"repeat", warning-light identifier, symptom troubleshooter, tire log, resale PDF, home-screen widget.
- Open: confirm Quick specs values against the manual.
- Done: all airbag recalls are completed on this VIN and recorded in the app (confirmed by James, Oct 2026).
