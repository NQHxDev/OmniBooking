"use client";

import Image from "next/image";
import Link from "next/link";
import { useParams, useRouter } from "next/navigation";
import { Mail, Lock, User, ArrowRight, ChevronLeft } from "lucide-react";

import { useState } from "react";
import { useAuthStore, type User as AuthUser } from "@/store/useAuthStore";
import { Loader2 } from "lucide-react";
import apiClient from "@/lib/api/apiClient";
import { toast } from "sonner";

export default function AuthPage() {
   const params = useParams();
   const router = useRouter();
   const mode = params?.mode as string;
   const isLogin = mode === "login";

   const setAuth = useAuthStore((state) => state.setAuth);

   const [formData, setFormData] = useState({
      email: "",
      password: "",
      fullName: "",
   });
   const [loading, setLoading] = useState(false);
   const [error, setError] = useState("");

   const handleChange = (e: React.ChangeEvent<HTMLInputElement>) => {
      setFormData({ ...formData, [e.target.name]: e.target.value });
   };

   const handleToggle = (login: boolean) => {
      setError("");
      router.push(`/auth/${login ? "login" : "register"}`, { scroll: false });
   };

   const handleSubmit = async (e: React.FormEvent) => {
      e.preventDefault();
      setLoading(true);
      setError("");

      const endpoint = isLogin ? "auth/login" : "auth/register";
      const payload = isLogin
         ? { email: formData.email, password: formData.password }
         : { email: formData.email, password: formData.password, fullName: formData.fullName };

      try {
         interface AuthApiResponse {
            status: string;
            data: AuthUser;
            message?: string;
         }

         const result = (await apiClient.post(endpoint, payload, {
            withCredentials: true,
         })) as AuthApiResponse;

         if (result.status === "success") {
            setAuth(result.data);
            if (!isLogin) {
               toast.success("Đăng ký thành công!", {
                  description: "Vui lòng kiểm tra email để xác nhận tài khoản.",
                  duration: 6000,
               });
            }
            router.push("/");
            router.refresh();
         }
      } catch (err: unknown) {
         const errorMessage = err instanceof Error ? err.message : "Đăng nhập thất bại";
         setError(errorMessage);
      } finally {
         setLoading(false);
      }
   };

   return (
      <div className="flex min-h-screen bg-white font-sans text-[#1a1a1a]">
         {/* Left Side: Image & Branding (Keep your UI exactly as is) */}
         <div className="relative hidden w-1/2 overflow-hidden lg:block">
            <Image
               src="/images/hero_banner.png"
               alt="Auth Background"
               fill
               className="object-cover transition-transform duration-10000 hover:scale-110"
               priority
            />
            {/* Overlay Gradient */}
            <div className="absolute inset-0 bg-gradient-to-br from-[#003580]/80 via-[#003580]/40 to-transparent" />

            {/* Branding Overlay */}
            <div className="absolute inset-0 flex flex-col justify-between p-16 text-white">
               {/* Minimalist Logo */}
               <Link
                  href="/"
                  className="flex items-center gap-2 text-2xl font-black tracking-tighter"
               >
                  <span className="tracking-tight">
                     OmniBooking<span className="text-blue-400">.</span>
                  </span>
               </Link>

               <div className="max-w-xl">
                  <div className="mb-6 inline-flex items-center rounded-full bg-white/10 px-4 py-1.5 text-xs font-bold uppercase tracking-widest backdrop-blur-md border border-white/10">
                     Trải nghiệm du lịch thoải mái
                  </div>
                  <h2 className="text-5xl font-extrabold leading-[1.1] tracking-tight font-[family-name:var(--font-be-vietnam-pro)]">
                     Khám phá <span className="text-blue-400">thế giới</span> <br />
                     theo cách của riêng bạn
                  </h2>
                  <p className="mt-6 text-lg text-white/70 leading-relaxed">
                     Tham gia cùng hơn 10 triệu du khách để nhận được các ưu đãi độc quyền và hỗ trợ
                     24/7 trên mọi hành trình.
                  </p>

                  {/* Glassmorphic Feature Cards */}
                  <div className="mt-12 grid grid-cols-2 gap-4">
                     <div className="rounded-2xl bg-white/5 p-4 backdrop-blur-lg border border-white/10">
                        <div className="mb-3 flex h-10 w-10 items-center justify-center rounded-xl bg-blue-500/20 text-blue-400">
                           <svg
                              className="h-6 w-6"
                              fill="none"
                              viewBox="0 0 24 24"
                              stroke="currentColor"
                           >
                              <path
                                 strokeLinecap="round"
                                 strokeLinejoin="round"
                                 strokeWidth={2}
                                 d="M12 8v4l3 3m6-3a9 9 0 11-18 0 9 9 0 0118 0z"
                              />
                           </svg>
                        </div>
                        <h4 className="font-bold text-sm">Đặt phòng cực nhanh</h4>
                        <p className="mt-1 text-xs text-white/50">Xác nhận ngay trong 30 giây</p>
                     </div>
                     <div className="rounded-2xl bg-white/5 p-4 backdrop-blur-lg border border-white/10">
                        <div className="mb-3 flex h-10 w-10 items-center justify-center rounded-xl bg-green-500/20 text-green-400">
                           <svg
                              className="h-6 w-6"
                              fill="none"
                              viewBox="0 0 24 24"
                              stroke="currentColor"
                           >
                              <path
                                 strokeLinecap="round"
                                 strokeLinejoin="round"
                                 strokeWidth={2}
                                 d="M9 12l2 2 4-4m5.618-4.016A11.955 11.955 0 0112 2.944a11.955 11.955 0 01-8.618 3.04A12.02 12.02 0 003 9c0 5.591 3.824 10.29 9 11.622 5.176-1.332 9-6.03 9-11.622 0-1.042-.133-2.052-.382-3.016z"
                              />
                           </svg>
                        </div>
                        <h4 className="font-bold text-sm">Bảo mật tuyệt đối</h4>
                        <p className="mt-1 text-xs text-white/50">Thanh toán an toàn 100%</p>
                     </div>
                  </div>
               </div>

               <div className="flex items-center gap-6 text-xs font-medium text-white/40">
                  <span>© 2026 OmniBooking™</span>
                  <div className="h-1 w-1 rounded-full bg-white/20" />
                  <span>Chính sách bảo mật</span>
                  <div className="h-1 w-1 rounded-full bg-white/20" />
                  <span>Điều khoản sử dụng</span>
               </div>
            </div>
         </div>

         {/* Right Side: Auth Form */}
         <div className="flex w-full flex-col lg:w-1/2">
            {/* Mobile Header */}
            <div className="flex items-center justify-between p-6 lg:hidden">
               <Link href="/" className="text-2xl font-black tracking-tighter text-[#003580]">
                  OmniBooking<span className="text-[#006ce4]">.</span>
               </Link>
               <Link href="/" className="rounded-full bg-zinc-100 p-2">
                  <ChevronLeft className="h-5 w-5" />
               </Link>
            </div>

            <div className="flex flex-1 items-center justify-center px-8 py-12">
               <div className="w-full max-w-[420px]">
                  {/* Back to Home (Desktop) */}
                  <Link
                     href="/"
                     className="mb-8 hidden items-center gap-2 text-sm font-medium text-zinc-500 hover:text-[#006ce4] lg:flex transition-colors"
                  >
                     <ChevronLeft className="h-4 w-4" /> Quay lại trang chủ
                  </Link>

                  {/* Tab Switcher */}
                  <div className="mb-10 flex p-1 bg-zinc-100 rounded-xl">
                     <button
                        onClick={() => handleToggle(true)}
                        className={`flex-1 py-2.5 text-sm font-bold rounded-lg transition-all duration-300 ${isLogin ? "bg-white text-[#006ce4] shadow-sm" : "text-zinc-500 hover:text-zinc-700"}`}
                     >
                        Đăng nhập
                     </button>
                     <button
                        onClick={() => handleToggle(false)}
                        className={`flex-1 py-2.5 text-sm font-bold rounded-lg transition-all duration-300 ${!isLogin ? "bg-white text-[#006ce4] shadow-sm" : "text-zinc-500 hover:text-zinc-700"}`}
                     >
                        Đăng ký
                     </button>
                  </div>

                  <div className="mb-8">
                     <h1 className="text-3xl font-bold tracking-tight">
                        {isLogin ? "Xin chào bạn quay lại!" : "Tạo tài khoản miễn phí"}
                     </h1>
                     <p className="mt-2 text-zinc-500">
                        {isLogin
                           ? "Vui lòng nhập thông tin để truy cập tài khoản của bạn."
                           : "Chỉ mất vài phút để bắt đầu tiết kiệm cho mọi chuyến đi."}
                     </p>
                  </div>

                  {error && (
                     <div className="mb-6 p-4 rounded-xl bg-red-50 border border-red-100 text-red-600 text-sm animate-in fade-in slide-in-from-top-1">
                        {error}
                     </div>
                  )}

                  <form onSubmit={handleSubmit} className="space-y-5">
                     {!isLogin && (
                        <div className="animate-in fade-in slide-in-from-top-2 duration-300">
                           <label className="mb-1.5 block text-sm font-semibold text-zinc-700">
                              Họ và tên
                           </label>
                           <div className="relative">
                              <User className="absolute left-3.5 top-1/2 -translate-y-1/2 h-4 w-4 text-zinc-400" />
                              <input
                                 name="fullName"
                                 type="text"
                                 required
                                 placeholder="Nguyễn Văn A"
                                 value={formData.fullName}
                                 onChange={handleChange}
                                 className="w-full rounded-xl border border-zinc-200 bg-white px-10 py-3.5 text-sm outline-none transition-all focus:border-[#006ce4] focus:ring-4 focus:ring-blue-50"
                              />
                           </div>
                        </div>
                     )}

                     <div>
                        <label className="mb-1.5 block text-sm font-semibold text-zinc-700">
                           Địa chỉ Email
                        </label>
                        <div className="relative">
                           <Mail className="absolute left-3.5 top-1/2 -translate-y-1/2 h-4 w-4 text-zinc-400" />
                           <input
                              name="email"
                              type="email"
                              required
                              placeholder="name@company.com"
                              value={formData.email}
                              onChange={handleChange}
                              className="w-full rounded-xl border border-zinc-200 bg-white px-10 py-3.5 text-sm outline-none transition-all focus:border-[#006ce4] focus:ring-4 focus:ring-blue-50"
                           />
                        </div>
                     </div>

                     <div>
                        <div className="flex justify-between items-center mb-1.5">
                           <label className="text-sm font-semibold text-zinc-700">Mật khẩu</label>
                           {isLogin && (
                              <a
                                 href="#"
                                 className="text-xs font-semibold text-[#006ce4] hover:underline"
                              >
                                 Quên mật khẩu?
                              </a>
                           )}
                        </div>
                        <div className="relative">
                           <Lock className="absolute left-3.5 top-1/2 -translate-y-1/2 h-4 w-4 text-zinc-400" />
                           <input
                              name="password"
                              type="password"
                              required
                              placeholder="••••••••"
                              value={formData.password}
                              onChange={handleChange}
                              className="w-full rounded-xl border border-zinc-200 bg-white px-10 py-3.5 text-sm outline-none transition-all focus:border-[#006ce4] focus:ring-4 focus:ring-blue-50"
                           />
                        </div>
                     </div>

                     <button
                        type="submit"
                        disabled={loading}
                        className="group flex w-full items-center justify-center gap-2 rounded-xl bg-[#006ce4] py-4 text-sm font-bold text-white shadow-xl shadow-blue-200 transition-all hover:bg-[#0057b7] hover:shadow-blue-300 active:scale-[0.98] disabled:opacity-70"
                     >
                        {loading ? (
                           <Loader2 className="h-5 w-5 animate-spin" />
                        ) : (
                           <>
                              {isLogin ? "Đăng nhập ngay" : "Tạo tài khoản ngay"}
                              <ArrowRight className="h-4 w-4 transition-transform group-hover:translate-x-1" />
                           </>
                        )}
                     </button>
                  </form>

                  <div className="relative my-8 text-center">
                     <span className="relative z-10 bg-white px-4 text-xs font-bold uppercase tracking-widest text-zinc-400">
                        Hoặc đăng nhập với
                     </span>
                     <div className="absolute inset-0 top-1/2 border-t border-zinc-100"></div>
                  </div>

                  <div className="grid grid-cols-2 gap-4">
                     <button className="flex items-center justify-center gap-2 rounded-xl border border-zinc-200 py-3.5 text-sm font-bold hover:bg-zinc-50 transition-all">
                        <GoogleIcon className="h-5 w-5" />
                        Google
                     </button>
                     <button className="flex items-center justify-center gap-2 rounded-xl border border-zinc-200 py-3.5 text-sm font-bold hover:bg-zinc-50 transition-all">
                        <FacebookIcon className="h-5 w-5 text-[#1877f2]" />
                        Facebook
                     </button>
                  </div>
               </div>
            </div>
         </div>
      </div>
   );
}

function GoogleIcon({ className }: { className?: string }) {
   return (
      <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 48 48" className={className}>
         <path
            fill="#FFC107"
            d="M43.611 20.083H42V20H24v8h11.303c-1.649 4.657-6.08 8-11.303 8-6.627 0-12-5.373-12-12s5.373-12 12-12c3.059 0 5.842 1.154 7.961 3.039l5.657-5.657C34.046 6.053 29.268 4 24 4 12.955 4 4 12.955 4 24s8.955 20 20 20 20-8.955 20-20c0-1.341-.138-2.65-.389-3.917z"
         />
         <path
            fill="#FF3D00"
            d="m6.306 14.691 6.571 4.819C14.655 15.108 18.961 12 24 12c3.059 0 5.842 1.154 7.961 3.039l5.657-5.657C34.046 6.053 29.268 4 24 4 16.318 4 9.656 8.337 6.306 14.691z"
         />
         <path
            fill="#4CAF50"
            d="M24 44c5.166 0 9.86-1.977 13.409-5.192l-6.19-5.238A11.91 11.91 0 0 1 24 36c-5.202 0-9.619-3.317-11.283-7.946l-6.522 5.025C9.505 39.556 16.227 44 24 44z"
         />
         <path
            fill="#1976D2"
            d="M43.611 20.083H42V20H24v8h11.303a12.04 12.04 0 0 1-4.087 5.571l.003-.002 6.19 5.238C36.971 39.205 44 34 44 24c0-1.341-.138-2.65-.389-3.917z"
         />
      </svg>
   );
}

function FacebookIcon({ className }: { className?: string }) {
   return (
      <svg
         xmlns="http://www.w3.org/2000/svg"
         viewBox="0 0 24 24"
         fill="currentColor"
         className={className}
      >
         <path d="M9.101 23.691v-7.98H6.627v-3.667h2.474v-1.58c0-4.03 1.764-5.908 5.73-5.908.8 0 2.483.21 2.483.21v3.461s-1.151-.02-2.078-.02c-1.45 0-1.834.732-1.834 2.02v1.817h3.654l-.569 3.667h-3.085v7.981H9.101z" />
      </svg>
   );
}
