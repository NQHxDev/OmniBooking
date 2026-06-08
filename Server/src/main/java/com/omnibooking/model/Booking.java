package com.omnibooking.model;

import com.omnibooking.model.enums.BookingStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Set;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

@Entity
@Table(name = "bookings")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class Booking extends BaseEntity {

   @ManyToOne
   @JoinColumn(name = "user_id", nullable = false)
   private User user;

   @ManyToOne
   @JoinColumn(name = "room_type_id", nullable = false)
   private RoomType roomType;

   @Column(name = "check_in_date", nullable = false)
   private LocalDate checkInDate;

   @Column(name = "check_out_date", nullable = false)
   private LocalDate checkOutDate;

   @Builder.Default
   @Column(name = "num_rooms", nullable = false)
   private Integer numRooms = 1;

   @Column(name = "total_price", nullable = false)
   private BigDecimal totalPrice;

   @Column(name = "final_price", nullable = false)
   private BigDecimal finalPrice;

   @Builder.Default
   @Column(name = "currency", nullable = false, length = 3)
   private String currency = "USD";

   @ManyToOne
   @JoinColumn(name = "coupon_id")
   private Coupon coupon;

   @Builder.Default
   @Enumerated(EnumType.STRING)
   @Column(nullable = false, length = 20)
   private BookingStatus status = BookingStatus.PENDING;

   @Column(name = "guest_name", nullable = false, length = 100)
   private String guestName;

   @Column(name = "guest_email", nullable = false, length = 100)
   private String guestEmail;

   @Column(name = "guest_phone_encrypted", length = 255)
   private String guestPhoneEncrypted;

   @Column(name = "guest_phone_search_hash", length = 64)
   private String guestPhoneSearchHash;

   @Column(name = "special_requests", columnDefinition = "TEXT")
   private String specialRequests;

   @Column(name = "payment_method", length = 50)
   private String paymentMethod;

   @Builder.Default
   @Column(name = "deposit_amount", nullable = false, precision = 19, scale = 4)
   private BigDecimal depositAmount = BigDecimal.ZERO;

   @Builder.Default
   @Column(name = "requires_deposit", nullable = false)
   private Boolean requiresDeposit = false;

   @OneToMany(mappedBy = "booking")
   private Set<BookingStatusLog> statusLogs;

   @OneToMany(mappedBy = "booking")
   private Set<Transaction> transactions;

}
