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
