import { create } from "zustand";
import { persist } from "zustand/middleware";
import axios from "axios";
import { getBaseURL } from "@/lib/api/config";

export interface User {
   id: string;
   username: string;
   email: string;
   fullName: string;
   avatarUrl?: string;
   roles: string[];
   reputationScore?: number;
   isVerified?: boolean;
   rankName?: string;
   partnerBio?: string;
}

interface AuthState {
   user: User | null;
   isLoggedIn: boolean;
   isReady: boolean;
   setAuth: (user: User) => void;
   setReady: (ready: boolean) => void;
   logout: () => Promise<void>;
}

export const useAuthStore = create<AuthState>()(
   persist(
      (set) => ({
         user: null,
         isLoggedIn: false,
         isReady: false,
         setAuth: (user) => set({ user, isLoggedIn: true }),
         setReady: (ready) => set({ isReady: ready }),
         logout: async () => {
            try {
               await axios.post(`${getBaseURL()}auth/logout`, {}, { withCredentials: true });
            } catch (error) {
               console.error("Logout failed:", error);
            } finally {
               set({ user: null, isLoggedIn: false });
            }
         },
      }),
      {
         name: "auth-storage",
         partialize: (state) => ({
            user: state.user,
            isLoggedIn: state.isLoggedIn,
         }),
      }
   )
);
