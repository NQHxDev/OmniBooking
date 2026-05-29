package com.omnibooking.document;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;
import org.springframework.data.elasticsearch.annotations.GeoPointField;
import org.springframework.data.elasticsearch.annotations.Setting;
import org.springframework.data.elasticsearch.core.geo.GeoPoint;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(indexName = "destinations")
@Setting(settingPath = "elasticsearch/settings.json")
public class DestinationDocument {

   @Id
   private String id;

   @Field(type = FieldType.Text, analyzer = "vi_analyzer", searchAnalyzer = "vi_analyzer")
   private String name;

   @Field(type = FieldType.Keyword)
   private String type; // CITY, LANDMARK, REGION

   @Field(type = FieldType.Keyword)
   private String countryCode;

   @Field(type = FieldType.Text, analyzer = "vi_analyzer")
   private String countryName;

   @GeoPointField
   private GeoPoint location;

   @Field(type = FieldType.Double)
   private Double popularityScore;

   @Field(type = FieldType.Keyword, index = false)
   private String imageUrl;

}
