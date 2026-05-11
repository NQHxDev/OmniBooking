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
import org.springframework.data.elasticsearch.core.geo.GeoPoint;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(indexName = "properties")
@org.springframework.data.elasticsearch.annotations.Setting(settingPath = "elasticsearch/settings.json")
public class PropertyDocument {

   @Id
   private String id;

   @Field(type = FieldType.Text, analyzer = "vi_analyzer", searchAnalyzer = "vi_analyzer")
   private String name;

   @Field(type = FieldType.Text, analyzer = "vi_analyzer")
   private String description;

   @Field(type = FieldType.Keyword)
   private String propertyType;

   @Field(type = FieldType.Text, analyzer = "vi_analyzer")
   private String address;

   @Field(type = FieldType.Keyword)
   private String city;

   @Field(type = FieldType.Keyword)
   private String country;

   @GeoPointField
   private GeoPoint location;

   @Field(type = FieldType.Integer)
   private Integer starRating;

   @Field(type = FieldType.Keyword)
   private List<String> amenities;

   @Field(type = FieldType.Double)
   private Double minPrice;

   @Field(type = FieldType.Double)
   private Double averageRating;

   @Field(type = FieldType.Integer)
   private Integer reviewCount;

   @Field(type = FieldType.Keyword, index = false)
   private String mainImageUrl;

   @Field(type = FieldType.Boolean)
   private Boolean isActive;

}
