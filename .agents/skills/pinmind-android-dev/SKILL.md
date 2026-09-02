---
name: pinmind-android-dev
description: Guidance for building or modifying PinMind, a location-based (geofencing) task reminder Android app in Kotlin/Jetpack Compose. Use this skill whenever the user is writing code, planning architecture, adding features, or debugging anything inside the PinMind project — including geofencing, background location, task CRUD, notifications, or Room database work. Also trigger this any time the user mentions "PinMind", "GeoTask", "geofence reminder app", or asks to add/fix a screen, use case, or repository within this codebase, even if they don't explicitly ask for architectural guidance.
---

# PinMind Android Dev

Enforces PinMind's architecture and Android platform constraints so that
generated code stays consistent across sessions and doesn't silently violate
OS-level limits (geofence count, background location permission flow,
battery optimization).

## When to consult this skill

- Any task that touches PinMind source code: new screen, new use case,
  Room entity/DAO change, geofence logic, notification logic, permission flow.
- Planning or re-planning architecture, module boundaries, or the phase order.
- Debugging why geofences aren't firing, permissions aren't granted, or
  notifications aren't appearing.
- Reviewing/refactoring existing PinMind code for consistency with the rules below.

Not needed for: general Kotlin/Android questions unrelated to this project,
or one-line syntax lookups.

## Project identity

**PinMind** — offline, no-login Android app. User attaches a location +
radius to a task; app notifies them when they physically arrive. No backend.

## Non-negotiable architecture

```
app/
core/location/       core/notification/    core/database/
data/local/  data/repository/  data/model/
domain/model/  domain/usecase/
presentation/{home,createTask,map,taskDetails,history}/
```

- MVVM + Clean Architecture. `domain` has zero Android/Compose imports.
- One class per use case. Repository interfaces in `domain`, impls in `data`.
- ViewModels expose `StateFlow<UiState>` only — no LiveData, no RxJava.
- No business logic inside `@Composable` functions.
- Stack: Kotlin, Jetpack Compose, Material 3, Hilt, Room, Coroutines,
  FusedLocationProviderClient, Android Geofencing API, WorkManager,
  Google Maps SDK. minSdk 26.

If a request would require deviating from this stack or layout (e.g. "just
put the query in the Composable" or "let's use LiveData here"), flag it and
propose the compliant alternative instead of silently complying.

## Hard platform constraints (always check code against these)

1. **Geofence cap = 100 per app, system-wide.** Never register a geofence
   per task unconditionally. There must be a `GeofenceScheduler`
   (`core/location`) that selects only the closest/soonest-relevant tasks
   (e.g. within 20km of last known location) and re-evaluates on
   significant location change. If you see or are about to write code that
   loops over *all* tasks and calls `addGeofences` for each, stop and route
   it through the scheduler instead.

2. **Background location permission is two-step on Android 10+.** Never
   request `ACCESS_BACKGROUND_LOCATION` alongside `ACCESS_FINE_LOCATION` in
   one prompt. Required order: in-app rationale UI → request
   `ACCESS_FINE_LOCATION` → only after granted, separate rationale screen →
   request background permission. All three permission states (denied,
   foreground-only, background) must have explicit UI handling — no
   assumed happy path.

3. **OEM battery optimization can kill background work.** Detection must
   rely on the OS-managed Geofencing API `PendingIntent`/`BroadcastReceiver`
   mechanism — never a custom long-running foreground service as the primary
   detection path. Include a one-time
   `ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS` prompt with user-facing
   copy explaining why, don't just fire it silently.

4. **Use ENTER + DWELL, not just ENTER**, on new geofences (default dwell
   ~60s) to avoid false triggers from passing by. Only use plain ENTER if
   the user explicitly asks for instant triggering.

5. **Re-register geofences after reboot.** Any geofence-creation code path
   needs a matching `BOOT_COMPLETED` receiver that reloads active tasks
   from Room and re-adds their geofences — the OS clears geofences on restart.

## Coding standards to enforce

- Use cases return `Result<T>` / a sealed `Outcome` — never throw across
  layer boundaries.
- No hardcoded UI strings — use `stringResource`.
- Write the unit test alongside each new ViewModel/use case, not as a
  follow-up. Prefer fake repositories over mocks; use Turbine for Flow tests.
- Add KDoc where the function's purpose isn't obvious from its signature.

## Build order (don't skip ahead unless the user explicitly asks to)

1. Room DB + Task CRUD + basic list/detail UI (no location)
2. Location permission flow + map picker + save lat/lng/radius
3. Geofence registration + BroadcastReceiver + notification firing
4. GeofenceScheduler (100-limit handling) + boot receiver + battery optimization prompt
5. Polish: dark mode, search, categories, history, animations

At the end of each phase, summarize what was built, what was deliberately
deferred, and any platform risk introduced before starting the next phase.

## Quick self-check before finishing any PinMind task

- [ ] Does new code respect the module boundaries above?
- [ ] If geofences are touched: does it go through the scheduler, respect the 100 cap, and use ENTER+DWELL by default?
- [ ] If permissions are touched: is the two-step background flow intact?
- [ ] Are there tests for new ViewModels/use cases?
- [ ] Is there a boot-receiver path if new geofences are registered?
