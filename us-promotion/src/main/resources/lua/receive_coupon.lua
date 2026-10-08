if(redis.call('exists', KEYS[1]) == 0) then
    return 1
end
if(tonumber(redis.call('hget', KEYS[1], 'totalNum')) <= 0) then
    return 2
end
local now = tonumber(redis.call('time')[1])
local endTime = tonumber(redis.call('hget', KEYS[1], 'issueEndTime'))
if(endTime and now > endTime) then
    return 3
end
local beginTime = tonumber(redis.call('hget', KEYS[1], 'issueBeginTime'))
if(beginTime and now < beginTime) then
    return 3
end
local count = tonumber(redis.call('hget', KEYS[2], ARGV[1])) or 0
if(tonumber(redis.call('hget', KEYS[1], 'userLimit')) < count + 1) then
    return 4
end
redis.call('hincrby', KEYS[2], ARGV[1], 1)
redis.call('hincrby', KEYS[1], "totalNum", "-1")
redis.call('hset', KEYS[3], ARGV[2], ARGV[3])
redis.call('zadd', KEYS[4], ARGV[4], ARGV[2])
return 0
