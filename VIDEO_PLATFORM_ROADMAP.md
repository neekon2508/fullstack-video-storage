# Video Streaming Platform — Learning & Coding Roadmap

## 0. Mục tiêu dự án

Xây dựng một **self-hosted video streaming platform** có trải nghiệm gần Netflix/YouTube nhưng quy mô nhỏ, tự triển khai trên Ubuntu Server.

Mục tiêu học tập không phải tự viết lại codec, BitTorrent protocol hay HTTP server, mà là tự thiết kế và triển khai **application architecture + business/domain logic + media pipeline** từ đầu, tận dụng các công cụ chuyên dụng như FFmpeg, qBittorrent và HLS.js.

### Mục tiêu chức năng cuối cùng

- Quản lý thư viện phim/TV/anime.
- Phân tích media bằng FFprobe.
- Lưu metadata video/audio/subtitle vào PostgreSQL.
- Tìm kiếm và browse thư viện.
- Playback trực tiếp bằng HTTP Range.
- Adaptive streaming bằng HLS.
- HLS player phía React.
- Chọn chất lượng, audio track, subtitle.
- Playback speed, fullscreen, seek/pause/resume.
- Watch history + Continue Watching.
- Playback session + progress tracking.
- Transcoding on-demand bằng FFmpeg.
- Background jobs để scan media và xử lý tác vụ.
- Tự động nhận media từ qBittorrent API ở phase automation.
- Realtime download/transcoding progress bằng WebSocket.
- Cache bằng Redis khi có nhu cầu thực tế.
- Load/stress testing và production hardening.

> Nguyên tắc: **Direct Play → HTTP Range → HLS → On-demand Transcoding**. Không mặc định transcode mọi file.

---

# 1. Kiến trúc tổng thể

## 1.1. Kiến trúc ứng dụng

```text
                         React Web
                TypeScript + Tailwind
                           │
                           │ HTTPS / REST / WebSocket
                           ▼
                         Nginx
                           │
                           ▼
                Spring Boot Modular Monolith
                           │
       ┌───────────────────┼────────────────────┐
       │                   │                    │
       ▼                   ▼                    ▼
   PostgreSQL            Redis              FFmpeg
    Source of Truth      Cache           Transcoding
       │
       │
       └──────────────┐
                      ▼
                 Media Storage
                  /srv/media

External / Integration:
    qBittorrent API
```

## 1.2. Nguyên tắc kiến trúc

### Modular Monolith trước

Không tách Auth Service, Media Service, Streaming Service thành microservice ngay từ đầu.

Spring Boot ban đầu là một application nhưng module phải có boundary rõ:

```text
auth/
user/
media/
library/
metadata/
playback/
streaming/
transcoding/
watch-history/
search/
torrent/
common/
```

Khi hệ thống thực sự có nhu cầu scale/deploy độc lập mới cân nhắc tách service.

### Jellyfin

Jellyfin chỉ dùng làm **phương án chữa cháy để xem phim**, không phải dependency của hệ thống tự build.

### Công cụ chuyên dụng được phép dùng

```text
FFmpeg / FFprobe      → media processing / analysis
qBittorrent API       → torrent automation
HLS.js                → frontend HLS playback
PostgreSQL            → persistence
Redis                 → cache/session khi cần
Nginx                 → reverse proxy
Docker Compose        → infrastructure/dev environment
```

---

# 2. Cấu trúc thư mục đích

## 2.1. Repository

```text
video-platform/
├── backend/
│   ├── src/
│   │   ├── main/
│   │   │   ├── java/com/videostreamingapi/
│   │   │   │   ├── auth/
│   │   │   │   ├── user/
│   │   │   │   ├── media/
│   │   │   │   ├── library/
│   │   │   │   ├── metadata/
│   │   │   │   ├── playback/
│   │   │   │   ├── streaming/
│   │   │   │   ├── transcoding/
│   │   │   │   ├── watchhistory/
│   │   │   │   ├── search/
│   │   │   │   ├── torrent/
│   │   │   │   └── common/
│   │   │   └── resources/
│   │   │       ├── application.yml
│   │   │       └── db/migration/
│   │   └── test/
│   └── pom.xml
│
├── frontend/
│   ├── src/
│   │   ├── app/
│   │   ├── components/
│   │   │   ├── video/
│   │   │   ├── media/
│   │   │   ├── playback/
│   │   │   └── common/
│   │   ├── features/
│   │   │   ├── auth/
│   │   │   ├── library/
│   │   │   ├── search/
│   │   │   ├── playback/
│   │   │   ├── watch-history/
│   │   │   └── torrent/
│   │   ├── pages/
│   │   ├── hooks/
│   │   ├── services/
│   │   ├── types/
│   │   └── utils/
│   └── package.json
│
├── infra/
│   ├── docker-compose.yml
│   ├── nginx/
│   └── postgres/
│
├── scripts/
│   ├── media/
│   └── dev/
│
└── docs/
    ├── architecture/
    ├── api/
    └── decisions/
```

## 2.2. Media storage

```text
/srv/media/
├── movies/
│   ├── Movie Title 1 (2024)/
│   │   ├── original.mkv
│   │   ├── subtitles/
│   │   └── hls/                  # generated only when needed
│   │       ├── 720p/
│   │       ├── 1080p/
│   │       └── 2160p/
│   └── Movie Title 2 (2024)/
│
├── tv/
│   └── Show/
│       └── Season 01/
│
├── anime/
│
└── incoming/                      # download/import staging
```

### Storage rule

`original` là source of truth.

HLS/transcoded output là **derived data / cache**, không phải dữ liệu gốc.

---

# 3. Phase 0 — Foundation & Domain Design

## Mục tiêu

Đảm bảo architecture và domain model đúng trước khi đụng đến media pipeline.

## Kiến thức cần học

- Modular Monolith.
- Clean module boundaries.
- Domain/Application/Infrastructure separation.
- Entity vs DTO.
- Transaction boundary.
- PostgreSQL indexing.
- Database migration.
- HTTP REST API design.
- Error handling.
- Validation.
- Configuration management.
- Docker Compose cơ bản.
- HTTP status code / Range request (khái niệm trước).

## Files cần code

### Backend

```text
backend/src/main/java/com/videostreamingapi/
├── common/
│   ├── exception/
│   ├── response/
│   ├── config/
│   └── security/
│
├── auth/
│   ├── controller/
│   ├── service/
│   └── dto/
│
├── user/
│   ├── entity/
│   ├── repository/
│   ├── service/
│   └── controller/
│
└── media/
    ├── entity/
    ├── repository/
    ├── service/
    ├── dto/
    └── controller/
```

### Database

```text
db/migration/
└── V1__create_users_and_media.sql
```

### Infrastructure

```text
infra/
└── docker-compose.yml
```

## Deliverable

```text
Login
   ↓
JWT/session
   ↓
GET /api/media
   ↓
React hiển thị danh sách media
```

Chưa cần HLS, qBittorrent hay transcoding.

---

# 4. Phase 1 — Media Analysis với FFprobe

## Mục tiêu

Từ một video file thật:

```text
/srv/media/movies/.../movie.mkv
```

phân tích bằng FFprobe và lưu metadata vào database.

## Kiến thức cần học

### FFprobe

- `ffprobe`
- `-show_format`
- `-show_streams`
- `-select_streams`
- JSON output.
- Exit code.
- Process execution.
- stdout/stderr.
- Timeout.
- File path validation.

### Media concepts

- Container vs codec.
- Video stream.
- Audio stream.
- Subtitle stream.
- Resolution.
- FPS.
- Bitrate.
- Duration.
- Pixel format.
- Language.
- Default / forced track.
- H.264 / HEVC / AV1.
- AAC / AC3 / DTS.
- SRT / ASS / WebVTT.

## Files cần code

```text
media/
├── entity/
│   ├── MediaItem.java
│   ├── MediaFile.java
│   ├── VideoTrack.java
│   ├── AudioTrack.java
│   └── SubtitleTrack.java
│
├── repository/
│   ├── MediaItemRepository.java
│   ├── MediaFileRepository.java
│   ├── VideoTrackRepository.java
│   ├── AudioTrackRepository.java
│   └── SubtitleTrackRepository.java
│
├── service/
│   ├── MediaAnalysisService.java
│   ├── MediaScannerService.java
│   └── MediaPersistenceService.java
│
├── infrastructure/
│   └── ffprobe/
│       ├── FFprobeClient.java
│       ├── FFprobeProcessRunner.java
│       ├── FFprobeResponse.java
│       └── FFprobeMapper.java
│
└── controller/
    └── MediaController.java
```

## Database

```text
media_items
media_files
video_tracks
audio_tracks
subtitle_tracks
```

Quan hệ:

```text
MediaItem
   └── MediaFile
        ├── VideoTrack
        ├── AudioTrack
        └── SubtitleTrack
```

## Deliverable

```text
MKV
 ↓
FFprobe
 ↓
Java object
 ↓
PostgreSQL
 ↓
GET /api/media/{id}
 ↓
React hiển thị:
- title
- duration
- resolution
- codec
- audio languages
- subtitle languages
```

---

# 5. Phase 2 — Media Scanner & Background Jobs

## Mục tiêu

Không cần thao tác thủ công từng file.

```text
/srv/media
   ↓
Scanner
   ↓
Detect new files
   ↓
FFprobe
   ↓
Persist
```

## Kiến thức cần học

- `@Scheduled`.
- Background processing.
- Idempotency.
- Job state.
- File system scanning.
- Retry cơ bản.
- Error classification.
- Transaction boundary giữa scan và persistence.
- File ổn định trước khi phân tích (tránh đọc file đang được copy).

## Files cần code

```text
library/
├── service/
│   ├── MediaScanService.java
│   ├── MediaImportService.java
│   └── MediaFileDetector.java
├── job/
│   ├── MediaScanJob.java
│   └── JobExecutionService.java
└── controller/
    └── LibraryController.java
```

## Có thể bổ sung

```text
common/job/
├── JobStatus.java
├── JobResult.java
└── JobExecution.java
```

## Deliverable

```text
File xuất hiện trong /srv/media
        ↓
Scheduled scanner
        ↓
Analyze
        ↓
Persist
        ↓
Movie xuất hiện trên UI
```

---

# 6. Phase 3 — Basic Video Streaming bằng HTTP Range

## Mục tiêu

Trước khi học HLS, tự xây playback cơ bản.

```text
Browser
   ↓
HTTP Range
   ↓
Spring Boot
   ↓
Original media file
```

## Kiến thức cần học

- HTTP `Range`.
- HTTP `206 Partial Content`.
- `Content-Range`.
- `Accept-Ranges`.
- `Content-Length`.
- Seek.
- Byte serving.
- Streaming response.
- Random access file.
- Resource abstraction trong Spring.

## Files cần code

```text
streaming/
├── controller/
│   └── StreamingController.java
├── service/
│   ├── VideoStreamingService.java
│   └── RangeRequestService.java
├── model/
│   ├── ByteRange.java
│   └── PartialContent.java
└── exception/
    └── RangeNotSatisfiableException.java
```

### Frontend

```text
components/video/
└── NativeVideoPlayer.tsx
```

## Deliverable

```text
<video controls>
```

Có thể:

- Play
- Pause
- Seek
- Resume sau khi seek

Chưa cần transcoding và HLS.

---

# 7. Phase 4 — Playback Session & Watch Progress

## Mục tiêu

Có trải nghiệm "Continue Watching" như Netflix.

## Kiến thức cần học

- Session lifecycle.
- Heartbeat.
- Idempotent progress update.
- Resume position.
- Device/session modeling.
- Event vs state.
- Concurrency cơ bản.

## Files cần code

```text
playback/
├── entity/
│   └── PlaybackSession.java
├── service/
│   ├── PlaybackService.java
│   └── PlaybackSessionService.java
├── controller/
│   └── PlaybackController.java
└── dto/
    ├── StartPlaybackRequest.java
    ├── PlaybackProgressRequest.java
    └── PlaybackSessionResponse.java

watchhistory/
├── entity/
│   └── WatchProgress.java
├── repository/
│   └── WatchProgressRepository.java
├── service/
│   └── WatchProgressService.java
└── controller/
    └── WatchHistoryController.java
```

## API mục tiêu

```text
POST /api/playback/start
POST /api/playback/{sessionId}/heartbeat
POST /api/playback/{sessionId}/stop
GET  /api/media/{id}/resume
GET  /api/watch-history/continue-watching
```

## Deliverable

```text
Xem phim 01:20:00
↓
thoát
↓
mở lại
↓
Continue Watching
↓
Resume từ 01:20:00
```

---

# 8. Phase 5 — HLS Streaming

## Mục tiêu

Chuyển từ basic range streaming sang adaptive streaming.

## Kiến thức cần học

### HLS

- Master playlist.
- Variant playlist.
- Segment.
- `.m3u8`.
- `.ts` / fMP4 segments.
- Target duration.
- Bandwidth.
- Resolution.
- Adaptive bitrate.

### FFmpeg HLS

- HLS output.
- Variant streams.
- Segment duration.
- Master playlist.
- GOP/keyframe alignment.
- Encoding preset.
- Bitrate ladder.

### Frontend

- HLS.js.
- Levels.
- Audio tracks.
- Subtitle tracks.
- Error/recovery.
- Browser native HLS vs HLS.js.

## Files cần code

```text
hls/
├── service/
│   ├── HlsService.java
│   ├── HlsPlaylistService.java
│   └── HlsSessionService.java
├── controller/
│   └── HlsController.java
├── model/
│   ├── HlsVariant.java
│   └── HlsManifest.java
└── infrastructure/
    └── ffmpeg/
        └── FFmpegHlsAdapter.java
```

### Frontend

```text
components/video/
├── HlsVideoPlayer.tsx
├── QualitySelector.tsx
├── AudioTrackSelector.tsx
└── SubtitleSelector.tsx
```

## Deliverable

```text
master.m3u8
├── 720p
├── 1080p
└── 2160p
```

và React có thể:

- Play HLS.
- Seek.
- Chọn quality.
- Chọn audio.
- Chọn subtitle.

---

# 9. Phase 6 — Audio & Subtitle Management

## Mục tiêu

Hỗ trợ nhiều audio/subtitle track một cách có hệ thống.

## Kiến thức cần học

- Embedded vs external subtitles.
- SRT.
- ASS.
- WebVTT.
- Language tag.
- Default track.
- Forced track.
- Audio language.
- HLS media groups.
- Subtitle rendering limitations trên browser/device.

## Files cần code

```text
media/
└── track/
    ├── AudioTrackService.java
    └── SubtitleTrackService.java

playback/
├── AudioTrackSelectionService.java
└── SubtitleSelectionService.java
```

### Frontend

```text
components/video/
├── AudioTrackSelector.tsx
├── SubtitleSelector.tsx
└── PlaybackSettings.tsx
```

## Deliverable

Trong player:

```text
Audio:
- Japanese
- English
- Thai

Subtitle:
- English
- Japanese
- Thai
- Off
```

---

# 10. Phase 7 — Transcoding On-Demand

## Mục tiêu

Chỉ transcoding khi thiết bị/client không thể Direct Play.

## Kiến thức cần học

- Transcoding vs remux vs direct stream.
- FFmpeg encoding.
- H.264 / HEVC.
- Resolution.
- Bitrate.
- Frame rate.
- Preset.
- Keyframes.
- GOP.
- Audio transcoding.
- Hardware acceleration.
- FFmpeg process lifecycle.
- Job cancellation.
- Transcoding cache.

## Playback decision

```text
Client capability
        +
Media capability
        ↓
Playback Decision Engine
        ↓
┌────────────┬──────────────┬────────────┐
│ DirectPlay │ Remux/Stream │ Transcode  │
└────────────┴──────────────┴────────────┘
```

## Files cần code

```text
playback/
├── decision/
│   ├── PlaybackDecisionEngine.java
│   ├── PlaybackMode.java
│   ├── DeviceCapabilities.java
│   └── MediaCapabilities.java
│
transcoding/
├── entity/
│   └── TranscodeJob.java
├── service/
│   ├── TranscodingService.java
│   ├── TranscodeJobService.java
│   └── TranscodeCacheService.java
├── infrastructure/
│   └── ffmpeg/
│       ├── FFmpegClient.java
│       ├── FFmpegProcessRunner.java
│       └── FFmpegCommandBuilder.java
└── controller/
    └── TranscodingController.java
```

## Đặc biệt với server hiện tại

Server:

```text
i5-3230M
Intel HD Graphics 4000
```

nên **không thiết kế dựa trên giả định server có thể transcode 4K HEVC mạnh mẽ**.

Ưu tiên:

```text
Direct Play
    ↓
Remux / Direct Stream
    ↓
On-demand Transcode
```

Không pre-generate toàn bộ:

```text
4K → 2160p
4K → 1080p
4K → 720p
```

ngay khi import.

---

# 11. Phase 8 — qBittorrent Automation

## Mục tiêu

Tự động đưa media từ downloader vào pipeline.

```text
User
 ↓
magnet
 ↓
Backend
 ↓
qBittorrent API
 ↓
Download
 ↓
completed
 ↓
Media Scanner
 ↓
FFprobe
 ↓
Database
```

## Kiến thức cần học

- qBittorrent Web API.
- Authentication/session.
- Add torrent.
- Torrent status.
- Progress polling.
- Category/tag.
- Save path.
- Completion detection.
- Import workflow.
- Idempotency.
- Polling/backoff.

## Files cần code

```text
torrent/
├── controller/
│   └── TorrentController.java
├── service/
│   ├── TorrentService.java
│   ├── TorrentMonitoringService.java
│   └── TorrentImportService.java
├── infrastructure/
│   └── qbittorrent/
│       ├── QBittorrentClient.java
│       ├── QBittorrentAuthClient.java
│       └── QBittorrentMapper.java
├── model/
│   ├── TorrentInfo.java
│   └── TorrentStatus.java
└── job/
    └── TorrentStatusPollJob.java
```

## Lưu ý kiến trúc

Torrent chỉ là **một nguồn ingestion**.

Không để domain media phụ thuộc trực tiếp vào qBittorrent.

Tư duy đúng:

```text
qBittorrent
     ↓
Downloaded File
     ↓
Media Ingestion
     ↓
Media Domain
```

Sau này có thể thêm:

```text
Local Upload
NFS
S3
Manual Import
```

mà không sửa core media domain.

> Chỉ dùng torrent cho nội dung bạn có quyền tải/chia sẻ.

---

# 12. Phase 9 — WebSocket & Realtime

## Mục tiêu

Hiển thị trạng thái real-time.

## Use cases

```text
Torrent downloading: 45%
Transcoding: 67%
Library scanning...
Import completed
Job failed
```

## Kiến thức cần học

- WebSocket.
- Connection lifecycle.
- Topic/channel.
- Push vs polling.
- Reconnect.
- Backpressure cơ bản.
- Authentication cho WebSocket.

## Files cần code

```text
common/websocket/
├── WebSocketConfig.java
├── WebSocketEventPublisher.java
└── WebSocketTopics.java

torrent/
└── realtime/
    └── TorrentProgressPublisher.java

transcoding/
└── realtime/
    └── TranscodeProgressPublisher.java
```

### Frontend

```text
services/
└── websocket/
    ├── WebSocketClient.ts
    └── WebSocketEventHandler.ts

features/
├── torrent/
│   └── TorrentProgressPanel.tsx
└── transcoding/
    └── TranscodingProgressPanel.tsx
```

---

# 13. Phase 10 — Redis & Caching

## Mục tiêu

Chỉ thêm cache khi có workload thực tế.

## Kiến thức cần học

- Cache-aside.
- TTL.
- Cache invalidation.
- Session storage.
- Rate limiting.
- Hot data.
- Cache key design.

## Candidate cache

```text
Movie detail
Homepage sections
Search results
Session
Playback capability
Job status
```

## Files cần code

```text
common/cache/
├── RedisConfig.java
├── CacheKeyBuilder.java
└── CacheService.java
```

Hoặc module-specific:

```text
media/
└── cache/
    └── MediaCacheService.java

search/
└── cache/
    └── SearchCacheService.java
```

## Nguyên tắc

PostgreSQL:

```text
Source of truth
```

Redis:

```text
Derived / cache
```

Không biến Redis thành database chính.

---

# 14. Phase 11 — Search, Browse & Netflix-like UX

## Mục tiêu

Xây trải nghiệm kiểu Netflix/YouTube.

## Kiến thức cần học

- PostgreSQL full-text search.
- Indexing.
- Pagination.
- Sorting.
- Filtering.
- Projection/DTO.
- Search ranking cơ bản.
- Feed/section design.

## Frontend features

```text
Home
├── Hero
├── Continue Watching
├── Recently Added
├── 4K
├── Genres
└── Recommendations

Search
Browse
Movie Detail
TV Show Detail
Season/Episode
```

## Files cần code

```text
search/
├── controller/
│   └── SearchController.java
├── service/
│   └── SearchService.java
├── repository/
│   └── MediaSearchRepository.java
└── dto/
    └── SearchResultDto.java
```

Frontend:

```text
features/search/
├── SearchPage.tsx
├── SearchInput.tsx
└── SearchResultGrid.tsx

features/library/
├── HomePage.tsx
├── MovieGrid.tsx
├── MovieDetail.tsx
├── TvShowDetail.tsx
└── ContinueWatching.tsx
```

---

# 15. Phase 12 — Load & Stress Testing

## Mục tiêu

Hiểu giới hạn thật của hệ thống.

## Không chỉ test "10 users"

Phải tách workload:

```text
10 users browse
10 users search
10 users Direct Play
10 users HLS
10 users transcoding
```

## Kiến thức cần học

- Throughput.
- Latency.
- p95/p99.
- Concurrency.
- CPU saturation.
- Memory.
- Disk throughput.
- Network throughput.
- Database connection pool.
- HikariCP.
- Bottleneck identification.

## Tools

```text
JMeter
Gatling
k6
```

## Files

```text
performance/
├── k6/
│   ├── browse.js
│   ├── playback.js
│   └── search.js
└── docs/
    └── benchmark-results.md
```

---

# 16. Phase 13 — Observability & Production Hardening

## Mục tiêu

Đưa project từ "chạy được" thành "có thể vận hành".

## Kiến thức cần học

### Logging

- Structured logging.
- Correlation ID.
- Request ID.
- Job ID.
- Error classification.

### Health

```text
/actuator/health
```

### Metrics

- Request count.
- Latency.
- Error rate.
- Active playback sessions.
- Active transcodes.
- Queue/job count.
- Disk usage.

### Security

- CORS.
- CSRF nếu phù hợp architecture.
- JWT/session security.
- Rate limit.
- File path traversal protection.
- Command injection protection khi gọi FFmpeg.
- Access control.
- Secrets management.

## Files

```text
common/
├── logging/
│   ├── CorrelationIdFilter.java
│   └── StructuredLoggingConfig.java
├── security/
│   ├── SecurityConfig.java
│   └── PathSecurityValidator.java
└── observability/
    ├── MetricsConfig.java
    └── HealthIndicatorConfig.java
```

---

# 17. Phase 14 — Docker & Deployment

## Mục tiêu

Deploy reproducibly trên Ubuntu.

## Kiến thức cần học

- Docker image.
- Multi-stage build.
- Docker Compose.
- Volume.
- Network.
- Environment variables.
- Secrets.
- Healthcheck.
- Restart policy.
- Reverse proxy.
- TLS.

## Files

```text
infra/
├── docker-compose.yml
├── docker-compose.prod.yml
├── nginx/
│   ├── nginx.conf
│   └── conf.d/
│       └── video-platform.conf
└── postgres/
    └── init/
```

### Lưu ý

Không cần containerize mọi thứ ngay ngày đầu.

Có thể:

```text
Docker:
PostgreSQL
qBittorrent
Redis

Local:
Spring Boot
React dev server
```

Sau khi app ổn định mới đưa application vào image.

---

# 18. Thứ tự học và code được khuyến nghị

## Must Learn

```text
1. Media / codec / container
2. FFprobe
3. HTTP Range
4. HLS
5. HLS.js
6. FFmpeg transcoding
7. Background jobs
8. Playback session / progress
```

## Second wave

```text
9. Subtitle / audio tracks
10. qBittorrent API
11. WebSocket
12. PostgreSQL search / indexing
```

## Third wave

```text
13. Redis / caching
14. Load testing
15. Observability
16. Docker production deployment
```

---

# 19. Thứ tự code thực tế

```text
Phase 0
Foundation
    ↓
Phase 1
MediaAnalysisService
    ↓
Phase 2
MediaScanner + Background Jobs
    ↓
Phase 3
HTTP Range Streaming
    ↓
Phase 4
Playback Session + Watch Progress
    ↓
Phase 5
HLS + HLS.js
    ↓
Phase 6
Audio + Subtitle
    ↓
Phase 7
On-demand Transcoding
    ↓
Phase 8
qBittorrent Automation
    ↓
Phase 9
WebSocket
    ↓
Phase 10
Redis
    ↓
Phase 11
Search + Netflix UX
    ↓
Phase 12
Load Testing
    ↓
Phase 13
Observability + Security
    ↓
Phase 14
Docker + Production
```

---

# 20. Definition of Done cho từng milestone

## Milestone A

```text
Local file
→ FFprobe
→ PostgreSQL
→ REST
→ React metadata page
```

## Milestone B

```text
Movie
→ HTTP Range
→ <video>
→ play / seek
```

## Milestone C

```text
Movie
→ HLS
→ HLS.js
→ adaptive quality
```

## Milestone D

```text
Playback
→ session
→ heartbeat
→ progress
→ Continue Watching
```

## Milestone E

```text
Unsupported client
→ playback decision
→ FFmpeg
→ HLS
```

## Milestone F

```text
qBittorrent
→ completed
→ media ingestion
→ FFprobe
→ library
```

## Milestone G

```text
Realtime
→ WebSocket
→ torrent progress
→ transcode progress
```

## Milestone H

```text
Production
→ Redis
→ metrics
→ load test
→ security
→ Docker
```

---

# 21. Những thứ KHÔNG nên tự xây

Không cần tự implement:

```text
❌ Video codec
❌ H.264 encoder
❌ HEVC encoder
❌ HLS protocol parser
❌ BitTorrent protocol
❌ HTTP server
❌ PostgreSQL driver
❌ JSON parser
❌ JWT cryptography
```

Nên tự thiết kế/implement:

```text
✅ Media domain
✅ Media ingestion
✅ FFprobe integration
✅ Playback decision engine
✅ Playback session
✅ Watch progress
✅ Streaming orchestration
✅ Transcoding job lifecycle
✅ Device capability model
✅ Caching strategy
✅ Background workflow
✅ Business APIs
```

---

# 22. Quy tắc thiết kế xuyên suốt project

### Rule 1

**Original media là source of truth.**

### Rule 2

**HLS/transcode output là derived data/cache.**

### Rule 3

**Direct Play được ưu tiên trước transcoding.**

### Rule 4

**Torrent là một ingestion adapter, không phải media domain.**

### Rule 5

**PostgreSQL là source of truth; Redis chỉ là cache.**

### Rule 6

**Modular monolith trước; microservice sau khi có lý do.**

### Rule 7

**Infrastructure chỉ được thêm khi có vấn đề cần giải quyết.**

### Rule 8

Mọi external process như FFmpeg/FFprobe phải:

- validate path
- validate arguments
- timeout
- capture stderr
- check exit code
- cleanup process/resources
- không nối trực tiếp user input vào shell command

---

# 23. Trạng thái dự án hiện tại

Bạn đã có:

```text
✅ Spring Boot
✅ React
✅ PostgreSQL
✅ REST API
✅ Authentication
✅ Ubuntu Server
✅ NAS storage
✅ Docker cơ bản
```

Cần bổ sung:

```text
🆕 Media/Codec concepts
🆕 FFprobe
🆕 HTTP Range Streaming
🆕 HLS
🆕 HLS.js
🆕 FFmpeg transcoding
🆕 Playback session
🆕 Background jobs
🆕 Audio/subtitle management
🆕 qBittorrent API
🆕 WebSocket
🆕 Redis
🆕 Load testing
🆕 Observability
```

---

# 24. Điểm bắt đầu được khuyến nghị

**Không cần chờ học hết roadmap. Có thể bắt đầu code ngay.**

### Task đầu tiên

```text
Create MediaAnalysisService
```

Input:

```text
/srv/media/movies/<movie>/<movie>.mkv
```

Pipeline:

```text
File
 ↓
FFprobe
 ↓
JSON
 ↓
Java DTO
 ↓
Domain model
 ↓
PostgreSQL
```

### Files đầu tiên nên tạo

```text
backend/
└── src/main/java/com/videostreamingapi/
    └── media/
        ├── entity/
        │   ├── MediaItem.java
        │   ├── MediaFile.java
        │   ├── VideoTrack.java
        │   ├── AudioTrack.java
        │   └── SubtitleTrack.java
        │
        ├── repository/
        │   ├── MediaItemRepository.java
        │   ├── MediaFileRepository.java
        │   ├── VideoTrackRepository.java
        │   ├── AudioTrackRepository.java
        │   └── SubtitleTrackRepository.java
        │
        ├── service/
        │   └── MediaAnalysisService.java
        │
        └── infrastructure/
            └── ffprobe/
                ├── FFprobeClient.java
                ├── FFprobeProcessRunner.java
                ├── FFprobeResponse.java
                └── FFprobeMapper.java
```

Sau khi vertical slice đầu tiên chạy hoàn chỉnh, chuyển sang **HTTP Range Streaming**.

---

# 25. Tiêu chuẩn "Middle" của project

Project không được đánh giá chỉ bằng số lượng feature.

Mục tiêu cuối cùng là bạn có thể giải thích rõ:

```text
Why PostgreSQL?
Why this schema?
Why modular monolith?
Why Range Streaming?
Why HLS?
Why Direct Play first?
When to transcode?
How is a playback session modeled?
How do you prevent duplicate jobs?
How do you recover from FFmpeg failure?
How do you handle a partially downloaded file?
What is cached?
What is the source of truth?
What happens when disk is full?
What happens when client disconnects?
What happens when transcoding is cancelled?
How would you scale transcoding independently?
```

Nếu bạn trả lời được những câu hỏi này bằng chính code của project, đó mới là kết quả đáng giá của roadmap.
