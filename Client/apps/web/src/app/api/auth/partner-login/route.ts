import { NextRequest, NextResponse } from "next/server";
import { getPartnerUrl } from "@omnibooking/shared";

export async function GET(request: NextRequest) {
   const accessToken = request.cookies.get("access_token")?.value;
   const refreshToken = request.cookies.get("refresh_token")?.value;
   const sessionId = request.cookies.get("session_id")?.value;

   const host = request.headers.get("x-forwarded-host") || request.headers.get("host") || "";
   const proto = request.headers.get("x-forwarded-proto") || "http";
   const browserUrl = `${proto}://${host}`;

   const partnerUrl = getPartnerUrl(browserUrl);
   const redirectUrl = new URL(`${partnerUrl}/dashboard`);

   if (accessToken) redirectUrl.searchParams.set("token", accessToken);
   if (refreshToken) redirectUrl.searchParams.set("refresh", refreshToken);
   if (sessionId) redirectUrl.searchParams.set("session", sessionId);

   return NextResponse.redirect(redirectUrl.toString());
}
