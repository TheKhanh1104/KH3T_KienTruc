package fit.iuh.kh3tshopbe.service;

import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;

@Service
public class RateLimitService {

    private final RedisTemplate<String, Object> redisTemplate;

    public RateLimitService(RedisTemplate<String, Object> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    public boolean isAllowed(String key, long maxRequests, Duration window) {
        Long requestCount = redisTemplate.opsForValue().increment(key);

        if (requestCount != null && requestCount == 1L) {
            redisTemplate.expire(key, window);
        }

        return requestCount != null && requestCount <= maxRequests;
    }

    public long getRetryAfterSeconds(String key) {
        Long ttl = redisTemplate.getExpire(key);
        return ttl == null || ttl < 0 ? 0L : ttl;
    }
}