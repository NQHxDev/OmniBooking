import { Toaster } from "@/components/ui/sonner";
import { NextIntlClientProvider } from "next-intl";
import { getMessages, getTranslations } from "next-intl/server";
import { notFound } from "next/navigation";
import { routing } from "@/i18n/routing";

export async function generateMetadata({ params }: { params: Promise<{ locale: string }> }) {
   const { locale } = await params;
   const t = await getTranslations({ locale, namespace: "Common" });

   const baseTitle = "OmniBooking.com";
   const pageTitle = t("heroTitle") || "Tìm chỗ nghỉ tiếp theo";

   return {
      title: `${baseTitle} | ${pageTitle}`,
      description: t("heroSub") || "Tìm ưu đãi khách sạn, chỗ nghỉ dạng nhà và nhiều hơn nữa...",
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

   // Providing all messages to the client
   // side is the easiest way to get started
   const messages = await getMessages();

   return (
      <NextIntlClientProvider messages={messages}>
         {children}
         <Toaster position="bottom-right" richColors closeButton />
      </NextIntlClientProvider>
   );
}
