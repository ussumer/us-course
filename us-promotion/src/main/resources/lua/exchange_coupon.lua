-- KEYS: 兑换位图、券缓存、用户领取计数、事件内容、待投递事件索引。
-- ARGV: 序列号、用户ID、事件ID、JSON消息、时间戳。
if redis.call('GETBIT', KEYS[1], ARGV[1]) == 1 then return 1 end
if redis.call('EXISTS', KEYS[2]) == 0 then return 2 end
local now = tonumber(redis.call('TIME')[1])
local beginTime = tonumber(redis.call('HGET', KEYS[2], 'issueBeginTime'))
local endTime = tonumber(redis.call('HGET', KEYS[2], 'issueEndTime'))
if (beginTime and now < beginTime) or (endTime and now > endTime) then return 3 end
if tonumber(redis.call('HGET', KEYS[2], 'totalNum')) <= 0 then return 4 end
local count = tonumber(redis.call('HGET', KEYS[3], ARGV[2])) or 0
if count >= tonumber(redis.call('HGET', KEYS[2], 'userLimit')) then return 5 end
redis.call('HINCRBY', KEYS[3], ARGV[2], 1)
redis.call('HINCRBY', KEYS[2], 'totalNum', -1)
redis.call('SETBIT', KEYS[1], ARGV[1], 1)
redis.call('HSET', KEYS[4], ARGV[3], ARGV[4])
redis.call('ZADD', KEYS[5], ARGV[5], ARGV[3])
return 0
