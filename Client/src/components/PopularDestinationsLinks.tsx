import { destinationService } from "@/services/destinationService";
import PopularDestinationsLinksClient from "./PopularDestinationsLinksClient";

interface PopularDestinationsLinksProps {
   locale: string;
   clientIp?: string;
}

export default async function PopularDestinationsLinks({
   locale,
   clientIp,
}: PopularDestinationsLinksProps) {
   let countryName = locale === "vi" ? "Việt Nam" : "Vietnam";

   try {
      const destinations = await destinationService.getTrending(locale, clientIp);
      if (destinations && destinations.length > 0) {
         countryName = destinations[0].country || countryName;
      }
   } catch (error) {
      console.error(
         "Failed to fetch trending destinations for popular links country detection",
         error
      );
   }

   return <PopularDestinationsLinksClient locale={locale} countryName={countryName} />;
}
