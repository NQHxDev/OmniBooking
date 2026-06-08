package com.omnibooking.model;

import com.omnibooking.model.enums.ReviewStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import java.time.Instant;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

@Entity
@Table(name = "reviews")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class Review extends BaseEntity {

   @OneToOne
   @JoinColumn(name = "booking_id", nullable = false, unique = true)
   private Booking booking;

   @ManyToOne
   @JoinColumn(name = "property_id", nullable = false)
   private Property property;

   @ManyToOne
   @JoinColumn(name = "user_id", nullable = false)
   private User user;

   @Column(nullable = false)
   private Integer rating;

   @Column(length = 1000)
   private String comment;

   @Column(columnDefinition = "TEXT")
   private String reply;

   @Builder.Default
   @Enumerated(EnumType.STRING)
   @Column(nullable = false, length = 20)
   private ReviewStatus status = ReviewStatus.PUBLISHED;

   @Column(name = "reply_updated_at")
   private Instant replyUpdatedAt;

   @ManyToOne
   @JoinColumn(name = "deleted_by")
   private User deletedBy;

   @Column(name = "deletion_reason")
   private String deletionReason;

   @Column(name = "moderated_at")
   private Instant moderatedAt;

   @ManyToOne
   @JoinColumn(name = "moderated_by")
   private User moderatedBy;

   @Column(name = "moderation_reason")
   private String moderationReason;

}
