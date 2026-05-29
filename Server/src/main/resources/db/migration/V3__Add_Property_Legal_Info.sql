-- V3: Add Property Legal Info and Seed Standard Amenities

-- 1. Add legal info columns to properties
ALTER TABLE properties ADD COLUMN business_registration_number VARCHAR(100);
ALTER TABLE properties ADD COLUMN tax_code VARCHAR(50);
ALTER TABLE properties ADD COLUMN legal_owner_name VARCHAR(100);

-- 2. Seed standard amenities
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
