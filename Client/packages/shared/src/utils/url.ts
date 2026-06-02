export function getPartnerUrl(origin: string): string {
   try {
      const url = new URL(origin);
      const hostname = url.hostname;
      if (hostname === "localhost" || hostname === "127.0.0.1") {
         return `http://localhost:3002`;
      }
      if (hostname.startsWith("partner.")) {
         return `${url.protocol}//${hostname}`;
      }
      return `${url.protocol}//partner.${hostname}`;
   } catch {
      return "https://partner.zeion.online";
   }
}

export function getOwnerUrl(origin: string): string {
   try {
      const url = new URL(origin);
      const hostname = url.hostname;
      if (hostname === "localhost" || hostname === "127.0.0.1") {
         return `http://localhost:3005`;
      }
      if (hostname.startsWith("owner.")) {
         return `${url.protocol}//${hostname}`;
      }
      const cleanHostname = hostname.replace("partner.", "");
      return `${url.protocol}//owner.${cleanHostname}`;
   } catch {
      return "https://owner.zeion.online";
   }
}

export function getWebUrl(origin: string): string {
   try {
      const url = new URL(origin);
      const hostname = url.hostname;
      if (hostname === "localhost" || hostname === "127.0.0.1") {
         return `http://localhost:3000`;
      }
      if (!hostname.startsWith("partner.") && !hostname.startsWith("owner.")) {
         return `${url.protocol}//${hostname}`;
      }
      const domain = hostname.replace("partner.", "").replace("owner.", "");
      return `${url.protocol}//${domain}`;
   } catch {
      return "https://zeion.online";
   }
}
