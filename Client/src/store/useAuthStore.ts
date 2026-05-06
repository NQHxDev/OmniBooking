import { create } from "zustand";
import { persist } from "zustand/middleware";
import apiClient from "@/lib/api/apiClient";

export interface User {
   id: string;
   username: string;
   email: string;
   fullName: string;
   roles: string[];
}

interface AuthState {
   user: User | null;
   isLoggedIn: boolean;
   setAuth: (user: User) => void;
   logout: () => Promise<void>;
}

export const useAuthStore = create<AuthState>()(
   persist(
      (set) => ({
         user: null,
         isLoggedIn: false,
         setAuth: (user) => set({ user, isLoggedIn: true }),
         logout: async () => {
            try {
               await apiClient.post("auth/logout", {}, { withCredentials: true });
            } catch (error) {
               console.error("Logout failed:", error);
            } finally {
               set({ user: null, isLoggedIn: false });
            }
         },
      }),
      {
         name: "auth-storage",
      }
   )
);
