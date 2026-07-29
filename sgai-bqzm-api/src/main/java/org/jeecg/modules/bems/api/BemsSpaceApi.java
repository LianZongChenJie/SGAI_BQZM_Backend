package org.jeecg.modules.bems.api;

import org.jeecg.modules.bems.api.fallback.BemsDeviceFallback;
import org.jeecg.modules.bems.entity.SpaceInfo;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

@FeignClient(value = "sgai-bems", fallbackFactory = BemsDeviceFallback.class)
public interface BemsSpaceApi {

    /**
     * 获取空间详细信息
     * @param spaceIds 空间id
     * @return 空间详细信息
     */
    @GetMapping(value = "/bems/space/api/spaceInfoList")
    List<SpaceInfo> spaceInfoList(@RequestParam String spaceIds);
}
