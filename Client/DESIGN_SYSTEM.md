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

- **Font Family**: Hệ thống Font Stack cao cấp.
  `"Avenir Next", BlinkMacSystemFont, -apple-system, "Segoe UI", Roboto, Helvetica, Arial, sans-serif`
- **Quy tắc**:
   - Headings: Bold (700), tracking-tight.
   - Body: Normal (400), leading-normal.
   - Buttons: Bold (700).

## 3. Thành phần UI (Components)

### Buttons

- **Primary**: Background `#006ce4`, Text white, `rounded-sm`.
- **Secondary**: Border `#006ce4`, Text `#006ce4`, `rounded-sm`.
- **Ghost**: No background/border, Text `#006ce4`, hover `bg-blue-50`.

### Inputs

- **Standard**: Border `#d9d9d9`, Text `#1a1a1a`, `rounded-sm`, hover/focus border `#006ce4`.

## 4. Kiến trúc dữ liệu (Data Architecture)

### API Integration

- **Client**: Luôn sử dụng `apiClient` từ `@/lib/api/apiClient`.
- **Nguyên tắc**:
   - KHÔNG sử dụng `fetch` hoặc `axios` trực tiếp trong components.
   - Sử dụng `withCredentials: true` cho các yêu cầu cần session.
   - Xử lý lỗi tập trung qua Interceptors (đã có Toast thông báo).

### State Management

- **Zustand**: Sử dụng `useAuthStore` để quản lý trạng thái đăng nhập.
- **Persistence**: Token và thông tin user được lưu vào `localStorage` qua middleware `persist`.
- **Hydration Safety**: Luôn sử dụng trạng thái `mounted` (với `useEffect` và `setTimeout`) để ngăn chặn lỗi mismatch giữa Server và Client khi truy cập dữ liệu từ Store trong quá trình hydration.

## 5. Cấu trúc thư mục (Folder Structure)

- `src/components`: UI components dùng chung.
- `src/store`: Quản lý trạng thái toàn cục (Zustand).
- `src/lib/api`: Cấu hình API Client.
- `src/app`: Routes và Pages (Next.js App Router).

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
