-- =====================================================================
-- Video Streaming Platform — MySQL 8.0+ DDL
-- Engine   : InnoDB
-- Charset  : utf8mb4 / utf8mb4_0900_ai_ci
-- Scope    : Roadmap Phase 0 → 11 + RBAC + i18n
-- =====================================================================

-- =====================================================================
-- 1. I18N CORE
-- =====================================================================

-- Bảng i18n duy nhất: gộp key dịch tĩnh (vd 'billing.plan.title') và nội dung
-- dịch động của entity theo quy ước key = '{context}.{entity_id}.{field}',
-- ví dụ 'media_item.123.title', 'media_item.123.overview', 'season.88.name'.
-- Không có FK tới entity gốc (entity_id nằm trong chuỗi key) và không có FK
-- tới bảng ngôn ngữ (language là mã tự do, vd 'vi', 'en', 'ja') - toàn bộ
-- việc map/toàn vẹn dữ liệu xử lý ở tầng application (JPA).
CREATE TABLE i18n (
    `key`           VARCHAR(255)    NOT NULL COMMENT 'vd: billing.plan.title, media_item.123.title',
    language        VARCHAR(16)     NOT NULL COMMENT 'Mã ngôn ngữ tự do: vi, en, ja...',
    value           TEXT            NOT NULL,
    created_at      DATETIME(3)     NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    updated_at      DATETIME(3)     NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),

    PRIMARY KEY (`key`, language),
    KEY idx_i18n_language (language)
) ENGINE = InnoDB;


-- =====================================================================
-- 2. RBAC
-- =====================================================================

CREATE TABLE users (
    id                          BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    username                    VARCHAR(64)     NOT NULL,
    email                       VARCHAR(255)    NOT NULL,
    password_hash               VARCHAR(255)    NOT NULL,
    display_name                VARCHAR(128)    NULL,

    preferred_language          VARCHAR(16)     NULL COMMENT 'Ngôn ngữ giao diện, khớp với i18n.language, vd vi/en/ja',
    preferred_audio_language    CHAR(3)         NULL COMMENT 'ISO 639-2, dùng cho auto-select audio track',
    preferred_subtitle_language CHAR(3)         NULL COMMENT 'ISO 639-2, dùng cho auto-select subtitle track',

    is_active                   TINYINT(1)      NOT NULL DEFAULT 1,
    is_locked                   TINYINT(1)      NOT NULL DEFAULT 0,
    failed_login_attempts       INT UNSIGNED    NOT NULL DEFAULT 0,
    locked_until                DATETIME(3)     NULL,
    last_login_at               DATETIME(3)     NULL,
    password_changed_at         DATETIME(3)     NULL,

    created_at                  DATETIME(3)     NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    updated_at                  DATETIME(3)     NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),

    PRIMARY KEY (id),
    UNIQUE KEY uq_users_username (username),
    UNIQUE KEY uq_users_email (email),
    KEY idx_users_active (is_active),
    KEY idx_users_preferred_language (preferred_language)
) ENGINE = InnoDB;


CREATE TABLE roles (
    id              BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    code            VARCHAR(64)     NOT NULL COMMENT 'ADMIN, LIBRARY_MANAGER, VIEWER',
    name            VARCHAR(128)    NOT NULL,
    description     VARCHAR(512)    NULL,
    parent_role_id  BIGINT UNSIGNED NULL COMMENT 'Role hierarchy: role con kế thừa permission của role cha',
    is_system       TINYINT(1)      NOT NULL DEFAULT 0 COMMENT 'Role hệ thống, không cho phép xóa',
    created_at      DATETIME(3)     NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    updated_at      DATETIME(3)     NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),

    PRIMARY KEY (id),
    UNIQUE KEY uq_roles_code (code),
    KEY idx_roles_parent (parent_role_id)
) ENGINE = InnoDB;


CREATE TABLE permissions (
    id              BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    code            VARCHAR(128)    NOT NULL COMMENT 'media:read, transcode:cancel, library:scan',
    resource        VARCHAR(64)     NOT NULL,
    action          VARCHAR(64)     NOT NULL,
    description     VARCHAR(512)    NULL,
    is_system       TINYINT(1)      NOT NULL DEFAULT 0,
    created_at      DATETIME(3)     NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    updated_at      DATETIME(3)     NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),

    PRIMARY KEY (id),
    UNIQUE KEY uq_permissions_code (code),
    UNIQUE KEY uq_permissions_resource_action (resource, action)
) ENGINE = InnoDB;


CREATE TABLE role_permissions (
    role_id         BIGINT UNSIGNED NOT NULL,
    permission_id   BIGINT UNSIGNED NOT NULL,
    granted_at      DATETIME(3)     NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    granted_by      BIGINT UNSIGNED NULL,

    PRIMARY KEY (role_id, permission_id),
    KEY idx_role_permissions_permission (permission_id)
) ENGINE = InnoDB;


CREATE TABLE user_roles (
    user_id         BIGINT UNSIGNED NOT NULL,
    role_id         BIGINT UNSIGNED NOT NULL,
    granted_at      DATETIME(3)     NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    granted_by      BIGINT UNSIGNED NULL,
    expires_at      DATETIME(3)     NULL COMMENT 'NULL = vĩnh viễn',

    PRIMARY KEY (user_id, role_id),
    KEY idx_user_roles_role (role_id),
    KEY idx_user_roles_expires (expires_at)
) ENGINE = InnoDB;


CREATE TABLE user_permissions (
    user_id         BIGINT UNSIGNED             NOT NULL,
    permission_id   BIGINT UNSIGNED             NOT NULL,
    effect          ENUM('ALLOW', 'DENY')       NOT NULL COMMENT 'Override trực tiếp trên user; DENY thắng ALLOW',
    granted_at      DATETIME(3)                 NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    granted_by      BIGINT UNSIGNED             NULL,
    expires_at      DATETIME(3)                 NULL,

    PRIMARY KEY (user_id, permission_id),
    KEY idx_user_permissions_permission (permission_id)
) ENGINE = InnoDB;


-- i18n cho nhãn hiển thị của role/permission: dùng bảng i18n với
-- key = 'role.{id}.name', 'permission.{id}.description'...


CREATE TABLE refresh_tokens (
    id                  BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    user_id             BIGINT UNSIGNED NOT NULL,
    token_hash          CHAR(64)        NOT NULL COMMENT 'SHA-256 của token, không lưu token gốc',
    issued_at           DATETIME(3)     NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    expires_at          DATETIME(3)     NOT NULL,
    revoked_at          DATETIME(3)     NULL,
    replaced_by_id      BIGINT UNSIGNED NULL COMMENT 'Refresh token rotation',
    client_ip           VARBINARY(16)   NULL,
    user_agent          VARCHAR(512)    NULL,

    PRIMARY KEY (id),
    UNIQUE KEY uq_refresh_tokens_hash (token_hash),
    KEY idx_refresh_tokens_user (user_id, expires_at)
) ENGINE = InnoDB;


-- =====================================================================
-- 3. LIBRARY
-- =====================================================================

CREATE TABLE libraries (
    id                      BIGINT UNSIGNED                             NOT NULL AUTO_INCREMENT,
    code                    VARCHAR(64)                                 NOT NULL,
    library_type            ENUM('MOVIE', 'TV', 'ANIME')                NOT NULL,
    root_path               VARCHAR(512)                                NOT NULL COMMENT '/srv/media/movies',
    is_enabled              TINYINT(1)                                  NOT NULL DEFAULT 1,
    scan_interval_seconds   INT UNSIGNED                                NULL,
    last_scan_at            DATETIME(3)                                 NULL,
    last_scan_status        ENUM('SUCCEEDED', 'FAILED', 'SKIPPED')      NULL,
    created_at              DATETIME(3)                                 NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    updated_at              DATETIME(3)                                 NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),

    PRIMARY KEY (id),
    UNIQUE KEY uq_libraries_code (code),
    UNIQUE KEY uq_libraries_root_path (root_path),
    KEY idx_libraries_enabled (is_enabled)
) ENGINE = InnoDB;


-- i18n cho library: dùng bảng i18n với key = 'library.{id}.name'


-- =====================================================================
-- 4. MEDIA DOMAIN
-- =====================================================================

CREATE TABLE media_items (
    id                  BIGINT UNSIGNED                         NOT NULL AUTO_INCREMENT,
    library_id          BIGINT UNSIGNED                         NOT NULL,
    item_type           ENUM('MOVIE', 'SERIES')                 NOT NULL,

    original_title      VARCHAR(512)                            NOT NULL,
    sort_title          VARCHAR(512)                            NULL,
    original_language   CHAR(3)                                 NULL COMMENT 'ISO 639-2',
    release_year        SMALLINT UNSIGNED                       NULL,
    release_date        DATE                                    NULL,
    runtime_seconds     INT UNSIGNED                            NULL,
    content_rating      VARCHAR(16)                             NULL,

    folder_path         VARCHAR(512)                            NOT NULL,
    poster_path         VARCHAR(512)                            NULL,
    backdrop_path       VARCHAR(512)                            NULL,

    metadata_source     VARCHAR(32)                             NULL COMMENT 'tmdb, tvdb, manual',
    metadata_source_id  VARCHAR(64)                             NULL,

    status              ENUM('ACTIVE', 'MISSING', 'HIDDEN')     NOT NULL DEFAULT 'ACTIVE',
    added_at            DATETIME(3)                             NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    updated_at          DATETIME(3)                             NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),

    PRIMARY KEY (id),
    UNIQUE KEY uq_media_items_folder (library_id, folder_path),
    KEY idx_media_items_library_type (library_id, item_type, status),
    KEY idx_media_items_added_at (added_at),
    KEY idx_media_items_release_year (release_year),
    KEY idx_media_items_metadata_source (metadata_source, metadata_source_id),
    FULLTEXT KEY ft_media_items_title (original_title, sort_title)
) ENGINE = InnoDB;


-- i18n cho media item: dùng bảng i18n với
-- key = 'media_item.{id}.title' / '.sort_title' / '.tagline' / '.overview'


CREATE TABLE seasons (
    id              BIGINT UNSIGNED     NOT NULL AUTO_INCREMENT,
    media_item_id   BIGINT UNSIGNED     NOT NULL,
    season_number   SMALLINT UNSIGNED   NOT NULL,
    folder_path     VARCHAR(512)        NULL,
    poster_path     VARCHAR(512)        NULL,
    air_date        DATE                NULL,
    created_at      DATETIME(3)         NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    updated_at      DATETIME(3)         NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),

    PRIMARY KEY (id),
    UNIQUE KEY uq_seasons_number (media_item_id, season_number)
) ENGINE = InnoDB;


-- i18n cho season: dùng bảng i18n với
-- key = 'season.{id}.name' / '.overview'


CREATE TABLE episodes (
    id              BIGINT UNSIGNED     NOT NULL AUTO_INCREMENT,
    season_id       BIGINT UNSIGNED     NOT NULL,
    media_item_id   BIGINT UNSIGNED     NOT NULL COMMENT 'Denormalized để truy vấn Continue Watching theo series',
    episode_number  SMALLINT UNSIGNED   NOT NULL,
    air_date        DATE                NULL,
    runtime_seconds INT UNSIGNED        NULL,
    still_path      VARCHAR(512)        NULL,
    created_at      DATETIME(3)         NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    updated_at      DATETIME(3)         NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),

    PRIMARY KEY (id),
    UNIQUE KEY uq_episodes_number (season_id, episode_number),
    KEY idx_episodes_media_item (media_item_id)
) ENGINE = InnoDB;


-- i18n cho episode: dùng bảng i18n với
-- key = 'episode.{id}.title' / '.overview'


CREATE TABLE genres (
    id          BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    code        VARCHAR(64)     NOT NULL COMMENT 'action, drama, sci-fi',
    created_at  DATETIME(3)     NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    updated_at  DATETIME(3)     NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),

    PRIMARY KEY (id),
    UNIQUE KEY uq_genres_code (code)
) ENGINE = InnoDB;


-- i18n cho genre: dùng bảng i18n với key = 'genre.{id}.name'


CREATE TABLE media_item_genres (
    media_item_id   BIGINT UNSIGNED NOT NULL,
    genre_id        BIGINT UNSIGNED NOT NULL,

    PRIMARY KEY (media_item_id, genre_id),
    KEY idx_media_item_genres_genre (genre_id)
) ENGINE = InnoDB;


-- =====================================================================
-- 5. MEDIA FILES & TRACKS (FFprobe)
-- =====================================================================

CREATE TABLE media_files (
    id                  BIGINT UNSIGNED                                             NOT NULL AUTO_INCREMENT,
    media_item_id       BIGINT UNSIGNED                                             NOT NULL,
    episode_id          BIGINT UNSIGNED                                             NULL COMMENT 'NULL với movie',

    file_path           VARCHAR(512)                                                NOT NULL,
    file_name           VARCHAR(255)                                                NOT NULL,
    file_size_bytes     BIGINT UNSIGNED                                             NULL,
    file_modified_at    DATETIME(3)                                                 NULL,
    content_hash        CHAR(64)                                                    NULL COMMENT 'Dùng để phát hiện file đổi/di chuyển khi rescan',

    container_format    VARCHAR(64)                                                 NULL COMMENT 'matroska, mov,mp4,m4a',
    duration_seconds    DECIMAL(12,3)                                               NULL,
    overall_bitrate     INT UNSIGNED                                                NULL,

    analysis_status     ENUM('PENDING', 'ANALYZING', 'ANALYZED', 'FAILED')          NOT NULL DEFAULT 'PENDING',
    analysis_error      TEXT                                                        NULL,
    analyzed_at         DATETIME(3)                                                 NULL,
    is_available        TINYINT(1)                                                  NOT NULL DEFAULT 1 COMMENT '0 khi file không còn tồn tại trên đĩa',

    created_at          DATETIME(3)                                                 NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    updated_at          DATETIME(3)                                                 NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),

    PRIMARY KEY (id),
    UNIQUE KEY uq_media_files_path (file_path),
    KEY idx_media_files_item (media_item_id, is_available),
    KEY idx_media_files_episode (episode_id),
    KEY idx_media_files_analysis_status (analysis_status)
) ENGINE = InnoDB;


CREATE TABLE video_tracks (
    id                      BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    media_file_id           BIGINT UNSIGNED NOT NULL,
    stream_index            INT             NOT NULL,

    codec_name              VARCHAR(32)     NOT NULL COMMENT 'h264, hevc, av1',
    codec_profile           VARCHAR(32)     NULL,
    codec_level             INT             NULL,
    width                   INT UNSIGNED    NULL,
    height                  INT UNSIGNED    NULL,
    display_aspect_ratio    VARCHAR(16)     NULL,
    frame_rate              DECIMAL(9,4)    NULL,
    bit_rate                INT UNSIGNED    NULL,
    pixel_format            VARCHAR(32)     NULL,
    bit_depth               TINYINT UNSIGNED NULL,
    color_space             VARCHAR(32)     NULL,
    color_transfer          VARCHAR(32)     NULL,
    color_primaries         VARCHAR(32)     NULL,
    is_hdr                  TINYINT(1)      NOT NULL DEFAULT 0,
    hdr_format              VARCHAR(32)     NULL COMMENT 'HDR10, HLG, DolbyVision',
    language                CHAR(3)         NULL,
    title                   VARCHAR(255)    NULL,
    is_default              TINYINT(1)      NOT NULL DEFAULT 0,

    PRIMARY KEY (id),
    UNIQUE KEY uq_video_tracks_stream (media_file_id, stream_index)
) ENGINE = InnoDB;


CREATE TABLE audio_tracks (
    id              BIGINT UNSIGNED     NOT NULL AUTO_INCREMENT,
    media_file_id   BIGINT UNSIGNED     NOT NULL,
    stream_index    INT                 NOT NULL,

    codec_name      VARCHAR(32)         NOT NULL COMMENT 'aac, ac3, dts, eac3',
    codec_profile   VARCHAR(32)         NULL,
    channels        TINYINT UNSIGNED    NULL,
    channel_layout  VARCHAR(32)         NULL,
    sample_rate     INT UNSIGNED        NULL,
    bit_rate        INT UNSIGNED        NULL,
    language        CHAR(3)             NULL COMMENT 'ISO 639-2',
    title           VARCHAR(255)        NULL,
    is_default      TINYINT(1)          NOT NULL DEFAULT 0,
    is_forced       TINYINT(1)          NOT NULL DEFAULT 0,

    PRIMARY KEY (id),
    UNIQUE KEY uq_audio_tracks_stream (media_file_id, stream_index),
    KEY idx_audio_tracks_language (media_file_id, language)
) ENGINE = InnoDB;


CREATE TABLE subtitle_tracks (
    id                      BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    media_file_id           BIGINT UNSIGNED NOT NULL,
    stream_index            INT             NULL COMMENT 'NULL khi là subtitle rời',

    is_external             TINYINT(1)      NOT NULL DEFAULT 0,
    external_path           VARCHAR(512)    NULL,
    subtitle_format         VARCHAR(32)     NOT NULL COMMENT 'subrip, ass, webvtt, hdmv_pgs_subtitle, dvd_subtitle',
    is_text_based           TINYINT(1)      NOT NULL DEFAULT 1 COMMENT '0 với PGS/VOBSUB - buộc phải burn-in khi transcode',
    language                CHAR(3)         NULL COMMENT 'ISO 639-2',
    title                   VARCHAR(255)    NULL,
    is_default              TINYINT(1)      NOT NULL DEFAULT 0,
    is_forced               TINYINT(1)      NOT NULL DEFAULT 0,
    is_hearing_impaired     TINYINT(1)      NOT NULL DEFAULT 0,

    PRIMARY KEY (id),
    UNIQUE KEY uq_subtitle_tracks_stream (media_file_id, stream_index),
    UNIQUE KEY uq_subtitle_tracks_external (external_path),
    KEY idx_subtitle_tracks_language (media_file_id, language)
) ENGINE = InnoDB;


-- =====================================================================
-- 6. PLAYBACK
-- =====================================================================

CREATE TABLE devices (
    id                      BIGINT UNSIGNED     NOT NULL AUTO_INCREMENT,
    user_id                 BIGINT UNSIGNED     NOT NULL,
    device_key              VARCHAR(128)        NOT NULL COMMENT 'Client-generated stable id',
    name                    VARCHAR(128)        NULL,
    platform                VARCHAR(64)         NULL COMMENT 'web, android, tvos',
    client_name             VARCHAR(64)         NULL,
    client_version          VARCHAR(32)         NULL,

    supports_hls            TINYINT(1)          NOT NULL DEFAULT 1,
    max_height              INT UNSIGNED        NULL,
    capabilities            JSON                NULL COMMENT 'Codec/container/audio profile mà client hỗ trợ',

    first_seen_at           DATETIME(3)         NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    last_seen_at            DATETIME(3)         NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),

    PRIMARY KEY (id),
    UNIQUE KEY uq_devices_user_key (user_id, device_key)
) ENGINE = InnoDB;


CREATE TABLE playback_sessions (
    id                          BIGINT UNSIGNED                                             NOT NULL AUTO_INCREMENT,
    session_key                 CHAR(36)                                                    NOT NULL COMMENT 'UUID public',
    user_id                     BIGINT UNSIGNED                                             NOT NULL,
    device_id                   BIGINT UNSIGNED                                             NULL,

    media_item_id               BIGINT UNSIGNED                                             NOT NULL,
    episode_id                  BIGINT UNSIGNED                                             NULL,
    media_file_id               BIGINT UNSIGNED                                             NOT NULL,

    playback_mode               ENUM('DIRECT_PLAY', 'DIRECT_STREAM', 'TRANSCODE')           NOT NULL,
    protocol                    ENUM('HTTP_RANGE', 'HLS')                                   NOT NULL,
    selected_audio_track_id     BIGINT UNSIGNED                                             NULL,
    selected_subtitle_track_id  BIGINT UNSIGNED                                             NULL,

    status                      ENUM('ACTIVE', 'ENDED')                                     NOT NULL DEFAULT 'ACTIVE',
    position_seconds            DECIMAL(12,3)                                               NOT NULL DEFAULT 0,
    started_at                  DATETIME(3)                                                 NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    last_heartbeat_at           DATETIME(3)                                                 NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    ended_at                    DATETIME(3)                                                 NULL,
    end_reason                  ENUM('COMPLETED', 'STOPPED', 'TIMEOUT', 'ERROR')            NULL,

    client_ip                   VARBINARY(16)                                               NULL,
    user_agent                  VARCHAR(512)                                                NULL,

    PRIMARY KEY (id),
    UNIQUE KEY uq_playback_sessions_key (session_key),
    KEY idx_playback_sessions_active (status, last_heartbeat_at),
    KEY idx_playback_sessions_user (user_id, started_at),
    KEY idx_playback_sessions_file (media_file_id),
    KEY idx_playback_sessions_item (media_item_id),
    KEY idx_playback_sessions_episode (episode_id),
    KEY idx_playback_sessions_device (device_id),
    KEY idx_playback_sessions_audio_track (selected_audio_track_id),
    KEY idx_playback_sessions_subtitle_track (selected_subtitle_track_id)
) ENGINE = InnoDB;


CREATE TABLE watch_progress (
    id                  BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    user_id             BIGINT UNSIGNED NOT NULL,
    media_item_id       BIGINT UNSIGNED NOT NULL,
    episode_id          BIGINT UNSIGNED NULL,
    media_file_id       BIGINT UNSIGNED NULL,

    position_seconds    DECIMAL(12,3)   NOT NULL DEFAULT 0,
    duration_seconds    DECIMAL(12,3)   NULL,
    is_completed        TINYINT(1)      NOT NULL DEFAULT 0,
    play_count          INT UNSIGNED    NOT NULL DEFAULT 0,
    first_played_at     DATETIME(3)     NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    last_played_at      DATETIME(3)     NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    version             INT UNSIGNED    NOT NULL DEFAULT 0 COMMENT 'Optimistic locking cho heartbeat đồng thời',

    -- Chuẩn hóa NULL để unique key hoạt động đúng với cả movie lẫn episode
    episode_key         BIGINT UNSIGNED
                        GENERATED ALWAYS AS (IFNULL(episode_id, 0)) STORED,

    PRIMARY KEY (id),
    UNIQUE KEY uq_watch_progress_target (user_id, media_item_id, episode_key),
    KEY idx_watch_progress_continue (user_id, is_completed, last_played_at),
    KEY idx_watch_progress_episode (episode_id),
    KEY idx_watch_progress_file (media_file_id)
) ENGINE = InnoDB;


-- =====================================================================
-- 7. TRANSCODING
-- =====================================================================

CREATE TABLE transcode_jobs (
    id                      BIGINT UNSIGNED                                                     NOT NULL AUTO_INCREMENT,
    job_key                 CHAR(36)                                                            NOT NULL COMMENT 'UUID public',
    media_file_id           BIGINT UNSIGNED                                                     NOT NULL,
    playback_session_id     BIGINT UNSIGNED                                                     NULL,
    requested_by_user_id    BIGINT UNSIGNED                                                     NULL,

    target_protocol         ENUM('HLS', 'PROGRESSIVE')                                          NOT NULL DEFAULT 'HLS',
    target_video_codec      VARCHAR(32)                                                         NULL,
    target_audio_codec      VARCHAR(32)                                                         NULL,
    target_width            INT UNSIGNED                                                        NULL,
    target_height           INT UNSIGNED                                                        NULL,
    target_video_bitrate    INT UNSIGNED                                                        NULL,
    target_audio_bitrate    INT UNSIGNED                                                        NULL,
    audio_stream_index      INT                                                                 NULL,
    subtitle_stream_index   INT                                                                 NULL,
    burn_in_subtitle        TINYINT(1)                                                          NOT NULL DEFAULT 0,
    start_offset_seconds    DECIMAL(12,3)                                                       NOT NULL DEFAULT 0,
    hw_accel                VARCHAR(32)                                                         NULL COMMENT 'none, qsv, vaapi',

    params_hash             CHAR(64)                                                            NOT NULL COMMENT 'Hash của toàn bộ tham số target - dùng cho dedupe & cache lookup',

    status                  ENUM('QUEUED', 'RUNNING', 'COMPLETED', 'FAILED', 'CANCELLED')       NOT NULL DEFAULT 'QUEUED',
    progress_percent        DECIMAL(5,2)                                                        NOT NULL DEFAULT 0,
    output_dir              VARCHAR(512)                                                        NULL,
    process_pid             INT                                                                 NULL,
    exit_code               INT                                                                 NULL,
    error_message           TEXT                                                                NULL,
    attempt_count           INT UNSIGNED                                                        NOT NULL DEFAULT 0,

    queued_at               DATETIME(3)                                                         NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    started_at              DATETIME(3)                                                         NULL,
    last_progress_at        DATETIME(3)                                                         NULL,
    finished_at             DATETIME(3)                                                         NULL,

    -- Chặn job trùng: chỉ một job QUEUED/RUNNING cho cùng (file, params)
    active_params_hash      CHAR(64)
                            GENERATED ALWAYS AS (
                                IF(status IN ('QUEUED', 'RUNNING'), params_hash, NULL)
                            ) VIRTUAL,

    PRIMARY KEY (id),
    UNIQUE KEY uq_transcode_jobs_key (job_key),
    UNIQUE KEY uq_transcode_jobs_active (media_file_id, active_params_hash),
    KEY idx_transcode_jobs_status (status, queued_at),
    KEY idx_transcode_jobs_session (playback_session_id),
    KEY idx_transcode_jobs_user (requested_by_user_id)
) ENGINE = InnoDB;


CREATE TABLE transcode_outputs (
    id                  BIGINT UNSIGNED                                     NOT NULL AUTO_INCREMENT,
    media_file_id       BIGINT UNSIGNED                                     NOT NULL,
    transcode_job_id    BIGINT UNSIGNED                                     NULL,

    variant_label       VARCHAR(32)                                         NOT NULL COMMENT '720p, 1080p, 2160p',
    width               INT UNSIGNED                                        NULL,
    height              INT UNSIGNED                                        NULL,
    bandwidth_bps       INT UNSIGNED                                        NULL,
    video_codec         VARCHAR(32)                                         NULL,
    audio_codec         VARCHAR(32)                                         NULL,

    output_dir          VARCHAR(512)                                        NOT NULL,
    playlist_path       VARCHAR(512)                                        NULL,
    segment_count       INT UNSIGNED                                        NULL,
    size_bytes          BIGINT UNSIGNED                                     NULL,

    state               ENUM('BUILDING', 'READY', 'STALE', 'DELETED')       NOT NULL DEFAULT 'BUILDING',
    last_accessed_at    DATETIME(3)                                         NULL,
    expires_at          DATETIME(3)                                         NULL,
    created_at          DATETIME(3)                                         NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    updated_at          DATETIME(3)                                         NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),

    PRIMARY KEY (id),
    UNIQUE KEY uq_transcode_outputs_variant (media_file_id, variant_label),
    KEY idx_transcode_outputs_state (state, last_accessed_at),
    KEY idx_transcode_outputs_job (transcode_job_id)
) ENGINE = InnoDB;


-- =====================================================================
-- 8. BACKGROUND JOBS
-- =====================================================================

CREATE TABLE job_executions (
    id              BIGINT UNSIGNED                                                     NOT NULL AUTO_INCREMENT,
    job_name        VARCHAR(128)                                                        NOT NULL COMMENT 'MEDIA_SCAN, TORRENT_POLL',
    job_key         VARCHAR(255)                                                        NULL COMMENT 'Idempotency key trong phạm vi job_name',
    status          ENUM('PENDING', 'RUNNING', 'SUCCEEDED', 'FAILED', 'CANCELLED')      NOT NULL DEFAULT 'PENDING',
    attempt         INT UNSIGNED                                                        NOT NULL DEFAULT 0,
    max_attempts    INT UNSIGNED                                                        NOT NULL DEFAULT 1,
    correlation_id  CHAR(36)                                                            NULL,
    started_at      DATETIME(3)                                                         NULL,
    finished_at     DATETIME(3)                                                         NULL,
    duration_ms     BIGINT UNSIGNED                                                     NULL,
    result          JSON                                                                NULL,
    error_message   TEXT                                                                NULL,
    created_at      DATETIME(3)                                                         NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    updated_at      DATETIME(3)                                                         NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),

    PRIMARY KEY (id),
    UNIQUE KEY uq_job_executions_key (job_name, job_key),
    KEY idx_job_executions_status (job_name, status, created_at),
    KEY idx_job_executions_correlation (correlation_id)
) ENGINE = InnoDB;


-- =====================================================================
-- 9. TORRENT INGESTION
-- =====================================================================

CREATE TABLE torrent_downloads (
    id                  BIGINT UNSIGNED                                                     NOT NULL AUTO_INCREMENT,
    info_hash           CHAR(40)                                                            NOT NULL,
    name                VARCHAR(512)                                                        NOT NULL,
    magnet_uri          TEXT                                                                NULL,
    category            VARCHAR(64)                                                         NULL,
    save_path           VARCHAR(512)                                                        NULL,
    size_bytes          BIGINT UNSIGNED                                                     NULL,

    state               VARCHAR(32)                                                         NULL COMMENT 'State thô từ qBittorrent API',
    progress_percent    DECIMAL(5,2)                                                        NOT NULL DEFAULT 0,
    last_polled_at      DATETIME(3)                                                         NULL,

    import_status       ENUM('PENDING', 'IMPORTING', 'IMPORTED', 'FAILED', 'SKIPPED')       NOT NULL DEFAULT 'PENDING',
    import_error        TEXT                                                                NULL,
    imported_at         DATETIME(3)                                                         NULL,
    media_item_id       BIGINT UNSIGNED                                                     NULL,

    added_by_user_id    BIGINT UNSIGNED                                                     NULL,
    added_at            DATETIME(3)                                                         NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    completed_at        DATETIME(3)                                                         NULL,
    updated_at          DATETIME(3)                                                         NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),

    PRIMARY KEY (id),
    UNIQUE KEY uq_torrent_downloads_info_hash (info_hash),
    KEY idx_torrent_downloads_import (import_status, completed_at),
    KEY idx_torrent_downloads_user (added_by_user_id),
    KEY idx_torrent_downloads_media_item (media_item_id)
) ENGINE = InnoDB;