-- V1.2: Add Partner and Cab Roles and Permissions

-- Insert New Roles
INSERT INTO roles (id, name, description, created_at, updated_at)
VALUES
   (gen_random_uuid(), 'ROLE_PARTNER', 'Property owner or hotel manager', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
   (gen_random_uuid(), 'ROLE_DRIVER', 'Cab or transportation service provider', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
ON CONFLICT (name) DO NOTHING;

-- Insert Granular Permissions
INSERT INTO permissions (id, name, description, created_at, updated_at)
VALUES
   -- Property & Room Management
   (gen_random_uuid(), 'property:read', 'View property listings', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
   (gen_random_uuid(), 'property:delete', 'Delete property listings', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
   (gen_random_uuid(), 'room:read', 'View room details', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
   (gen_random_uuid(), 'room:write', 'Create or edit rooms', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
   (gen_random_uuid(), 'room:delete', 'Delete rooms', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
   
   -- Booking Management
   (gen_random_uuid(), 'booking:cancel', 'Cancel a booking', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
   (gen_random_uuid(), 'booking:manage', 'Approve or reject booking requests', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
   
   -- Reviews & Feedback
   (gen_random_uuid(), 'review:read', 'View reviews', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
   (gen_random_uuid(), 'review:write', 'Write a review', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
   (gen_random_uuid(), 'review:delete', 'Delete reviews', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
   (gen_random_uuid(), 'review:reply', 'Reply to customer reviews', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
   
   -- Financials
   (gen_random_uuid(), 'earning:read', 'View earnings and transaction history', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
   
   -- Partner Profile
   (gen_random_uuid(), 'partner:profile:read', 'View partner profile information', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
   (gen_random_uuid(), 'partner:profile:write', 'Update partner profile information', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
   
   -- Cab & Transportation
   (gen_random_uuid(), 'ride:read', 'View ride history/requests', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
   (gen_random_uuid(), 'ride:write', 'Book or request a ride', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
   (gen_random_uuid(), 'ride:manage', 'Manage/Accept ride requests', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
   (gen_random_uuid(), 'vehicle:read', 'View vehicle details', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
   (gen_random_uuid(), 'vehicle:write', 'Register or edit vehicles', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
   (gen_random_uuid(), 'vehicle:delete', 'Remove vehicles', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
ON CONFLICT (name) DO NOTHING;

-- Map Permissions to ROLE_PARTNER (Hosts)
INSERT INTO roles_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM roles r, permissions p
WHERE r.name = 'ROLE_PARTNER'
AND p.name IN (
   'property:read', 'property:write', 'property:delete',
   'room:read', 'room:write', 'room:delete',
   'booking:read', 'booking:manage', 'booking:cancel',
   'review:read', 'review:reply',
   'earning:read',
   'partner:profile:read', 'partner:profile:write'
)
ON CONFLICT DO NOTHING;

-- Map Permissions to ROLE_DRIVER (Drivers)
INSERT INTO roles_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM roles r, permissions p
WHERE r.name = 'ROLE_DRIVER'
AND p.name IN (
   'ride:read', 'ride:manage',
   'vehicle:read', 'vehicle:write', 'vehicle:delete',
   'earning:read',
   'partner:profile:read', 'partner:profile:write'
)
ON CONFLICT DO NOTHING;

-- Map Permissions to ROLE_USER (Customers)
INSERT INTO roles_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM roles r, permissions p
WHERE r.name = 'ROLE_USER'
AND p.name IN (
   'property:read', 'room:read',
   'booking:read', 'booking:write', 'booking:cancel',
   'review:read', 'review:write',
   'ride:read', 'ride:write',
   'vehicle:read'
)
ON CONFLICT DO NOTHING;

-- Map All Permissions to ROLE_ADMIN
INSERT INTO roles_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM roles r, permissions p
WHERE r.name = 'ROLE_ADMIN'
ON CONFLICT DO NOTHING;
