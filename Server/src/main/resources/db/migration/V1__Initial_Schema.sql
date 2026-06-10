-- Initial Complete Schema Setup

-- 1. Base Tables (No Foreign Keys)
CREATE TABLE IF NOT EXISTS roles (
   id UUID PRIMARY KEY,
   name VARCHAR(50) UNIQUE NOT NULL,
   description VARCHAR(255),
   version BIGINT DEFAULT 0,
   created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
   updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
   deleted_at TIMESTAMP WITH TIME ZONE
);

CREATE TABLE IF NOT EXISTS permissions (
   id UUID PRIMARY KEY,
   name VARCHAR(50) UNIQUE NOT NULL,
   description VARCHAR(255),
   version BIGINT DEFAULT 0,
   created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
   updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
   deleted_at TIMESTAMP WITH TIME ZONE
);

CREATE TABLE IF NOT EXISTS ranks (
   id UUID PRIMARY KEY,
   name VARCHAR(50) UNIQUE NOT NULL,
   min_points INTEGER NOT NULL DEFAULT 0,
   benefits TEXT,
   version BIGINT DEFAULT 0,
   created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
   updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
   deleted_at TIMESTAMP WITH TIME ZONE
);

CREATE TABLE IF NOT EXISTS users (
   id UUID PRIMARY KEY,
   username VARCHAR(50) UNIQUE NOT NULL,
   email VARCHAR(100) UNIQUE NOT NULL,
   password VARCHAR(255), -- Nullable for social login users
   is_active BOOLEAN DEFAULT TRUE,
   version BIGINT DEFAULT 0,
   token_version INT NOT NULL DEFAULT 0,
   created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
   updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
   deleted_at TIMESTAMP WITH TIME ZONE
);

CREATE TABLE IF NOT EXISTS amenities (
   id UUID PRIMARY KEY,
   name VARCHAR(100) NOT NULL UNIQUE,
   category VARCHAR(50), -- GENERAL, ROOM, BATHROOM, KITCHEN
   icon_url VARCHAR(255),
   version BIGINT DEFAULT 0,
   created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
   updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
   deleted_at TIMESTAMP WITH TIME ZONE
);

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

CREATE TABLE IF NOT EXISTS currencies (
   id UUID PRIMARY KEY,
   code VARCHAR(3) UNIQUE NOT NULL,
   name VARCHAR(50) NOT NULL,
   symbol VARCHAR(10),
   is_active BOOLEAN DEFAULT TRUE,
   is_base BOOLEAN NOT NULL DEFAULT FALSE,
   version BIGINT DEFAULT 0,
   created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
   updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
   deleted_at TIMESTAMP WITH TIME ZONE
);

CREATE TABLE IF NOT EXISTS registration_inbox (
   request_id UUID PRIMARY KEY,
   payload JSONB NOT NULL,
   status VARCHAR(20) NOT NULL, -- PENDING, SENT, PROCESSING, SUCCESS, FAILED
   created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL,
   published_at TIMESTAMP WITH TIME ZONE,
   retry_count INTEGER NOT NULL DEFAULT 0,
   last_error TEXT,
   next_retry_at TIMESTAMP WITH TIME ZONE,
   processed_at TIMESTAMP WITH TIME ZONE
);

CREATE TABLE IF NOT EXISTS registration_dlt (
   request_id UUID PRIMARY KEY,
   email VARCHAR(255) NOT NULL,
   payload JSONB NOT NULL,
   partition_id INTEGER NOT NULL,
   offset_val BIGINT NOT NULL,
   original_error TEXT,
   status VARCHAR(20) NOT NULL, -- PENDING, REPLAYED, FAILED
   created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL,
   last_replayed_at TIMESTAMP WITH TIME ZONE
);

CREATE TABLE IF NOT EXISTS registration_dlt_audit (
   id UUID PRIMARY KEY,
   request_id UUID NOT NULL,
   replayed_by VARCHAR(255) NOT NULL,
   replayed_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL,
   original_error TEXT,
   replay_result VARCHAR(50) NOT NULL,
   error_message TEXT
);

CREATE TABLE IF NOT EXISTS pricing_audit_logs (
   id UUID PRIMARY KEY,
   entity_type VARCHAR(20) NOT NULL,
   entity_id UUID NOT NULL,
   operation_type VARCHAR(20) NOT NULL,
   actor_id UUID NOT NULL,
   old_values JSONB,
   new_values JSONB,
   correlation_id UUID NOT NULL,
   created_at TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE TABLE IF NOT EXISTS shedlock (
   name VARCHAR(64) PRIMARY KEY,
   lock_until TIMESTAMP WITH TIME ZONE NOT NULL,
   locked_at TIMESTAMP WITH TIME ZONE NOT NULL,
   locked_by VARCHAR(255) NOT NULL
);

CREATE TABLE IF NOT EXISTS coupon_release_retries (
   id UUID PRIMARY KEY,
   booking_id UUID NOT NULL,
   coupon_id UUID NOT NULL,
   user_id UUID NOT NULL,
   attempt_count INT NOT NULL,
   last_attempt_at TIMESTAMP,
   next_attempt_at TIMESTAMP NOT NULL,
   status VARCHAR(20) NOT NULL,
   created_at TIMESTAMP NOT NULL
);

-- 2. Dependent Tables (With Foreign Keys)
CREATE TABLE IF NOT EXISTS user_profiles (
   user_id UUID PRIMARY KEY REFERENCES users(id) ON DELETE CASCADE,
   display_name VARCHAR(100),
   phone_encrypted VARCHAR(255),
   phone_search_hash VARCHAR(64),
   avatar_url VARCHAR(255),
   date_of_birth DATE,
   gender VARCHAR(20),
   address VARCHAR(255),
   nationality VARCHAR(100),
   points INTEGER DEFAULT 0,
   rank_id UUID REFERENCES ranks(id) ON DELETE SET NULL,
   reputation_score DOUBLE PRECISION DEFAULT 100.0,
   is_verified BOOLEAN DEFAULT FALSE,
   partner_bio TEXT,
   version BIGINT DEFAULT 0,
   updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
   deleted_at TIMESTAMP WITH TIME ZONE
);

-- Junction Tables
CREATE TABLE IF NOT EXISTS roles_permissions (
   role_id UUID REFERENCES roles(id) ON DELETE CASCADE,
   permission_id UUID REFERENCES permissions(id) ON DELETE CASCADE,
   PRIMARY KEY (role_id, permission_id)
);

CREATE TABLE IF NOT EXISTS users_roles (
   user_id UUID REFERENCES users(id) ON DELETE CASCADE,
   role_id UUID REFERENCES roles(id) ON DELETE CASCADE,
   PRIMARY KEY (user_id, role_id)
);

-- Business Logic & Property Modules
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
   deleted_at TIMESTAMP WITH TIME ZONE,
   business_registration_number VARCHAR(255),
   tax_code VARCHAR(255),
   legal_owner_name VARCHAR(255),

   -- Aggregated Rating Fields
   average_rating NUMERIC(4,2) DEFAULT 0.00,
   review_count INTEGER DEFAULT 0,
   rating_sum BIGINT DEFAULT 0,
   expected_image_count INTEGER
);

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
   deleted_at TIMESTAMP WITH TIME ZONE,
   UNIQUE (room_type_id, availability_date)
);

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
   status VARCHAR(30) NOT NULL DEFAULT 'PENDING_PAYMENT', -- PENDING_PAYMENT, CONFIRMED, CHECKED_IN, CHECKED_OUT, CANCELLED, EXPIRED, NO_SHOW, REFUNDED
   expires_at TIMESTAMP WITH TIME ZONE,

   -- Guest Info (can be different from user, using searchable encryption)
   guest_name VARCHAR(100) NOT NULL,
   guest_email VARCHAR(100) NOT NULL,
   guest_phone_encrypted VARCHAR(255),
   guest_phone_search_hash VARCHAR(64),
   special_requests TEXT,

   version BIGINT DEFAULT 0,
   created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
   updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
   deleted_at TIMESTAMP WITH TIME ZONE,

   currency VARCHAR(3) NOT NULL DEFAULT 'USD',
   deposit_amount DECIMAL(19, 4) NOT NULL DEFAULT 0.0000,
   requires_deposit BOOLEAN NOT NULL DEFAULT FALSE,
   payment_method VARCHAR(50),

   CONSTRAINT check_booking_dates CHECK (check_out_date > check_in_date),
   CONSTRAINT chk_pending_payment_has_expiry CHECK (status <> 'PENDING_PAYMENT' OR expires_at IS NOT NULL),
   CONSTRAINT chk_confirmed_no_expiry CHECK (status <> 'CONFIRMED' OR expires_at IS NULL),
   CONSTRAINT chk_expired_has_expiry CHECK (status <> 'EXPIRED' OR expires_at IS NOT NULL)
);

CREATE TABLE IF NOT EXISTS booking_status_logs (
   id UUID PRIMARY KEY,
   booking_id UUID NOT NULL REFERENCES bookings(id) ON DELETE CASCADE,
   old_status VARCHAR(30),
   new_status VARCHAR(30) NOT NULL,
   reason TEXT,
   changed_by UUID REFERENCES users(id),
   created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
   updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
   deleted_at TIMESTAMP WITH TIME ZONE,
   version BIGINT DEFAULT 0
);

CREATE TABLE IF NOT EXISTS inventory_operations (
   id UUID PRIMARY KEY,
   booking_id UUID NOT NULL REFERENCES bookings(id),
   room_type_id UUID NOT NULL REFERENCES room_types(id),
   availability_date DATE NOT NULL,
   operation_type VARCHAR(10) NOT NULL,
   num_rooms INTEGER NOT NULL,
   created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
   updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
   deleted_at TIMESTAMP WITH TIME ZONE,
   version BIGINT DEFAULT 0
);

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
   updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
   deleted_at TIMESTAMP WITH TIME ZONE,

   CONSTRAINT uq_transactions_provider_tx_id UNIQUE (provider_transaction_id)
);

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

CREATE TABLE IF NOT EXISTS outbox_events (
   id UUID PRIMARY KEY,
   aggregate_id UUID NOT NULL,
   aggregate_type VARCHAR(50) NOT NULL,
   event_type VARCHAR(100) NOT NULL,
   payload TEXT NOT NULL,
   created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
   updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
   deleted_at TIMESTAMP WITH TIME ZONE,
   version BIGINT DEFAULT 0,
   status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
   retry_count INTEGER NOT NULL DEFAULT 0,
   next_retry_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
   last_error TEXT,
   event_version INTEGER NOT NULL DEFAULT 1
);

CREATE TABLE IF NOT EXISTS social_accounts (
   id UUID PRIMARY KEY,
   user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
   provider VARCHAR(50) NOT NULL,
   provider_id VARCHAR(255) NOT NULL,
   version BIGINT DEFAULT 0,
   created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
   updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
   deleted_at TIMESTAMP WITH TIME ZONE,
   UNIQUE(provider, provider_id)
);

CREATE TABLE IF NOT EXISTS search_logs (
   id UUID PRIMARY KEY,
   user_id UUID REFERENCES users(id) ON DELETE SET NULL,
   query_text TEXT NOT NULL,
   country_code VARCHAR(10),
   ip_address VARCHAR(45),
   is_boosted BOOLEAN DEFAULT FALSE,

   version BIGINT DEFAULT 0,
   created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
   updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
   deleted_at TIMESTAMP WITH TIME ZONE
);

CREATE TABLE IF NOT EXISTS exchange_rates (
   id UUID PRIMARY KEY,
   from_currency VARCHAR(3) NOT NULL,
   to_currency VARCHAR(3) NOT NULL,
   rate DECIMAL(19, 6) NOT NULL,
   last_updated TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
   created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
   updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
   deleted_at TIMESTAMP WITH TIME ZONE,
   version BIGINT DEFAULT 0,
   provider VARCHAR(50),
   fetched_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,

   UNIQUE(from_currency, to_currency)
);

CREATE TABLE IF NOT EXISTS user_passkeys (
   id UUID PRIMARY KEY,
   version BIGINT,
   created_at TIMESTAMP WITH TIME ZONE NOT NULL,
   updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
   deleted_at TIMESTAMP WITH TIME ZONE,
   user_id UUID NOT NULL REFERENCES users(id),
   credential_id TEXT NOT NULL,
   public_key TEXT NOT NULL,
   sign_count BIGINT NOT NULL,
   label VARCHAR(255),
   aaguid VARCHAR(255)
);

CREATE TABLE IF NOT EXISTS user_two_factor (
   id UUID PRIMARY KEY,
   user_id UUID NOT NULL UNIQUE REFERENCES users(id) ON DELETE CASCADE,
   secret_key VARCHAR(255) NOT NULL,
   is_enabled BOOLEAN DEFAULT FALSE,
   backup_codes TEXT, -- Hashed JSON list of backup codes
   version BIGINT DEFAULT 0,
   created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
   updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
   deleted_at TIMESTAMP WITH TIME ZONE
);

CREATE TABLE IF NOT EXISTS partner_legal_profiles (
   id UUID PRIMARY KEY,
   partner_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
   business_registration_number VARCHAR(255) NOT NULL,
   tax_code VARCHAR(255) NOT NULL,
   legal_owner_name VARCHAR(255) NOT NULL,
   is_active BOOLEAN NOT NULL DEFAULT TRUE,
   version BIGINT NOT NULL DEFAULT 0,
   created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
   updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
   deleted_at TIMESTAMP WITH TIME ZONE
);

CREATE TABLE IF NOT EXISTS processed_events (
   event_id UUID NOT NULL,
   consumer_group VARCHAR(100) NOT NULL,
   processed_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
   status VARCHAR(20) NOT NULL DEFAULT 'PROCESSING',
   updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
   lease_until TIMESTAMP WITH TIME ZONE,
   PRIMARY KEY (event_id, consumer_group)
);

-- Reviews
CREATE TABLE IF NOT EXISTS reviews (
   id UUID PRIMARY KEY,
   booking_id UUID NOT NULL UNIQUE,
   property_id UUID NOT NULL,
   user_id UUID, -- Nullable to support anonymization (ON DELETE SET NULL)
   rating INT NOT NULL CHECK (rating >= 1 AND rating <= 5),
   comment VARCHAR(1000) CHECK (char_length(comment) >= 10),
   reply TEXT,
   status VARCHAR(20) NOT NULL DEFAULT 'PUBLISHED',
   reply_updated_at TIMESTAMP WITH TIME ZONE,
   deleted_at TIMESTAMP WITH TIME ZONE,
   deleted_by UUID,
   deletion_reason VARCHAR(255),
   moderated_at TIMESTAMP WITH TIME ZONE,
   moderated_by UUID,
   moderation_reason VARCHAR(255),
   created_at TIMESTAMP WITH TIME ZONE NOT NULL,
   updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
   version BIGINT NOT NULL DEFAULT 0,

   CONSTRAINT reviews_booking_id_fkey FOREIGN KEY (booking_id) REFERENCES bookings(id) ON DELETE RESTRICT,
   CONSTRAINT reviews_property_id_fkey FOREIGN KEY (property_id) REFERENCES properties(id) ON DELETE CASCADE,
   CONSTRAINT reviews_user_id_fkey FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE SET NULL,
   FOREIGN KEY (deleted_by) REFERENCES users(id) ON DELETE SET NULL,
   FOREIGN KEY (moderated_by) REFERENCES users(id) ON DELETE SET NULL
);

-- Price Rules & Versioning
CREATE TABLE IF NOT EXISTS price_rules (
   id UUID PRIMARY KEY,
   property_id UUID NOT NULL REFERENCES properties(id) ON DELETE CASCADE,
   room_type_id UUID REFERENCES room_types(id) ON DELETE CASCADE,
   name VARCHAR(100) NOT NULL,
   rule_type VARCHAR(20) NOT NULL,
   start_date DATE,
   end_date DATE,
   adjustment_type VARCHAR(20) NOT NULL,
   adjustment_value NUMERIC(12,2) NOT NULL,
   occupancy_threshold INT,
   priority INT NOT NULL DEFAULT 0,
   is_active BOOLEAN NOT NULL DEFAULT true,
   version INT NOT NULL DEFAULT 0,
   deleted_at TIMESTAMP WITH TIME ZONE,
   created_at TIMESTAMP WITH TIME ZONE NOT NULL,
   updated_at TIMESTAMP WITH TIME ZONE NOT NULL,

   CONSTRAINT chk_rule_adjustment_nonzero CHECK (adjustment_value <> 0.00),
   CONSTRAINT chk_rule_priority_nonnegative CHECK (priority >= 0),
   CONSTRAINT chk_rule_dates_valid CHECK (start_date IS NULL OR end_date IS NULL OR start_date <= end_date),
   CONSTRAINT chk_rule_occupancy_valid CHECK (rule_type <> 'OCCUPANCY' OR occupancy_threshold > 0)
);

CREATE TABLE IF NOT EXISTS price_rule_versions (
   id UUID PRIMARY KEY,
   price_rule_id UUID NOT NULL REFERENCES price_rules(id) ON DELETE CASCADE,
   version INT NOT NULL,
   name VARCHAR(100) NOT NULL,
   rule_type VARCHAR(20) NOT NULL,
   start_date DATE,
   end_date DATE,
   adjustment_type VARCHAR(20) NOT NULL,
   adjustment_value NUMERIC(12,2) NOT NULL,
   occupancy_threshold INT,
   priority INT NOT NULL,
   is_active BOOLEAN NOT NULL,
   created_at TIMESTAMP WITH TIME ZONE NOT NULL,
   created_by UUID NOT NULL,

   CONSTRAINT uq_rule_version UNIQUE (price_rule_id, version),
   CONSTRAINT chk_version_occupancy_valid CHECK (rule_type <> 'OCCUPANCY' OR occupancy_threshold > 0)
);

-- Coupon Reservations
CREATE TABLE IF NOT EXISTS coupon_reservations (
   id UUID PRIMARY KEY,
   coupon_id UUID NOT NULL REFERENCES coupons(id) ON DELETE CASCADE,
   booking_session_id VARCHAR(100) NOT NULL UNIQUE,
   reservation_token VARCHAR(255) NOT NULL UNIQUE,
   customer_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
   property_id UUID NOT NULL REFERENCES properties(id) ON DELETE CASCADE,
   status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
   reserved_at TIMESTAMP WITH TIME ZONE NOT NULL,
   expires_at TIMESTAMP WITH TIME ZONE NOT NULL,
   CONSTRAINT chk_reservation_status CHECK (status IN ('ACTIVE', 'CONSUMED', 'EXPIRED'))
);

-- Booking Price Breakdowns
CREATE TABLE IF NOT EXISTS booking_price_breakdowns (
   id UUID PRIMARY KEY,
   booking_id UUID NOT NULL REFERENCES bookings(id) ON DELETE CASCADE,
   stay_date DATE NOT NULL,
   base_price NUMERIC(12,2) NOT NULL,
   seasonal_adjustment NUMERIC(12,2) NOT NULL DEFAULT 0.00,
   weekend_adjustment NUMERIC(12,2) NOT NULL DEFAULT 0.00,
   occupancy_adjustment NUMERIC(12,2) NOT NULL DEFAULT 0.00,
   coupon_discount NUMERIC(12,2) NOT NULL DEFAULT 0.00,
   final_price NUMERIC(12,2) NOT NULL,
   applied_coupon_id UUID,
   applied_coupon_code VARCHAR(50),
   created_at TIMESTAMP WITH TIME ZONE NOT NULL,
   UNIQUE(booking_id, stay_date),
   CONSTRAINT chk_breakdown_final_price CHECK (final_price >= 0)
);

CREATE TABLE IF NOT EXISTS booking_applied_rule_versions (
   id UUID PRIMARY KEY,
   booking_breakdown_id UUID NOT NULL REFERENCES booking_price_breakdowns(id) ON DELETE CASCADE,
   rule_version_id UUID NOT NULL REFERENCES price_rule_versions(id) ON DELETE RESTRICT,
   adjustment_amount NUMERIC(12,2) NOT NULL,
   created_at TIMESTAMP WITH TIME ZONE NOT NULL,
   CONSTRAINT chk_applied_rule_adj_nonzero CHECK (adjustment_amount <> 0)
);

-- Coupon scoping & reservation tracking
ALTER TABLE coupons ADD COLUMN property_id UUID REFERENCES properties(id) ON DELETE CASCADE;
ALTER TABLE coupons ADD COLUMN reserved_count INT NOT NULL DEFAULT 0;

ALTER TABLE coupons ADD CONSTRAINT chk_coupon_reserved_nonnegative CHECK (reserved_count >= 0);
ALTER TABLE coupons ADD CONSTRAINT chk_coupon_used_nonnegative CHECK (used_count >= 0);
ALTER TABLE coupons ADD CONSTRAINT chk_coupon_limit_nonnegative CHECK (usage_limit IS NULL OR usage_limit >= 0);

-- 3. Indexes
CREATE INDEX idx_users_username ON users(username);
CREATE INDEX idx_users_email ON users(email);
CREATE INDEX idx_roles_name ON roles(name);
CREATE INDEX idx_permissions_name ON permissions(name);
CREATE INDEX idx_user_profiles_user_id ON user_profiles(user_id);
CREATE INDEX idx_user_profiles_phone_search_hash ON user_profiles(phone_search_hash);

CREATE INDEX idx_properties_owner_id ON properties(owner_id);
CREATE INDEX idx_properties_city ON properties(city);
CREATE INDEX idx_room_types_property_id ON room_types(property_id);
CREATE INDEX idx_room_availability_date ON room_availability(availability_date);
CREATE INDEX idx_bookings_user_id ON bookings(user_id);
CREATE INDEX idx_bookings_status ON bookings(status);
CREATE INDEX idx_bookings_check_in ON bookings(check_in_date);
CREATE INDEX idx_bookings_guest_phone_search_hash ON bookings(guest_phone_search_hash);
CREATE INDEX idx_transactions_booking_id ON transactions(booking_id);
CREATE INDEX idx_coupons_code ON coupons(code);
CREATE INDEX idx_media_entity ON media(entity_id, entity_type);

CREATE INDEX idx_outbox_processing ON outbox_events(status, next_retry_at) WHERE status IN ('PENDING', 'PROCESSING');
CREATE INDEX idx_social_accounts_user_id ON social_accounts(user_id);
CREATE INDEX idx_social_accounts_provider_id ON social_accounts(provider, provider_id);
CREATE INDEX idx_search_logs_query ON search_logs(query_text);
CREATE INDEX idx_search_logs_created ON search_logs(created_at);

CREATE INDEX idx_user_passkeys_user_id ON user_passkeys(user_id);
CREATE UNIQUE INDEX idx_user_passkeys_credential_id_active ON user_passkeys(credential_id) WHERE deleted_at IS NULL;

CREATE INDEX idx_user_two_factor_user_id ON user_two_factor(user_id);
CREATE INDEX idx_partner_legal_profiles_partner_active ON partner_legal_profiles(partner_id, is_active);
CREATE INDEX IF NOT EXISTS idx_processed_events_time ON processed_events(processed_at);

-- Registration inbox & DLT indexes
CREATE INDEX IF NOT EXISTS idx_reg_inbox_status_retry ON registration_inbox(status, next_retry_at) WHERE status IN ('PENDING', 'PROCESSING');
CREATE INDEX IF NOT EXISTS idx_reg_dlt_status ON registration_dlt(status);

-- Reviews indexes
CREATE INDEX idx_reviews_property_created ON reviews(property_id, created_at DESC) WHERE deleted_at IS NULL AND status = 'PUBLISHED';
CREATE INDEX idx_reviews_user ON reviews(user_id) WHERE deleted_at IS NULL;
CREATE INDEX idx_reviews_booking ON reviews(booking_id);
CREATE INDEX idx_reviews_moderation ON reviews(status, created_at) WHERE deleted_at IS NULL;

-- Pricing audit & reservation indexes
CREATE INDEX idx_coupon_res_active_expiry ON coupon_reservations(expires_at) WHERE status = 'ACTIVE';
CREATE INDEX idx_audit_old_values ON pricing_audit_logs USING gin (old_values);
CREATE INDEX idx_audit_new_values ON pricing_audit_logs USING gin (new_values);
CREATE INDEX idx_audit_correlation ON pricing_audit_logs(correlation_id);

-- Foreign Key Indexes
-- Every FK column should be indexed to avoid sequential scans during
-- parent-table DELETE/UPDATE and referential integrity checks.
CREATE INDEX idx_properties_cancellation_policy_id ON properties(cancellation_policy_id);
CREATE INDEX idx_bookings_room_type_id ON bookings(room_type_id);
CREATE INDEX idx_bookings_coupon_id ON bookings(coupon_id);
CREATE INDEX idx_booking_status_logs_booking_id ON booking_status_logs(booking_id);
CREATE INDEX idx_booking_status_logs_changed_by ON booking_status_logs(changed_by);
CREATE INDEX idx_search_logs_user_id ON search_logs(user_id);
CREATE INDEX idx_partner_legal_profiles_partner_id ON partner_legal_profiles(partner_id);
CREATE INDEX idx_reviews_property_id ON reviews(property_id);
CREATE INDEX idx_reviews_user_id ON reviews(user_id);
CREATE INDEX idx_reviews_deleted_by ON reviews(deleted_by);
CREATE INDEX idx_reviews_moderated_by ON reviews(moderated_by);
CREATE INDEX idx_price_rules_property_id ON price_rules(property_id);
CREATE INDEX idx_price_rules_room_type_id ON price_rules(room_type_id);
CREATE INDEX idx_price_rule_versions_price_rule_id ON price_rule_versions(price_rule_id);
CREATE INDEX idx_coupon_reservations_coupon_id ON coupon_reservations(coupon_id);
CREATE INDEX idx_coupon_reservations_customer_id ON coupon_reservations(customer_id);
CREATE INDEX idx_coupon_reservations_property_id ON coupon_reservations(property_id);
CREATE INDEX idx_booking_price_breakdowns_booking_id ON booking_price_breakdowns(booking_id);
CREATE INDEX idx_booking_applied_rule_versions_breakdown_id ON booking_applied_rule_versions(booking_breakdown_id);
CREATE INDEX idx_booking_applied_rule_versions_rule_version_id ON booking_applied_rule_versions(rule_version_id);

-- Booking lifecycle / expiration worker indexes
CREATE INDEX idx_bookings_status_expires
   ON bookings(status, expires_at)
   WHERE expires_at IS NOT NULL AND deleted_at IS NULL;
CREATE INDEX idx_inv_ops_booking ON inventory_operations(booking_id);
CREATE INDEX idx_inv_ops_type ON inventory_operations(operation_type, booking_id);

-- Coupon release retry scheduler indexes
CREATE UNIQUE INDEX ux_coupon_retry_pending
ON coupon_release_retries (booking_id, coupon_id)
WHERE status = 'PENDING';

CREATE INDEX idx_coupon_retry_pending_sched
ON coupon_release_retries (status, next_attempt_at);

COMMENT ON COLUMN user_passkeys.deleted_at IS 'Timestamp of soft deletion';

-- 4. Seed Data
-- Default Ranks
INSERT INTO ranks (id, name, min_points, benefits)
VALUES
   (gen_random_uuid(), 'BRONZE', 0, 'Basic member benefits'),
   (gen_random_uuid(), 'SILVER', 1000, '5% discount on bookings'),
   (gen_random_uuid(), 'GOLD', 5000, '10% discount and late checkout'),
   (gen_random_uuid(), 'DIAMOND', 10000, '15% discount, room upgrades, and priority support')
ON CONFLICT (name) DO NOTHING;

-- Default Roles
INSERT INTO roles (id, name, description)
VALUES
   (gen_random_uuid(), 'ROLE_ADMIN', 'System administrator with full access'),
   (gen_random_uuid(), 'ROLE_MANAGER', 'Manager with access to specific property management'),
   (gen_random_uuid(), 'ROLE_PARTNER', 'Property owner or hotel manager'),
   (gen_random_uuid(), 'ROLE_DRIVER', 'Cab or transportation service provider'),
   (gen_random_uuid(), 'ROLE_USER', 'Standard registered user')
ON CONFLICT (name) DO NOTHING;

-- Default Permissions
INSERT INTO permissions (id, name, description)
VALUES
   ('00000000-0000-0000-0000-000000000001', 'user:read', 'Permission to view user details'),
   ('00000000-0000-0000-0000-000000000002', 'user:write', 'Permission to create/edit users'),
   ('00000000-0000-0000-0000-000000000003', 'booking:read', 'Permission to view bookings'),
   ('00000000-0000-0000-0000-000000000004', 'booking:write', 'Permission to create/edit bookings'),
   ('00000000-0000-0000-0000-000000000005', 'property:write', 'Permission to manage properties'),
   ('00000000-0000-0000-0000-000000000006', 'property:read', 'View property listings'),
   ('00000000-0000-0000-0000-000000000007', 'property:delete', 'Delete property listings'),
   ('00000000-0000-0000-0000-000000000008', 'room:read', 'View room details'),
   ('00000000-0000-0000-0000-000000000009', 'room:write', 'Create or edit rooms'),
   ('00000000-0000-0000-0000-000000000010', 'room:delete', 'Delete rooms'),
   ('00000000-0000-0000-0000-000000000011', 'booking:cancel', 'Cancel a booking'),
   ('00000000-0000-0000-0000-000000000012', 'booking:manage', 'Approve or reject booking requests'),
   ('00000000-0000-0000-0000-000000000013', 'review:read', 'View reviews'),
   ('00000000-0000-0000-0000-000000000014', 'review:write', 'Write a review'),
   ('00000000-0000-0000-0000-000000000015', 'review:delete', 'Delete reviews'),
   ('00000000-0000-0000-0000-000000000016', 'review:reply', 'Reply to customer reviews'),
   ('00000000-0000-0000-0000-000000000017', 'earning:read', 'View earnings and transaction history'),
   ('00000000-0000-0000-0000-000000000018', 'partner:profile:read', 'View partner profile information'),
   ('00000000-0000-0000-0000-000000000019', 'partner:profile:write', 'Update partner profile information'),
   ('00000000-0000-0000-0000-000000000020', 'ride:read', 'View ride history/requests'),
   ('00000000-0000-0000-0000-000000000021', 'ride:write', 'Book or request a ride'),
   ('00000000-0000-0000-0000-000000000022', 'ride:manage', 'Manage/Accept ride requests'),
   ('00000000-0000-0000-0000-000000000023', 'vehicle:read', 'View vehicle details'),
   ('00000000-0000-0000-0000-000000000024', 'vehicle:write', 'Register or edit vehicles'),
   ('00000000-0000-0000-0000-000000000025', 'vehicle:delete', 'Remove vehicles')
ON CONFLICT (name) DO NOTHING;

-- Map Permissions to Roles
-- ROLE_ADMIN
INSERT INTO roles_permissions (role_id, permission_id)
SELECT r.id, p.id FROM roles r, permissions p WHERE r.name = 'ROLE_ADMIN' ON CONFLICT DO NOTHING;

-- ROLE_PARTNER
INSERT INTO roles_permissions (role_id, permission_id)
SELECT r.id, p.id FROM roles r, permissions p WHERE r.name = 'ROLE_PARTNER'
AND p.name IN ('property:read', 'property:write', 'property:delete', 'room:read', 'room:write', 'room:delete', 'booking:read', 'booking:manage', 'booking:cancel', 'review:read', 'review:reply', 'earning:read', 'partner:profile:read', 'partner:profile:write')
ON CONFLICT DO NOTHING;

-- ROLE_DRIVER
INSERT INTO roles_permissions (role_id, permission_id)
SELECT r.id, p.id FROM roles r, permissions p WHERE r.name = 'ROLE_DRIVER'
AND p.name IN ('ride:read', 'ride:manage', 'vehicle:read', 'vehicle:write', 'vehicle:delete', 'earning:read', 'partner:profile:read', 'partner:profile:write')
ON CONFLICT DO NOTHING;

-- ROLE_USER
INSERT INTO roles_permissions (role_id, permission_id)
SELECT r.id, p.id FROM roles r, permissions p WHERE r.name = 'ROLE_USER'
AND p.name IN ('property:read', 'room:read', 'booking:read', 'booking:write', 'booking:cancel', 'review:read', 'review:write', 'ride:read', 'ride:write', 'vehicle:read', 'user:read')
ON CONFLICT DO NOTHING;

-- Default Currencies
INSERT INTO currencies (id, code, name, symbol, is_base)
VALUES
   (gen_random_uuid(), 'USD', 'US Dollar', '$', TRUE),
   (gen_random_uuid(), 'VND', 'Vietnamese Dong', '₫', FALSE)
ON CONFLICT (code) DO NOTHING;

-- Standard Amenities
-- General Amenities
INSERT INTO amenities (id, name, category, icon_url) VALUES
(gen_random_uuid(), 'Free Wi-Fi', 'GENERAL', 'wifi'),
(gen_random_uuid(), 'Swimming Pool', 'GENERAL', 'pool'),
(gen_random_uuid(), 'Parking', 'GENERAL', 'parking'),
(gen_random_uuid(), 'Airport Shuttle', 'GENERAL', 'shuttle'),
(gen_random_uuid(), 'Gym / Fitness Center', 'GENERAL', 'gym'),
(gen_random_uuid(), 'Spa & Wellness Center', 'GENERAL', 'spa'),
(gen_random_uuid(), '24-Hour Front Desk', 'GENERAL', 'reception'),
(gen_random_uuid(), 'Elevator', 'GENERAL', 'elevator'),
(gen_random_uuid(), 'Family Rooms', 'GENERAL', 'family'),
(gen_random_uuid(), 'Pet Friendly', 'GENERAL', 'pet')
ON CONFLICT (name) DO NOTHING;

-- Room Amenities
INSERT INTO amenities (id, name, category, icon_url) VALUES
(gen_random_uuid(), 'Air Conditioning', 'ROOM', 'ac'),
(gen_random_uuid(), 'Flat-screen TV', 'ROOM', 'tv'),
(gen_random_uuid(), 'Balcony', 'ROOM', 'balcony'),
(gen_random_uuid(), 'Minibar', 'ROOM', 'minibar'),
(gen_random_uuid(), 'Safe', 'ROOM', 'safe'),
(gen_random_uuid(), 'Work Desk', 'ROOM', 'desk'),
(gen_random_uuid(), 'Ironing Facilities', 'ROOM', 'iron')
ON CONFLICT (name) DO NOTHING;

-- Bathroom Amenities
INSERT INTO amenities (id, name, category, icon_url) VALUES
(gen_random_uuid(), 'Private Bathroom', 'BATHROOM', 'bathroom'),
(gen_random_uuid(), 'Hairdryer', 'BATHROOM', 'hairdryer'),
(gen_random_uuid(), 'Free Toiletries', 'BATHROOM', 'toiletries'),
(gen_random_uuid(), 'Bathrobe', 'BATHROOM', 'bathrobe'),
(gen_random_uuid(), 'Shower', 'BATHROOM', 'shower'),
(gen_random_uuid(), 'Bathtub', 'BATHROOM', 'bathtub')
ON CONFLICT (name) DO NOTHING;

-- Kitchen Amenities
INSERT INTO amenities (id, name, category, icon_url) VALUES
(gen_random_uuid(), 'Refrigerator', 'KITCHEN', 'refrigerator'),
(gen_random_uuid(), 'Microwave', 'KITCHEN', 'microwave'),
(gen_random_uuid(), 'Electric Kettle', 'KITCHEN', 'kettle'),
(gen_random_uuid(), 'Kitchenware', 'KITCHEN', 'kitchenware'),
(gen_random_uuid(), 'Dining Table', 'KITCHEN', 'table')
ON CONFLICT (name) DO NOTHING;
