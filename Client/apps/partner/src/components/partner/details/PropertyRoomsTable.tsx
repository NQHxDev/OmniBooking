"use client";

import { PropertyDetailResponse } from "@/lib/api/propertyService";
import { Bed, Users, Square, Plus } from "lucide-react";
import { useTranslations } from "next-intl";

interface PropertyRoomsTableProps {
   property: PropertyDetailResponse;
}

export default function PropertyRoomsTable({ property }: PropertyRoomsTableProps) {
   const t = useTranslations("Partner.propertyDetail.rooms");
   const tForm = useTranslations("Partner.createPropertyForm");

   return (
      <div className="rounded-[2rem] border border-zinc-100 bg-white p-6 shadow-xs mb-8 overflow-hidden">
         <div className="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-4 mb-6">
            <div>
               <h2 className="text-lg font-bold text-zinc-900">{t("title")}</h2>
            </div>
            <button className="flex items-center gap-1.5 rounded-xl bg-[#006ce4] px-4 py-2.5 text-xs font-bold text-white hover:bg-[#0057b7] transition-all shadow-md shadow-blue-50 active:scale-[0.98]">
               <Plus className="h-4 w-4" />
               Thêm phòng
            </button>
         </div>

         {!property.roomTypes || property.roomTypes.length === 0 ? (
            <div className="text-center py-10">
               <span className="text-sm font-bold text-zinc-400">{t("noRooms")}</span>
            </div>
         ) : (
            <div className="overflow-x-auto">
               <table className="w-full text-left border-collapse">
                  <thead>
                     <tr className="border-b border-zinc-100 text-xs font-bold uppercase tracking-wider text-zinc-400">
                        <th className="pb-4 font-bold">{t("name")}</th>
                        <th className="pb-4 font-bold text-center">{t("capacity")}</th>
                        <th className="pb-4 font-bold text-center">{t("size")}</th>
                        <th className="pb-4 font-bold">{t("bedType")}</th>
                        <th className="pb-4 font-bold text-center">{t("totalRooms")}</th>
                        <th className="pb-4 font-bold text-right">{t("basePrice")}</th>
                     </tr>
                  </thead>
                  <tbody>
                     {property.roomTypes.map((room) => (
                        <tr
                           key={room.id}
                           className="border-b border-zinc-50 last:border-b-0 hover:bg-zinc-50/50 transition-colors"
                        >
                           <td className="py-4 font-semibold text-zinc-900 text-sm">
                              {room.name}
                              {room.description && (
                                 <span className="block text-xs text-zinc-400 font-medium mt-0.5 max-w-sm truncate">
                                    {room.description}
                                 </span>
                              )}
                           </td>
                           <td className="py-4 text-center">
                              <div className="inline-flex items-center gap-1 text-sm font-bold text-zinc-700 bg-zinc-50 border border-zinc-100 px-2.5 py-1 rounded-xl">
                                 <Users className="h-3.5 w-3.5 text-zinc-400" />
                                 <span>
                                    {room.capacityAdults} NL / {room.capacityChildren} TE
                                 </span>
                              </div>
                           </td>
                           <td className="py-4 text-center">
                              <div className="inline-flex items-center gap-1 text-sm font-bold text-zinc-700 bg-zinc-50 border border-zinc-100 px-2.5 py-1 rounded-xl">
                                 <Square className="h-3.5 w-3.5 text-zinc-400" />
                                 <span>{room.roomSizeSqm} m²</span>
                              </div>
                           </td>
                           <td className="py-4 text-sm font-semibold text-zinc-700">
                              <div className="flex items-center gap-1.5">
                                 <Bed className="h-4 w-4 text-zinc-400" />
                                 <span>{tForm("bedTypes." + room.bedType) || room.bedType}</span>
                              </div>
                           </td>
                           <td className="py-4 text-center text-sm font-bold text-zinc-900">
                              {room.totalRooms}
                           </td>
                           <td className="py-4 text-right text-sm font-bold text-[#006ce4]">
                              ${room.basePrice.toLocaleString()}/đêm
                           </td>
                        </tr>
                     ))}
                  </tbody>
               </table>
            </div>
         )}
      </div>
   );
}
