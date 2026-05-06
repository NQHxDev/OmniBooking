-- Seed Roles and Permissions

-- Insert Default Ranks
INSERT INTO ranks (id, name, min_points, benefits, created_at, updated_at)
VALUES
   (gen_random_uuid(), 'BRONZE', 0, 'Basic member benefits', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
   (gen_random_uuid(), 'SILVER', 1000, '5% discount on bookings', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
   (gen_random_uuid(), 'GOLD', 5000, '10% discount and late checkout', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
   (gen_random_uuid(), 'DIAMOND', 10000, '15% discount, room upgrades, and priority support', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
ON CONFLICT (name) DO NOTHING;

-- Insert Default Roles
INSERT INTO roles (id, name, description, created_at, updated_at)
VALUES
   (gen_random_uuid(), 'ROLE_ADMIN', 'System administrator with full access', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
   (gen_random_uuid(), 'ROLE_MANAGER', 'Manager with access to specific property management', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
   (gen_random_uuid(), 'ROLE_USER', 'Standard registered user', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
ON CONFLICT (name) DO NOTHING;

-- Insert Default Permissions
INSERT INTO permissions (id, name, description, created_at, updated_at)
VALUES
   (gen_random_uuid(), 'user:read', 'Permission to view user details', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
   (gen_random_uuid(), 'user:write', 'Permission to create/edit users', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
   (gen_random_uuid(), 'booking:read', 'Permission to view bookings', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
   (gen_random_uuid(), 'booking:write', 'Permission to create/edit bookings', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
   (gen_random_uuid(), 'property:write', 'Permission to manage properties', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
ON CONFLICT (name) DO NOTHING;

-- Link Permissions to ADMIN Role (All permissions)
INSERT INTO roles_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM roles r, permissions p
WHERE r.name = 'ROLE_ADMIN'
ON CONFLICT DO NOTHING;

-- Link Permissions to USER Role
INSERT INTO roles_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM roles r, permissions p
WHERE r.name = 'ROLE_USER' AND p.name IN ('user:read', 'booking:read', 'booking:write')
ON CONFLICT DO NOTHING;
