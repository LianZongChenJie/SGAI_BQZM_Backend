package org.jeecg.modules.bems.lighting.dto;

import lombok.Data;
import lombok.EqualsAndHashCode;
import org.jeecg.modules.bems.lighting.entity.LightingArea;

import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 泛光-空间
 */
@Data
@EqualsAndHashCode
public class LightingSpaceDto {

    /**
     * 空间id
     */
    private String spaceId;
    /**
     * 空间名称
     */
    private String spaceName;

    public static List<LightingSpaceDto> convert(List<LightingArea> areas){
        return areas.stream()
                .collect(Collectors.toMap(
                        LightingArea::getSpaceName,
                        area -> {
                            LightingSpaceDto dto = new LightingSpaceDto();
                            dto.setSpaceId(area.getSpace());
                            dto.setSpaceName(area.getSpaceName());
                            return dto;
                        },
                        (existing, replacement) -> replacement
                ))
                .values()
                .stream()
                .sorted(Comparator.comparing(LightingSpaceDto::getSpaceName))
                .toList();
    }
}
