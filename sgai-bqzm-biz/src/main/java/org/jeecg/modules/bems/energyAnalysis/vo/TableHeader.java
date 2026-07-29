package org.jeecg.modules.bems.energyAnalysis.vo;

import lombok.Data;

/**
 * 描述:
 *
 * @author ppliu-life
 * created in 2024/4/19 16:26
 */
@Data
public class TableHeader {
    /**表头名称.*/
    private String label;
    /**表头字段.*/
    private String field;
    /**
     * 是否固定列
     */
    private Boolean fixed = false;
    /**
     * 列宽，单位：px
     */
    private Integer width = 80;

    public TableHeader() {
    }

    public TableHeader(String label,String field,Boolean fixed,Integer width){
        this.label = label;
        this.fixed = fixed;
        this.field = field;
        this.width = width;
    }
}
