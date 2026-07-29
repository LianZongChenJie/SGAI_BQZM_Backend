package org.jeecg.modules.bems.alarm.vo;

import lombok.Data;
import org.jeecg.modules.bems.alarm.entity.AlarmLevel;

@Data
public class AlarmRecordStatisticsVo {

    /**
     * 报警等级ID
     */
    private Long alarmLevelId;

    /**
     * 报警等级名称
     */
    private String alarmLevelName;

    /**
     * 数量
     */
    private Long quantity;

    /**
     * 排序
     */
    private Integer sort;

    public static AlarmRecordStatisticsVo create(AlarmLevel level, Long quantity){
        AlarmRecordStatisticsVo vo = new AlarmRecordStatisticsVo();
        vo.setAlarmLevelId(level.getId());
        vo.setAlarmLevelName(level.getAlarmLevelName());
        vo.setQuantity(quantity);
        vo.setSort(level.getSort());
        return vo;
    }
}
