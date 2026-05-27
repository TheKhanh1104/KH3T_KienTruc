# Hướng Dẫn Chạy Các Chức Năng Dự Án KH3T Shop

Dưới đây là hướng dẫn nhanh cách chạy từng thành phần (Task 1 đến Task 5) trong dự án.

---

## 🐳 Task 1: Chạy Docker & Docker Compose
Hệ thống sử dụng các Dockerfile có sẵn để chạy 6 container (MySQL, Redis, Backend, Frontend, Nginx Gateway, Jenkins).

**Lệnh khởi chạy:**
```bash
docker-compose up -d --build
```
* **Giao diện Frontend (Nginx Gateway):** Truy cập `http://localhost`
* **API Backend:** Truy cập `http://localhost/api/...`

---

## 🏗️ Task 2: Chạy Jenkins CI/CD
Jenkins chạy tự động tại cổng `8081` sau khi bật Docker Compose.

1. **Lấy mật khẩu Admin để kích hoạt:**
   ```bash
   docker exec -it kh3t-jenkins cat /var/jenkins_home/secrets/initialAdminPassword
   ```
2. **Cấu hình trên Jenkins Dashboard (`http://localhost:8081`):**
   * Đăng nhập -> Cài đặt các plugin gợi ý (Suggested Plugins).
   * Cài đặt thêm plugin: **NodeJS**, **Maven Integration**.
   * Cấu hình JDK 21, Maven 3, NodeJS 20 trong *Manage Jenkins* -> *Tools* (đặt tên trùng với khai báo trong [Jenkinsfile](file:///c:/Users/thanh/Desktop/ThietkeHeThong/DoAn/Jenkinsfile): `JDK21`, `Maven3`, `NodeJS20`).
3. **Chạy Pipeline:**
   * Tạo **New Item** -> Kiểu **Pipeline**.
   * Phần **Pipeline Script from SCM**: Chọn **Git**, nhập link repo của bạn và chọn script path là `Jenkinsfile`. Bấm **Build Now**.

---

## 🦊 Task 3: Chạy GitLab CI/CD
File cấu hình [.gitlab-ci.yml](file:///c:/Users/thanh/Desktop/ThietkeHeThong/DoAn/.gitlab-ci.yml) đã có sẵn. Để chạy tự động khi push code:

1. **Cài đặt GitLab Runner** trên máy/server của bạn.
2. **Đăng ký Runner với GitLab:**
   ```bash
   gitlab-runner register
   ```
   * Nhập URL GitLab và Registration Token lấy từ mục *Settings > CI/CD > Runners* trên GitLab của bạn.
   * Chọn executor là `docker` (sử dụng image `docker:latest` và bind volume `/var/run/docker.sock:/var/run/docker.sock` để runner có quyền build container).

---

## ⚙️ Task 4: Chạy Terraform (Khởi tạo container test)
Thư mục [terraform](file:///c:/Users/thanh/Desktop/ThietkeHeThong/DoAn/terraform) đã cấu hình sẵn nhà cung cấp Docker offline qua file [terraform.rc](file:///c:/Users/thanh/Desktop/ThietkeHeThong/DoAn/terraform/terraform.rc).

**Các lệnh thực thi:**
```bash
cd terraform
terraform init
terraform apply -auto-approve
```
* Container test Nginx sẽ được khởi chạy tại cổng `8000` (`http://localhost:8000`).
* Để xóa container test:
  ```bash
  terraform destroy -auto-approve
  ```

---

## 🚀 Task 5: Triển khai lên Internet (Deploy Online)

### 1. Triển khai Backend (Render)
* Tạo dịch vụ **Web Service** trên [Render](https://render.com).
* Chọn Repo, set *Root Directory* là `kh3tshop-be` và *Runtime* là `Docker`.
* Cấu hình biến môi trường kết nối Database của bạn (ví dụ: `SPRING_DATASOURCE_URL`).

### 2. Triển khai Frontend (Vercel)
* Tạo project trên [Vercel](https://vercel.com) liên kết với Repo.
* Chọn *Root Directory* là `kh3tshop-be/src/main/resources/static/kh3tshop-fe`.
* Vercel sẽ tự động nhận diện file [vercel.json](file:///c:/Users/thanh/Desktop/ThietkeHeThong/DoAn/kh3tshop-be/src/main/resources/static/kh3tshop-fe/vercel.json) để chuyển tiếp request `/api/*` về link backend trên Render mà không bị lỗi CORS.
