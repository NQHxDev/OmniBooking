-- Init Database Core

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
   password VARCHAR(255) NOT NULL,
   is_active BOOLEAN DEFAULT TRUE,
   version BIGINT DEFAULT 0,
   created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
   updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
   deleted_at TIMESTAMP WITH TIME ZONE
);

-- Create User_Profiles Table
CREATE TABLE IF NOT EXISTS user_profiles (
   user_id UUID PRIMARY KEY REFERENCES users(id) ON DELETE CASCADE,
   first_name VARCHAR(50),
   last_name VARCHAR(50),
   phone_number VARCHAR(20) UNIQUE,
   avatar_url VARCHAR(255),
   points INTEGER DEFAULT 0,
   rank_id UUID REFERENCES ranks(id) ON DELETE SET NULL,
   version BIGINT DEFAULT 0,
   updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
   deleted_at TIMESTAMP WITH TIME ZONE
);

-- Create Role_Permissions Junction Table
CREATE TABLE IF NOT EXISTS roles_permissions (
   role_id UUID REFERENCES roles(id) ON DELETE CASCADE,
   permission_id UUID REFERENCES permissions(id) ON DELETE CASCADE,
   PRIMARY KEY (role_id, permission_id)
);

-- Create User_Roles Junction Table
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
