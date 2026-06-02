import { Toaster } from "sonner";
import { NextIntlClientProvider } from "next-intl";
import { getMessages, getTranslations } from "next-intl/server";
import { notFound } from "next/navigation";
import { routing } from "@/i18n/routing";
import QueryProvider from "@/providers/QueryProvider";
import AppInitializer from "@/providers/AppInitializer";
import OwnerAuthGuard from "@/components/owner/OwnerAuthGuard";

export async function generateMetadata({ params }: { params: Promise<{ locale: string }> }) {
   const { locale } = await params;
   const t = await getTranslations({ locale, namespace: "Common" });

   const baseTitle = t("title") || "OmniBooking Admin Panel";
   const pageTitle = "Quản trị hệ thống";

   return {
      title: `${baseTitle} | ${pageTitle}`,
      description: "Hệ thống quản trị dành riêng cho quản trị viên OmniBooking.",
   };
}

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
               <OwnerAuthGuard>{children}</OwnerAuthGuard>
            </AppInitializer>
            <Toaster position="bottom-right" richColors closeButton />
         </QueryProvider>
      </NextIntlClientProvider>
   );
}
