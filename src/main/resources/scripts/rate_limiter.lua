-- KEYS[1] = the rate limit key, e.g. "rate_limit:192.168.1.5"
-- ARGV[1] = bucket capacity (e.g. 10)
-- ARGV[2] = refill rate per second (e.g. 1)
-- ARGV[3] = current timestamp (seconds, passed from Java — never trust Redis's clock across instances)

local key = KEYS[1]
local capacity = tonumber(ARGV[1])
local refill_rate = tonumber(ARGV[2])
local now = tonumber(ARGV[3])

local bucket = redis.call("HMGET", key, "tokens", "last_refill")
local tokens = tonumber(bucket[1])
local last_refill = tonumber(bucket[2])

-- If bucket doesn't exist yet, initialize as full
if tokens == nil then
    tokens = capacity
    last_refill = now
end

-- Calculate refill since last check
local elapsed = math.max(0, now - last_refill)
local refill_amount = elapsed * refill_rate
tokens = math.min(capacity, tokens + refill_amount)

local allowed = 0
if tokens >= 1 then
    allowed = 1
    tokens = tokens - 1
end

-- Save updated state, with an expiry so idle clients' keys clean themselves up
redis.call("HMSET", key, "tokens", tokens, "last_refill", now)
redis.call("EXPIRE", key, 3600)

return allowed