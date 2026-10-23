# Accord Garage

A personal Android app for one car: a red 2008 Honda Accord EX-L coupe (VIN 1HGCS12838A014367, 2.4L K24Z3, 5-speed automatic). It keeps the service log, reminders, the owner's manual with search, step-by-step guides, recalls and quick specs, all offline. It is for personal use only and is installed by APK.

- **Install:** download the latest `.apk` from [Releases](https://github.com/JamesSallee/accord-garage/releases) on the phone and open it. Each build installs over the last one.
- **How it's built:** Kotlin and Jetpack Compose. Every push to `main` builds a signed APK and publishes it as release `build-N`. Pull requests get a build check without publishing. See [docs/PROJECT_BRIEF.md](docs/PROJECT_BRIEF.md) for the car, the decisions made so far and how the code is laid out.
- **Plan:** the PRD and the [PRD coverage check](https://claude.ai/artifact/JEp8KxiXXtQWkTWazyYcFY) (Oct 2026) list what the app is meant to do and how far it has got.

## What's left to do

Status as of build 13 (Oct 2026), after the v1.0 gaps in PR #3 were closed. Nothing has been tested on the phone yet.

### 1. On the car

These need someone at the car, not code.

- [ ] Take a daylight photo of the car for the home screen. It replaces the illustration in v1.1.
- [ ] Read the paint code off the driver's door-jamb label.
- [ ] Read the battery group size off the current battery.
- [ ] Measure or look up the driver and passenger wiper blade sizes.
- [ ] Find the factory radio's anti-theft code (glovebox sticker, the manual, or a Honda dealer with the VIN). It's needed before the head unit install.
- [ ] Check the under-hood emissions label for PZEV or ULEV. It decides which spark plug to buy.
- [ ] After the head unit install, note its firmware version.
- [ ] Choose the auto-dimming mirror and dash cam models, so their guide can be finished.

All of these except the photo and the mirror and dash cam go into Quick specs in the app.

### 2. Test on the phone

- [ ] Install build 13 or later and check that data logged on an earlier build is still there. This build moved the database to version 2.
- [ ] Import Honda's 2008 Accord Coupe manual PDF and time it. The target is under 60 seconds for about 400 pages.
- [ ] Add the AINAVITO manual from photos and check that its pages come up in search.
- [ ] Write a 50-question test set for manual search. The target is the right page in the top 3 for 90% of questions.
- [ ] Log a service with one receipt photo in under 30 seconds.
- [ ] Turn on airplane mode and check that reminders still notify.
- [ ] Check contrast (WCAG AA) in the dark and sunlight themes, 56 dp touch targets on guide and roadside screens, and smooth scrolling.

### 3. Rest of v1.0

Smaller items the coverage check found that PR #3 didn't cover.

- [ ] Catch-up: ask on first launch when each 100,000-mile item was last done. Add engine and transmission mounts, the belt tensioner and suspension bushings to the list, and add a prompt to log every top-up for the next 3,000 miles.
- [ ] Maintenance Minder: show parts and guide links for each code, and remind about resetting the Minder right after a Minder service is logged. Check whether this car's manual has a sub-code 6; the app has 1–5.
- [ ] Manual: download it inside the app instead of opening Honda's site, and let the owner highlight passages (bookmarks and page notes are done).
- [ ] Quick specs: add a fuse map.
- [ ] Recalls: list known service bulletins. NHTSA's public API works by model year, so the in-app list covers every 2008 Accord, and the "Check my VIN" link gives the per-car answer.
- [ ] Fuel log: chart MPG over time (a sudden drop is already flagged).
- [ ] Guides: add a cost estimate and DIY savings to each header, OEM and aftermarket part numbers, inline photos or clips, a verification state (written, checked against the manual, tested on this car), and content updates that don't need a new APK.
- [ ] Car profile: list the new head unit and the factory radio (now the amplifier) with its code and input setting.
- [ ] Design: switch to the sunlight theme automatically in bright light, show the service timeline as a horizontal track against mileage, use thin arc gauges for oil life, pads and tread, count numbers up when a screen opens, add haptic ticks, and make each system on the car illustration tappable on its own.
- [ ] Engineering: raise the target SDK from 34, and add automated tests (CI only builds today).

### 4. v1.1: Diagnose and learn

- [ ] Photo of the car on the home screen (waiting on the daylight photo).
- [ ] OBD-II scanner over a Bluetooth ELM327 adapter: read and clear codes, explain them for this car, show live data, log readiness monitors. Pick the first adapter to support.
- [ ] Warning-light identifier by pointing the camera at the dash (the picture grid is done).
- [ ] Symptom troubleshooter weighted by this model's known issues.
- [ ] Full guide catalog across every system in the PRD, beyond the 15 shipped.
- [ ] Hands-free mode: "next" and "repeat" by voice (reading steps aloud is done).
- [ ] Auto-dimming mirror and dash cam guide, with a shared fuse tap and one headliner run.
- [ ] Tire log: brand, size, DOT date code and age warning, rotation reminders (tread depth per corner is in Wear trends).
- [ ] Parts and shopping list gathered from guides.
- [ ] Seasonal checklists: winter, summer and long-trip.
- [ ] Resale packet: one PDF of the full history, receipts and photos.
- [ ] Home-screen widget: next service, miles to go, open recalls.

### 5. v2.0: Ownership insight

- [ ] Total cost of ownership, including insurance, and a repair-or-replace calculator.
- [ ] Parking memory when the phone disconnects from the car's Bluetooth.
- [ ] Shop directory linked to past services.
- [ ] Upgrade guides with photo builds from this car.

### Open questions

- [ ] Is there past service history to import, especially for spark plugs, coolant, ATF and rear brakes?
- [ ] Which OBD-II adapter should be supported first?
- [ ] The radio code (see "On the car").

Done: every airbag recall on this VIN is completed and recorded in the app.
