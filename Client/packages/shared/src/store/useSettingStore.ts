import { create } from "zustand";
import { persist } from "zustand/middleware";

interface SettingState {
   currency: string;
   setCurrency: (currency: string) => void;
}

export const useSettingStore = create<SettingState>()(
   persist(
      (set) => ({
         currency: "USD",
         setCurrency: (currency) => set({ currency }),
      }),
      {
         name: "setting-storage",
      }
   )
);
