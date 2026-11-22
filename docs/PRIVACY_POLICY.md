# Privacy Policy — Priyo Azan

Priyo Azan is designed as an offline-first, privacy-respecting Android application.

1. **No Accounts & No Tracking**: Priyo Azan does not require user registration and does not include third-party analytics, advertising SDKs, or behavioral tracking.
2. **Local Storage Only**: Your location coordinates, prayer calculation preferences, Azan alarm schedules, Quran bookmarks, and reading progress are stored exclusively on your device inside a local SQLite (Room) database.
3. **Location Usage**: GPS/Network location is requested solely to calculate local prayer times and Qibla bearing. Coordinates are never uploaded to any Priyo Azan server. Reverse geocoding uses Android's system `Geocoder` only when you explicitly request location detection.
4. **Network Requests**: Internet connectivity is used only when checking GitHub Releases for app updates (with SHA-256 verification before prompting installation) or optional system city-name geocoding.
5. **Permissions**:
   - `ACCESS_COARSE_LOCATION` / `ACCESS_FINE_LOCATION`: Compute local prayer times & Qibla angle.
   - `POST_NOTIFICATIONS`: Display prayer alerts and active Azan playback controls on Android 13+.
   - `SCHEDULE_EXACT_ALARM`: Trigger Azan alerts accurately at prayer times on Android 12+.
   - `RECEIVE_BOOT_COMPLETED`: Automatically restore scheduled prayer alarms after device restart.
