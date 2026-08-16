Với phần cứng hiện tại, mình khuyên bạn giữ Ubuntu Server làm nền, không cài lại sang TrueNAS/Unraid. Mô hình phù hợp nhất là:

                    LAN / Wi-Fi
                        │
            ┌───────────┴───────────┐
            │                       │
      PC / Laptop               TV / Phone
            │                       │
         SMB/NAS                 Jellyfin
            │                       │
            └───────────┬───────────┘
                        │
                 Ubuntu Server
                 ┌──────────────┐
                 │ Samba        │ ← chia sẻ file
                 │ Jellyfin     │ ← xem phim
                 │ SSH          │
                 └──────┬───────┘
                        │
                 USB 3.x Dock
                        │
                 WD Red Plus 2TB

Jellyfin hỗ trợ Intel Quick Sync/VA-API trên Linux, nên nếu CPU của laptop có iGPU Intel đủ khả năng thì phần transcoding có thể được đẩy sang iGPU thay vì bắt CPU 2C/4T xử lý toàn bộ.

1. Kiến trúc mình khuyên dùng

Không cần Docker ở giai đoạn đầu.

Ubuntu Server

Chạy:

Samba → biến máy thành NAS
Jellyfin → thư viện phim
SSH → quản trị
UFW → firewall
smartmontools → kiểm tra HDD
systemd → tự mount HDD khi boot
HDD 256GB trong laptop

Dùng cho:

OS
Jellyfin database
Jellyfin metadata/cache
logs
HDD WD Red Plus 2TB

Dùng chỉ cho dữ liệu media:

/srv/media/
├── movies/
├── tv/
├── anime/
├── music/
└── photos/

Điều này rất quan trọng: đừng để Jellyfin metadata/cache chạy trên HDD 2TB nếu SSD/HDD 256GB còn đủ chỗ. Database nhỏ nhưng nhiều file nhỏ, để trên system disk sẽ phản hồi tốt hơn.

2. Trước tiên kiểm tra server

SSH vào server:

ssh <username>@<IP_SERVER>

Kiểm tra Ubuntu:

lsb_release -a

Kiểm tra CPU:

lscpu

Kiểm tra GPU/iGPU:

lspci | grep -Ei "vga|3d|display"

Kiểm tra RAM:

free -h

Kiểm tra ổ:

lsblk -o NAME,SIZE,MODEL,FSTYPE,TYPE,MOUNTPOINTS

Ví dụ:

NAME SIZE MODEL FSTYPE MOUNTPOINTS
sda 238G Samsung SSD ext4 /
sdb 1.8T WDC WD20EFRX ext4

Bước này rất quan trọng vì không được nhầm /dev/sda với /dev/sdb.

Đặc biệt, nếu HDD 2TB đã có dữ liệu thì tuyệt đối không chạy mkfs hoặc parted mklabel lên nó.

3. Cố định IP cho NAS

Đây là việc mình rất khuyên làm.

Ví dụ server hiện tại:

192.168.1.50

Bạn nên vào router và tạo:

DHCP Reservation
MAC của laptop → 192.168.1.50

Ưu điểm là:

PC → \\192.168.1.50\Media
Jellyfin → http://192.168.1.50:8096
TV → 192.168.1.50

Sau reboot IP vẫn giữ nguyên.

Mình thích DHCP reservation trên router hơn là hard-code IP trong Netplan, đặc biệt với máy gia đình.

4. Đặt hostname

Ví dụ đặt:

nas

Chạy:

sudo hostnamectl set-hostname nas

Kiểm tra:

hostname

Bạn có thể dùng:

nas.local

nếu mạng/router hỗ trợ mDNS, nhưng để ổn định nhất vẫn nên nhớ IP tĩnh/reservation.

5. Cài các package cơ bản
   sudo apt update
   sudo apt upgrade -y

Cài:

sudo apt install -y \
 samba \
 smartmontools \
 acl \
 curl \
 ca-certificates \
 ufw \
 lm-sensors

Ubuntu hiện vẫn hướng dẫn Samba là giải pháp chuẩn để Ubuntu chia sẻ file tới Windows và các client khác trong LAN.

6. Kiểm tra HDD 2TB

Giả sử HDD là:

/dev/sdb

Kiểm tra:

lsblk -f

Kiểm tra SMART:

sudo smartctl -a /dev/sdb

Nếu dock USB không cho truy cập SMART, thử:

sudo smartctl -d sat -a /dev/sdb

Bạn nên chú ý:

SMART overall-health
Reallocated_Sector_Ct
Current_Pending_Sector
Offline_Uncorrectable
Power_On_Hours
Temperature

Đừng dùng HDD cho phim trước khi kiểm tra tình trạng ổ nếu bạn chưa biết nó đang khỏe hay đã có lỗi.

7. Nếu HDD 2TB hoàn toàn mới / không cần dữ liệu cũ

Giả sử:

/dev/sdb

CẢNH BÁO: các lệnh dưới đây xóa toàn bộ dữ liệu trên /dev/sdb.

-Tạo GPT:

sudo parted /dev/sdb --script mklabel gpt

+Nếu đã mount trước đó:
sudo umount /dev/sdb\*
sudo partprobe /dev/sdb
Rồi chạy lại lệnh trên

-Tạo một partition:

sudo parted /dev/sdb --script mkpart primary ext4 0% 100%

-Kiểm tra:

lsblk

Bạn sẽ có:

/dev/sdb1

Format:

sudo mkfs.ext4 -m 0 -L MEDIA /dev/sdb1 (wait for a while)

Mình chọn ext4 vì đây là một filesystem đơn giản, ổn định và rất phù hợp cho một disk media duy nhất.

8. Tạo thư mục mount
   sudo mkdir -p /srv/media

Lấy UUID:

sudo blkid /dev/sdb1

Ví dụ:

/dev/sdb1: UUID="abcd-1234-...." TYPE="ext4" LABEL="MEDIA"

Mở:

sudo nano /etc/fstab

Thêm:

UUID=abcd-1234-xxxx-xxxx /srv/media ext4 defaults,noatime 0 2

Thay UUID bằng UUID thật.

noatime giúp giảm các lần ghi metadata không cần thiết khi đọc thư viện phim.

Test:

sudo mount -a

Kiểm tra:

df -h

Bạn cần thấy:

/dev/sdb1 1.8T ... /srv/media
Đây là một trong những bước quan trọng nhất

Không nên dùng:

/dev/sdb1 /srv/media ...

vì thứ tự /dev/sdX có thể thay đổi sau reboot.

UUID sẽ ổn định hơn.

9. Tạo cấu trúc thư mục media
   sudo mkdir -p /srv/media/movies
   sudo mkdir -p /srv/media/tv
   sudo mkdir -p /srv/media/anime
   sudo mkdir -p /srv/media/music
   sudo mkdir -p /srv/media/photos

Ví dụ:

/srv/media/
├── movies/
│ ├── Movie A (2025)/
│ │ └── Movie A (2025).mkv
│ └── Movie B (2024)/
│
├── tv/
│ ├── Breaking Bad/
│ │ ├── Season 01/
│ │ └── Season 02/
│
├── anime/
├── music/
└── photos/

Cấu trúc này rất thân thiện với Jellyfin.

10. Tạo group chung cho media
    sudo groupadd --system media

Nếu báo group đã tồn tại thì bỏ qua.

Đặt ownership:

sudo chown -R root:media /srv/media

Folder:

sudo find /srv/media -type d -exec chmod 2775 {} \;

File:

sudo find /srv/media -type f -exec chmod 0664 {} \;

2 trong 2775 giúp các file/folder mới tiếp tục kế thừa group media.

11. Cài Jellyfin

Jellyfin hiện cung cấp repository chính thức cho Debian/Ubuntu. Tài liệu hiện tại khuyến nghị dùng repository chính thức thay vì tải .deb thủ công; Jellyfin cũng cung cấp script cài đặt cho Ubuntu/Debian.

Cài curl nếu chưa có:

sudo apt update
sudo apt install -y curl

Tải script và kiểm tra checksum:

curl -s https://repo.jellyfin.org/install-debuntu.sh -O
curl -s https://repo.jellyfin.org/install-debuntu.sh.sha256sum -O

sha256sum -c install-debuntu.sh.sha256sum

Bạn cần thấy:

install-debuntu.sh: OK

Sau đó:

sudo bash install-debuntu.sh

Kiểm tra:

systemctl status jellyfin

12. Cho Jellyfin đọc được HDD

Sau khi cài Jellyfin:

id jellyfin

Thêm Jellyfin vào group:

sudo usermod -aG media jellyfin

Kiểm tra:

groups jellyfin

Phải thấy:

media

Restart:

sudo systemctl restart jellyfin

Test quyền:

sudo -u jellyfin ls /srv/media

Phải ra:

movies
tv
anime
music
photos

13. Truy cập Jellyfin

Từ máy khác trong LAN:

http://192.168.1.50:8096

Thay IP bằng IP thật của server.

Lần đầu Jellyfin sẽ cho bạn tạo:

Admin account
Language
Libraries
Metadata settings

14. Tạo Movies library

Trong Jellyfin:

Dashboard
→ Libraries
→ Add Media Library

Chọn:

Content type:
Movies

Folder:

/srv/media/movies

Sau đó:

TV Shows
→ /srv/media/tv

Anime:

Other
→ /srv/media/anime

Music:

Music
→ /srv/media/music

15. Samba — biến server thành NAS thực sự

Tạo user riêng cho NAS.

Ví dụ:

sudo useradd -M -s /usr/sbin/nologin nas

Thêm vào group:

sudo usermod -aG media nas

Tạo password Samba:

sudo smbpasswd -a nas

Enable account:

sudo smbpasswd -e nas

Như vậy bạn có:

Linux user : nas
Samba user : nas
Group : media

16. Cấu hình Samba

Backup config trước:

sudo cp /etc/samba/smb.conf /etc/samba/smb.conf.backup

Mở:

sudo nano /etc/samba/smb.conf

Ở cuối file thêm:

[Media]
comment = Media Storage
path = /srv/media
browseable = yes
read only = no
guest ok = no
valid users = nas
force group = media
create mask = 0664
force create mode = 0660
directory mask = 2775
force directory mode = 2770

Mình không khuyến nghị guest ok = yes cho NAS gia đình nếu không có lý do đặc biệt. Tài liệu Ubuntu có ví dụ guest share, nhưng đó là cấu hình mở cho client trong mạng; với NAS cá nhân nên sử dụng user/password.

17. Kiểm tra Samba config

sudo testparm

Nếu cuối cùng không có lỗi nghiêm trọng:

sudo systemctl enable smbd
sudo systemctl restart smbd

Kiểm tra:

systemctl status smbd

18. Truy cập NAS từ Windows

Trên Windows:

Win + R

Nhập:

\\192.168.1.50\Media

Windows sẽ hỏi:

Username: nas
Password: <password Samba>

Sau đó bạn sẽ thấy:

movies
tv
anime
music
photos

Bạn có thể map thành:

Z:

Ví dụ:

Z: → \\192.168.1.50\Media

19. Firewall

Trước khi bật firewall, đảm bảo SSH đang hoạt động.

Ví dụ LAN của bạn:

192.168.1.0/24

Cho SSH:

sudo ufw allow ssh

Jellyfin:

sudo ufw allow from 192.168.1.0/24 to any port 8096 proto tcp

Samba:

sudo ufw allow from 192.168.1.0/24 to any port 445 proto tcp

Sau đó:

sudo ufw enable

Kiểm tra:

sudo ufw status

Bạn không cần mở Jellyfin/Samba ra Internet trong cấu hình NAS gia đình cơ bản.

20. Cấu hình Hardware Acceleration cho Jellyfin

Đây là phần rất đáng làm với server của bạn.

Jellyfin cho biết transcoding bằng CPU có thể rất nặng; Intel Quick Sync/VA-API là một trong các phương thức tăng tốc được hỗ trợ trên Linux.

Trên server:

lspci | grep -Ei "vga|3d|display"

Ví dụ nếu thấy:

Intel Corporation HD Graphics ...

thì có khả năng sử dụng iGPU.

Kiểm tra:

ls -l /dev/dri

Bạn muốn thấy dạng:

card0
renderD128

Tài liệu Jellyfin cũng yêu cầu kiểm tra /dev/dri/renderD\* và quyền truy cập device khi cấu hình Intel GPU trên Ubuntu/Debian.

Kiểm tra group:

getent group render

Sau đó:

sudo usermod -aG render jellyfin
sudo usermod -aG video jellyfin

Restart:

sudo systemctl restart jellyfin

Trong Jellyfin:

Dashboard
→ Playback
→ Transcoding

Chọn:

Hardware acceleration:
Intel Quick Sync (QSV)

hoặc lựa chọn tương ứng mà Jellyfin hiển thị.

Test bằng:

Dashboard → Playback

khi đang xem video.

21. Nhưng ưu tiên Direct Play

Đây là điểm rất quan trọng với cấu hình 2C/4T.

Bạn không nên thiết kế NAS theo tư duy:

"Jellyfin sẽ transcode mọi video."

Mà nên:

Direct Play
↓
Direct Stream
↓
Hardware Transcoding
↓
CPU Transcoding

Ưu tiên trên xuống dưới.

Ví dụ:

File:
4K HEVC
HDR
10-bit
H.265
AAC/AC3
MKV

TV hỗ trợ:
HEVC + HDR

→ Direct Play.

CPU gần như không phải làm gì ngoài việc đọc file và truyền qua mạng.

Đây là cách để server 2C/4T vẫn hoạt động rất ổn.

22. Mạng LAN quan trọng hơn CPU

Nếu có thể, hãy kết nối laptop server bằng:

Ethernet Gigabit

thay vì:

Wi-Fi

Topology tốt:

           Router / Switch
           ┌──────┼───────┐
           │      │       │
          NAS     PC     TV
           │
       USB 3.x Dock
           │
        HDD 2TB

Gigabit Ethernet thực tế khoảng gần 1 Gbps là quá đủ cho phần lớn phim, kể cả file bitrate cao.

Không nên:

NAS
↓
Wi-Fi 2.4GHz
↓
TV

nếu bạn muốn xem 4K bitrate cao ổn định.

23. USB Dock của HDD

Đây là một điểm mình đặc biệt lưu ý.

HDD của bạn:

WD Red Plus 2TB
↓
USB Dock
↓
Laptop

không phải NAS-grade connection thực sự.

Nhưng để xem phim thì vẫn hoàn toàn có thể đủ, miễn là:

Dock hỗ trợ USB 3.x
dùng nguồn riêng nếu dock yêu cầu
dây USB tốt
không bị disconnect ngẫu nhiên
HDD không quá nóng
server không đi vào sleep

Kiểm tra USB:

lsusb -t

Bạn muốn thấy thiết bị chạy ở:

5000M

hoặc cao hơn, thay vì:

480M

- Nếu tháo lắp USB, cần mount lại:
  mkdir -p /srv/media
  mount -a
  lsblk -o NAME,SIZE,MODEL,FSTYPE,MOUNTPOINTS
  df -h /srv/media

24. Tắt sleep khi đóng nắp laptop

Đây là lỗi rất dễ gặp khi biến laptop thành server.

Kiểm tra:

grep -i HandleLidSwitch /etc/systemd/logind.conf

Mở:

sudo nano /etc/systemd/logind.conf

Đặt:

HandleLidSwitch=ignore
HandleLidSwitchExternalPower=ignore
HandleLidSwitchDocked=ignore

Sau đó:

sudo systemctl restart systemd-logind

Cẩn thận: restart systemd-logind có thể làm phiên SSH hiện tại bị ảnh hưởng. Cách an toàn nhất là reboot sau khi cấu hình.

25. Cho máy tự boot sau mất điện

Nếu BIOS/UEFI của laptop có:

Restore AC Power

hoặc:

Power On After AC Loss

thì nên bật.

Mục tiêu:

Mất điện
↓
Điện trở lại
↓
Laptop tự bật
↓
Ubuntu boot
↓
HDD mount
↓
Samba chạy
↓
Jellyfin chạy

Đó mới là NAS có tính tự phục hồi tương đối tốt.

26. Kiểm tra toàn bộ stack

Sau khi hoàn tất:

-Disk
lsblk -f
df -h
-Mount
mount | grep /srv/media
-Samba
systemctl status smbd
-Jellyfin
systemctl status jellyfin
-Firewall
sudo ufw status
-HDD
sudo smartctl -a /dev/sdb
-GPU
ls -l /dev/dri

27. Cấu hình thư mục cuối cùng

Mình khuyên bạn giữ layout như sau:

/
├── ...
│
└── srv/
└── media/
├── movies/
├── tv/
├── anime/
├── music/
└── photos/

Trong đó:

/srv/media
│
├── Samba
│ └── \\nas\Media
│
└── Jellyfin
├── Movies
├── TV Shows
├── Anime
└── Music

28. Rất quan trọng: hiện tại NAS của bạn chưa có backup

Bạn đang có:

1 HDD 2TB

nên hiện tại không có RAID và cũng không có redundancy.

Nếu HDD chết:

HDD 2TB chết
↓
Phim mất

RAID1 cũng không phải backup, nhưng với 2 HDD thì sau này bạn có thể cân nhắc mirror.

Ở giai đoạn hiện tại mình khuyên:

Laptop 256GB
├── Ubuntu
├── Jellyfin DB
└── Config

HDD 2TB
└── Media

Sau này thêm:

HDD thứ 2
↓
Backup

hoặc:

HDD thứ 2
↓
RAID1 / mirror

Tuyệt đối đừng nghĩ:

NAS = backup

29. Có nên dùng Docker không?

Có thể, nhưng mình chưa khuyên ở giai đoạn này.

Với server của bạn:

2C / 4T
16GB RAM
Ubuntu Server

bare-metal:

Ubuntu
├── Samba
└── Jellyfin

là đơn giản nhất.

Docker sẽ hợp lý hơn khi bạn bắt đầu có:

Jellyfin
Sonarr
Radarr
Prowlarr
qBittorrent
Bazarr
Nginx Proxy Manager
...

Lúc đó Docker Compose rất tiện.

30. Cấu hình mục tiêu mình đề xuất

Nếu làm từ đầu, mình sẽ đưa máy của bạn về trạng thái:

┌───────────────────────────────────────────┐
│ UBUNTU SERVER │
│ │
│ SSH │
│ UFW │
│ │
│ Samba │
│ └── /srv/media │
│ │
│ Jellyfin │
│ ├── Movies │
│ ├── TV Shows │
│ ├── Anime │
│ └── Music │
│ │
└───────────────────┬───────────────────────┘
│
USB 3.x
│
┌───────▼────────┐
│ WD Red Plus 2TB│
│ ext4 │
└────────────────┘

Và luồng sử dụng:

Windows PC
└── SMB → lưu/copy phim

TV / Android TV / Phone
└── Jellyfin → xem phim

Jellyfin
└── Direct Play nếu client hỗ trợ

Nếu phải transcode
└── Intel QSV/iGPU
└── nếu CPU của bạn hỗ trợ

Jellyfin hiện khuyến nghị sử dụng GPU/iGPU trên Linux khi cần transcoding; Intel Graphics là một lựa chọn đặc biệt phù hợp cho Linux.

Mình đề xuất thứ tự triển khai thực tế

Đừng làm tất cả cùng lúc. Làm theo thứ tự này sẽ ít lỗi nhất:

1. Cố định IP
   ↓
2. Kiểm tra HDD + SMART
   ↓
3. Partition + ext4
   ↓
4. /etc/fstab + mount
   ↓
5. Permission + media group
   ↓
6. Samba
   ↓
7. Test copy file từ Windows
   ↓
8. Jellyfin
   ↓
9. Hardware acceleration
   ↓
10. Firewall
    ↓
11. Disable sleep/lid
    ↓
12. Reboot test
    ↓
13. Test xem phim 1080p
    ↓
14. Test 4K Direct Play
    ↓
15. Test transcoding

Riêng bước Hardware Acceleration, mình cần biết chính xác model CPU của laptop để xác định iGPU Intel thuộc thế hệ nào, có QSV cho codec nào và cấu hình Jellyfin nên chọn QSV hay VA-API. Bạn chỉ cần chạy:

lscpu | grep "Model name"

và

lspci | grep -Ei "vga|3d|display"

rồi gửi 2 kết quả đó. Mình có thể tiếp tục chỉ cho bạn cấu hình Jellyfin tối ưu cho đúng CPU của máy, bao gồm 1080p/4K, HEVC 10-bit, HDR, subtitle và giới hạn transcoding.

-Test transcode:
sudo apt install -y intel-gpu-tools
sudo intel_gpu_top
