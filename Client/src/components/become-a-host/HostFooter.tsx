import Link from "next/link";

export default function HostFooter() {
   return (
      <footer className="py-12 border-t border-zinc-100 bg-white">
         <div className="mx-auto max-w-[1100px] px-4 text-center">
            <div className="flex flex-wrap justify-center gap-6 text-sm font-medium text-[#006ce4] mb-8">
               <Link href="#" className="hover:underline">
                  Về chúng tôi
               </Link>
               <Link href="#" className="hover:underline">
                  Điều khoản & Điều kiện
               </Link>
               <Link href="#" className="hover:underline">
                  Chính sách bảo mật
               </Link>
               <Link href="#" className="hover:underline">
                  Trợ giúp
               </Link>
            </div>
            <p className="text-xs text-zinc-400">
               © 1996-2026 OmniBooking.com™. Bảo lưu mọi quyền.
            </p>
         </div>
      </footer>
   );
}
