import { Toaster } from "sonner";
import { NextIntlClientProvider } from "next-intl";
import { getMessages } from "next-intl/server";
import { notFound } from "next/navigation";
import { routing } from "@/i18n/routing";

export async function generateMetadata() {
   const baseTitle = "OmniBooking Partner Hub";
   const pageTitle = "Quản lý chỗ nghỉ";

   return {
      title: `${baseTitle} | ${pageTitle}`,
      description: "Hệ thống dành riêng cho đối tác của hệ sinh thái OmniBooking.",
   };
}

import QueryProvider from "@/providers/QueryProvider";
import AppInitializer from "@/providers/AppInitializer";
import PartnerAuthGuard from "@/components/partner/PartnerAuthGuard";
import UploadProgressManager from "@/components/partner/UploadProgressManager";

export default async function LocaleLayout({
   children,
   params,
}: {
   children: React.ReactNode;
   params: Promise<{ locale: string }>;
}) {
   const { locale } = await params;

   // Ensure that the incoming `locale` is valid
   if (!routing.locales.includes(locale as "vi" | "en")) {
      notFound();
   }

   // Providing all messages to the client side
   const messages = await getMessages();

   return (
      <NextIntlClientProvider messages={messages}>
         <QueryProvider>
            <AppInitializer>
               <PartnerAuthGuard>{children}</PartnerAuthGuard>
            </AppInitializer>
            <Toaster position="bottom-right" richColors closeButton />
            <UploadProgressManager />
         </QueryProvider>
      </NextIntlClientProvider>
   );
}
