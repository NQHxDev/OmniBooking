"use client";

import { PropertyResponse } from "@/lib/api/propertyService";
import { Star, MapPin, MoreVertical, Building2 } from "lucide-react";
import Link from "next/link";

interface PropertyTableProps {
   properties: PropertyResponse[];
}

export default function PropertyTable({ properties }: PropertyTableProps) {
   if (properties.length === 0) {
      return (
         <div className="flex flex-col items-center justify-center py-20 bg-white rounded-3xl border border-dashed border-zinc-200">
            <div className="h-16 w-16 bg-zinc-50 rounded-2xl flex items-center justify-center mb-4">
               <Building2 className="h-8 w-8 text-zinc-300" />
            </div>
            <h3 className="text-lg font-bold text-zinc-900">Chưa có chỗ nghỉ nào</h3>
            <p className="text-zinc-500 text-sm mt-1 mb-6">
               Hãy bắt đầu bằng việc đăng ký chỗ nghỉ đầu tiên của bạn.
            </p>
            <Link
               href="/partner/properties/new"
               className="px-6 py-2.5 bg-[#006ce4] text-white rounded-xl font-bold text-sm hover:bg-[#0057b7] transition-all shadow-lg shadow-blue-100"
            >
               Thêm chỗ nghỉ ngay
            </Link>
         </div>
      );
   }

   return (
      <div className="overflow-hidden bg-white rounded-3xl border border-zinc-100 shadow-sm">
         <table className="w-full text-left">
            <thead>
               <tr className="border-b border-zinc-50 bg-zinc-50/50">
                  <th className="px-6 py-4 text-[11px] font-bold uppercase tracking-widest text-zinc-400">
                     Chỗ nghỉ
                  </th>
                  <th className="px-6 py-4 text-[11px] font-bold uppercase tracking-widest text-zinc-400">
                     Loại hình
                  </th>
                  <th className="px-6 py-4 text-[11px] font-bold uppercase tracking-widest text-zinc-400">
                     Vị trí
                  </th>
                  <th className="px-6 py-4 text-[11px] font-bold uppercase tracking-widest text-zinc-400">
                     Trạng thái
                  </th>
                  <th className="px-6 py-4 text-right"></th>
               </tr>
            </thead>
            <tbody className="divide-y divide-zinc-50">
               {properties.map((property) => (
                  <tr key={property.id} className="group hover:bg-zinc-50/50 transition-colors">
                     <td className="px-6 py-4">
                        <div className="flex items-center gap-4">
                           <div className="h-12 w-12 rounded-xl bg-blue-50 flex items-center justify-center text-[#003580] font-bold overflow-hidden border border-blue-100">
                              {property.name.charAt(0)}
                           </div>
                           <div>
                              <p className="font-bold text-zinc-900 group-hover:text-[#006ce4] transition-colors">
                                 {property.name}
                              </p>
                              <div className="flex items-center gap-1 mt-1">
                                 <Star className="h-3 w-3 fill-yellow-400 text-yellow-400" />
                                 <span className="text-[11px] font-bold text-zinc-500">
                                    4.8 (124 đánh giá)
                                 </span>
                              </div>
                           </div>
                        </div>
                     </td>
                     <td className="px-6 py-4">
                        <span className="inline-flex items-center px-2.5 py-0.5 rounded-full text-[10px] font-bold bg-zinc-100 text-zinc-600 uppercase tracking-tighter">
                           {property.propertyType}
                        </span>
                     </td>
                     <td className="px-6 py-4">
                        <div className="flex items-center gap-1.5 text-zinc-500">
                           <MapPin className="h-3.5 w-3.5" />
                           <span className="text-sm">
                              {property.city}, {property.country}
                           </span>
                        </div>
                     </td>
                     <td className="px-6 py-4">
                        <div className="flex items-center gap-1.5 text-green-600">
                           <div className="h-1.5 w-1.5 rounded-full bg-green-600 animate-pulse" />
                           <span className="text-[13px] font-bold">Đang hoạt động</span>
                        </div>
                     </td>
                     <td className="px-6 py-4 text-right">
                        <button className="p-2 hover:bg-zinc-100 rounded-lg transition-colors text-zinc-400">
                           <MoreVertical className="h-5 w-5" />
                        </button>
                     </td>
                  </tr>
               ))}
            </tbody>
         </table>
      </div>
   );
}
