"use client";

import Script from "next/script";
import { useEffect, useRef, useImperativeHandle, forwardRef } from "react";

interface TurnstileProps {
   siteKey: string;
   onVerify: (token: string) => void;
   onError?: () => void;
   onExpire?: () => void;
   theme?: "light" | "dark" | "auto";
}

export interface TurnstileRef {
   reset: () => void;
}

declare global {
   interface Window {
      turnstile?: {
         render: (
            container: string | HTMLElement,
            options: {
               sitekey: string;
               callback: (token: string) => void;
               "error-callback"?: () => void;
               "expired-callback"?: () => void;
               theme?: "light" | "dark" | "auto";
            }
         ) => string;
         reset: (widgetId?: string) => void;
         remove: (widgetId?: string) => void;
      };
      onloadTurnstileCallback?: () => void;
   }
}

const Turnstile = forwardRef<TurnstileRef, TurnstileProps>(
   ({ siteKey, onVerify, onError, onExpire, theme = "auto" }, ref) => {
      const containerRef = useRef<HTMLDivElement>(null);
      const widgetIdRef = useRef<string | null>(null);

      useImperativeHandle(ref, () => ({
         reset: () => {
            if (window.turnstile && widgetIdRef.current) {
               try {
                  window.turnstile.reset(widgetIdRef.current);
               } catch (err) {
                  console.error("Turnstile reset error:", err);
               }
            }
         },
      }));

      useEffect(() => {
         const renderWidget = () => {
            if (window.turnstile && containerRef.current && !widgetIdRef.current) {
               try {
                  const id = window.turnstile.render(containerRef.current, {
                     sitekey: siteKey,
                     callback: (token: string) => {
                        onVerify(token);
                     },
                     "error-callback": () => {
                        if (onError) onError();
                     },
                     "expired-callback": () => {
                        if (onExpire) onExpire();
                     },
                     theme,
                  });
                  widgetIdRef.current = id;
               } catch (err) {
                  console.error("Turnstile render error:", err);
               }
            }
         };

         if (window.turnstile) {
            renderWidget();
         } else {
            window.onloadTurnstileCallback = renderWidget;
         }

         return () => {
            if (window.turnstile && widgetIdRef.current) {
               try {
                  window.turnstile.remove(widgetIdRef.current);
                  widgetIdRef.current = null;
               } catch (err) {
                  console.error("Turnstile cleanup error:", err);
               }
            }
         };
      }, [siteKey, onVerify, onError, onExpire, theme]);

      return (
         <div className="flex justify-center my-4">
            <Script
               src="https://challenges.cloudflare.com/turnstile/v0/api.js?onload=onloadTurnstileCallback"
               strategy="afterInteractive"
            />
            <div ref={containerRef} />
         </div>
      );
   }
);

Turnstile.displayName = "Turnstile";

export default Turnstile;
