                  Internet
                     │
                  No-IP
                     │
                   :443
                     │
             ┌───────▼───────┐
             │ VNPT Router   │
             │ Port Forward  │
             └───────┬───────┘
                     │
                     ▼
                  Nginx
                     │
                     ▼
               127.0.0.1:8096
                     │
                     ▼
                  Jellyfin

Bước 1 — Xác định hostname No-IP

Giả sử hostname của bạn là:

yourname.ddns.net

Trong các lệnh dưới đây, thay:

YOUR_DOMAIN

bằng hostname thật.

Không cần gửi hostname cho mình nếu bạn không muốn.

Bước 2 — Kiểm tra Nginx hiện tại

Bạn đã có Nginx vì ss trước đó cho thấy:

0.0.0.0:3000 → nginx

Trước khi sửa, backup toàn bộ config:

sudo cp -a /etc/nginx /etc/nginx.backup-$(date +%F-%H%M%S)

Sau đó:

sudo nginx -t

Kỳ vọng:

syntax is ok
test is successful

Tiếp theo xem cấu hình hiện tại:

sudo nginx -T

Lệnh này có thể cho output khá dài. Không cần gửi toàn bộ cho mình ngay.

Đặc biệt, vì Nginx hiện đang dùng port 3000, chúng ta không được xóa config cũ một cách mù quáng.

Bước 3 — Kiểm tra port 80 và 443 trên Ubuntu

Chạy:

sudo ss -lntp | grep -E ':(80|443|3000)\b'

Hiện tại có thể bạn chỉ thấy:

:3000

Đó là bình thường.

Mục tiêu sau này sẽ là:

:80 nginx
:443 nginx
:3000 nginx/ứng dụng cũ
Bước 4 — Mở port 80 và 443 trên UFW

Jellyfin yêu cầu proxy server có thể nhận traffic trên TCP 80 và 443; nếu dùng HTTP/3 sau này thì mới cần thêm UDP 443.

Chạy:

sudo ufw allow 80/tcp
sudo ufw allow 443/tcp

Kiểm tra:

sudo ufw status numbered

Bạn cần có:

80/tcp ALLOW IN Anywhere
443/tcp ALLOW IN Anywhere
Bước 5 — Cấu hình VNPT Router

Trong port forwarding của VNPT, tạo:

WAN 80 → 192.168.1.110:80
WAN 443 → 192.168.1.110:443

Tức là:

80 → 192.168.1.110:80
443 → 192.168.1.110:443

Không cần forward 8096 nữa sau khi cấu hình xong.

Tạm thời bạn có thể giữ 8096 trong lúc setup để tránh tự khóa mình, nhưng sau khi HTTPS hoạt động phải đóng nó.

Bước 6 — Cài Certbot

Jellyfin khuyến nghị dùng chứng chỉ CA tin cậy như Let's Encrypt khi expose dịch vụ ra Internet. Certbot hiện khuyến nghị cài qua Snap và hỗ trợ tự động chỉnh Nginx.

Kiểm tra Snap:

snap version

Nếu chưa có:

sudo apt update
sudo apt install -y snapd

Sau đó:

sudo snap install snapd

Nếu hệ thống đã có Snap thì bỏ qua phần này.

Cài Certbot:

sudo snap install --classic certbot

Tạo symlink:

sudo ln -s /snap/bin/certbot /usr/local/bin/certbot

Kiểm tra:

certbot --version
Bước 7 — Tạo HTTP server cho domain

Trước khi xin chứng chỉ, ta cần để Nginx trả lời hostname trên port 80.

Tạo file:

sudo nano /etc/nginx/sites-available/jellyfin

Nội dung ban đầu:

server {
listen 80;
listen [::]:80;

    server_name YOUR_DOMAIN;


    location / {
        return 200 "Jellyfin reverse proxy setup\n";
        add_header Content-Type text/plain;
    }

}

Thay:

YOUR_DOMAIN

bằng hostname No-IP thật.

Ví dụ:

server_name mynas.ddns.net;
Bước 8 — Enable site
sudo ln -s /etc/nginx/sites-available/jellyfin /etc/nginx/sites-enabled/jellyfin

Test:

sudo nginx -t

Nếu:

syntax is ok
test is successful

thì:

sudo systemctl reload nginx
Bước 9 — Test từ Internet

Dùng điện thoại:

Tắt Wi-Fi → bật 4G/5G

Mở:

http://YOUR_DOMAIN

Bạn phải nhìn thấy:

Jellyfin reverse proxy setup

Nếu thấy được, ta đã xác nhận:

No-IP
↓
VNPT
↓
Port 80
↓
Nginx

đã hoạt động.

Bước 10 — Xin Let's Encrypt certificate

Bây giờ chạy:

sudo certbot --nginx -d YOUR_DOMAIN

Certbot sẽ hỏi email, đồng ý Terms of Service và có thể hỏi có redirect HTTP → HTTPS hay không.

Chọn redirect HTTPS.

Certbot có thể tự chỉnh Nginx để kích hoạt HTTPS; tài liệu Certbot chính thức có hỗ trợ trực tiếp certbot --nginx.

Sau khi xong:

sudo nginx -t

rồi:

sudo systemctl reload nginx
Bước 11 — Kiểm tra HTTPS

Trên điện thoại 4G/5G:

https://YOUR_DOMAIN

Nếu browser hiện:

🔒 HTTPS

thì chứng chỉ đã hoạt động.

Bước 12 — Bây giờ thay server HTTP bằng reverse proxy Jellyfin

Mở:

sudo nano /etc/nginx/sites-available/jellyfin

Thay nội dung bằng cấu hình sau.

server {
listen 443 ssl;
listen [::]:443 ssl;

    http2 on;


    server_name YOUR_DOMAIN;


    client_max_body_size 20M;


    ssl_protocols TLSv1.2 TLSv1.3;


    ssl_certificate /etc/letsencrypt/live/YOUR_DOMAIN/fullchain.pem;
    ssl_certificate_key /etc/letsencrypt/live/YOUR_DOMAIN/privkey.pem;


    include /etc/letsencrypt/options-ssl-nginx.conf;
    ssl_dhparam /etc/letsencrypt/ssl-dhparams.pem;


    add_header X-Content-Type-Options "nosniff";


    location / {
        proxy_pass http://127.0.0.1:8096;


        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
        proxy_set_header X-Forwarded-Protocol $scheme;
        proxy_set_header X-Forwarded-Host $http_host;


        proxy_buffering off;
    }


    location /socket {
        proxy_pass http://127.0.0.1:8096;


        proxy_http_version 1.1;


        proxy_set_header Upgrade $http_upgrade;
        proxy_set_header Connection "upgrade";


        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
        proxy_set_header X-Forwarded-Protocol $scheme;
        proxy_set_header X-Forwarded-Host $http_host;
    }

}

server {
listen 80;
listen [::]:80;

    server_name YOUR_DOMAIN;


    return 301 https://$host$request_uri;

}

Cấu hình này bám theo mẫu Nginx chính thức của Jellyfin: proxy HTTP tới 127.0.0.1:8096, tắt buffering cho streaming, và proxy WebSocket tại /socket.

Bước 13 — Test Nginx
sudo nginx -t

Nếu thành công:

sudo systemctl reload nginx
Bước 14 — Truy cập Jellyfin

Từ điện thoại 4G/5G:

https://YOUR_DOMAIN

Bạn phải thấy trang đăng nhập Jellyfin.

Luồng lúc này là:

Phone
↓
HTTPS :443
↓
No-IP
↓
VNPT
↓
Nginx :443
↓
127.0.0.1:8096
↓
Jellyfin

Đây chính là thứ chúng ta muốn.

Bước 15 — Cấu hình Jellyfin "Known Proxies"

Đây là bước rất quan trọng, không nên bỏ qua.

Jellyfin cần biết Nginx là proxy đáng tin cậy để xử lý đúng:

X-Forwarded-For
X-Forwarded-Proto
X-Forwarded-Host

Jellyfin chính thức yêu cầu cấu hình reverse proxy trong Known Proxies khi đặt server phía sau proxy.

Trong Jellyfin:

Dashboard
→ Networking
→ Known Proxies

Thêm:

127.0.0.1

vì Nginx và Jellyfin cùng chạy trên Ubuntu.

Sau đó Save và restart Jellyfin:

sudo systemctl restart jellyfin
Bước 16 — Đóng port 8096 public

Chỉ làm sau khi HTTPS hoạt động.

Xóa rule public mà chúng ta đã thêm lúc test:

sudo ufw delete allow 8096/tcp

Kiểm tra:

sudo ufw status numbered

Bạn muốn thấy:

8096/tcp ALLOW IN 192.168.1.0/24

hoặc có thể siết chặt hơn nữa nếu muốn Nginx là đường duy nhất.

Sau đó vào VNPT router và xóa port forwarding:

WAN 8096 → 192.168.1.110:8096

Giữ:

WAN 80 → 192.168.1.110:80
WAN 443 → 192.168.1.110:443

Jellyfin sẽ không còn public trực tiếp 8096.

Bước 17 — Kiểm tra từ LAN

Trong mạng nhà:

http://192.168.1.110:8096

vẫn phải vào được.

Ngoài Internet:

https://YOUR_DOMAIN

phải vào được.

Như vậy ta có:

LAN
└── 192.168.1.110:8096
↓
Jellyfin

Internet
└── https://YOUR_DOMAIN
↓
Nginx
↓
Jellyfin
Bước 18 — Kiểm tra WebSocket

Đây là phần nhiều người quên.

Jellyfin dùng WebSocket cho một số chức năng, nên reverse proxy phải chuyển tiếp WebSocket đúng cách.

Sau khi login từ điện thoại:

Dashboard
→ Active Devices

phát một media khi bạn có file.

Nếu UI cập nhật session/playback bình thường thì khá ổn.

Bước 19 — Kiểm tra certificate renewal

Certbot hiện cài cơ chế tự động renewal và chính Certbot khuyến nghị test:

sudo certbot renew --dry-run

Nếu thấy renewal test thành công:

Congratulations, all simulated renewals succeeded

thì phần certificate gần như hoàn tất.

20. Cuối cùng, firewall của bạn nên gần như thế này

Sau khi dọn các port public không cần thiết:

22/tcp → SSH
80/tcp → Nginx
443/tcp → Nginx
8096/tcp → LAN only
445/tcp → LAN only

Và không nên public:

3306
1521
3000
8080

Các port database/dev không có lý do để xuất hiện trên Internet.

Có một điểm mình muốn bạn làm theo kiểu "từng checkpoint"

Đừng chạy tất cả các bước trên một lượt. Làm theo 4 checkpoint:

Checkpoint 1
Nginx :80
↓
http://YOUR_DOMAIN
↓
"Jellyfin reverse proxy setup"
Checkpoint 2
Let's Encrypt
↓
https://YOUR_DOMAIN
↓
🔒
Checkpoint 3
Nginx
↓
Jellyfin :8096
↓
https://YOUR_DOMAIN
↓
Jellyfin login
Checkpoint 4
Remove public :8096
↓
Only 80/443 public

Đừng làm checkpoint 4 trước khi checkpoint 3 thành công.

Và lưu ý thêm: Jellyfin hiện khuyến nghị Nginx/Caddy/Traefik... cho reverse proxy và nhấn mạnh HTTPS khi expose server ra Internet; cấu hình Nginx của họ cũng bao gồm cả WebSocket và proxy_buffering off cho streaming.

Bắt đầu với Checkpoint 1: hãy chạy sudo nginx -t trước. Nếu bạn gửi cho mình output của lệnh đó cùng sudo ss -lntp | grep -E ':(80|443|3000)\b', mình sẽ dựa đúng vào cấu hình Nginx hiện tại của máy bạn để chỉ tiếp bước tiếp theo, tránh đè nhầm config đang chạy.
