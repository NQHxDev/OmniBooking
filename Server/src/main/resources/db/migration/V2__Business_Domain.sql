-- V2: Property, Booking, Payment Schema

-- Cancellation policies
CREATE TABLE IF NOT EXISTS cancellation_policies (
   id UUID PRIMARY KEY,
   name VARCHAR(100) NOT NULL,
   description TEXT,
   free_cancellation_days INTEGER NOT NULL DEFAULT 0,
   penalty_percentage DECIMAL(5, 2) NOT NULL DEFAULT 100.00,
   version BIGINT DEFAULT 0,
   created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
   updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
   deleted_at TIMESTAMP WITH TIME ZONE
);

-- Properties for rent
CREATE TABLE IF NOT EXISTS properties (
   id UUID PRIMARY KEY,
   owner_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
   name VARCHAR(255) NOT NULL,
   description TEXT,
   property_type VARCHAR(50) NOT NULL, -- HOTEL, APARTMENT, VILLA, RESORT
   address TEXT NOT NULL,
   city VARCHAR(100) NOT NULL,
   country VARCHAR(100) NOT NULL,
   latitude DECIMAL(10, 8),
   longitude DECIMAL(11, 8),
   star_rating INTEGER CHECK (star_rating >= 1 AND star_rating <= 5),
   check_in_time TIME DEFAULT '14:00:00',
   check_out_time TIME DEFAULT '12:00:00',
   cancellation_policy_id UUID REFERENCES cancellation_policies(id) ON DELETE SET NULL,
   is_active BOOLEAN DEFAULT TRUE,
   version BIGINT DEFAULT 0,
   created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
   updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
   deleted_at TIMESTAMP WITH TIME ZONE
);

-- Room Types for rent property
CREATE TABLE IF NOT EXISTS room_types (
   id UUID PRIMARY KEY,
   property_id UUID NOT NULL REFERENCES properties(id) ON DELETE CASCADE,
   name VARCHAR(100) NOT NULL,
   description TEXT,
   base_price DECIMAL(19, 4) NOT NULL,
   capacity_adults INTEGER NOT NULL DEFAULT 2,
   capacity_children INTEGER NOT NULL DEFAULT 0,
   total_rooms INTEGER NOT NULL DEFAULT 1,
   room_size_sqm DECIMAL(10, 2),
   bed_type VARCHAR(50),
   version BIGINT DEFAULT 0,
   created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
   updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
   deleted_at TIMESTAMP WITH TIME ZONE
);

-- Amenities for rent property
CREATE TABLE IF NOT EXISTS amenities (
   id UUID PRIMARY KEY,
   name VARCHAR(100) NOT NULL UNIQUE,
   category VARCHAR(50), -- GENERAL, ROOM, BATHROOM, KITCHEN
   icon_url VARCHAR(255),
   created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
   updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

-- Junction Tables for Amenities
CREATE TABLE IF NOT EXISTS property_amenities (
   property_id UUID REFERENCES properties(id) ON DELETE CASCADE,
   amenity_id UUID REFERENCES amenities(id) ON DELETE CASCADE,
   PRIMARY KEY (property_id, amenity_id)
);

CREATE TABLE IF NOT EXISTS room_amenities (
   room_type_id UUID REFERENCES room_types(id) ON DELETE CASCADE,
   amenity_id UUID REFERENCES amenities(id) ON DELETE CASCADE,
   PRIMARY KEY (room_type_id, amenity_id)
);

-- Room Availability for rent property
CREATE TABLE IF NOT EXISTS room_availability (
   id UUID PRIMARY KEY,
   room_type_id UUID NOT NULL REFERENCES room_types(id) ON DELETE CASCADE,
   availability_date DATE NOT NULL,
   available_count INTEGER NOT NULL,
   price_override DECIMAL(19, 4), -- NULL means use base_price from room_types
   is_closed BOOLEAN DEFAULT FALSE,
   version BIGINT DEFAULT 0,
   created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
   updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
   UNIQUE (room_type_id, availability_date)
);

-- Coupons
CREATE TABLE IF NOT EXISTS coupons (
   id UUID PRIMARY KEY,
   code VARCHAR(50) UNIQUE NOT NULL,
   discount_type VARCHAR(20) NOT NULL, -- PERCENT, FIXED_AMOUNT
   discount_value DECIMAL(19, 4) NOT NULL,
   min_booking_amount DECIMAL(19, 4) DEFAULT 0,
   max_discount_amount DECIMAL(19, 4),
   valid_from TIMESTAMP WITH TIME ZONE NOT NULL,
   valid_until TIMESTAMP WITH TIME ZONE NOT NULL,
   usage_limit INTEGER,
   used_count INTEGER DEFAULT 0,
   is_active BOOLEAN DEFAULT TRUE,
   version BIGINT DEFAULT 0,
   created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
   updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
   deleted_at TIMESTAMP WITH TIME ZONE
);

-- Bookings for rent property
CREATE TABLE IF NOT EXISTS bookings (
   id UUID PRIMARY KEY,
   user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
   room_type_id UUID NOT NULL REFERENCES room_types(id),
   check_in_date DATE NOT NULL,
   check_out_date DATE NOT NULL,
   num_rooms INTEGER NOT NULL DEFAULT 1,
   total_price DECIMAL(19, 4) NOT NULL, -- Original price before discount
   final_price DECIMAL(19, 4) NOT NULL, -- Price after coupons
   coupon_id UUID REFERENCES coupons(id) ON DELETE SET NULL,
   status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
   -- PENDING, CONFIRMED, STAYED, CANCELLED, REFUNDED

   -- Guest Info (can be different from user)
   guest_name VARCHAR(100) NOT NULL,
   guest_email VARCHAR(100) NOT NULL,
   guest_phone VARCHAR(20) NOT NULL,
   special_requests TEXT,

   version BIGINT DEFAULT 0,
   created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
   updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
   deleted_at TIMESTAMP WITH TIME ZONE,

   CONSTRAINT check_booking_dates CHECK (check_out_date > check_in_date)
);

-- Booking Status Logs
CREATE TABLE IF NOT EXISTS booking_status_logs (
   id UUID PRIMARY KEY,
   booking_id UUID NOT NULL REFERENCES bookings(id) ON DELETE CASCADE,
   old_status VARCHAR(20),
   new_status VARCHAR(20) NOT NULL,
   reason TEXT,
   changed_by UUID REFERENCES users(id),
   created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

-- Payments for rent property
CREATE TABLE IF NOT EXISTS transactions (
   id UUID PRIMARY KEY,
   booking_id UUID NOT NULL REFERENCES bookings(id) ON DELETE CASCADE,
   amount DECIMAL(19, 4) NOT NULL,
   transaction_type VARCHAR(20) NOT NULL, -- PAYMENT, REFUND
   payment_method VARCHAR(50), -- STRIPE, PAYPAL, MOMO, VNPAY, BANK_TRANSFER
   status VARCHAR(20) NOT NULL DEFAULT 'PENDING', -- PENDING, SUCCESS, FAILED, VOIDED
   provider_transaction_id VARCHAR(255),
   metadata JSONB,
   version BIGINT DEFAULT 0,
   created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
   updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

-- Media Management Table
CREATE TABLE IF NOT EXISTS media (
   id UUID PRIMARY KEY,
   url TEXT NOT NULL,
   public_id VARCHAR(255) NOT NULL,
   format VARCHAR(20),
   resource_type VARCHAR(20),
   bytes BIGINT,
   
   -- Relation fields
   entity_id UUID NOT NULL,
   entity_type VARCHAR(50) NOT NULL, -- PROPERTY, ROOM_TYPE, USER_AVATAR
   is_main BOOLEAN DEFAULT FALSE,
   
   version BIGINT DEFAULT 0,
   created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
   updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
   deleted_at TIMESTAMP WITH TIME ZONE
);

-- Create Indexes
CREATE INDEX idx_properties_owner_id ON properties(owner_id);
CREATE INDEX idx_properties_city ON properties(city);
CREATE INDEX idx_room_types_property_id ON room_types(property_id);
CREATE INDEX idx_room_availability_date ON room_availability(availability_date);
CREATE INDEX idx_bookings_user_id ON bookings(user_id);
CREATE INDEX idx_bookings_status ON bookings(status);
CREATE INDEX idx_bookings_check_in ON bookings(check_in_date);
CREATE INDEX idx_transactions_booking_id ON transactions(booking_id);
CREATE INDEX idx_coupons_code ON coupons(code);
CREATE INDEX idx_media_entity ON media(entity_id, entity_type);
