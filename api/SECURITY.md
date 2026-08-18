Luồng Authentication (Xác thực)

Xác thực dựa trên users (lưu password_hash) + refresh_tokens (quản lý phiên đăng nhập dài hạn). Không còn FK ràng buộc ở DB, tầng application (JPA/Service) tự đảm bảo tính nhất quán.

Đăng nhập lần đầu:

Client gửi username/email + password.
Server tra users theo uq_users_username/uq_users_email, kiểm tra is_active, is_locked, locked_until.
So khớp password với password_hash (bcrypt/argon2). Sai → tăng failed_login_attempts, có thể set is_locked + locked_until nếu vượt ngưỡng.
Đúng → reset failed_login_attempts = 0, cập nhật last_login_at, phát hành access token (JWT, sống ngắn, không lưu DB) + refresh token (sống dài, lưu DB).
Refresh token gốc không lưu trực tiếp mà lưu token_hash (SHA-256) vào refresh_tokens, kèm expires_at, client_ip, user_agent.

Refresh / rotation: khi access token hết hạn, client gửi refresh token → server hash lại, tìm theo uq_refresh_tokens_hash, kiểm tra revoked_at IS NULL và chưa expires_at. Nếu hợp lệ: phát access token mới + tạo dòng refresh_tokens mới, set replaced_by_id của dòng cũ trỏ sang dòng mới rồi đánh dấu revoked_at (rotation chống replay).

Luồng Authorization (Phân quyền)

Sau khi có access token hợp lệ (biết được user_id), hệ thống phải tổng hợp quyền hạn của user để quyết định cho phép hay từ chối một hành động. Có 3 nguồn quyền, kết hợp theo thứ tự ưu tiên:

Role → Permission (gián tiếp qua role):
user_roles gắn user với 1+ role (có thể có expires_at, hết hạn thì bỏ qua).
roles.parent_role_id tạo role hierarchy: role con tự động kế thừa toàn bộ quyền của role cha (đệ quy lên tới root).
role_permissions map mỗi role với các permissions cụ thể (resource + action, vd media:read).
→ Tập hợp tất cả permission từ mọi role user đang giữ (kể cả role cha) tạo thành tập ALLOW mặc định.
User → Permission (override trực tiếp):
user_permissions cho phép gán/thu hồi 1 permission riêng cho từng user, với effect = ALLOW hoặc DENY.
Dùng khi cần ngoại lệ cá nhân mà không muốn tạo hẳn 1 role riêng.
Quy tắc kết hợp: như comment trong DDL — DENY luôn thắng ALLOW. Thứ tự resolve thường là:
Có user_permissions với effect = DENY và còn hiệu lực (expires_at chưa qua) → từ chối ngay, bất kể role nói gì.
Ngược lại, nếu có user_permissions ALLOW hoặc permission đến từ role (trực tiếp/kế thừa) → cho phép.
Không có gì khớp → mặc định từ chối.