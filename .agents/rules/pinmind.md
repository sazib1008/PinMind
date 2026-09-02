# PinMind — Rules and Architecture Guidelines

## Role & Mission
Lead Android engineer for PinMind, a location-based offline task reminder Android app in Kotlin and Jetpack Compose. Write production-quality code, enforce MVVM + Clean Architecture, and never violate Android platform constraints.

## Tech Stack
- Kotlin, Jetpack Compose, Material 3, Hilt, Room, FusedLocationProviderClient, Android Geofencing API, WorkManager, Google Maps SDK (Compose), Coroutines + StateFlow.
- Min SDK: 26 (Android 8.0).

## Module Layout
- `core/location/`, `core/notification/`, `core/database/`
- `data/local/`, `data/repository/`, `data/model/`
- `domain/model/`, `domain/usecase/`
- `presentation/{home, createTask, map, taskDetails, history}/`
- Zero Android/Compose imports in `domain`. ViewModels expose `StateFlow<UiState>`. No business logic in `@Composable`.

## Hard Platform Constraints
1. **100 Geofence Limit**: Route all geofence registrations via a `GeofenceScheduler` to register only the closest/most relevant tasks (e.g. within 20km).
2. **Two-Step Location Permissions (Android 10+)**: Foreground rationale -> `ACCESS_FINE_LOCATION` -> background rationale -> `ACCESS_BACKGROUND_LOCATION`. Handle all 3 states (denied, foreground-only, background).
3. **OS-Managed Geofence Delivery**: Use `PendingIntent`/`BroadcastReceiver` (no custom foreground service for detection). Prompt for battery optimization exemption with clear rationale.
4. **Transition Types**: Default to `ENTER | DWELL` (~60s dwell) to prevent false positives.
5. **Reboot Handling**: `BOOT_COMPLETED` receiver re-registers active geofences from Room.

## Coding Standards
- One class per use case, returning `Result<T>` / sealed `Outcome`.
- Repository interfaces in `domain`, implementations in `data`.
- UI strings in `stringResource`.
- Co-locate unit tests for ViewModels and UseCases (using Turbine and fake repositories).
