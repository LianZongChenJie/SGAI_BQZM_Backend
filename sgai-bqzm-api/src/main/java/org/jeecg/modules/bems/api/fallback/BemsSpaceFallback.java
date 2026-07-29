package org.jeecg.modules.bems.api.fallback;

import lombok.extern.slf4j.Slf4j;
import org.jeecg.modules.bems.api.BemsSpaceApi;
import org.springframework.cloud.openfeign.FallbackFactory;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class BemsSpaceFallback implements FallbackFactory<BemsSpaceApi> {
    /**
     * Returns an instance of the fallback appropriate for the given cause.
     *
     * @param cause cause of an exception.
     * @return fallback
     */
    @Override
    public BemsSpaceApi create(Throwable cause) {
        log.error("微服务接口调用失败： {}", cause);
        return null;
    }
}
