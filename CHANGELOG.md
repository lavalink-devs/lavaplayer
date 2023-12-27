# Change Log

## [2.0.4] -- 2023-03-12

* Fix NPE on missing author in playlist tracks in https://github.com/lavalink-devs/lavaplayer/pull/45
* Add ISRC to soundcloud in https://github.com/lavalink-devs/lavaplayer/pull/35
* Use Twitch stream preview thumbnail instead of channel icon in https://github.com/lavalink-devs/lavaplayer/pull/47

## [2.0.3] -- 2023-05-11

- Updated `IDENTITY_REGEX` in https://github.com/lavalink-devs/lavaplayer/pull/44

## [2.0.2] -- 2023-27-09

- Fix a bug with MPEG parsing that would lead to range exceptions in https://github.com/lavalink-devs/lavaplayer/pull/31
- Specify all request timeouts in https://github.com/lavalink-devs/lavaplayer/pull/33

## [2.0.1] -- 2023-14-08

### Added

- Support MPEG 2.5 by [@markozajc](https://github.com/markozajc) in https://github.com/lavalink-devs/lavaplayer/pull/30

## [2.0.0] -- 2023-03-08

### Fixed

- Fixed YouTube 403 errors in https://github.com/lavalink-devs/lavaplayer/pull/16
- Fixed YouTube access token issue in https://github.com/lavalink-devs/lavaplayer/pull/15

### Removed

- Removed `stream-merger` module
- Removed `node` module & classes in `main` module
- Removed `test-samples` module

### Updated

- Updated `commons-io:commons-io` to `2.13.0`
- Updated `org.apache.httpcomponents:httpclient` to `4.5.14`
- Updated `com.fasterxml.jackson.core:jackson-core` to `2.15.2`
- Updated `com.fasterxml.jackson.core:jackson-databind` to `2.15.2`
- Updated `org.slf4j:slf4j-api` to `2.0.7`
- Updated `ch.qos.logback:logback-classice` to `1.4.8`
- Updated `org.codehaus.groovy:groovy` to `4.0.13`
- Updated `org.spockframework:spock-core` to `2.4-M1-groovy-4.0`
- Updated `org.json` to `20230618`
- Updated `org.jsoup:jsoup` to `1.16.1`

## Added

- Added `AudioPlayerManager#loadItemSync` to allow loading tracks synchronously
- Added `natives` & `natives-publish` modules for lavaplayer native libraries
- Added support soundcloud short urls for in https://github.com/lavalink-devs/lavaplayer/pull/4

## [1.3.50] -- 2020-06-17

### Fixed

- Fixed YT search sometimes not finding anything because of YT providing different format (PR 492 by Frederikam).

## [1.3.49] -- 2020-05-21

### Fixed

- Fixed Twitch streaming breaking due to API change (PR 480 by Devoxin).
- Fixed YT private videos providing invalid result instead of failing (PR 479 by Devoxin).

## [1.3.48] -- 2020-05-12

### Fixed

- Fixed some YouTube tracks not working because of cipher detection issues (PR 475 by Devoxin).

## [1.3.47] -- 2020-04-13

### Fixed

- Fixed loading YouTube live streams within playlists (PR 294 by Devoxin).

## [1.3.46] -- 2020-04-07

### Fixed

- Fixed YouTube mix loading, mixes loaded with just one request now (PR 293 by Devoxin).

## [1.3.45] -- 2020-04-07

### Fixed

- Fixed some YouTube live streams not being loaded and randomly stopping (PR 291 by Devoxin).

## [1.3.44] -- 2020-04-04

### Fixed

- Fixed failing to load SoundCloud playlists with more than 50 tracks (PR 290 by Devoxin).

## [1.3.43] -- 2020-04-02

### Fixed

- Fixed SoundCloud connection leaks which eventually caused pool limit to be reached and new connections getting stuck.

## [1.3.42] -- 2020-04-01

### Added

- Added `AbstractHttpContextFilter` to easily create additional HTTP context filters with delegates.

## [1.3.41] -- 2020-04-01

### Added

- Added support for GetYarn.io as a source (PR 277 by duncte123).

### Fixed

- Fixed audio event adapter method with stuck stacktrace included not getting called.

## [1.3.40] -- 2020-03-28

### Added

- Added `SimpleHttpClientConnectionManager` to sources which can be used to disable pooling for HTTP of some source.

## [1.3.39] -- 2020-03-27

### Added

- Added method to set Apache HTTP connection manager factory to customize connection handling.

### Changed

- Track stuck events now provide stacktrace of the struck thread.

## [1.3.38] -- 2020-03-23

### Fixed

- Fixed unable to fetch YouTube player script for ciphered URLs.

## [1.3.37] -- 2020-03-23

### Changed

- Unexpected response code from YouTube player script request now logs more details.

## [1.3.36] -- 2020-03-22

### Added

- Added method to provide a set of IPs to be used for making an HTTP connection without choosing a specific one.

### Changed

- HTTP connection failure exceptions now contain all details about the connection that was being established.

## [1.3.35] -- 2020-03-09

### Fixed

- Fixed NPE for unavailable SoundCloud tracks (PR 276 by duncte123).

## [1.3.34] -- 2020-01-27

### Fixed

- Fixed YouTube missing fallback to embed page for "objectionable content" videos (PR 270 by Xavinlol).

## [1.3.33] -- 2020-01-05

### Fixed

- Fixed NPE instead of common exception for missing YouTube video (PR 264 by duncte123).
- Fixed blocked tracks included in SoundCloud liked tracks result (PR 261 by nikammerlaan).

### Added

- Added builder for SoundCloud source manager.
- Added a more concise way to create an extended media container registry.

## [1.3.32] -- 2019-12-01

### Fixed

- Fixed SoundCloud tracks longer than 30 minutes breaking during playback due to URLs expiring.

## [1.3.31] -- 2019-12-01

### Fixed

- Fixed redundant retry attempts for SoundCloud liked tracks and search result pages.

## [1.3.30] -- 2019-12-01

### Changed

- SoundCloud source manager is now modular and more easily extendable.

### Fixed

- Fixed non-opus tracks no longer working due to old stream API not being available anymore.
- Fixed SoundCloud opus seeking not working when applied before track starts.

## [1.3.29] -- 2019-11-25

### Fixed

- Fixed SoundCloud opus seeking not working.

## [1.3.28] -- 2019-11-25

### Fixed

- Fixed response code 203 treated as an error everywhere.

## [1.3.27] -- 2019-11-19

### Fixed

- Fixed SoundCloud track loading failing if no opus track is available.

## [1.3.26] -- 2019-11-19

### Added

- Support for SoundCloud opus streams by default, old MP3 streams only used if opus is not available.

## [1.3.25] -- 2019-11-06

### Changed

- Manipulation of HTTP requests and triggering retries with HTTP context filter instead of a simple request modifier.

## [1.3.24] -- 2019-11-06

### Added

- HTTP request modifier interface which allows modifying outgoing requests.

## [1.3.23] -- 2019-10-28

### Changed

- Use YouTube pbj=1 requests for tracks and playlists.

## [1.3.22] -- 2019-09-11

### Fixed

- Fixed new track formats breaking YouTube track loading (PR 210 by Ferderikam)

## [1.3.21] -- 2019-09-09

### Fixed

- Fixed playing direct SoundCloud tracks which broke due to site change (PR 208 by Devoxin).
- Fixed some cases where YouTube tracks broke because of duplicate parameters (PR 180 by Joakim).
- Fixed new format YouTube pages where no cipher was used (PR 205 by Devoxin).

## [1.3.20] -- 2019-08-08

### Fixed

- Fixed playback for YouTube when format list is only provided through player_response.

## [1.3.19] -- 2019-07-30

### Fixed

- Fixed broken YouTube track loading due to changes the site (PR 199 by Frederikam).

## [1.3.18] -- 2019-07-19

### Changed

- YouTube track info JSON now gets logged if it does not contain any expected fields.

### Fixed

- Fixed closing MKV Opus track throwing an exception if playback initialization had thrown an exception earlier.
- Fixed exception from track close reported instead of playback exception on MKV close.
- Fixed passing an invalid parameter to YouTube embed page URL.

## [1.3.17] -- 2019-04-26

### Fixed

- Fixed age-restricted videos not being playable (PR 172 by Devoxin).

## [1.3.16] -- 2019-04-02

### Fixed

- Fixed probe hint matching logic for aac/ts causing HTTP source to not resolve tracks when mime type was present.

## [1.3.15] -- 2019-04-01

### Fixed

- Fixed no automatic reconnect on connection reset for some JDK versions.

## [1.3.14] -- 2019-03-28

### Fixed

- Fixed MKV frame size parsing which caused some MKV/WEBM files to fail in the middle of playback.

## [1.3.13] -- 2019-03-21

### Fixed

- Fixed an old x86 Linux native being bundled with main LP artifact, causing previous libc conflict to still manifest.

## [1.3.12] -- 2019-03-20

### Fixed

- Fixed natives crashing due to libc conflict.
- Fixed exception when calling provide on an ending track.

## [1.3.11] -- 2019-02-28

### Added

- Support for playing MPEG-TS files directly, including metadata support.
- Metadata for OGG vorbis files.

### Changed

- AAC and MP3 decoders updated to newer versions.
- Linux native libraries have better compatibility with alternate libc implementations (musl on Alpine for example)
- More flexibility for specifying which native libraries and from where to load.

### Fixed

- Fixed file format detected incorrectly due to check order when file extension is known.
- Fixed exception when processing the last frame of some FLAC files.
- Fixed YouTube video signature using wrong parameter name in some cases.
- Fixed ImmutableAudioFrame#getData doing the wrong thing.
- Fixed MP3 track length detected incorrectly if they contain certain type of metadata.
- Fixed a race condition which could sometimes drop the last frame of a track.

## [1.3.10] -- 2018-10-24

### Added

- Generic HLS stream support.
- Ability to add custom file format support (as custom MediaContainerRegistry).

### Fixed

- Fixed YouTube streams not reconnecting due to SSL exception wrapper changes in JDK10+.

## [1.3.9] -- 2018-10-18

### Fixed

- Fixed playing age-restricted videos using the embed page (PR 136 by duncte123).

## [1.3.8] -- 2018-10-18

### Added

- Added support for YouTube watch_videos urls (PR 123 by matsprehn).
- Added support for YouTube music.youtube.com domain links (PR 134 by GigaFyde).

### Fixed

- Fixed Mixer streams.
- Fixed Twitch streams by using the Helix API (PR 132 by tdeeb).
- Fixed OGG metadata memory usage exploit (PR 131 by napstr).

## [1.3.7] -- 2018-07-02

### Fixed

- Fixed YouTube search not working in some regions due to different time format.

## [1.3.6] -- 2018-06-19

### Changed

- Made it easier to use custom AudioPlayer implementations.

## [1.3.5] -- 2018-06-13

### Fixed

- Fixed volume formula not scaling well beyond 150 - now switches to linear mode after 150.

## [1.3.4] -- 2018-06-12

### Changed

- Maximum volume setting set to 1000.

### Fixed

- Fixed AAC+SBR+PS not working properly for MP4/MKV containers.

## [1.3.3] -- 2018-06-05

### Added

- Ability to write more custom audio players as the player is now an interface.
- HTTP stream reconnect when using Conscrypt as SSL provider.

## [1.3.2] -- 2018-06-03

### Fixed

- Fixed mutable audio frames and non-allocating buffer exploding in some use cases.

## [1.3.1] -- 2018-06-03

### Fixed

- Fixed PCM output format regression.

## [1.3.0] -- 2018-06-02

### Added

- Option for allocation-free frame provision pipeline by allowing to set NonAllocatingAudioFrameBuffer as the default
  frame buffer implementation and then using the new provide methods that take mutable audio frame instances.
- Ability to set SSL context for internally used HTTP clients. This allows for custom SSL providers such as Conscrypt.
- Support for custom output formats using custom implementations of AudioDataFormat.

### Changed

- Audio frame is now an interface, which is a breaking change for all use cases.
- Removed the concept of audio hooks.

### Fixed

- Some audio sources not freeing their HTTP clients when they were discarded.
- Potential NPE when the only node went down when a track was playing on it.
- Not closing OGG decoders created during track probing (still got closed on finalize).
- Missing Javadocs.

## [1.2.64] -- 2018-06-01

### Fixed

- Fixed WAV files with format header longer than 16 bytes not working.

## [1.2.63] -- 2018-04-12

### Fixed

- Fixed Twitch or Mixer tracks ending when next chunk of the stream is not available yet.
- Fixed non-mono inputs being distorted or passing the wrong number of channels to custom filters.
- Fixed loading a mix on a non-existing track throwing an exception instead of triggering no matches.

### Added

- Ogg FLAC and Ogg Opus metadata (title and artist).
- Track length detection for Ogg Opus and Ogg FLAC on a seekable stream.
- Metadata detection from ShoutCast stream headers.

## [1.2.62] -- 2018-04-02

### Added

- Method to set track frame buffer duration per player.

## [1.2.61] -- 2018-04-02

### Fixed

- Fixed HTTP 403 for some YouTube tracks due to adding one parameter to the playback URL twice.

## [1.2.60] -- 2018-04-02

### Added

- Equalizer filter and its filter factory to LavaPlayer classes (still need to be applied manually).

## [1.2.59] -- 2018-03-30

### Added

- Ability to add a custom audio filter factory to an instance of an audio player.

## [1.2.58] - 2018-03-26

### Fixed

- Fixed Twitch stream loading regression.

## [1.2.57] - 2018-03-26

### Fixed

- Fixed YouTube mix loading regression.

## [1.2.56] - 2018-02-28

### Added

- Added method to get Twitch track channel name.

### Fixed

- Fixed NPE when YouTube URL has parameters without values.

## [1.2.55] - 2018-02-27

### Fixed

- Fixed Windows native libraries being mixed up.

## [1.2.54] - 2018-02-23

### Changed

- Attempt to retry connection in case of socket read timeout.

### Fixed

- Fixed mobile SoundCloud links not working.
- Fixed 32-bit Windows natives being incompatible.

## [1.2.53] - 2018-02-21

### Changed

- YouTube search result loader performs HTTP requests with cookies disabled by default now.

## [1.2.52] - 2018-02-20

### Fixed

- Fixed YouTube URLs with extra characters after video ID not working even though YouTube accepts them.
- Fixed NPE when video ID is not in valid format, now triggers `noMatches` instead.
- Fixed missing YouTube video triggering `loadFailed` instead of `noMatches`.

## [1.2.51] - 2018-02-15

### Fixed

- Fixed attempting to load a native library that does not exist since last version on Windows x86.
- Fixed all consecutive HTTP connections failing after the HTTP of a source manager is reconfigured.

## [1.2.50] - 2018-02-15

### Fixed

- Fixed ADTS/AAC streams with SBR and/or PS enabled (also known as AAC+ streams).
- Fixed ADTS/AAC streams with MPEG version 2 refused.
- Fixed MPEG2 MP3 file frames read incorrectly, causing artifacts and spam in standard error stream.

## [1.2.49] - 2018-02-12

### Added

- Methods to allow reconfiguring HTTP builders used by source managers.

## [1.2.48] - 2018-02-11

### Added

- Method to change YouTube mix loader thread pool size.

## [1.2.47] - 2018-02-07

### Fixed

- Fixed YouTube ciphers being broken due to slight change to YouTube player script.

## [1.2.46] - 2018-02-04

### Changed

- All dependencies updated to latest versions (solves some issues with mismatching versions of Jackson).

## [1.2.45] - 2017-11-25

### Added

- Support for twitch streams with domain `go.twitch.tv`.
- Support for `m.soundcloud.com` domain for Soundcloud URLs.

### Fixed

- Fixed Twitch stream segment URL resolution occasionally producing incorrect URLs.
- Fixed YouTube links not working when they contain extra parameters.

## [1.2.44] - 2017-10-08

### Added

- Local tracks can now be encoded.

### Fixed

- Fixed Bandcamp source being broken due to URL format change.

## [1.2.43] - 2017-08-15

### Added

- Support for `mixer.com` (new `beam.pro` domain name).

### Fixed

- Fixed YouTube search being broken due to page style change.

## [1.2.42] - 2017-06-17

### Changed

- Improved efficiency of `.mkv`/`.webm` processing by making it practically garbage-free.

## [1.2.41] - 2017-06-14

### Fixed

- Fixed NPE thrown when an MPEG stream ends gracefully.

## [1.2.40] - 2017-06-05

### Fixed

- Fixed dual channel mono MP3 files not working.

## [1.2.39] - 2017-05-18

### Fixed

- Properly fixed the YouTube cipher detection (previous one missed some operations).

## [1.2.38] - 2017-05-18

### Fixed

- Partially fixed YouTube cipher detection where reserved keywords are function names.

## [1.2.37] - 2017-05-18

### Added

- Ability to store one user data object in audio track instances.
- Dumping YouTube scripts where cipher detection does not work.

## [1.2.36] - 2017-04-09

### Fixed

- Fixed YouTube tracks with only mixed audio-video MP4.

## [1.2.35] - 2017-04-08

### Changed

- Twitch stream title is now stream status rather than channel name.

## [1.2.34] - 2017-03-22

### Fixed

- Fixed MP3 not working with local files.

## [1.2.33] - 2017-03-22

### Fixed

- Fixed only MP3 working with local files.

## [1.2.32] - 2017-03-21

### Fixed

- Fixed deadlock when starting tracks from exception event

## [1.2.31] - 2017-03-19

### Fixed

- Fixed seeking while track is initialising possibly interrupting load.

## [1.2.30] - 2017-03-12

### Fixed

- Fixed stopped track threads sometimes not exiting.

## [1.2.29] - 2017-03-08

### Added

- Added track URL field to track info object.

## [1.2.28] - 2017-03-06

### Fixed

- Fixed SoundCloud client ID not updated if expiration was discovered on a request for search or playlist.

## [1.2.27] - 2017-03-06

### Fixed

- Fixed SoundCloud not working due to client ID change.

### Changed

- SoundCloud client ID now fetched from their page now, not hardcoded.

## [1.2.26] - 2017-03-05

### Added

- Support for mono MP3 format.
- PlayerLibrary which exposes the library version.

## [1.2.25] - 2017-03-04

### Fixed

- Fixed using nodes not working after node list is cleared once.

## [1.2.24] - 2017-03-04

### Fixed

- Fixed read timeout exception thrown when playing some YouTube live streams.
- Fixed excessive amount of requests made for YouTube live streams.

## [1.2.23] - 2017-03-02

### Added

- Added a way to configure HTTP request settings for source managers.

### Fixed

- Changing volume with PCM output rebuilds frame buffer into silence.

## [1.2.22] - 2017-03-01

### Added

- Added NicoNico source manager.

## [1.2.21] - 2017-02-26

### Fixed

- Fixed exploding on seekable YouTube live streams.
- Fixed using an unnecessary high track start message version when communicating with node.

## [1.2.20] - 2017-02-26

### Fixed

- Fixed a potential issue with YouTube ciphers not being detected correctly (403 error) and added more cipher logging.

## [1.2.19] - 2017-02-26

### Fixed

- Fixed some reconnect cases for HTTP connections not actually working.
- Fixed inaccurate seek on resampled streams.

## [1.2.18] - 2017-02-26

### Fixed

- Retry connection also on premature HTTP response end exception.

## [1.2.17] - 2017-02-26

### Changed

- Abandoned tracks are taken over by other nodes more quickly.

## [1.2.16] - 2017-02-26

### Changed

- When remote nodes go offline, their tracks are slowly taken over by other nodes.

### Fixed

- Fixed Beam.pro tracks not playable in nodes.
- Fixed seeking on tracks which had not been started yet.

## [1.2.15] - 2017-02-25

### Fixed

- Fixed a possible crash when a Vorbis track was shut down before being successfully initialised.

## [1.2.14] - 2017-02-24

### Fixed

- Retry stream connection on NoHttpResponseException, which may be caused by connection reuse.
- Retry stream connection on SSL errors, which may be caused by severed connection.
- Retry stream connection once on 500 series errors, due to possibly flaky server side issues.
- Fixed loadFailed being called for exceptions in other callbacks.

### Changed

- Improved logging to know causes of all failed requests and changes some inappropriate logging levels.

## [1.2.13] - 2017-02-22

### Fixed

- Fixed a regression with YouTube dashmpd-only tracks not working.

## [1.2.12] - 2017-02-18

### Added

- Support for alternative YouTube JSON format (adaptive_fmts -> url_encoded_fmt_stream_map).

## [1.2.11] - 2017-02-18

### Fixed

- Fixed interrupts from previous tracks may be carried over to executors of new tracks.

## [1.2.10] - 2017-02-11

### Added

- Beam.pro source manager.

## [1.2.9] - 2017-02-09

### Fixed

- Fixed reused clients restricting the number of concurrent connections to a very low value.
- Fixed track stop or seek throwing an exception in some conditions due to InterruptedException getting wrapped.

## [1.2.8] - 2017-02-09

### Changed

- Track info loader queue full exception is passed to loadFailed instead of being thrown directly.

### Fixed

- Fixed using wrong version of lavaplayer-common dependency (again).

## [1.2.7] - 2017-02-09

### Fixed

- Fixed track loading queue throwing "queue full" exception when the queue is not empty.

## [1.2.6] - 2017-02-08

### Fixed

- Fixed using wrong version of lavaplayer-common dependency.

## [1.2.5] - 2017-02-08

### Added

- Added the option to increase the number of threads used to load tracks.

## Changed

- Reusing HTTP client objects as much as possible.
- Limited the size of the track info loader queue.

## [1.2.4] - 2017-02-08

### Fixed

- Fixed YouTube live streams opening a new connection for each request.
- Fixed no read timeout applied to requests to nodes.
- Fixed track getting locked when seek gets stuck.
- Fixed attempting to read the entire response on closing a stream mid-way.

## [1.2.3] - 2017-02-07

### Changed

- Increased timeout for terminating the tracks of a node if its processing is stuck.

## [1.2.2] - 2017-02-06

### Fixed

- Fixed Vorbis MKV tracks not working if the channel count was not specified in MKV audio section.

## [1.2.1] - 2017-02-05

### Added

- Partial support for YouTube live streams (only MP4).

## [1.2.0] - 2017-02-04

### Added

- Method to poll frames from AudioPlayer in a blocking way.
- Support for different audio output formats.
- Utility class for getting an AudioInputStream from AudioPlayer.

## [1.1.47] - 2017-02-01

### Changed

- Fixed another smaller leak with Vorbis.

## [1.1.46] - 2017-02-01

### Fixed

- Fixed a native memory leak with Vorbis tracks.

## [1.1.45] - 2017-01-28

### Changed

- ARM binaries are now loaded from natives/linux-arm and natives/linux-aarch64 directories instead of x86 ones.

## [1.1.44] - 2017-01-28

### Added

- WAV file support (16-bit PCM).

## [1.1.43] - 2017-01-28

### Added

- Loading unlisted SoundCloud tracks.
- Searching on SoundCloud with scsearch: prefix.
- Option to specify maximum number of YouTube playlist pages to load (was hardcoded to 6).

### Fixed

- Fixed SoundCloud playlist tracks in wrong order.
- Fixed paid movies appearing in YouTube search results.
- Fixed YouTube playlists with UU prefix not working.

## [1.1.42] - 2017-01-16

### Added

- Support for OS X (native library).

## [1.1.41] - 2017-01-15

### Fixed

- Fixed YouTube tracks broken when player.js URL was given without hostname.

## [1.1.40] - 2017-01-14

### Fixed

- Fixed constant delay on processing node messages.
- Fixed making a new HTTP connection for each node request.

## [1.1.39] - 2017-01-14

### Changed

- Reduced playing track count effect on node balancing.

## [1.1.38] - 2017-01-09

### Changed

- Node balancing takes CPU and latency more seriously.
- Node messaging changed, requires node update to 1.1.38.

## [1.1.37] - 2017-01-04

### Changed

- MKV file handling refactored to be more lightweight.

## [1.1.36] - 2017-01-01

### Fixed

- Fixed track marker and position reset when starting the track.

## [1.1.35] - 2016-12-31

### Fixed

- Fixed loading HTTP urls with local redirects.

## [1.1.34] - 2016-12-29

### Fixed

- YouTube search results no longer include ads.

## [1.1.33] - 2016-12-27

### Fixed

- Fixed a regression with loading native libraries on Windows.

## [1.1.32] - 2016-12-24

### Fixed

- Fixed an issue when JDA-NAS and LavaPlayer were used together with different classloaders.

## [1.1.31] - 2016-12-22

### Added

- Made node balancing weight data available.

### Changed

- Request time to nodes also used for balancing between nodes.

## [1.1.30] - 2016-12-19

### Fixed

- Fixed an issue with MP3 files sometimes being detected as ADTS streams.

## [1.1.29] - 2016-12-17

### Added

- Made various statistics about remote nodes available.

## [1.1.28] - 2016-12-13

### Changed

- Made requests to nodes tolerate higher latency without stuttering.

## [1.1.27] - 2016-12-12

### Changed

- Changing remote node list does not stop tracks on unaffected nodes.

## [1.1.26] - 2016-12-11

### Changed

- Dependency restructuring for compatibility with JDA-NAS.

## [1.1.25] - 2016-12-08

### Added

- Support for legacy MP3 ID3v2.2 tags.

### Fixed

- Fixed nonexistent YouTube tracks being tried as raw HTTP urls.

## [1.1.24] - 2016-12-08

### Added

- Title and artist information for MP4/M4A files.

## [1.1.23] - 2016-12-01

### Added

- Can check if playlist is a search result.

### Fixed

- Fixed SoundCloud not working because because of SoundCloud client ID expiring.

## [1.1.22] - 2016-11-19

### Added

- Option to disable YouTube searches.

## [1.1.21] - 2016-11-18

### Added

- Support YouTube searches by using "ytsearch: query" as identifier.

## [1.1.20] - 2016-11-18

### Added

- Made it simple to register all bundled sources through the methods of AudioSourceManagers.

## [1.1.19] - 2016-11-18

### Changed

- Allow redirects from raw HTTP urls to other source providers.

### Fixed

- Fixed an exception on the end of some MP4 tracks.

## [1.1.18] - 2016-11-18

### Added

- Added mayEndTrack field to end reasons.

### Changed

- Special end reason for failed track initialization.

## [1.1.17] - 2016-11-16

### Fixed

- Fixed an issue with resolving correct YouTube stream URL due to a bug in cipher detection.

## [1.1.16] - 2016-11-13

### Changed

- Lowered default resampling quality.

## [1.1.15] - 2016-11-12

### Fixed

- Fixed some tracks mistakenly detected as MP3 streams.

## [1.1.14] - 2016-11-11

### Added

- Allow icy:// urls, which are used in some radio stream playlists.

## [1.1.13] - 2016-11-11

### Fixed

- Fixed exception on OGG streams when they end.
- SoundCloud fix + provider more resilient to site updates.

## [1.1.12] - 2016-11-11

### Added

- Track markers.

### Removed

- Loop feature. It can be done with markers.

## [1.1.11] - 2016-11-06

### Added

- SoundCloud liked tracks page loadable as a playlist.

## [1.1.10] - 2016-11-05

### Changed

- Common exception in case a playlist is private.

### Fixed

- Fixed YouTube track not loaded if it is part of a private playlist.

## [1.1.9] - 2016-11-05

### Added

- Twitch stream support.

## [1.1.8] - 2016-11-05

### Added

- Vimeo support.
- Support for the more common non-fragmented format of MP4/M4A.

## [1.1.7] - 2016-11-03

### Added

- IDv2.3 tag support.

### Fixed

- Fixed exception on long ID3 tags on streams.

## [1.1.6] - 2016-11-03

### Added

- Plain text files with an URL loaded as radio stream playlist.

### Fixed

- Fixed SoundCloud not working due to site update.

## [1.1.5] - 2016-11-03

### Added

- Support for PLS playlists for radio streams.
- Support for ICY protocol to fix some SHOUTcast streams.

## [1.1.4] - 2016-11-01

### Added

- Support for ADTS+AAC radio streams.

## [1.1.3] - 2016-10-31

### Added

- Bundled some LetsEncrypt SSL root certificate missing on some JDK installations.
- Support for adding custom SSL certificates.

### Fixed

- Fixed SoundCloud URLs not working with a slash in the end.

## [1.1.2] - 2016-10-30

### Added

- BandCamp support.

## [1.1.1] - 2016-10-28

### Added

- M3U support for radio streams.

## [1.1.0] - 2016-10-27

### Added

- MPEG2+layerIII MP3 format support.

### Fixed

- Fixed an exception on OGG stream playback.

## [1.0.14] - 2016-10-27

### Added

- OGG stream support.

### Fixed

- Fixed exception on loading age-restricted YouTube videos.
- Fixed an exception on decoding some FLAC files.

## [1.0.13] - 2016-10-23

### Fixed

- Fixed huge CPU load on tracks from local files.

## [1.0.12] - 2016-10-23

### Added

- FLAC support.

## [1.0.11] - 2016-10-22

### Added

- MP3 stream support.

### Fixed

- Fixed NPE on missing Content-Length in HTTP responses.

## [1.0.10] - 2016-10-22

### Added

- HTTP source for loading any URL as a track.
- Loading artist and title from MP3 tags.
- Custom executor support.

### Fixed

- Fixed track end not triggering on exception.

## [1.0.9] - 2016-10-22

### Added

- Accurate seeking and duration for VBR MP3 files.

## [1.0.8] - 2016-10-22

### Added

- Track serialization.

### Fixed

- Fixed only one thread used for loading track info.

## [1.0.7] - 2016-10-21

### Added

- Shutting down player managers and source managers.
- Cleaning up abandoned players.
- File type detection based on content.
- Track end reasons.

### Fixed

- Fixed TrackStartEvent event not called.

## [1.0.6] - 2016-10-17

### Added

- Audio output hook for experimental native UDP packet scheduling.

## [1.0.5] - 2016-10-17

### Fixed

- Fixed huge CPU load from GC monitor.
- Fixed track duration reported in microsecons.

## [1.0.4] - 2016-10-16

### Added

- GC pause monitor for logging the number of GC pauses in different duration ranges.

## [1.0.3] - 2016-10-16

### Changed

- Made null a valid argument for playTrack with identical behavior to stopTrack.

## [1.0.2] - 2016-10-15

### Added

- SoundCloud playlist support.

## [1.0.1] - 2016-10-15

### Fixed

- Fixed YouTube tracks which use dash XML and are protected with cipher.

## [1.0.0] - 2016-10-14

### Added

- Offloading audio processing to remote nodes with load balancing.

### Changed

- Refactoring of track execution.

## [0.1.9] - 2016-10-08

### Added

- Method for loading items in order.
- Opus encoding quality setting.

### Fixed

- Fixed SoundCloud tracks with underscores not working.

## [0.1.8] - 2016-10-07

### Added

- Resampling quality setting.

## [0.1.7] - 2016-10-07

### Changed

- Track length info provided in milliseconds instead of seconds.

### Fixed

- Fixed getPosition always returning 0 when setPosition was called before start.

## [0.1.6] - 2016-10-07

### Added

- Tracks can be cloned (for replay).

## [0.1.5] - 2016-10-06

### Added

- SoundCloud support (single tracks)
- Support for special playlist types on YouTube (liked videos, favorites, mixes).
- MP3 support.

## [0.1.4] - 2016-10-05

### Changed

- YouTube track is loaded as a playlist if playlist is referenced in the URL.

## [0.1.3] - 2016-10-05

### Fixed

- Fixed broken special characters in track info when system default charset is not UTF8.

## [0.1.2] - 2016-10-04

### Added

- Severity levels for FriendlyException.
- Report YouTube errors with the proper error message and COMMON severity.

### Fixed

- Fixed smooth transition not working from volume 0.

## [0.1.1] - 2016-10-04

### Added

- Volume support with smooth transition.
- Support for nonstandard opus streams, such as mono.

### Fixed

- Fixed onTrackException not being called.

## [0.1.0] - 2016-10-02

### Added

- Initial release.
