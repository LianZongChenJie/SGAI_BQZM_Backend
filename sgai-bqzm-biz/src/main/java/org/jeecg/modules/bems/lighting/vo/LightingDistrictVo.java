package org.jeecg.modules.bems.lighting.vo;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.jeecg.modules.bems.lighting.entity.LightingDistrict;

/**
 * 照明片区 VO（在实体基础上扩展展示字段）
 */
@Data
@EqualsAndHashCode(callSuper = true)
@ApiModel(value = "照明片区VO", description = "照明片区信息展示对象")
public class LightingDistrictVo extends LightingDistrict {


    private String relIds;

    private String relType;

}
