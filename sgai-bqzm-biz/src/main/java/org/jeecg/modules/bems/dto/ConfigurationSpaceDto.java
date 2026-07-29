package org.jeecg.modules.bems.dto;

import lombok.Data;

import java.util.List;

@Data
public class ConfigurationSpaceDto {

    private Long spaceId;

    private String spaceName;

    private Long parentId;

    private List<ConfigurationSpaceDto> children;
}
