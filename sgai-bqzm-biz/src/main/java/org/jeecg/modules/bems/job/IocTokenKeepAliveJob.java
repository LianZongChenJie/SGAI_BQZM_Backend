package org.jeecg.modules.bems.job;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.jeecg.common.constant.CommonConstant;
import org.jeecg.common.system.util.JwtUtil;
import org.jeecg.common.util.RedisUtil;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * IOC 平台嵌入 token 保活定时任务
 * <p>
 * 背景：照明系统被外部 IOC 平台以 iframe 方式嵌入，前端通过 .env 的 VITE_IOC_TOKEN 使用一个固定 token 免登录。
 * 后端登录态是以 Redis 中的 {@code prefix_user_token:<token>} 缓存为准：
 * <ul>
 *   <li>JWT 本身有效期 3.5 天，但以 Redis 缓存 key 是否存在为准；</li>
 *   <li>Redis 缓存有效期 = JwtUtil.EXPIRE_TIME * 2 = 7 天；</li>
 *   <li>只要缓存 key 存在，框架（ShiroRealm.jwtTokenRefresh）会在收到请求时自动重签 JWT 并续期，token 就不会失效。</li>
 * </ul>
 * 因此本任务每天中午 12:00 对该 token 的缓存做一次续期，保证其永不因空闲而过期，
 * 从而避免 IOC 嵌入页突然 401（IOC 场景前端又不会走登录页）。
 * <p>
 * 注意：本任务只对"已存在"的缓存续期，不会凭空重建；若缓存已不存在（token 已失效/用户已登出），
 * 只会打印告警，需要重新登录生成新 token 并更新前后端配置。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class IocTokenKeepAliveJob {

    private final RedisUtil redisUtil;

    /** 需要保活的 IOC token（与前端 .env 的 VITE_IOC_TOKEN 保持一致） */
    @Value("${bqzm.ioc.token:}")
    private String iocToken;

    /** 是否启用保活任务 */
    @Value("${bqzm.ioc.keep-alive-enabled:true}")
    private boolean enabled;

    /**
     * 每天中午 12:00 续期 IOC token 的 Redis 缓存，防止过期
     */
    @Scheduled(cron = "0 0 12 * * ?")
    public void keepAlive() {
        if (!enabled) {
            log.debug("【IOC Token保活】已禁用，跳过");
            return;
        }
        if (StringUtils.isBlank(iocToken)) {
            log.warn("【IOC Token保活】未配置 bqzm.ioc.token，跳过");
            return;
        }
        String key = CommonConstant.PREFIX_USER_TOKEN + iocToken;
        try {
            if (!redisUtil.hasKey(key)) {
                log.warn("【IOC Token保活】缓存不存在（token 可能已过期或用户已登出），保活未生效，请重新登录生成新 token 并更新配置。key={}", key);
                return;
            }
            // 续期 7 天（与登录时一致：EXPIRE_TIME(ms) * 2 / 1000 秒）
            redisUtil.expire(key, JwtUtil.EXPIRE_TIME * 2 / 1000);
            log.info("【IOC Token保活】成功，token 缓存已续期 {} 天", JwtUtil.EXPIRE_TIME * 2 / 1000 / 86400);
        } catch (Exception e) {
            log.error("【IOC Token保活】执行异常", e);
        }
    }
}
