-- V6: Create Partner Legal Profiles and alter Properties legal fields length

CREATE TABLE partner_legal_profiles (
   id UUID PRIMARY KEY,
   partner_id UUID NOT NULL,
   business_registration_number VARCHAR(255) NOT NULL,
   tax_code VARCHAR(255) NOT NULL,
   legal_owner_name VARCHAR(255) NOT NULL,
   is_active BOOLEAN NOT NULL DEFAULT TRUE,
   version BIGINT NOT NULL DEFAULT 0,
   created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
   updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
   deleted_at TIMESTAMP WITH TIME ZONE,
   CONSTRAINT fk_partner_legal_profiles_partner FOREIGN KEY (partner_id) REFERENCES users(id) ON DELETE CASCADE
);

CREATE INDEX idx_partner_legal_profiles_partner_active ON partner_legal_profiles(partner_id, is_active);

ALTER TABLE properties ALTER COLUMN business_registration_number TYPE VARCHAR(255);
ALTER TABLE properties ALTER COLUMN tax_code TYPE VARCHAR(255);
ALTER TABLE properties ALTER COLUMN legal_owner_name TYPE VARCHAR(255);
