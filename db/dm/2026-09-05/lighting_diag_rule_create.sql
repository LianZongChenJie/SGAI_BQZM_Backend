-- =====================================================================
-- 照明回路智能诊断规则表 lighting_diag_rule
-- 供前端"规则与知识库"页展示规则清单，并按 类型分类(rule_domain) 过滤：
--   数据 / 供电 / 线路 / 负载 / 控制
-- 数据库：达梦 DM8 / schema=BQZM
-- 注意：字段不能用 domain/level 等保留字，故命名为 rule_domain/rule_level。
-- 幂等：表不存在则创建；已存在则仅补插缺失的规则(按 rule_code 判断)
-- =====================================================================
CREATE TABLE IF NOT EXISTS lighting_diag_rule (
    id             BIGINT IDENTITY(1,1) PRIMARY KEY,   -- 主键(自增)
    rule_code      VARCHAR(20)  NOT NULL,              -- 规则编码：R1/R2/R3/R4/R5C/R5M
    rule_domain    VARCHAR(20),                        -- 类型分类：数据/供电/线路/负载/控制
    name           VARCHAR(50),                        -- 规则名称
    severity       VARCHAR(20),                        -- 严重程度：严重/预警/一般
    summary        VARCHAR(100),                       -- 一句话摘要
    definition     CLOB,                               -- 规则机理/定义说明
    rule_level     VARCHAR(10),                        -- 命中级别：alarm报警/warn预警/note提示
    status         VARCHAR(10) DEFAULT '启用',          -- 启用/停用
    sort           INT,                                -- 排序
    remark         VARCHAR(500),                       -- 备注
    create_time    TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    update_time    TIMESTAMP
);

COMMENT ON TABLE  lighting_diag_rule IS '照明回路智能诊断规则表';
COMMENT ON COLUMN lighting_diag_rule.id           IS '主键ID';
COMMENT ON COLUMN lighting_diag_rule.rule_code    IS '规则编码：R1/R2/R3/R4/R5C/R5M';
COMMENT ON COLUMN lighting_diag_rule.rule_domain  IS '类型分类：数据/供电/线路/负载/控制';
COMMENT ON COLUMN lighting_diag_rule.name         IS '规则名称';
COMMENT ON COLUMN lighting_diag_rule.severity     IS '严重程度：严重/预警/一般';
COMMENT ON COLUMN lighting_diag_rule.summary      IS '一句话摘要';
COMMENT ON COLUMN lighting_diag_rule.definition   IS '规则机理/定义说明';
COMMENT ON COLUMN lighting_diag_rule.rule_level   IS '命中级别：alarm报警/warn预警/note提示';
COMMENT ON COLUMN lighting_diag_rule.status       IS '启用/停用';
COMMENT ON COLUMN lighting_diag_rule.sort         IS '排序';

-- 初始规则数据（参照《照明回路智能诊断规则手册》裁剪落地）
INSERT INTO lighting_diag_rule (rule_code, rule_domain, name, severity, summary, definition, rule_level, status, sort)
SELECT 'R1', '线路', '开路·灯具失效', '严重', '合闸无流 · 回路不亮',
       '回路处于开启状态但电流低于额定电流且持续数分钟，疑似线路开路、空开跳闸或灯具/驱动整体失效。判定口径：status=开启 且 电流 < 额定电流 持续 3 分钟。',
       'alarm', '启用', 1
WHERE NOT EXISTS (SELECT 1 FROM lighting_diag_rule WHERE rule_code = 'R1');

INSERT INTO lighting_diag_rule (rule_code, rule_domain, name, severity, summary, definition, rule_level, status, sort)
SELECT 'R2', '控制', '触点粘连·漏电', '严重', '分闸有流 · 关不灭',
       '回路处于关闭(分闸)状态但仍有明显残留电流，疑似接触器触点粘连未断开或对地漏电。',
       'alarm', '启用', 2
WHERE NOT EXISTS (SELECT 1 FROM lighting_diag_rule WHERE rule_code = 'R2');

INSERT INTO lighting_diag_rule (rule_code, rule_domain, name, severity, summary, definition, rule_level, status, sort)
SELECT 'R3', '供电', '电压越限', '预警', '箱电压偏离允许带',
       '有箱子区域的箱电压(分相核验)超出额定 220V 允许偏差带，长期偏高加大驱动损耗与寿命风险，偏低造成灯具变暗或驱动保护退出。',
       'warn', '启用', 3
WHERE NOT EXISTS (SELECT 1 FROM lighting_diag_rule WHERE rule_code = 'R3');

INSERT INTO lighting_diag_rule (rule_code, rule_domain, name, severity, summary, definition, rule_level, status, sort)
SELECT 'R4', '负载', '过载', '严重', '电流超回路额定',
       '回路电流超过该回路额定电流，持续过载存在线缆发热与保护动作风险。',
       'alarm', '启用', 4
WHERE NOT EXISTS (SELECT 1 FROM lighting_diag_rule WHERE rule_code = 'R4');

INSERT INTO lighting_diag_rule (rule_code, rule_domain, name, severity, summary, definition, rule_level, status, sort)
SELECT 'R5C', '数据', '通信中断', '严重', '数据超时 · 全箱盲区',
       '有箱子区域连续无有效数据上报，冻结最后数据，该区域全部电气判定暂停，形成监测盲区。',
       'alarm', '启用', 5
WHERE NOT EXISTS (SELECT 1 FROM lighting_diag_rule WHERE rule_code = 'R5C');

INSERT INTO lighting_diag_rule (rule_code, rule_domain, name, severity, summary, definition, rule_level, status, sort)
SELECT 'R5M', '数据', '计量与台账缺陷', '一般', '静态缺陷 · 影响统计口径',
       '台账静态缺陷(无法计量/控制缺陷/箱子未安装/测试通道)，不构成电气故障，但影响能耗统计口径与判定覆盖。',
       'note', '启用', 6
WHERE NOT EXISTS (SELECT 1 FROM lighting_diag_rule WHERE rule_code = 'R5M');
