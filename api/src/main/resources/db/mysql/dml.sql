-- =====================================================================
-- Video Streaming Platform — Sample Data (DML)
-- Khớp với ddl.sql (i18n gộp 1 bảng, không còn FOREIGN KEY)
-- Thứ tự insert vẫn tôn trọng quan hệ logic cha -> con để dữ liệu nhất
-- quán, dù DB không còn ép buộc bằng constraint.
-- =====================================================================

-- =====================================================================
-- 1. I18N
-- =====================================================================

INSERT INTO i18n (`key`, language, value) VALUES
('billing.plan.title', 'vi', 'Gói Cao Cấp'),
('billing.plan.title', 'en', 'Premium Plan'),
('billing.plan.title', 'ja', 'プレミアムプラン'),

('library.1.name', 'vi', 'Phim lẻ'),
('library.1.name', 'en', 'Movies'),
('library.2.name', 'vi', 'Phim bộ'),
('library.2.name', 'en', 'TV Shows'),
('library.3.name', 'vi', 'Anime'),
('library.3.name', 'en', 'Anime'),

('media_item.1.title', 'vi', 'Kẻ Đánh Cắp Giấc Mơ'),
('media_item.1.title', 'en', 'Inception'),
('media_item.1.overview', 'vi', 'Một tay trộm chuyên đánh cắp bí mật qua giấc mơ nhận nhiệm vụ cấy ý tưởng vào tiềm thức người khác.'),
('media_item.1.overview', 'en', 'A thief who steals secrets through dream-sharing is given a chance to plant an idea instead.'),

('media_item.2.title', 'vi', 'Ma Trận'),
('media_item.2.title', 'en', 'The Matrix'),
('media_item.2.overview', 'vi', 'Một lập trình viên phát hiện thế giới thực chỉ là một mô phỏng máy tính.'),
('media_item.2.overview', 'en', 'A computer programmer discovers reality as he knows it is a simulation.'),

('media_item.3.title', 'vi', 'Tập Làm Người Xấu'),
('media_item.3.title', 'en', 'Breaking Bad'),

('media_item.4.title', 'vi', 'Đại Chiến Titan'),
('media_item.4.title', 'en', 'Attack on Titan'),

('season.1.name', 'vi', 'Phần 1'),
('season.1.name', 'en', 'Season 1'),

('genre.1.name', 'vi', 'Hành động'),
('genre.1.name', 'en', 'Action'),
('genre.3.name', 'vi', 'Khoa học viễn tưởng'),
('genre.3.name', 'en', 'Science Fiction'),

('role.3.description', 'vi', 'Toàn quyền quản trị hệ thống'),
('role.3.description', 'en', 'Full system administration access');


-- =====================================================================
-- 2. RBAC
-- =====================================================================

INSERT INTO users
    (id, username, email, password_hash, display_name, preferred_language,
     preferred_audio_language, preferred_subtitle_language, is_active, is_locked,
     failed_login_attempts, locked_until, last_login_at, password_changed_at)
VALUES
    (1, 'admin', 'admin@example.com', SHA2('admin-secret-pw', 256), 'Quản trị viên', 'vi',
     'eng', 'vie', 1, 0, 0, NULL, '2026-08-16 08:00:00.000', '2026-06-01 00:00:00.000'),
    (2, 'manager1', 'manager1@example.com', SHA2('manager-secret-pw', 256), 'Nguyễn Quản Lý', 'vi',
     'eng', 'eng', 1, 0, 0, NULL, '2026-08-15 20:15:00.000', '2026-05-10 00:00:00.000'),
    (3, 'alice', 'alice@example.com', SHA2('alice-secret-pw', 256), 'Alice Tran', 'en',
     'eng', 'eng', 1, 0, 0, NULL, '2026-08-17 07:45:00.000', '2026-04-20 00:00:00.000'),
    (4, 'bob', 'bob@example.com', SHA2('bob-secret-pw', 256), 'Bob Le', 'vi',
     'jpn', 'vie', 1, 0, 2, NULL, '2026-08-10 22:00:00.000', '2026-03-01 00:00:00.000'),
    (5, 'guest_locked', 'guest.locked@example.com', SHA2('guest-secret-pw', 256), 'Tài khoản bị khoá', 'en',
     NULL, NULL, 1, 1, 5, '2026-08-18 00:00:00.000', '2026-07-01 12:00:00.000', '2026-01-15 00:00:00.000');


INSERT INTO roles (id, code, name, description, parent_role_id, is_system) VALUES
(1, 'VIEWER', 'Người xem', 'Quyền cơ bản: xem và phát nội dung', NULL, 1),
(2, 'LIBRARY_MANAGER', 'Quản lý thư viện', 'Kế thừa VIEWER, thêm quyền quét/sửa thư viện', 1, 1),
(3, 'ADMIN', 'Quản trị viên', 'Kế thừa LIBRARY_MANAGER, thêm quyền quản trị hệ thống', 2, 1);


INSERT INTO permissions (id, code, resource, action, description, is_system) VALUES
(1, 'media:read', 'media', 'read', 'Xem thông tin & phát media', 1),
(2, 'media:write', 'media', 'write', 'Sửa metadata media', 1),
(3, 'playback:start', 'playback', 'start', 'Khởi tạo phiên phát', 1),
(4, 'library:scan', 'library', 'scan', 'Kích hoạt quét lại thư viện', 1),
(5, 'transcode:cancel', 'transcode', 'cancel', 'Huỷ job transcode', 1),
(6, 'user:manage', 'user', 'manage', 'Tạo/sửa/khoá user', 1),
(7, 'role:manage', 'role', 'manage', 'Gán role & permission', 1),
(8, 'torrent:manage', 'torrent', 'manage', 'Thêm/xoá torrent download', 1);


INSERT INTO role_permissions (role_id, permission_id, granted_by) VALUES
(1, 1, 1),   -- VIEWER: media:read
(1, 3, 1),   -- VIEWER: playback:start
(2, 2, 1),   -- LIBRARY_MANAGER: media:write
(2, 4, 1),   -- LIBRARY_MANAGER: library:scan
(3, 5, 1),   -- ADMIN: transcode:cancel
(3, 6, 1),   -- ADMIN: user:manage
(3, 7, 1),   -- ADMIN: role:manage
(3, 8, 1);   -- ADMIN: torrent:manage


INSERT INTO user_roles (user_id, role_id, granted_by, expires_at) VALUES
(1, 3, 1, NULL),   -- admin      -> ADMIN
(2, 2, 1, NULL),   -- manager1   -> LIBRARY_MANAGER
(3, 1, 1, NULL),   -- alice      -> VIEWER
(4, 1, 1, NULL),   -- bob        -> VIEWER
(5, 1, 1, '2026-09-01 00:00:00.000'); -- guest_locked -> VIEWER tạm thời, hết hạn


-- Override cá nhân: alice được cấp thêm library:scan dù không phải LIBRARY_MANAGER
-- bob bị chặn playback:start dù role VIEWER cho ALLOW -> DENY thắng ALLOW
INSERT INTO user_permissions (user_id, permission_id, effect, granted_by, expires_at) VALUES
(3, 4, 'ALLOW', 1, '2026-12-31 23:59:59.000'),
(4, 3, 'DENY', 1, NULL);


INSERT INTO refresh_tokens
    (id, user_id, token_hash, issued_at, expires_at, revoked_at, replaced_by_id, client_ip, user_agent)
VALUES
(1, 1, SHA2('refresh-token-admin-session-1', 256), '2026-08-16 08:00:00.000',
    '2026-09-16 08:00:00.000', NULL, NULL, INET6_ATON('118.70.12.5'), 'Mozilla/5.0 (Windows NT 10.0) Chrome/128'),
(2, 3, SHA2('refresh-token-alice-old', 256), '2026-08-01 07:00:00.000',
    '2026-09-01 07:00:00.000', '2026-08-17 07:45:00.000', 3, INET6_ATON('27.72.1.10'), 'okhttp/4.10 (Android 14)'),
(3, 3, SHA2('refresh-token-alice-current', 256), '2026-08-17 07:45:00.000',
    '2026-09-17 07:45:00.000', NULL, NULL, INET6_ATON('27.72.1.10'), 'okhttp/4.10 (Android 14)');


-- =====================================================================
-- 3. LIBRARY
-- =====================================================================

INSERT INTO libraries
    (id, code, library_type, root_path, is_enabled, scan_interval_seconds, last_scan_at, last_scan_status)
VALUES
(1, 'movies-main', 'MOVIE', '/srv/media/movies', 1, 3600, '2026-08-17 03:00:00.000', 'SUCCEEDED'),
(2, 'tv-main', 'TV', '/srv/media/tv', 1, 3600, '2026-08-17 03:05:00.000', 'SUCCEEDED'),
(3, 'anime-main', 'ANIME', '/srv/media/anime', 1, 7200, '2026-08-16 03:00:00.000', 'SUCCEEDED');


-- =====================================================================
-- 4. MEDIA DOMAIN
-- =====================================================================

INSERT INTO media_items
    (id, library_id, item_type, original_title, sort_title, original_language, release_year,
     release_date, runtime_seconds, content_rating, folder_path, poster_path, backdrop_path,
     metadata_source, metadata_source_id, status)
VALUES
(1, 1, 'MOVIE', 'Inception', 'Inception', 'eng', 2010, '2010-07-16', 8880, 'PG-13',
    '/srv/media/movies/Inception (2010)', '/srv/media/movies/Inception (2010)/poster.jpg',
    '/srv/media/movies/Inception (2010)/backdrop.jpg', 'tmdb', '27205', 'ACTIVE'),
(2, 1, 'MOVIE', 'The Matrix', 'Matrix, The', 'eng', 1999, '1999-03-31', 8160, 'R',
    '/srv/media/movies/The Matrix (1999)', '/srv/media/movies/The Matrix (1999)/poster.jpg',
    '/srv/media/movies/The Matrix (1999)/backdrop.jpg', 'tmdb', '603', 'ACTIVE'),
(3, 2, 'SERIES', 'Breaking Bad', 'Breaking Bad', 'eng', 2008, '2008-01-20', NULL, 'TV-MA',
    '/srv/media/tv/Breaking Bad', '/srv/media/tv/Breaking Bad/poster.jpg',
    '/srv/media/tv/Breaking Bad/backdrop.jpg', 'tvdb', '81189', 'ACTIVE'),
(4, 3, 'SERIES', 'Shingeki no Kyojin', 'Attack on Titan', 'jpn', 2013, '2013-04-07', NULL, 'TV-MA',
    '/srv/media/anime/Attack on Titan', '/srv/media/anime/Attack on Titan/poster.jpg',
    '/srv/media/anime/Attack on Titan/backdrop.jpg', 'tvdb', '267440', 'ACTIVE');


INSERT INTO seasons (id, media_item_id, season_number, folder_path, poster_path, air_date) VALUES
(1, 3, 1, '/srv/media/tv/Breaking Bad/Season 01', '/srv/media/tv/Breaking Bad/Season 01/poster.jpg', '2008-01-20'),
(2, 3, 2, '/srv/media/tv/Breaking Bad/Season 02', '/srv/media/tv/Breaking Bad/Season 02/poster.jpg', '2009-03-08'),
(3, 4, 1, '/srv/media/anime/Attack on Titan/Season 01', '/srv/media/anime/Attack on Titan/Season 01/poster.jpg', '2013-04-07');


INSERT INTO episodes
    (id, season_id, media_item_id, episode_number, air_date, runtime_seconds, still_path)
VALUES
(1, 1, 3, 1, '2008-01-20', 3480, '/srv/media/tv/Breaking Bad/Season 01/s01e01-still.jpg'),
(2, 1, 3, 2, '2008-01-27', 2940, '/srv/media/tv/Breaking Bad/Season 01/s01e02-still.jpg'),
(3, 3, 4, 1, '2013-04-07', 1440, '/srv/media/anime/Attack on Titan/Season 01/s01e01-still.jpg');


INSERT INTO genres (id, code) VALUES
(1, 'action'),
(2, 'drama'),
(3, 'sci-fi'),
(4, 'crime'),
(5, 'anime');


INSERT INTO media_item_genres (media_item_id, genre_id) VALUES
(1, 1), (1, 3),   -- Inception: Action, Sci-Fi
(2, 1), (2, 3),   -- The Matrix: Action, Sci-Fi
(3, 4), (3, 2),   -- Breaking Bad: Crime, Drama
(4, 5), (4, 1);   -- Attack on Titan: Anime, Action


-- =====================================================================
-- 5. MEDIA FILES & TRACKS
-- =====================================================================

INSERT INTO media_files
    (id, media_item_id, episode_id, file_path, file_name, file_size_bytes, file_modified_at,
     content_hash, container_format, duration_seconds, overall_bitrate, analysis_status, analyzed_at, is_available)
VALUES
(1, 1, NULL, '/srv/media/movies/Inception (2010)/Inception.2010.2160p.mkv', 'Inception.2010.2160p.mkv',
    32212254720, '2026-01-10 12:00:00.000', SHA2('inception-2160p-file', 256), 'matroska', 8880.000, 29000000, 'ANALYZED', '2026-01-10 12:05:00.000', 1),
(2, 2, NULL, '/srv/media/movies/The Matrix (1999)/The.Matrix.1999.2160p.mkv', 'The.Matrix.1999.2160p.mkv',
    30064771072, '2026-08-16 21:00:00.000', SHA2('matrix-2160p-file', 256), 'matroska', 8160.000, 30500000, 'ANALYZED', '2026-08-16 21:10:00.000', 1),
(3, 3, 1, '/srv/media/tv/Breaking Bad/Season 01/Breaking.Bad.S01E01.mkv', 'Breaking.Bad.S01E01.mkv',
    2147483648, '2026-02-01 09:00:00.000', SHA2('bb-s01e01-file', 256), 'matroska', 3480.000, 4900000, 'ANALYZED', '2026-02-01 09:05:00.000', 1),
(4, 3, 2, '/srv/media/tv/Breaking Bad/Season 01/Breaking.Bad.S01E02.mkv', 'Breaking.Bad.S01E02.mkv',
    1932735283, '2026-02-01 09:10:00.000', SHA2('bb-s01e02-file', 256), 'matroska', 2940.000, 5250000, 'ANALYZED', '2026-02-01 09:15:00.000', 1),
(5, 4, 3, '/srv/media/anime/Attack on Titan/Season 01/AoT.S01E01.mkv', 'AoT.S01E01.mkv',
    966367641, '2026-03-05 18:00:00.000', SHA2('aot-s01e01-file', 256), 'matroska', 1440.000, 5400000, 'ANALYZED', '2026-03-05 18:05:00.000', 1);


INSERT INTO video_tracks
    (id, media_file_id, stream_index, codec_name, codec_profile, codec_level, width, height,
     display_aspect_ratio, frame_rate, bit_rate, pixel_format, bit_depth, color_space, color_transfer,
     color_primaries, is_hdr, hdr_format, language, title, is_default)
VALUES
(1, 1, 0, 'hevc', 'Main 10', 153, 3840, 1606, '1.90:1', 23.9760, 26500000, 'yuv420p10le', 10,
    'bt2020nc', 'smpte2084', 'bt2020', 1, 'HDR10', NULL, NULL, 1),
(2, 2, 0, 'h264', 'High', 51, 3840, 1600, '2.40:1', 23.9760, 28000000, 'yuv420p', 8,
    'bt709', 'bt709', 'bt709', 0, NULL, NULL, NULL, 1),
(3, 3, 0, 'h264', 'High', 41, 1920, 1080, '16:9', 23.9760, 4500000, 'yuv420p', 8,
    'bt709', 'bt709', 'bt709', 0, NULL, NULL, NULL, 1);


INSERT INTO audio_tracks
    (id, media_file_id, stream_index, codec_name, codec_profile, channels, channel_layout,
     sample_rate, bit_rate, language, title, is_default, is_forced)
VALUES
(1, 1, 1, 'truehd', 'Atmos', 8, '7.1', 48000, 4000000, 'eng', 'English Atmos 7.1', 1, 0),
(2, 1, 2, 'aac', 'LC', 2, 'stereo', 48000, 192000, 'vie', 'Tiếng Việt', 0, 0),
(3, 2, 1, 'dts', NULL, 6, '5.1', 48000, 1500000, 'eng', 'English DTS 5.1', 1, 0),
(4, 3, 1, 'ac3', NULL, 6, '5.1', 48000, 640000, 'eng', 'English AC3 5.1', 1, 0);


INSERT INTO subtitle_tracks
    (id, media_file_id, stream_index, is_external, external_path, subtitle_format, is_text_based,
     language, title, is_default, is_forced, is_hearing_impaired)
VALUES
(1, 1, NULL, 1, '/srv/media/movies/Inception (2010)/Inception.2010.vi.srt', 'subrip', 1,
    'vie', 'Tiếng Việt', 1, 0, 0),
(2, 2, 2, 0, NULL, 'hdmv_pgs_subtitle', 0,
    'eng', 'English (PGS)', 0, 0, 0),
(3, 3, NULL, 1, '/srv/media/tv/Breaking Bad/Season 01/Breaking.Bad.S01E01.vi.srt', 'subrip', 1,
    'vie', 'Tiếng Việt', 1, 0, 0);


-- =====================================================================
-- 6. PLAYBACK
-- =====================================================================

INSERT INTO devices
    (id, user_id, device_key, name, platform, client_name, client_version,
     supports_hls, max_height, capabilities, first_seen_at, last_seen_at)
VALUES
(1, 1, 'web-chrome-admin-01', 'Chrome trên Windows', 'web', 'chrome', '128.0',
    1, 2160, JSON_OBJECT('codecs', JSON_ARRAY('h264', 'hevc'), 'maxAudioChannels', 8),
    '2026-01-05 09:00:00.000', '2026-08-17 07:50:00.000'),
(2, 3, 'android-pixel-alice', 'Pixel 8 của Alice', 'android', 'exoplayer', '2.19',
    1, 1080, JSON_OBJECT('codecs', JSON_ARRAY('h264'), 'maxAudioChannels', 6),
    '2026-02-01 10:00:00.000', '2026-08-17 07:45:00.000'),
(3, 3, 'tvos-appletv-alice', 'Apple TV phòng khách', 'tvos', 'avplayer', '17.5',
    1, 2160, JSON_OBJECT('codecs', JSON_ARRAY('h264', 'hevc'), 'maxAudioChannels', 8),
    '2026-03-10 20:00:00.000', '2026-08-15 21:30:00.000');


INSERT INTO playback_sessions
    (id, session_key, user_id, device_id, media_item_id, episode_id, media_file_id,
     playback_mode, protocol, selected_audio_track_id, selected_subtitle_track_id,
     status, position_seconds, started_at, last_heartbeat_at, ended_at, end_reason, client_ip, user_agent)
VALUES
(1, 'b1f8e6b0-2c1a-4e2b-9c3d-111111111111', 1, 1, 1, NULL, 1,
    'DIRECT_PLAY', 'HTTP_RANGE', 1, 1,
    'ACTIVE', 1423.500, '2026-08-17 07:55:00.000', '2026-08-17 08:02:00.000', NULL, NULL,
    INET6_ATON('118.70.12.5'), 'Mozilla/5.0 (Windows NT 10.0) Chrome/128'),
(2, 'b1f8e6b0-2c1a-4e2b-9c3d-222222222222', 3, 2, 3, 1, 3,
    'TRANSCODE', 'HLS', 4, 3,
    'ENDED', 3480.000, '2026-08-16 20:00:00.000', '2026-08-16 20:58:00.000', '2026-08-16 20:58:00.000', 'COMPLETED',
    INET6_ATON('27.72.1.10'), 'okhttp/4.10 (Android 14)');


INSERT INTO watch_progress
    (id, user_id, media_item_id, episode_id, media_file_id, position_seconds, duration_seconds,
     is_completed, play_count, first_played_at, last_played_at, version)
VALUES
(1, 1, 1, NULL, 1, 1423.500, 8880.000, 0, 1, '2026-08-17 07:55:00.000', '2026-08-17 08:02:00.000', 4),
(2, 3, 3, 1, 3, 3480.000, 3480.000, 1, 1, '2026-08-16 20:00:00.000', '2026-08-16 20:58:00.000', 12),
(3, 3, 3, 2, 4, 640.200, 2940.000, 0, 1, '2026-08-17 07:30:00.000', '2026-08-17 07:45:00.000', 3);


-- =====================================================================
-- 7. TRANSCODING
-- =====================================================================

INSERT INTO transcode_jobs
    (id, job_key, media_file_id, playback_session_id, requested_by_user_id,
     target_protocol, target_video_codec, target_audio_codec, target_width, target_height,
     target_video_bitrate, target_audio_bitrate, audio_stream_index, subtitle_stream_index,
     burn_in_subtitle, start_offset_seconds, hw_accel, params_hash,
     status, progress_percent, output_dir, process_pid, exit_code, error_message, attempt_count,
     queued_at, started_at, last_progress_at, finished_at)
VALUES
(1, 'c2d9f7c1-3d2b-5f3c-ad4e-333333333333', 2, NULL, 1,
    'HLS', 'h264', 'aac', 1920, 800,
    5000000, 192000, 1, NULL,
    0, 0, 'vaapi', SHA2('matrix-transcode-1080p-h264', 256),
    'COMPLETED', 100.00, '/srv/transcode/out/matrix-1080p', 48213, 0, NULL, 1,
    '2026-08-16 21:15:00.000', '2026-08-16 21:15:05.000', '2026-08-16 21:42:00.000', '2026-08-16 21:42:30.000');


INSERT INTO transcode_outputs
    (id, media_file_id, transcode_job_id, variant_label, width, height, bandwidth_bps,
     video_codec, audio_codec, output_dir, playlist_path, segment_count, size_bytes,
     state, last_accessed_at, expires_at)
VALUES
(1, 2, 1, '1080p', 1920, 800, 5192000, 'h264', 'aac',
    '/srv/transcode/out/matrix-1080p', '/srv/transcode/out/matrix-1080p/playlist.m3u8', 204, 4831838208,
    'READY', '2026-08-17 07:00:00.000', '2026-08-24 21:42:30.000'),
(2, 2, NULL, '720p', 1280, 534, 2600000, 'h264', 'aac',
    '/srv/transcode/out/matrix-720p', '/srv/transcode/out/matrix-720p/playlist.m3u8', 204, 2415919104,
    'STALE', '2026-08-01 10:00:00.000', '2026-08-08 00:00:00.000');


-- =====================================================================
-- 8. BACKGROUND JOBS
-- =====================================================================

INSERT INTO job_executions
    (id, job_name, job_key, status, attempt, max_attempts, correlation_id,
     started_at, finished_at, duration_ms, result, error_message)
VALUES
(1, 'MEDIA_SCAN', 'library:1:2026-08-17', 'SUCCEEDED', 1, 3, 'a4b1c2d3-e4f5-4061-8a2b-000000000001',
    '2026-08-17 03:00:00.000', '2026-08-17 03:04:12.000', 252000,
    JSON_OBJECT('newItems', 0, 'updatedItems', 1, 'library', 'movies-main'), NULL),
(2, 'TORRENT_POLL', NULL, 'RUNNING', 1, 5, 'a4b1c2d3-e4f5-4061-8a2b-000000000002',
    '2026-08-17 08:00:00.000', NULL, NULL, NULL, NULL);


-- =====================================================================
-- 9. TORRENT INGESTION
-- =====================================================================

INSERT INTO torrent_downloads
    (id, info_hash, name, magnet_uri, category, save_path, size_bytes,
     state, progress_percent, last_polled_at, import_status, import_error, imported_at,
     media_item_id, added_by_user_id, added_at, completed_at)
VALUES
(1, 'aa11bb22cc33dd44ee55ff66aa77bb88cc99dd00', 'The.Matrix.1999.2160p.UHD.BluRay.x265',
    'magnet:?xt=urn:btih:aa11bb22cc33dd44ee55ff66aa77bb88cc99dd00&dn=The.Matrix.1999.2160p',
    'movies', '/srv/downloads/movies/The.Matrix.1999.2160p', 30064771072,
    'uploading', 100.00, '2026-08-16 20:50:00.000', 'IMPORTED', NULL, '2026-08-16 21:00:00.000',
    2, 1, '2026-08-16 15:00:00.000', '2026-08-16 20:45:00.000'),
(2, 'ff11ee22dd33cc44bb55aa66ff77ee88dd99cc00', 'Attack.on.Titan.S01.1080p.WEB-DL',
    'magnet:?xt=urn:btih:ff11ee22dd33cc44bb55aa66ff77ee88dd99cc00&dn=Attack.on.Titan.S01.1080p',
    'anime', '/srv/downloads/anime/Attack.on.Titan.S01.1080p', 8589934592,
    'downloading', 63.40, '2026-08-17 08:01:00.000', 'PENDING', NULL, NULL,
    NULL, 2, '2026-08-17 06:00:00.000', NULL);