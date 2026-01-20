# Private IPTV Starter (JetStream Compose Fork)

This project is a TV-focused IPTV-style starter built on the JetStream Compose sample. It plays
standard HLS/MP4 streams from a local JSON playlist and is ready to be wired to your own backend.

## Where the channel list lives
- `jetstream/src/main/assets/movies.json` is the channel list.
- `jetstream/src/main/assets/movieCategories.json` is the category list.

The app reads these assets at startup and renders them as rows/grids. Each item needs the same
shape as the current JSON entries. The keys are required by the Kotlin serializer, so keep them
in place.

## Replace streams safely
- Replace the `videoUri` fields with your own HLS/DASH URLs.
- Keep `subtitleUri` as `null` unless you have WebVTT subtitles.
- If you want different logos, update `image_16_9` and `image_2_3`.

## Build
Open `JetStreamCompose` in Android Studio and run the `jetstream` app configuration.

## Notes
- The Media3 HLS extension is included, so HLS streams will play.
- The UI is tailored for D-pad navigation on Fire TV/Android TV.
