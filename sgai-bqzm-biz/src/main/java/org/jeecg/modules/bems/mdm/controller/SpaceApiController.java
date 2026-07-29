package org.jeecg.modules.bems.mdm.controller;

import lombok.AllArgsConstructor;
import org.jeecg.modules.bems.entity.SpaceInfo;
import org.jeecg.modules.bems.mdm.entity.Space;
import org.jeecg.modules.bems.mdm.service.ISpaceService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@RestController
@RequestMapping("/bems/space/api")
@AllArgsConstructor
public class SpaceApiController {

    private final ISpaceService service;

    @GetMapping("/spaceInfoList")
    public List<SpaceInfo> spaceInfoList(@RequestParam String spaceIds){
        List<Space> spaces = service.listByIds(Arrays.asList(spaceIds.split(",")));
        List<SpaceInfo> res = new ArrayList<>();
        for (Space space : spaces) {
            SpaceInfo info = new SpaceInfo();
            info.setId(space.getId().toString());
            info.setSpaceName(space.getSpaceName());
            info.setFullName(space.getFullName());
            res.add(info);
        }
        return res;
    }

}
