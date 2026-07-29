package org.jeecg.modules.bems.energyAnalysis.vo.chat;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 饼图数据格式
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class PieChat {

    private String name;

    private List<PieChatSeriesData> seriesData;
}
