"use client";

import * as React from "react";
import { addDays, format } from "date-fns";
import { vi } from "date-fns/locale";
import { DayPicker, DateRange } from "react-day-picker";
import { Be_Vietnam_Pro } from "next/font/google";
import "react-day-picker/dist/style.css";

const beVietnamPro = Be_Vietnam_Pro({
   subsets: ["vietnamese"],
   weight: ["400", "500", "600", "700", "800", "900"],
   display: "swap",
});

interface DateRangePickerProps {
   date: DateRange | undefined;
   onDateChange: (date: DateRange | undefined) => void;
}

export default function DateRangePicker({ date, onDateChange }: DateRangePickerProps) {
   return (
      <div
         className={`${beVietnamPro.className} p-4 bg-white rounded-3xl shadow-2xl border border-zinc-100 animate-in fade-in zoom-in-95 duration-200`}
      >
         <style>{`
            .rdp-months {
               display: flex;
               flex-direction: row;
               gap: 3rem;
               justify-content: center;
            }
            .rdp {
               --rdp-cell-size: 44px;
               --rdp-accent-color: #006ce4;
               --rdp-background-color: #eef6ff;
               margin: 0;
               width: 100%;
            }
            .rdp, .rdp * {
               font-family: var(--font-be-vietnam-pro), "Be Vietnam Pro", sans-serif !important;
            }
            .rdp-day_selected, .rdp-day_selected:hover {
               background-color: var(--rdp-accent-color) !important;
               color: white !important;
               border-radius: 12px !important;
               font-weight: bold;
            }
            .rdp-day_range_middle {
               background-color: var(--rdp-background-color) !important;
               color: #006ce4 !important;
               border-radius: 0 !important;
            }
            .rdp-day_range_start {
               border-top-right-radius: 0 !important;
               border-bottom-right-radius: 0 !important;
            }
            .rdp-day_range_end {
               border-top-left-radius: 0 !important;
               border-bottom-left-radius: 0 !important;
            }
            .rdp-day {
               font-size: 0.85rem;
               border-radius: 12px;
            }
            .rdp-day:hover:not(.rdp-day_selected) {
               background-color: #f4f4f5;
            }
            .rdp-head_cell {
               font-size: 0.75rem;
               font-weight: 700;
               text-transform: uppercase;
               color: #a1a1aa;
               padding-bottom: 1rem;
            }
            .rdp-caption_label {
               font-size: 1rem;
               font-weight: 800;
               color: #1a1a1a;
            }
            .rdp-nav_button {
               background-color: #f4f4f5;
               border-radius: 10px;
               color: #71717a;
               padding: 4px;
            }
            .rdp-day_today {
               font-weight: bold;
               color: #006ce4;
               text-decoration: underline;
               text-underline-offset: 4px;
            }
            .rdp-day_disabled {
               opacity: 0.2;
               cursor: not-allowed;
            }
         `}</style>
         <DayPicker
            mode="range"
            defaultMonth={new Date()}
            startMonth={new Date()}
            endMonth={addDays(new Date(), 365)}
            fromDate={new Date()}
            toDate={addDays(new Date(), 365)}
            numberOfMonths={2}
            selected={date}
            onSelect={onDateChange}
            locale={vi}
            disabled={{ before: new Date() }}
            className="border-none"
         />

         <div className="mt-4 flex items-center justify-between border-t border-zinc-100 pt-4 px-2">
            <div className="flex flex-col">
               <span className="text-[10px] font-bold text-zinc-400 uppercase tracking-wider">
                  Thời gian chọn
               </span>
               <span className="text-sm font-bold text-black">
                  {date?.from
                     ? date.to
                        ? `${format(date.from, "dd/MM/yyyy")} — ${format(date.to, "dd/MM/yyyy")}`
                        : `Từ ${format(date.from, "dd/MM/yyyy")}`
                     : "Vui lòng chọn ngày"}
               </span>
            </div>
            <button
               onClick={() => onDateChange(undefined)}
               className="text-xs font-bold text-zinc-400 hover:text-red-500 transition-colors"
            >
               Xóa chọn
            </button>
         </div>
      </div>
   );
}
