import { defineRouting } from "next-intl/routing";
import { createNavigation } from "next-intl/navigation";

export const routing = defineRouting({
   // Các ngôn ngữ hỗ trợ
   locales: ["vi", "en"],

   // Ngôn ngữ mặc định nếu không nhận diện được
   defaultLocale: "vi",

   // Tự động nhận diện ngôn ngữ dựa trên trình duyệt
   localeDetection: true,

   // Luôn hiển thị prefix trên URL để tránh nhầm lẫn cho người dùng quốc tế
   localePrefix: "always",
});

export const { Link, redirect, usePathname, useRouter, getPathname } = createNavigation(routing);
