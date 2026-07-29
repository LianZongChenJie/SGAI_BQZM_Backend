package org.jeecg.modules.bems.permission.config;

import lombok.extern.slf4j.Slf4j;
import org.jeecg.common.util.RedisUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * 数据权限缓存管理器
 * 负责管理缓存版本号，用于在 @Cacheable 注解的 SpEL 表达式中引用
 */
@Component
@Slf4j
public class DataPermissionCacheManager {

    private static final String CACHE_VERSION_KEY = "dataPermission:version";
    private static final long CACHE_EXPIRE_SECONDS = 300; // 5分钟缓存过期

    @Autowired
    private RedisUtil redisUtil;

    /**
     * 获取当前缓存版本号
     * 此方法需要在 SpEL 表达式中调用，必须是 public 的
     */
    public long getCurrentVersion() {
        try {
            Object version = redisUtil.get(CACHE_VERSION_KEY);
            if (version == null) {
                version = System.currentTimeMillis();
                redisUtil.set(CACHE_VERSION_KEY, version, CACHE_EXPIRE_SECONDS);
            }
            return (Long) version;
        } catch (Exception e) {
            log.warn("获取缓存版本号失败，使用默认值", e);
            return 0L;
        }
    }

    /**
     * 更新缓存版本号（使旧缓存失效）
     */
    public void updateCacheVersion() {
        try {
            Long newVersion = System.currentTimeMillis();
            redisUtil.set(CACHE_VERSION_KEY, newVersion, CACHE_EXPIRE_SECONDS);
            log.info("数据权限缓存版本已更新: {}", newVersion);
        } catch (Exception e) {
            log.error("更新缓存版本号失败", e);
        }
    }
}
