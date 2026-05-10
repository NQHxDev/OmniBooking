# OmniBooking Design System

Tài liệu này quy định các tiêu chuẩn về giao diện (UI) và trải nghiệm người dùng (UX) cho dự án OmniBooking, dựa trên cảm hứng từ Booking.com nhưng được tối ưu hóa hiện đại hơn.

## 1. Bảng màu (Color Palette)

| Loại               | Mã Màu (HEX) | OKLCH (Tương ứng)      | Mục đích sử dụng                              |
| :----------------- | :----------- | :--------------------- | :-------------------------------------------- |
| **Brand Blue**     | `#003580`    | `oklch(0.28 0.1 260)`  | Header, Hero background, Brand elements       |
| **Action Blue**    | `#006ce4`    | `oklch(0.48 0.18 255)` | Primary Buttons, Links, Main actions          |
| **Accent Yellow**  | `#ffb700`    | `oklch(0.82 0.18 80)`  | Search bar borders, highlights, notifications |
| **Success Green**  | `#008009`    | `oklch(0.46 0.16 145)` | Confirmations, price drops, status            |
| **Text Primary**   | `#1a1a1a`    | `oklch(0.18 0 0)`      | Body text, headings                           |
| **Text Secondary** | `#595959`    | `oklch(0.45 0 0)`      | Captions, labels, faint text                  |
| **Background**     | `#ffffff`    | `oklch(1 0 0)`         | Page background                               |
| **Soft Gray**      | `#f5f5f5`    | `oklch(0.97 0 0)`      | Footers, input backgrounds                    |

## 2. Typo (Typography)

- **Font Family**: Primary: `Be Vietnam Pro` (optimized for Vietnamese).
  Fallbacks: `Inter`, `BlinkMacSystemFont`, `-apple-system`, `Segoe UI`, `Roboto`, `Helvetica`, `Arial`, sans-serif.
- **Quy tắc**:
   - Headings: Bold (700-900), tracking-tighter.
   - Body: Normal (400), leading-relaxed.
   - UI Elements: Medium (500) for labels and buttons.

## 3. Thành phần UI (Components)

### Buttons

- **Primary**: Background `#006ce4`, Text white, `rounded-sm`.
- **Secondary**: Border `#006ce4`, Text `#006ce4`, `rounded-sm`.
- **Ghost**: No background/border, Text `#006ce4`, hover `bg-blue-50`.

### Inputs

- **Standard**: Border `#d9d9d9`, Text `#1a1a1a`, `rounded-sm`, hover/focus border `#006ce4`.
- **Error**: Border `#d4111e`, Text `#d4111e`, bg `#fff8f8`.

## 4. Chuẩn đa ngôn ngữ (i18n Standards)

Để đảm bảo dự án OmniBooking hỗ trợ tốt đa ngôn ngữ (hiện tại là Tiếng Việt và Tiếng Anh), tất cả các thành phần code phải tuân thủ các quy tắc sau:

### 4.1. Tuyệt đối không hardcode chuỗi văn bản

- Mọi chuỗi ký tự hiển thị trên giao diện (labels, placeholders, buttons, messages) **PHẢI** được định nghĩa trong các tệp JSON tại thư mục `/messages`.
- Sử dụng hook `useTranslations` từ `next-intl` để lấy chuỗi dịch.

```tsx
const t = useTranslations("Common");
return <button>{t("login")}</button>;
```

### 4.2. Cấu trúc Key dịch thuật

- Sử dụng kiểu **camelCase** cho các key (ví dụ: `searchPlaceholder`, `listProperty`).
- Phân nhóm rõ ràng:
   - `Common`: Các chuỗi dùng chung (Home, Login, Register...).
   - `Auth`: Các chuỗi liên quan đến xác thực.
   - `Errors`: Các thông báo lỗi (map theo `errorCode` từ Backend).

### 4.3. Xử lý định dạng (Formatting)

- **Ngày tháng**: Sử dụng thư viện `date-fns` kết hợp với `locale` từ `next-intl` để định dạng ngày tháng chuẩn theo quốc gia.
- **Tiền tệ**: Sử dụng `Intl.NumberFormat` để hiển thị giá tiền (VNĐ cho Tiếng Việt, USD/Local cho các ngôn ngữ khác).

### 4.4. Error Messages

- Backend chỉ trả về `errorCode` (ví dụ: `AUTH_001`).
- Frontend sử dụng `errorCode` này làm key để tra cứu trong bộ từ điển `Errors`. điều này giúp thay đổi thông báo lỗi mà không cần sửa code logic.

---

_Lưu ý: Bất kỳ Pull Request nào chứa chuỗi văn bản hardcode hoặc không tuân thủ cấu trúc i18n sẽ bị từ chối._

## 4. Kiến trúc dữ liệu (Data Architecture)

### API Integration

- **Client**: Luôn sử dụng `apiClient` từ `@/lib/api/apiClient`.
- **Nguyên tắc**:
   - KHÔNG sử dụng `fetch` hoặc `axios` trực tiếp trong components.
   - **Idempotency**: Mọi request quan trọng (POST/PUT/PATCH) PHẢI gửi kèm `X-Idempotency-Key` để bảo vệ dữ liệu.
   - Xử lý lỗi tập trung qua Interceptors (đã có Toast thông báo).

### Global Feedback States (Trạng thái phản hồi toàn cục)

- **Loading States**: Sử dụng màn hình loading toàn trang (`loading.tsx`) với 3D Illustration cao cấp và hiệu ứng `animate-bounce` để mang lại trải nghiệm mượt mà khi chuyển trang.
- **Error Boundaries**: Triển khai `error.tsx` chuyên nghiệp. Thông báo lỗi phải thân thiện, có nút "Thử lại" hoặc "Về trang chủ", tuyệt đối không hiển thị lỗi code thô cho người dùng cuối.

## 5. Cấu trúc thư mục (Folder Structure - Feature-based)

Dự án chuyển dịch sang cấu trúc dựa trên Tính năng (Features) để tăng khả năng mở rộng:

- `src/features`: Chứa các module nghiệp vụ (auth, properties, bookings...). Mỗi module có components, hooks, services riêng.
- `src/components/ui`: Các UI components nguyên tử (Shadcn/ui).
- `src/store`: Quản lý trạng thái toàn cục (Zustand).
- `src/lib`: Cấu hình thư viện dùng chung (apiClient, utils...).
- `src/app`: Routes và Layouts (Next.js App Router).

## 6. Nguyên tắc thiết kế (Design Principles)

1. **Tin cậy (Trust)**: Sử dụng các icon minh họa rõ ràng, thông báo bảo mật.
2. **Tối giản (Minimalism)**: Không làm người dùng xao nhãng khỏi mục tiêu chính (Đặt phòng/Đăng ký).
3. **Phản hồi (Feedback)**: Hiệu ứng hover cho mọi thành phần tương tác.

## 7. Thẩm mỹ cao cấp (Rich Aesthetics)

Dự án hướng tới một giao diện hiện đại, sang trọng và tràn đầy sức sống:

- **Animations**: Sử dụng `framer-motion` hoặc Tailwind CSS animations (animate-in, fade-in, slide-in) để tạo cảm giác mượt mà khi chuyển trang hoặc hiển thị component.
- **Micro-interactions**: Hiệu ứng hover cho buttons, cards phải tinh tế (thay đổi độ bóng, scale nhẹ 1.02x).
- **Glassmorphism**: Sử dụng hiệu ứng nền mờ (backdrop-blur) cho các thành phần nổi (overlays, floating menus).
- **Gradients**: Kết hợp các dải màu gradient nhẹ nhàng để tạo chiều sâu, tránh sử dụng màu phẳng đơn điệu.

## 8. Các mẫu Layout phổ biến (Common Layout Patterns)

### Hero 60/40 Split

Sử dụng cho các trang landing quan trọng (Home, Become a Host):

- **Cột Trái (60%)**: Headline lớn, sub-headline và các đặc điểm nổi bật.
- **Cột Phải (40%)**: CTA Card nổi bật với Shadow sâu và Gradient border nhẹ.

### Auth Card

Thẻ đăng nhập/đăng ký tập trung ở giữa màn hình hoặc lệch phải trên Desktop, hỗ trợ chuyển đổi mượt mà giữa các tab Login/Register.

## 9. Thẩm mỹ Partner Hub (Partner Dashboard Aesthetics)

Giao diện dành cho đối tác cần sự chuyên nghiệp, sạch sẽ nhưng vẫn giữ được nét hiện đại:

- **Dashboard Layout**: Sử dụng cấu hình Sidebar cố định (64px/256px) kết hợp Header dính (Sticky).
- **Stats Cards**: Sử dụng các mảng màu Pastel nhẹ nhàng kết hợp icon màu sắc để phân loại dữ liệu (Green cho doanh thu, Blue cho booking).
- **Tables**: Sử dụng bo góc lớn (rounded-3xl), khoảng cách hàng thưa, và hiệu ứng hover hàng (bg-zinc-50/50).
- **Empty States**: Luôn có hình ảnh minh họa (icon lớn) kèm CTA rõ ràng để dẫn dắt người dùng.

## 10. Quy tắc xử lý Hình ảnh (Image Handling)

- **Next.js Image**: Bắt buộc sử dụng `next/image` thay cho thẻ `<img>` truyền thống.
- **Placeholder**: Sử dụng màu nền hoặc Skeleton trong lúc tải ảnh.
- **Preview**: Đối với ảnh vừa upload, sử dụng `URL.createObjectURL` để hiển thị preview tức thì và đừng quên cleanup URL sau khi component unmount.
- **Optimization**: Thiết lập thuộc tính `sizes` phù hợp để Next.js tự động tối ưu hóa kích thước ảnh theo viewport.

## 11. Cấu trúc Routing & Đa ngôn ngữ (Routing & i18n Folder Structure)

Để tránh nhầm lẫn trong quá trình phát triển (như việc sửa nhầm file clone), dự án quy định nghiêm ngặt về cấu trúc thư mục App Router khi kết hợp với `next-intl`:

### 11.1. Bẫy Routing kép (The Dual Routing Trap)

- **Vấn đề**: Khi sử dụng `middleware` để tự động chuyển hướng ngôn ngữ (ví dụ: `/auth` -> `/vi/auth`), có thể tồn tại 2 file `page.tsx` ở `app/auth/page.tsx` và `app/[locale]/auth/page.tsx`.
- **Quy định**:
   - **Tuyệt đối KHÔNG** duy trì 2 file `page.tsx` có cùng chức năng ở cả root `/app` và thư mục `/[locale]`.
   - Mọi Logic hiển thị (UI/UX) phải nằm tập trung tại thư mục **`src/app/[locale]/...`**.
   - Các folder ở root `/app` chỉ nên chứa các file mang tính chất điều hướng (redirect) hoặc các API Route (`route.ts`).

### 11.2. Source of Truth (Nguồn sự thật duy nhất)

- Khi cần chỉnh sửa giao diện một trang (ví dụ trang Auth), lập trình viên **PHẢI** kiểm tra URL đang chạy trên trình duyệt.
- Nếu URL có tiền tố ngôn ngữ (ví dụ: `/vi/auth/login`), file cần sửa chắc chắn nằm trong `src/app/[locale]/auth/[mode]/page.tsx`.
- Xóa bỏ ngay các file "bóng ma" (ghost files) ở thư mục root nếu chúng không còn mục đích sử dụng để tránh gây lúng túng cho team.

### 11.4. Quy tắc phân cấp Layout (Layout Hierarchy)

Để tối ưu hóa hiệu suất và tránh lỗi runtime "Missing html/body tags", dự án áp dụng quy tắc phân tầng layout như sau:

- **Root Layout (`src/app/layout.tsx`)**:
   - Đóng vai trò là "Vỏ bọc tối cao" (Master Shell).
   - Chứa các thẻ `<html>`, `<body>`, import `globals.css` và cấu hình Font chính (`Be_Vietnam_Pro`).
   - Giúp đảm bảo các trang lỗi hệ thống (404, 500) nằm ngoài `[locale]` vẫn có đầy đủ style và không bị crash.
- **Locale Layout (`src/app/[locale]/layout.tsx`)**:
   - Là layout lồng nhau (Nested Layout), **không** được chứa thẻ `<html>` và `<body>` để tránh lỗi nested tags.
   - Chứa các Provider đặc thù cho ngôn ngữ (`NextIntlClientProvider`) và các thành phần UI toàn cục như `Toaster`.

### 11.5. Xử lý trang lỗi (Error & Not Found Handling)

- **Trang 404 toàn cục (`src/app/not-found.tsx`)**: Dùng cho các lỗi xảy ra khi Next.js không thể xác định được locale hoặc path nằm ngoài matcher của middleware. Phải đảm bảo trang này tự thân đầy đủ style (thừa hưởng từ Root Layout).
- **Trang 404 theo vùng (`src/app/[locale]/not-found.tsx`)**: Dùng khi user truy cập sai path bên trong một ngôn ngữ cụ thể. Trang này có quyền sử dụng các hook đa ngôn ngữ (`useTranslations`).

### 11.6. Danh sách các Folder đã chuẩn hóa

Các tính năng sau đã được chuyển hoàn toàn vào trong `src/app/[locale]/` để hỗ trợ đa ngôn ngữ và bảo mật:

- `become-a-host`: Trang đăng ký đối tác mới.
- `partner`: Dashboard và quản lý tài sản của đối tác.
- `profile`: Thông tin cá nhân người dùng.
- `auth/verify`: Trang xác thực email/tài khoản.

### 11.7. UX Pointer & Interaction

- Tất cả các thành phần tương tác được (Buttons, Links, Labels liên kết với Input) **PHẢI** có class `cursor-pointer`.
- Sử dụng `htmlFor` cho nhãn (Label) và `id` cho ô nhập liệu (Input) để tối ưu hóa trải nghiệm click-to-focus.

---

_Lưu ý: Sự minh bạch trong cấu trúc thư mục quan trọng tương đương với chất lượng mã nguồn._
