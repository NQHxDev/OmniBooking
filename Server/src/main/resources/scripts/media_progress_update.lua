-- media_progress_update.lua
-- Idempotent: Mark an image as processed or failed
-- KEYS[1] = media:progress:{propertyId}                 (Hash)
-- KEYS[2] = media:progress:{propertyId}:completed_ids   (Set — or :failed_ids)
-- KEYS[3] = media:progress:active                        (Sorted Set index)
-- ARGV[1] = correlationId
-- ARGV[2] = field name ("processed" or "failed")
-- ARGV[3] = epoch millis timestamp
-- ARGV[4] = uploadWeight (e.g. "25")
-- ARGV[5] = processingWeight (e.g. "75")
-- ARGV[6] = propertyId (for ZADD/ZREM member)

-- Step 1: Deduplication check and transition from opposite state
local added = redis.call('SADD', KEYS[2], ARGV[1])
if added == 0 then
   return 0 -- Duplicate. No state change.
end

-- Check if it existed in the opposite set (failed_ids/completed_ids) due to retry
local oppositeSet = ""
local oppositeField = ""
if ARGV[2] == "processed" then
   oppositeSet = KEYS[1] .. ":failed_ids"
   oppositeField = "failed"
else
   oppositeSet = KEYS[1] .. ":completed_ids"
   oppositeField = "processed"
end

local wasInOpposite = redis.call('SREM', oppositeSet, ARGV[1])
if wasInOpposite == 1 then
   redis.call('HINCRBY', KEYS[1], oppositeField, -1)
end


-- Step 2: Atomic counter increment + timestamp
redis.call('HINCRBY', KEYS[1], ARGV[2], 1)
redis.call('HSET', KEYS[1], 'lastUpdatedAt', ARGV[3])

-- Step 3: Update ZSET index (O(log N) score update)
redis.call('ZADD', KEYS[3], ARGV[3], ARGV[6])

-- Step 4: Recompute percentage
local total     = tonumber(redis.call('HGET', KEYS[1], 'total') or '0')
local queued    = tonumber(redis.call('HGET', KEYS[1], 'queued') or '0')
local processed = tonumber(redis.call('HGET', KEYS[1], 'processed') or '0')
local failed    = tonumber(redis.call('HGET', KEYS[1], 'failed') or '0')

local uploadW  = tonumber(ARGV[4])
local processW = tonumber(ARGV[5])

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

-- Step 6: Auto-detect terminal states
if (processed + failed) >= total then
   if failed == 0 then
      redis.call('HSET', KEYS[1], 'status', 'COMPLETED')
   elseif processed == 0 then
      redis.call('HSET', KEYS[1], 'status', 'FAILED')
   else
      redis.call('HSET', KEYS[1], 'status', 'PARTIAL_SUCCESS')
   end
   -- Remove from active ZSET (no longer needs stall monitoring)
   redis.call('ZREM', KEYS[3], ARGV[6])
end

return 1 -- State changed successfully
