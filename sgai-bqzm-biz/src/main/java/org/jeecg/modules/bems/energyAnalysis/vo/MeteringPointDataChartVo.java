package org.jeecg.modules.bems.energyAnalysis.vo;

import lombok.Data;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Data
public class MeteringPointDataChartVo {
    private Table table;
    private Chat chat;
    public MeteringPointDataChartVo(Table table) {
        this.table = table;
        //给chat赋值
        Chat result = new Chat();
        List<TableData> tableDataList = table.getTableDataList();
        List<TableHeader> tableHeaderList = table.getTableHeaderList();
        result.setXAxis(
                tableHeaderList.stream()
                        .filter(tableHeader -> !"sum".equals(tableHeader.getField()))
                        .filter(tableHeader -> !"name".equals(tableHeader.getField()))
                        .map(TableHeader::getLabel).collect(Collectors.toList())
        );
        result.setChatSeriesList(
                tableDataList.stream().map(tableData -> {
                    ChatSeries series = new ChatSeries();
                    series.setName(tableData.get("name"));
                    series.setData(tableData.entrySet().stream()
                            .filter(entry -> !entry.getKey().equals("name") && !entry.getKey().equals("sum"))
                            .map(Map.Entry::getValue)
                            .collect(Collectors.toList())
                    );
                    return series;
                }).collect(Collectors.toList())
        );
        this.chat = result;
    }

}
