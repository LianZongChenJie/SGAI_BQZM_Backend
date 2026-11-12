package org.jeecg.modules.bems.lighting.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;
import org.jeecg.modules.bems.permission.annotation.DataPermissionField;
import org.jeecg.modules.bems.permission.entity.RoleDataPermission;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 回路电流整点采样历史（每小时一行，2026-11-12 新增）。
 * <p>
 * 由来：{@code lighting_circuit.electric_current} 是覆盖式最新值，回路电流本身没有历史
 * （唯一留痕只有报警快照 {@code lighting_circuit_alarm_log.electric_current}）。
 * 本表由 {@code LightingCircuitCurrentSnapshotJob} 每小时整点把 903 全部回路当时的电流快照落一行，
 * 供趋势图/事后追溯使用。暂不做保留期清理。
 */
@Data
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
@TableName("lighting_circuit_current_history")
@ApiModel(value = "回路电流采样历史", description = "回路电流整点采样历史（每小时一行）")
public class LightingCircuitCurrentHistory implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    @ApiModelProperty(value = "主键ID")
    private Long id;

    @ApiModelProperty(value = "回路ID")
    private Long circuitId;

    @ApiModelProperty(value = "回路名称（快照）")
    private String circuitName;

    @ApiModelProperty(value = "回路编号（快照）")
    private String circuitCode;

    @ApiModelProperty(value = "区域ID")
    private Long areaId;

    @ApiModelProperty(value = "区域编码")
    private String areaCode;

    @ApiModelProperty(value = "区域名称")
    private String areaName;

    @DataPermissionField(type = RoleDataPermission.TYPE_DISTRICT, value = "district_id")
    @ApiModelProperty(value = "所属片区ID")
    private Long districtId;

    @ApiModelProperty(value = "空间编码，当前仅 903")
    private String space;

    @ApiModelProperty(value = "采样时的回路开关状态（开启/关闭）")
    private String status;

    @ApiModelProperty(value = "实时电流（A）")
    private Double electricCurrent;

    @ApiModelProperty(value = "额定电流（A）")
    private Double ratedElectricCurrent;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    @ApiModelProperty(value = "采样时刻（整点）")
    private LocalDateTime collectTime;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    @ApiModelProperty(value = "写入时间")
    private LocalDateTime createTime;

    @ApiModelProperty(value = "所属部门")
    private String sysOrgCode;
}
