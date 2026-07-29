package org.jeecg.modules.bems.energyAnalysis.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 描述:
 *
 * @author ppliu-life
 * created in 2024/4/22 14:26
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class Chat {
    String name;
    /** 横坐标. */
    List<String> xAxis;
    /** 折线. */
    List<ChatSeries> chatSeriesList;

    List<String> errorMessage;
}
