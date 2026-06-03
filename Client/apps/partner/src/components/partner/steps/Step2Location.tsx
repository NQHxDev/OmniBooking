"use client";

import { LocationPicker } from "@/components/Map";

interface Step2LocationProps {
   handleAddressDetailsChange: (details: {
      address: string;
      city: string;
      country: string;
   }) => void;
   onBack: () => void;
   onNext: () => void;
}

export default function Step2Location({
   handleAddressDetailsChange,
   onBack,
   onNext,
}: Step2LocationProps) {
   return (
      <div className="fixed inset-x-0 bottom-0 top-20 z-40 bg-zinc-50">
         <LocationPicker
            onAddressDetailsChange={handleAddressDetailsChange}
            showNavigation={true}
            onBack={onBack}
            onNext={onNext}
            className="h-full w-full border-none rounded-none shadow-none"
         />
      </div>
   );
}
