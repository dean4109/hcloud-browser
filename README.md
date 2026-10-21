# hCloud Browser

A minimal web browser for Android Automotive OS (built for a 2024 Chevy Blazer EV), installed through a private Play Console internal-testing track.

- Opens to a start page of streaming tiles (YouTube, Netflix, Hulu, Disney+, HBO Max, Prime Video, …). Tap **Edit** to add or remove tiles; the ⌂ button returns there.
- Streaming sites load their desktop versions automatically and can use the car's Widevine copy protection, so DRM video may play where the service allows browsers.
- Back / forward / reload, an address bar that also searches, and a Mobile/Desktop site toggle.
- Fullscreen video support.
- Uses the car's built-in Android System WebView, so the app stays tiny.
- Not marked distraction-optimized, so the car only allows it while parked.

Build: Actions → **Build hCloud Browser (Play bundle)** → Run workflow, then download the `.aab` artifact.
