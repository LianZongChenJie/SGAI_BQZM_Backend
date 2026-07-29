package org.jeecg.modules.bems.patterned.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 场景控制延迟消息
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class PatterningDelayMessage implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 场景控制ID */
    private Long id;

    /** 执行时间 */
    private LocalDateTime executeTime;

    /** 版本号 */
    private String version;
}
