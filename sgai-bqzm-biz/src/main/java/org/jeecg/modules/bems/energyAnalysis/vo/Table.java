package org.jeecg.modules.bems.energyAnalysis.vo;

import lombok.Data;

import java.util.List;

/**
 * 描述:
 *
 * @author ppliu-life
 * created in 2024/4/19 16:26
 */
@Data
public class Table {
    private List<TableHeader> tableHeaderList;
    private List<TableData> tableDataList;
}
