-- media_progress_queued.lua
-- Idempotent: Mark an image as queued to Kafka
-- KEYS[1] = media:progress:{propertyId}             (Hash)
-- KEYS[2] = media:progress:{propertyId}:queued_ids   (Set)
-- KEYS[3] = media:progress:active                     (Sorted Set index)
-- ARGV[1] = correlationId
-- ARGV[2] = epoch millis timestamp
-- ARGV[3] = uploadWeight
-- ARGV[4] = processingWeight
-- ARGV[5] = propertyId (for ZADD member)

-- Step 1: Deduplication via Set
local added = redis.call('SADD', KEYS[2], ARGV[1])
if added == 0 then
   return 0 -- Duplicate, no state change
end

-- Step 2: Increment queued counter + update timestamp
redis.call('HINCRBY', KEYS[1], 'queued', 1)
redis.call('HSET', KEYS[1], 'lastUpdatedAt', ARGV[2])

-- Step 3: Update ZSET index
redis.call('ZADD', KEYS[3], ARGV[2], ARGV[5])

-- Step 4: Recompute percentage
local total     = tonumber(redis.call('HGET', KEYS[1], 'total') or '0')
local queued    = tonumber(redis.call('HGET', KEYS[1], 'queued') or '0')
local processed = tonumber(redis.call('HGET', KEYS[1], 'processed') or '0')

local uploadW  = tonumber(ARGV[3])
local processW = tonumber(ARGV[4])

local pct = 0
if total > 0 then
   pct = math.floor((queued / total * uploadW) + (processed / total * processW))
end
redis.call('HSET', KEYS[1], 'percentage', tostring(pct))

-- Step 5: Clear STALLED if was stalled (processing resumed)
local currentStatus = redis.call('HGET', KEYS[1], 'status')
if currentStatus == 'STALLED' then
   redis.call('HSET', KEYS[1], 'status', 'PROCESSING')
end

return 1
