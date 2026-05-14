"use client";

import { useEffect } from "react";
import { Button } from "@/components/ui/button";
import { RotateCcw, Home } from "lucide-react";
import Link from "next/link";
import { useTranslations } from "next-intl";

export default function Error({
   error,
   reset,
}: {
   error: Error & { digest?: string };
   reset: () => void;
}) {
   const t = useTranslations("Errors");

   useEffect(() => {
      console.error(error);
   }, [error]);

   return (
      <div className="flex flex-col items-center bg-zinc-50 px-4 pt-12 pb-8 sm:h-screen sm:overflow-hidden overflow-y-auto text-center">
         {/* Static Content Wrapper */}
         <div className="flex shrink-0 flex-col items-center">
            <div className="mb-8 flex h-24 w-24 items-center justify-center rounded-full bg-red-100 text-red-600 shadow-sm border-4 border-white">
               <svg className="h-12 w-12" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                  <path
                     strokeLinecap="round"
                     strokeLinejoin="round"
                     strokeWidth={2}
                     d="M12 9v2m0 4h.01m-6.938 4h13.856c1.54 0 2.502-1.667 1.732-3L13.732 4c-.77-1.333-2.694-1.333-3.464 0L3.34 16c-.77 1.333.192 3 1.732 3z"
                  />
               </svg>
            </div>

            <h1 className="mb-3 text-4xl font-black tracking-tighter text-zinc-900">
               {t("somethingWentWrong")}
            </h1>
            <p className="mb-8 max-w-lg text-lg text-zinc-500 leading-relaxed">
               {t("description")}
            </p>

            <div className="flex flex-col gap-4 sm:flex-row mb-10">
               <Button
                  onClick={() => reset()}
                  className="flex h-12 items-center gap-2 bg-blue-600 px-8 font-bold hover:bg-blue-700 transition-all active:scale-95 shadow-lg shadow-blue-200"
               >
                  <RotateCcw className="h-4 w-4" />
                  {t("tryAgain")}
               </Button>
               <Link href="/">
                  <Button
                     variant="outline"
                     className="flex h-12 items-center gap-2 px-8 font-bold border-zinc-200 hover:bg-zinc-50 transition-all active:scale-95"
                  >
                     <Home className="h-4 w-4" />
                     {t("goHome")}
                  </Button>
               </Link>
            </div>
         </div>

         {/* Scrollable Dev Block */}
         {process.env.NODE_ENV === "development" && (
            <div className="w-full max-w-3xl flex flex-col flex-1 min-h-0 rounded-2xl bg-zinc-900 p-6 text-left shadow-2xl border border-white/5 overflow-hidden">
               <div className="flex items-center justify-between mb-4 border-b border-white/10 pb-3 shrink-0">
                  <p className="font-mono text-sm font-bold text-red-400">{t("devDetails")}</p>
                  <span className="text-[10px] font-mono text-zinc-500 uppercase tracking-widest">
                     Internal Stack Trace
                  </span>
               </div>
               <div className="flex-1 overflow-y-auto pr-2 scrollbar-thin scrollbar-thumb-zinc-700 scrollbar-track-transparent">
                  <pre className="font-mono text-[11px] text-zinc-300 whitespace-pre-wrap break-all leading-relaxed opacity-80 hover:opacity-100 transition-opacity">
                     {error.message}
                     {"\n\n"}
                     {error.stack}
                  </pre>
               </div>
            </div>
         )}
      </div>
   );
}
