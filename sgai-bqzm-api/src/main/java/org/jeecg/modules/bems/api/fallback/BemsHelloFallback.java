package org.jeecg.modules.bems.api.fallback;

import org.springframework.cloud.openfeign.FallbackFactory;
import org.jeecg.modules.bems.api.BemsHelloApi;
import lombok.Setter;
import org.springframework.stereotype.Component;
import lombok.extern.slf4j.Slf4j;

/**
 * @author JeecgBoot
 */
@Slf4j
@Component
public class BemsHelloFallback implements FallbackFactory<BemsHelloApi> {
    @Setter
    private Throwable cause;

    @Override
    public BemsHelloApi create(Throwable throwable) {
        log.error("微服务接口调用失败： {}", cause);
        return null;
    }

}
