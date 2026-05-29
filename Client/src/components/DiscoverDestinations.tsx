import { getTranslations } from "next-intl/server";
import { destinationService, DestinationSuggestionResponse } from "@/services/destinationService";
import DiscoverDestinationsCarousel from "./DiscoverDestinationsCarousel";

interface DiscoverDestinationsProps {
   locale: string;
   clientIp?: string;
}

export default async function DiscoverDestinations({
   locale,
   clientIp,
}: DiscoverDestinationsProps) {
   const t = await getTranslations({ locale, namespace: "Home" });
   let destinations: DestinationSuggestionResponse[] = [];

   try {
      destinations = await destinationService.getTrending(locale, clientIp);
      // Filter out "Hạ Long" (ID: 12) to avoid duplication with Quảng Ninh
      destinations = destinations.filter(
         (dest) => dest.id !== "12" && dest.name !== "Hạ Long" && dest.name !== "Ha Long"
      );
   } catch (error) {
      console.error("Failed to fetch trending destinations", error);
   }

   if (!destinations || destinations.length === 0) {
      return null;
   }

   // Get country name from the first suggestion, fallback to localized Vietnam name
   const firstDest = destinations[0];
   const countryName = firstDest?.country || (locale === "vi" ? "Việt Nam" : "Vietnam");

   return (
      <section className="mt-10">
         <div>
            <h3 className="text-2xl font-bold text-black">
               {t("discoverCountry", { country: countryName })}
            </h3>
            <p className="mt-1 text-zinc-500">{t("discoverCountrySub")}</p>
         </div>

         <DiscoverDestinationsCarousel destinations={destinations} />
      </section>
   );
}
