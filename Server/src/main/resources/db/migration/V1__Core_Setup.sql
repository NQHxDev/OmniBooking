-- V1: Init Database Core (Users, Roles, Profiles)

-- Create Roles Table
CREATE TABLE IF NOT EXISTS roles (
   id UUID PRIMARY KEY,
   name VARCHAR(50) UNIQUE NOT NULL,
   description VARCHAR(255),
   version BIGINT DEFAULT 0,
   created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
   updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
   deleted_at TIMESTAMP WITH TIME ZONE
);

-- Create Permissions Table
CREATE TABLE IF NOT EXISTS permissions (
   id UUID PRIMARY KEY,
   name VARCHAR(50) UNIQUE NOT NULL,
   description VARCHAR(255),
   version BIGINT DEFAULT 0,
   created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
   updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
   deleted_at TIMESTAMP WITH TIME ZONE
);

-- Create Ranks Table
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

-- Create Users Table
CREATE TABLE IF NOT EXISTS users (
   id UUID PRIMARY KEY,
   username VARCHAR(50) UNIQUE NOT NULL,
   email VARCHAR(100) UNIQUE NOT NULL,
   password VARCHAR(255), -- Nullable for social login users
   is_active BOOLEAN DEFAULT TRUE,
   version BIGINT DEFAULT 0,
   created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
   updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
   deleted_at TIMESTAMP WITH TIME ZONE
);

-- Create User_Profiles Table
CREATE TABLE IF NOT EXISTS user_profiles (
   user_id UUID PRIMARY KEY REFERENCES users(id) ON DELETE CASCADE,
   display_name VARCHAR(100),
   phone_number VARCHAR(20) UNIQUE,
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

-- Create Indexes
CREATE INDEX idx_users_username ON users(username);
CREATE INDEX idx_users_email ON users(email);
CREATE INDEX idx_roles_name ON roles(name);
CREATE INDEX idx_permissions_name ON permissions(name);
CREATE INDEX idx_user_profiles_user_id ON user_profiles(user_id);

-- SEED DATA --

-- Insert Default Ranks
INSERT INTO ranks (id, name, min_points, benefits)
VALUES
   (gen_random_uuid(), 'BRONZE', 0, 'Basic member benefits'),
   (gen_random_uuid(), 'SILVER', 1000, '5% discount on bookings'),
   (gen_random_uuid(), 'GOLD', 5000, '10% discount and late checkout'),
   (gen_random_uuid(), 'DIAMOND', 10000, '15% discount, room upgrades, and priority support')
ON CONFLICT (name) DO NOTHING;

-- Insert Default Roles
INSERT INTO roles (id, name, description)
VALUES
   (gen_random_uuid(), 'ROLE_ADMIN', 'System administrator with full access'),
   (gen_random_uuid(), 'ROLE_MANAGER', 'Manager with access to specific property management'),
   (gen_random_uuid(), 'ROLE_PARTNER', 'Property owner or hotel manager'),
   (gen_random_uuid(), 'ROLE_DRIVER', 'Cab or transportation service provider'),
   (gen_random_uuid(), 'ROLE_USER', 'Standard registered user')
ON CONFLICT (name) DO NOTHING;

-- Insert Default Permissions
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
