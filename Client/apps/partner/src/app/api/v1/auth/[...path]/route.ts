import { NextRequest, NextResponse } from "next/server";

export async function POST(
   request: NextRequest,
   { params }: { params: Promise<{ path: string[] }> }
) {
   return handleProxy(request, await params);
}

export async function GET(
   request: NextRequest,
   { params }: { params: Promise<{ path: string[] }> }
) {
   return handleProxy(request, await params);
}

export async function PUT(
   request: NextRequest,
   { params }: { params: Promise<{ path: string[] }> }
) {
   return handleProxy(request, await params);
}

export async function DELETE(
   request: NextRequest,
   { params }: { params: Promise<{ path: string[] }> }
) {
   return handleProxy(request, await params);
}

async function handleProxy(request: NextRequest, { path }: { path: string[] }) {
   const subPath = path.join("/");
   const backendUrl = `http://localhost:8080/api/v1/auth/${subPath}${request.nextUrl.search}`;

   const headers: Record<string, string> = {};
   request.headers.forEach((value, key) => {
      if (key.toLowerCase() !== "host") {
         headers[key] = value;
      }
   });

   let body = undefined;
   if (request.method !== "GET" && request.method !== "HEAD") {
      body = await request.text();
   }

   try {
      const res = await fetch(backendUrl, {
         method: request.method,
         headers,
         body,
         redirect: "manual",
      });

      const responseHeaders = new Headers();
      res.headers.forEach((value, key) => {
         if (key.toLowerCase() !== "set-cookie") {
            responseHeaders.set(key, value);
         }
      });

      const response = new NextResponse(res.body, {
         status: res.status,
         statusText: res.statusText,
         headers: responseHeaders,
      });

      const rawSetCookies = res.headers.getSetCookie();
      if (rawSetCookies && rawSetCookies.length > 0) {
         rawSetCookies.forEach((sc) => {
            const parts = sc.split(";").map((p) => p.trim());
            const nameValue = parts[0];
            const eqIdx = nameValue.indexOf("=");
            if (eqIdx !== -1) {
               const name = nameValue.substring(0, eqIdx);
               const value = nameValue.substring(eqIdx + 1);

               const cookieOpt: {
                  path?: string;
                  domain?: string;
                  maxAge?: number;
                  expires?: Date;
                  secure?: boolean;
                  httpOnly?: boolean;
                  sameSite?: "lax" | "strict" | "none";
               } = { path: "/" };
               parts.slice(1).forEach((opt) => {
                  const eqSign = opt.indexOf("=");
                  let key = opt;
                  let val = "";
                  if (eqSign !== -1) {
                     key = opt.substring(0, eqSign).trim();
                     val = opt.substring(eqSign + 1).trim();
                  }
                  const lowerKey = key.toLowerCase();
                  if (lowerKey === "path") cookieOpt.path = val;
                  else if (lowerKey === "domain") cookieOpt.domain = val;
                  else if (lowerKey === "max-age") cookieOpt.maxAge = parseInt(val, 10);
                  else if (lowerKey === "expires") cookieOpt.expires = new Date(val);
                  else if (lowerKey === "secure") cookieOpt.secure = true;
                  else if (lowerKey === "httponly") cookieOpt.httpOnly = true;
                  else if (lowerKey === "samesite") {
                     const s = val.toLowerCase();
                     if (s === "lax" || s === "strict" || s === "none") {
                        cookieOpt.sameSite = s;
                     }
                  }
               });
               response.cookies.set(name, value, cookieOpt);
            }
         });
      }

      return response;
   } catch (err) {
      console.error("Auth proxy error on Partner app:", err);
      return NextResponse.json({ message: "Internal Auth Proxy Error" }, { status: 500 });
   }
}
