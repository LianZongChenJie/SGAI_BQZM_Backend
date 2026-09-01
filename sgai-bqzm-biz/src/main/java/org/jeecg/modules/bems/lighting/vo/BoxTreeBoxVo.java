package org.jeecg.modules.bems.lighting.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.util.Date;

/**
 * 箱子树节点（第三层）：区域下的箱子（电箱）
 * 展示名称：有箱则显示网关名，无箱显示"xxx（无箱）"
 */
@Data
@ApiModel(value = "箱子树节点", description = "片区-区域-箱子 树结构的第三层（箱子）")
public class BoxTreeBoxVo {

    @ApiModelProperty(value = "箱子(网关)编号")
    private String gatewayCode;

    @ApiModelProperty(value = "箱子展示名称（无箱时为：xxx（无箱））")
    private String boxName;

    @ApiModelProperty(value = "是否有箱子：true-有、false-无")
    private Boolean hasBox;

    @ApiModelProperty(value = "交流电压1(A相)(V)")
    private Double voltageA;

    @ApiModelProperty(value = "交流电压2(B相)(V)")
    private Double voltageB;

    @ApiModelProperty(value = "交流电压3(C相)(V)")
    private Double voltageC;

    @ApiModelProperty(value = "交流电流1(A相)(A)")
    private Double currentA;

    @ApiModelProperty(value = "交流电流2(B相)(A)")
    private Double currentB;

    @ApiModelProperty(value = "交流电流3(C相)(A)")
    private Double currentC;

    @ApiModelProperty(value = "有功功率(kW)")
    private Double activePower;

    @ApiModelProperty(value = "无功功率(kVar)")
    private Double reactivePower;

    @ApiModelProperty(value = "视在功率(kVA)")
    private Double apparentPower;

    @ApiModelProperty(value = "功率因数")
    private Double powerFactor;

    @ApiModelProperty(value = "累积电量(kWh)")
    private Double totalEnergy;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    @ApiModelProperty(value = "采集时间")
    private Date collectTime;
}
