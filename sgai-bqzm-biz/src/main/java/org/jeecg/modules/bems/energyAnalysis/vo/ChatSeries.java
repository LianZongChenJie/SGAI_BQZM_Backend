package org.jeecg.modules.bems.energyAnalysis.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 描述:
 *
 * @author ppliu-life
 * created in 2024/4/22 14:33
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class ChatSeries {
    private Object name;
    private List<Object> data;
}
