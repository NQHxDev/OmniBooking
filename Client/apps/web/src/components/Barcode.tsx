"use client";

import { useEffect, useRef } from "react";
import JsBarcode from "jsbarcode";

interface BarcodeProps {
   value: string;
   format?: "CODE128" | "CODE39" | "EAN13" | "UPC";
   width?: number;
   height?: number;
   displayValue?: boolean;
   font?: string;
   fontSize?: number;
   textMargin?: number;
   background?: string;
   lineColor?: string;
   className?: string;
}

export default function Barcode({
   value,
   format = "CODE128",
   width = 2,
   height = 60,
   displayValue = true,
   font = "monospace",
   fontSize = 12,
   textMargin = 4,
   background = "#ffffff",
   lineColor = "#000000",
   className,
}: BarcodeProps) {
   const svgRef = useRef<SVGSVGElement>(null);

   useEffect(() => {
      if (svgRef.current) {
         try {
            JsBarcode(svgRef.current, value, {
               format,
               width,
               height,
               displayValue,
               font,
               fontSize,
               textMargin,
               background,
               lineColor,
               margin: 10,
            });
         } catch (error) {
            console.error("Barcode generation failed:", error);
         }
      }
   }, [
      value,
      format,
      width,
      height,
      displayValue,
      font,
      fontSize,
      textMargin,
      background,
      lineColor,
   ]);

   return <svg ref={svgRef} className={className} />;
}
