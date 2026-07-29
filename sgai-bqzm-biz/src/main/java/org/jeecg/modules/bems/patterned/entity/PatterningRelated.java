package org.jeecg.modules.bems.patterned.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

@Data
@TableName("patterning_related")
public class PatterningRelated {

    /** 主键. */
    @TableId(type = IdType.AUTO)
    private Long id;
    /** 前关联主键. */
    private Long preAssociationId;
    /** 后关联主键. */
    private Long postAssociationId;
    /** 后关联策略名称*/
    private String postAssociationName;
}
