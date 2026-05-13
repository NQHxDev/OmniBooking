import apiClient from "../apiClient";

export interface UserProfile {
   email: string;
   displayName: string;
   dateOfBirth: string | null;
   gender: string | null;
   address: string | null;
   nationality: string | null;
   phoneNumber: string | null;
   avatarUrl: string | null;
   verified: boolean;
   points: number;
   rankName: string;
}

export interface UpdateProfileRequest {
   displayName?: string;
   dateOfBirth?: string;
   gender?: string;
   address?: string;
   nationality?: string;
   avatarUrl?: string;
}

export interface ApiResponse<T> {
   message: string;
   errorCode?: string;
   data: T;
}

export const profileService = {
   /**
    * Gets the current user's profile details.
    */
   getMyProfile: async (): Promise<UserProfile> => {
      const response = (await apiClient.get("/profile/me")) as unknown as ApiResponse<UserProfile>;
      return response.data;
   },

   /**
    * Updates the current user's profile details.
    */
   updateMyProfile: async (payload: UpdateProfileRequest): Promise<UserProfile> => {
      const response = (await apiClient.patch(
         "/profile/me",
         payload
      )) as unknown as ApiResponse<UserProfile>;
      return response.data;
   },
};
