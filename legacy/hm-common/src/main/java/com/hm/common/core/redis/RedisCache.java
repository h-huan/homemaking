package com.hm.common.core.redis;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.BoundSetOperations;
import org.springframework.data.redis.core.HashOperations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.stereotype.Component;

/**
 * spring redis 工具类
 *
 * @author hm
 **/
@SuppressWarnings(value = { "unchecked", "rawtypes" })
@Component
public class RedisCache
{
    private static final Logger log = LoggerFactory.getLogger(RedisCache.class);

    private final ConcurrentMap<String, LocalCacheValue> localCache = new ConcurrentHashMap<>();

    private volatile boolean localFallbackLogged = false;

    @Autowired
    public RedisTemplate redisTemplate;

    /**
     * 缓存基本的对象，Integer、String、实体类等
     *
     * @param key 缓存的键值
     * @param value 缓存的值
     */
    public <T> void setCacheObject(final String key, final T value)
    {
        executeVoid(() -> redisTemplate.opsForValue().set(key, value), () -> putLocalValue(key, value, null));
    }

    /**
     * 缓存基本的对象，Integer、String、实体类等
     *
     * @param key 缓存的键值
     * @param value 缓存的值
     * @param timeout 时间
     * @param timeUnit 时间颗粒度
     */
    public <T> void setCacheObject(final String key, final T value, final Integer timeout, final TimeUnit timeUnit)
    {
        executeVoid(() -> redisTemplate.opsForValue().set(key, value, timeout, timeUnit),
                () -> putLocalValue(key, value, resolveExpireAt(timeout, timeUnit)));
    }

    /**
     * 设置有效时间
     *
     * @param key Redis键
     * @param timeout 超时时间
     * @return true=设置成功；false=设置失败
     */
    public boolean expire(final String key, final long timeout)
    {
        return expire(key, timeout, TimeUnit.SECONDS);
    }

    /**
     * 设置有效时间
     *
     * @param key Redis键
     * @param timeout 超时时间
     * @param unit 时间单位
     * @return true=设置成功；false=设置失败
     */
    public boolean expire(final String key, final long timeout, final TimeUnit unit)
    {
        return execute(() -> redisTemplate.expire(key, timeout, unit), () -> {
            LocalCacheValue cacheValue = getLocalValue(key);
            if (cacheValue == null)
            {
                return false;
            }
            cacheValue.setExpireAt(resolveExpireAt(timeout, unit));
            return true;
        });
    }

    /**
     * 获取有效时间
     *
     * @param key Redis键
     * @return 有效时间
     */
    public long getExpire(final String key)
    {
        return execute(() -> redisTemplate.getExpire(key), () -> {
            LocalCacheValue cacheValue = getLocalValue(key);
            if (cacheValue == null || cacheValue.getExpireAt() == null)
            {
                return -1L;
            }
            long remainMillis = cacheValue.getExpireAt() - System.currentTimeMillis();
            return remainMillis <= 0 ? -2L : TimeUnit.MILLISECONDS.toSeconds(remainMillis);
        });
    }

    /**
     * 判断 key是否存在
     *
     * @param key 键
     * @return true 存在 false不存在
     */
    public Boolean hasKey(String key)
    {
        return execute(() -> redisTemplate.hasKey(key), () -> getLocalValue(key) != null);
    }

    /**
     * 获得缓存的基本对象。
     *
     * @param key 缓存键值
     * @return 缓存键值对应的数据
     */
    public <T> T getCacheObject(final String key)
    {
        return execute(() -> {
            ValueOperations<String, T> operation = redisTemplate.opsForValue();
            return operation.get(key);
        }, () -> {
            LocalCacheValue cacheValue = getLocalValue(key);
            return cacheValue == null ? null : (T) cacheValue.getValue();
        });
    }

    /**
     * 删除单个对象
     *
     * @param key
     */
    public boolean deleteObject(final String key)
    {
        return execute(() -> redisTemplate.delete(key), () -> localCache.remove(key) != null);
    }

    /**
     * 删除集合对象
     *
     * @param collection 多个对象
     * @return
     */
    public boolean deleteObject(final Collection collection)
    {
        return execute(() -> redisTemplate.delete(collection) > 0, () -> {
            boolean removed = false;
            for (Object item : collection)
            {
                removed |= item != null && localCache.remove(String.valueOf(item)) != null;
            }
            return removed;
        });
    }

    /**
     * 缓存List数据
     *
     * @param key 缓存的键值
     * @param dataList 待缓存的List数据
     * @return 缓存的对象
     */
    public <T> long setCacheList(final String key, final List<T> dataList)
    {
        return execute(() -> {
            Long count = redisTemplate.opsForList().rightPushAll(key, dataList);
            return count == null ? 0 : count;
        }, () -> {
            putLocalValue(key, new ArrayList<>(dataList), null);
            return (long) dataList.size();
        });
    }

    /**
     * 获得缓存的list对象
     *
     * @param key 缓存的键值
     * @return 缓存键值对应的数据
     */
    public <T> List<T> getCacheList(final String key)
    {
        return execute(() -> redisTemplate.opsForList().range(key, 0, -1), () -> {
            LocalCacheValue cacheValue = getLocalValue(key);
            if (cacheValue == null)
            {
                return Collections.emptyList();
            }
            Object value = cacheValue.getValue();
            if (value instanceof List<?> list)
            {
                return (List<T>) list;
            }
            return Collections.emptyList();
        });
    }

    /**
     * 缓存Set
     *
     * @param key 缓存键值
     * @param dataSet 缓存的数据
     * @return 缓存数据的对象
     */
    public <T> BoundSetOperations<String, T> setCacheSet(final String key, final Set<T> dataSet)
    {
        return execute(() -> {
            BoundSetOperations<String, T> setOperation = redisTemplate.boundSetOps(key);
            Iterator<T> it = dataSet.iterator();
            while (it.hasNext())
            {
                setOperation.add(it.next());
            }
            return setOperation;
        }, () -> {
            putLocalValue(key, new LinkedHashSet<>(dataSet), null);
            return null;
        });
    }

    /**
     * 获得缓存的set
     *
     * @param key
     * @return
     */
    public <T> Set<T> getCacheSet(final String key)
    {
        return execute(() -> redisTemplate.opsForSet().members(key), () -> {
            LocalCacheValue cacheValue = getLocalValue(key);
            if (cacheValue == null)
            {
                return Collections.emptySet();
            }
            Object value = cacheValue.getValue();
            if (value instanceof Set<?> set)
            {
                return (Set<T>) set;
            }
            return Collections.emptySet();
        });
    }

    /**
     * 缓存Map
     *
     * @param key
     * @param dataMap
     */
    public <T> void setCacheMap(final String key, final Map<String, T> dataMap)
    {
        if (dataMap != null)
        {
            executeVoid(() -> redisTemplate.opsForHash().putAll(key, dataMap),
                    () -> putLocalValue(key, new HashMap<>(dataMap), null));
        }
    }

    /**
     * 获得缓存的Map
     *
     * @param key
     * @return
     */
    public <T> Map<String, T> getCacheMap(final String key)
    {
        return execute(() -> redisTemplate.opsForHash().entries(key), () -> {
            LocalCacheValue cacheValue = getLocalValue(key);
            if (cacheValue == null)
            {
                return Collections.emptyMap();
            }
            Object value = cacheValue.getValue();
            if (value instanceof Map<?, ?> map)
            {
                return (Map<String, T>) map;
            }
            return Collections.emptyMap();
        });
    }

    /**
     * 往Hash中存入数据
     *
     * @param key Redis键
     * @param hKey Hash键
     * @param value 值
     */
    public <T> void setCacheMapValue(final String key, final String hKey, final T value)
    {
        executeVoid(() -> redisTemplate.opsForHash().put(key, hKey, value), () -> {
            Map<String, Object> localMap = ensureLocalMap(key);
            localMap.put(hKey, value);
        });
    }

    /**
     * 获取Hash中的数据
     *
     * @param key Redis键
     * @param hKey Hash键
     * @return Hash中的对象
     */
    public <T> T getCacheMapValue(final String key, final String hKey)
    {
        return execute(() -> {
            HashOperations<String, String, T> opsForHash = redisTemplate.opsForHash();
            return opsForHash.get(key, hKey);
        }, () -> {
            Map<String, Object> localMap = ensureLocalMap(key);
            return (T) localMap.get(hKey);
        });
    }

    /**
     * 获取多个Hash中的数据
     *
     * @param key Redis键
     * @param hKeys Hash键集合
     * @return Hash对象集合
     */
    public <T> List<T> getMultiCacheMapValue(final String key, final Collection<Object> hKeys)
    {
        return execute(() -> redisTemplate.opsForHash().multiGet(key, hKeys), () -> {
            Map<String, Object> localMap = ensureLocalMap(key);
            List<T> values = new ArrayList<>(hKeys.size());
            for (Object hKey : hKeys)
            {
                values.add((T) localMap.get(String.valueOf(hKey)));
            }
            return values;
        });
    }

    /**
     * 删除Hash中的某条数据
     *
     * @param key Redis键
     * @param hKey Hash键
     * @return 是否成功
     */
    public boolean deleteCacheMapValue(final String key, final String hKey)
    {
        return execute(() -> redisTemplate.opsForHash().delete(key, hKey) > 0, () -> {
            Map<String, Object> localMap = ensureLocalMap(key);
            return localMap.remove(hKey) != null;
        });
    }

    /**
     * 获得缓存的基本对象列表
     *
     * @param pattern 字符串前缀
     * @return 对象列表
     */
    public Collection<String> keys(final String pattern)
    {
        return execute(() -> redisTemplate.keys(pattern), () -> {
            purgeExpiredEntries();
            if ("*".equals(pattern))
            {
                return new ArrayList<>(localCache.keySet());
            }
            if (pattern.endsWith("*"))
            {
                String prefix = pattern.substring(0, pattern.length() - 1);
                List<String> matches = new ArrayList<>();
                for (String key : localCache.keySet())
                {
                    if (key.startsWith(prefix))
                    {
                        matches.add(key);
                    }
                }
                return matches;
            }
            return localCache.containsKey(pattern) ? List.of(pattern) : Collections.emptyList();
        });
    }

    private <T> T execute(Supplier<T> redisSupplier, Supplier<T> localSupplier)
    {
        try
        {
            return redisSupplier.get();
        }
        catch (RuntimeException ex)
        {
            logFallback(ex);
            return localSupplier.get();
        }
    }

    private void executeVoid(Runnable redisAction, Runnable localAction)
    {
        try
        {
            redisAction.run();
        }
        catch (RuntimeException ex)
        {
            logFallback(ex);
            localAction.run();
        }
    }

    private void logFallback(RuntimeException ex)
    {
        if (!localFallbackLogged)
        {
            synchronized (this)
            {
                if (!localFallbackLogged)
                {
                    log.warn("Redis unavailable, falling back to in-memory cache for local development: {}", ex.getMessage());
                    localFallbackLogged = true;
                }
            }
        }
    }

    private void putLocalValue(String key, Object value, Long expireAt)
    {
        localCache.put(key, new LocalCacheValue(value, expireAt));
    }

    private LocalCacheValue getLocalValue(String key)
    {
        LocalCacheValue cacheValue = localCache.get(key);
        if (cacheValue == null)
        {
            return null;
        }
        if (cacheValue.isExpired())
        {
            localCache.remove(key);
            return null;
        }
        return cacheValue;
    }

    private Map<String, Object> ensureLocalMap(String key)
    {
        LocalCacheValue cacheValue = getLocalValue(key);
        if (cacheValue != null && cacheValue.getValue() instanceof Map<?, ?> map)
        {
            return (Map<String, Object>) map;
        }
        Map<String, Object> localMap = new HashMap<>();
        putLocalValue(key, localMap, null);
        return localMap;
    }

    private void purgeExpiredEntries()
    {
        for (String key : new ArrayList<>(localCache.keySet()))
        {
            getLocalValue(key);
        }
    }

    private Long resolveExpireAt(long timeout, TimeUnit unit)
    {
        if (timeout <= 0)
        {
            return null;
        }
        return System.currentTimeMillis() + unit.toMillis(timeout);
    }

    private static final class LocalCacheValue
    {
        private final Object value;

        private volatile Long expireAt;

        private LocalCacheValue(Object value, Long expireAt)
        {
            this.value = value;
            this.expireAt = expireAt;
        }

        private Object getValue()
        {
            return value;
        }

        private Long getExpireAt()
        {
            return expireAt;
        }

        private void setExpireAt(Long expireAt)
        {
            this.expireAt = expireAt;
        }

        private boolean isExpired()
        {
            return expireAt != null && expireAt <= System.currentTimeMillis();
        }
    }
}
