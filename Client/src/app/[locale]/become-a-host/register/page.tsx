"use client";

import { useEffect, useState, useRef } from "react";
import { Lock, ArrowLeft, ShieldCheck, FileText, CheckCircle2 } from "lucide-react";
import Link from "next/link";
import { useRouter } from "next/navigation";
import { partnerService } from "@/lib/api/services/partnerService";
import PartnerNavbar from "@/components/PartnerNavbar";
import { useAuthStore } from "@/store/useAuthStore";
import { toast } from "sonner";

type Step = "VERIFY" | "TERMS" | "SUCCESS";

export default function PartnerRegisterPage() {
   const router = useRouter();
   const { isLoggedIn, setAuth } = useAuthStore();
   const [mounted, setMounted] = useState(false);
   const [step, setStep] = useState<Step>("VERIFY");
   const [code, setCode] = useState("");
   const [isAgreed, setIsAgreed] = useState(false);
   const [countdown, setCountdown] = useState(5);
   const [resendCountdown, setResendCountdown] = useState(0);
   const [isResending, setIsResending] = useState(false);
   const hasRequestedOtp = useRef(false);

   const handleSendOtp = async (isManual = false) => {
      if (!isManual && hasRequestedOtp.current) return;
      if (isManual) setIsResending(true);
      else hasRequestedOtp.current = true;

      // Start countdown immediately to prevent spamming
      setResendCountdown(30);

      try {
         await partnerService.sendOtp();
         toast.success("Mã xác thực đã được gửi đến email của bạn!", {
            description: "Vui lòng kiểm tra hộp thư đến hoặc thư rác!",
            duration: 5000,
         });
      } catch (err) {
         console.error("Failed to send OTP", err);
         if (!isManual) hasRequestedOtp.current = false;
         toast.error("Không thể gửi mã xác thực. Vui lòng thử lại sau.");
      } finally {
         if (isManual) setIsResending(false);
      }
   };

   useEffect(() => {
      if (mounted && !isLoggedIn) {
         router.push("/auth/login?callbackUrl=/become-a-host/register");
      }
   }, [mounted, isLoggedIn, router]);

   useEffect(() => {
      const timer = setTimeout(() => setMounted(true), 0);

      // Initial send
      if (!hasRequestedOtp.current) {
         handleSendOtp(false);
      }

      return () => clearTimeout(timer);
   }, []);

   useEffect(() => {
      let timer: NodeJS.Timeout;
      if (step === "SUCCESS" && countdown > 0) {
         timer = setInterval(() => {
            setCountdown((prev) => prev - 1);
         }, 1000);
      } else if (step === "SUCCESS" && countdown === 0) {
         router.push("/partner/dashboard");
      }
      return () => clearInterval(timer);
   }, [step, countdown, router]);

   useEffect(() => {
      let timer: NodeJS.Timeout;
      if (resendCountdown > 0) {
         timer = setInterval(() => {
            setResendCountdown((prev) => prev - 1);
         }, 1000);
      }
      return () => clearInterval(timer);
   }, [resendCountdown]);

   const handleVerify = async () => {
      try {
         await partnerService.verifyOtp(code);
         setStep("TERMS");
         toast.success("Xác thực email thành công!");
      } catch {
         toast.error("Mã xác thực không đúng hoặc đã hết hạn.");
      }
   };

   const handleComplete = async () => {
      if (!isAgreed) {
         toast.error("Vui lòng đồng ý với điều khoản!");
         return;
      }
      try {
         const userData = await partnerService.completeRegistration();
         // Cập nhật lại thông tin user trong store (bao gồm cả role mới)
         setAuth(userData);
         setStep("SUCCESS");
         toast.success("Chúc mừng! Bạn đã trở thành đối tác của OmniBooking");
      } catch (err) {
         console.error("Failed to complete registration", err);
         toast.error("Đã có lỗi xảy ra khi hoàn tất đăng ký. Vui lòng thử lại sau.");
      }
   };

   if (!mounted) return null;

   return (
      <div className="min-h-screen bg-zinc-50 font-sans selection:bg-blue-100 selection:text-blue-900">
         <PartnerNavbar />

         <main className="mx-auto max-w-2xl px-4 py-12">
            {step !== "SUCCESS" && (
               <Link
                  href="/become-a-host"
                  className="inline-flex items-center gap-2 text-sm font-medium text-zinc-500 hover:text-[#006ce4] mb-8 transition-colors group"
               >
                  <ArrowLeft className="h-4 w-4 transition-transform group-hover:-translate-x-1" />
                  Quay lại
               </Link>
            )}

            <div className="bg-white rounded-2xl shadow-sm border border-zinc-200 overflow-hidden">
               {/* Progress Bar */}
               {step !== "SUCCESS" && (
                  <div className="h-1.5 w-full bg-zinc-100 flex">
                     <div
                        className={`h-full bg-[#006ce4] transition-all duration-500 ${
                           step === "VERIFY" ? "w-1/2" : "w-full"
                        }`}
                     ></div>
                  </div>
               )}

               <div className="p-8 sm:p-12">
                  {step === "VERIFY" && (
                     <div className="animate-in fade-in slide-in-from-bottom-4 duration-500">
                        <div className="h-12 w-12 bg-blue-50 text-blue-600 rounded-xl flex items-center justify-center mb-6">
                           <Lock className="h-6 w-6" />
                        </div>
                        <h1 className="text-2xl font-bold text-zinc-900 mb-2">
                           Xác thực tài khoản của bạn
                        </h1>
                        <p className="text-zinc-500 mb-8">
                           Chúng tôi đã gửi mã xác thực gồm 6 ký tự đến email của bạn. Vui lòng nhập
                           mã đó bên dưới để tiếp tục.
                        </p>

                        <div className="space-y-6">
                           <div>
                              <label className="block text-sm font-bold text-zinc-700 mb-2">
                                 Mã xác thực
                              </label>
                              <input
                                 type="text"
                                 placeholder="A0A000"
                                 value={code}
                                 onChange={(e) => setCode(e.target.value)}
                                 className="w-full px-4 py-3 bg-zinc-50 border border-zinc-200 rounded-xl focus:ring-2 focus:ring-[#006ce4] focus:bg-white outline-none transition-all font-mono tracking-[0.5em] uppercase text-center text-xl pl-[0.5em]"
                              />
                           </div>

                           <button
                              onClick={handleVerify}
                              className="w-full bg-[#006ce4] text-white py-4 rounded-xl font-bold hover:bg-[#0057b7] transition-all shadow-lg shadow-blue-100 active:scale-[0.98]"
                           >
                              Xác nhận email
                           </button>

                           <p className="text-center text-sm text-zinc-500">
                              Không nhận được mã?{" "}
                              <button
                                 onClick={() => handleSendOtp(true)}
                                 disabled={isResending || resendCountdown > 0}
                                 className="text-[#006ce4] font-bold hover:underline cursor-pointer disabled:opacity-50 disabled:cursor-not-allowed"
                              >
                                 {isResending
                                    ? "Đang gửi..."
                                    : resendCountdown > 0
                                      ? `Gửi lại sau ${resendCountdown}s`
                                      : "Gửi lại"}
                              </button>
                           </p>
                        </div>
                     </div>
                  )}

                  {step === "TERMS" && (
                     <div className="animate-in fade-in slide-in-from-bottom-4 duration-500">
                        <div className="h-12 w-12 bg-blue-50 text-blue-600 rounded-xl flex items-center justify-center mb-6">
                           <FileText className="h-6 w-6" />
                        </div>
                        <h1 className="text-2xl font-bold text-zinc-900 mb-2">
                           Điều khoản & Điều kiện Đối tác
                        </h1>
                        <p className="text-zinc-500 mb-8">
                           Vui lòng đọc kỹ các điều khoản dịch vụ dành cho đối tác trước khi bắt đầu
                           kinh doanh trên nền tảng của chúng tôi.
                        </p>

                        <div className="bg-zinc-50 border border-zinc-200 rounded-xl p-6 h-64 overflow-y-auto mb-8 text-sm text-zinc-600 leading-relaxed scrollbar-thin">
                           <div className="space-y-6">
                              <section>
                                 <h3 className="font-bold text-zinc-900 mb-2">1. Định nghĩa</h3>
                                 <p>
                                    &quot;Đối tác&quot; là cá nhân hoặc tổ chức đăng ký cung cấp
                                    dịch vụ lưu trú trên OmniBooking.com. &quot;Nền tảng&quot; là hệ
                                    thống website và ứng dụng di động của OmniBooking.
                                 </p>
                              </section>
                              <section>
                                 <h3 className="font-bold text-zinc-900 mb-2">
                                    2. Quyền và nghĩa vụ
                                 </h3>
                                 <p>
                                    Đối tác có quyền tự quyết định giá phòng và chính sách hủy. Đối
                                    tác có nghĩa vụ đảm bảo chất lượng dịch vụ như đã mô tả và tuân
                                    thủ các quy định pháp luật hiện hành.
                                 </p>
                              </section>
                              <section>
                                 <h3 className="font-bold text-zinc-900 mb-2">
                                    3. Phí hoa hồng và Thanh toán
                                 </h3>
                                 <p>
                                    OmniBooking sẽ thu phí hoa hồng cố định trên mỗi đơn đặt phòng
                                    thành công. Thanh toán sẽ được chuyển vào tài khoản đối tác định
                                    kỳ sau khi trừ phí dịch vụ.
                                 </p>
                              </section>
                              <section>
                                 <h3 className="font-bold text-zinc-900 mb-2">
                                    4. Bảo mật dữ liệu
                                 </h3>
                                 <p>
                                    Cả hai bên cam kết bảo mật thông tin khách hàng và không sử dụng
                                    vào mục đích trái phép.
                                 </p>
                              </section>
                           </div>
                        </div>

                        <div className="space-y-6">
                           <label className="flex items-start gap-4 p-4 rounded-xl border border-zinc-200 hover:border-[#006ce4] hover:bg-blue-50/50 transition-all cursor-pointer group">
                              <div className="relative flex items-center h-5 mt-0.5">
                                 <input
                                    type="checkbox"
                                    checked={isAgreed}
                                    onChange={(e) => setIsAgreed(e.target.checked)}
                                    className="h-5 w-5 rounded border-zinc-300 text-[#006ce4] focus:ring-[#006ce4] cursor-pointer"
                                 />
                              </div>
                              <div className="flex-1">
                                 <p className="text-sm font-bold text-zinc-900">
                                    Tôi đồng ý với tất cả điều khoản
                                 </p>
                                 <p className="text-xs text-zinc-500 mt-1">
                                    Bằng cách tích vào đây, bạn xác nhận đã hiểu rõ các quy định của
                                    hệ thống.
                                 </p>
                              </div>
                           </label>

                           <button
                              disabled={!isAgreed}
                              onClick={handleComplete}
                              className="w-full bg-[#006ce4] text-white py-4 rounded-xl font-bold hover:bg-[#0057b7] transition-all disabled:opacity-50 disabled:hover:bg-[#006ce4] shadow-lg shadow-blue-100"
                           >
                              Hoàn tất đăng ký
                           </button>
                        </div>
                     </div>
                  )}

                  {step === "SUCCESS" && (
                     <div className="text-center py-4 animate-in zoom-in-95 duration-700">
                        <div className="relative mx-auto h-24 w-24 mb-8">
                           <div className="absolute inset-0 bg-green-100 rounded-full animate-ping opacity-25"></div>
                           <div className="relative h-24 w-24 bg-green-500 text-white rounded-full flex items-center justify-center shadow-lg shadow-green-100">
                              <CheckCircle2 className="h-12 w-12" />
                           </div>
                        </div>
                        <h1 className="text-3xl font-bold text-zinc-900 mb-4">Đăng ký hoàn tất!</h1>
                        <p className="text-zinc-500 max-w-md mx-auto mb-6 leading-relaxed">
                           Cảm ơn bạn đã lựa chọn OmniBooking.com. Hồ sơ của bạn đã được gửi đi và
                           đang trong quá trình xét duyệt.
                        </p>

                        <div className="bg-blue-50 text-[#006ce4] py-3 px-6 rounded-full inline-flex items-center gap-2 text-sm font-bold mb-10 animate-pulse">
                           <span className="relative flex h-2 w-2">
                              <span className="animate-ping absolute inline-flex h-full w-full rounded-full bg-blue-400 opacity-75"></span>
                              <span className="relative inline-flex rounded-full h-2 w-2 bg-blue-500"></span>
                           </span>
                           Hệ thống sẽ tự động chuyển đến Dashboard sau {countdown} giây...
                        </div>

                        <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
                           <Link
                              href="/"
                              className="px-6 py-4 bg-zinc-900 text-white rounded-xl font-bold hover:bg-black transition-all shadow-lg"
                           >
                              Về trang chủ
                           </Link>
                           <Link
                              href="/become-a-host"
                              className="px-6 py-4 bg-white border border-zinc-200 text-zinc-900 rounded-xl font-bold hover:bg-zinc-50 transition-all"
                           >
                              Xem giới thiệu lại
                           </Link>
                        </div>
                     </div>
                  )}
               </div>
            </div>

            {/* Footer Trust badges */}
            {step !== "SUCCESS" && (
               <div className="mt-12 flex flex-wrap items-center justify-center gap-8 opacity-50 grayscale">
                  <div className="flex items-center gap-2">
                     <ShieldCheck className="h-5 w-5" />
                     <span className="text-xs font-bold uppercase tracking-widest">Secure SSL</span>
                  </div>
                  <div className="flex items-center gap-2">
                     <Lock className="h-5 w-5" />
                     <span className="text-xs font-bold uppercase tracking-widest">
                        Private Data
                     </span>
                  </div>
                  <div className="flex items-center gap-2">
                     <FileText className="h-5 w-5" />
                     <span className="text-xs font-bold uppercase tracking-widest">
                        Verified Partner
                     </span>
                  </div>
               </div>
            )}
         </main>
      </div>
   );
}
