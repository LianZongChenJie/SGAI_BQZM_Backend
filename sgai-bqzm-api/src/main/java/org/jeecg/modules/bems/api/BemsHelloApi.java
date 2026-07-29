package org.jeecg.modules.bems.api;
import org.jeecg.modules.bems.api.fallback.BemsHelloFallback;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

@FeignClient(value = "jeecg-bems", fallbackFactory = BemsHelloFallback.class)
public interface BemsHelloApi {

    /**
     * bems hello 微服务接口
     * @param
     * @return
     */
    @GetMapping(value = "/bems/hello")
    String callHello();
}
