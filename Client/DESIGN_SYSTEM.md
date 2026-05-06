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

- Border: `1px solid #868686`.
- Focus: `outline: 2px solid #006ce4`, `outline-offset: -1px`.
- Border-radius: `4px` (sm).

### Layout

- Max-width: `1280px` (`max-w-7xl`).
- Spacing: Sử dụng hệ thống Tailwind (px, py, gap).

## 4. Nguyên tắc thiết kế (Design Principles)

1. **Tin cậy (Trust)**: Sử dụng các icon minh họa rõ ràng, thông báo bảo mật.
2. **Tối giản (Minimalism)**: Không làm người dùng xao nhãng khỏi mục tiêu chính (Đặt phòng/Đăng ký).
3. **Phản hồi (Feedback)**: Hiệu ứng hover cho mọi thành phần tương tác.
