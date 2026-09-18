package org.jeecg.modules.bems.lighting.util;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.jeecg.modules.bems.lighting.entity.LightingArea;
import org.jeecg.modules.bems.lighting.entity.LightingCircuit;
import org.jeecg.modules.bems.lighting.entity.LightingPlan;
import org.jeecg.modules.bems.lighting.entity.LightingScene;
import org.jeecg.modules.bems.lighting.service.ILightingAreaService;
import org.jeecg.modules.bems.lighting.service.ILightingCircuitService;
import org.jeecg.modules.bems.lighting.service.ILightingSceneService;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 照明计划（页面上的"定时任务"）配置日志文案。
 * <p>
 * 计划与定时任务是同一张表 {@code lighting_plan} 的两套接口
 * （{@code /bems/lighting/plan} 与 {@code /bems/lighting/timerTask}），
 * 这里统一"新增/修改/启用/停用/删除"留痕的字段拼装口径，避免两套接口各写一份导致日志格式漂移。
 */
@Slf4j
@Component
@AllArgsConstructor
public class LightingPlanLogText {

    private static final String[] WEEK_NAMES = {"", "周一", "周二", "周三", "周四", "周五", "周六", "周日"};

    private final ILightingAreaService areaService;

    private final ILightingCircuitService circuitService;

    private final ILightingSceneService sceneService;

    /**
     * 控制目标描述：区域/回路/场景 + 目标名称（按 relType 反查名称，查不到回落 relIds）
     */
    public String targetText(LightingPlan plan) {
        String names = null;
        try {
            names = resolveTargetNames(plan);
        } catch (Exception e) {
            // 目标名称只是展示用，解析失败不能影响日志写入与业务流程
            log.warn("解析计划目标名称失败，计划id：{}，relIds：{}", plan.getId(), plan.getRelIds(), e);
        }
        if (StringUtils.isEmpty(names)) {
            names = plan.getRelIds();
        }
        return nullToEmpty(plan.getRelType()) + " " + nullToEmpty(names);
    }

    /**
     * 计划快照（用于对比"改了哪些字段"）。
     * 不含 state/status 字段：启停由 /enable、/disable 单独记日志，避免出现"状态：禁用 → 禁用"这类噪音。
     */
    public Map<String, String> snapshot(LightingPlan plan, String executionTime,
                                        String startDate, String endDate, String enabledWeek) {
        Map<String, String> snapshot = new LinkedHashMap<>();
        snapshot.put("名称", plan.getPlanName());
        snapshot.put("计划类型", plan.getPlanType());
        snapshot.put("周期类型", plan.getCycleType());
        snapshot.put("目标", targetText(plan));
        snapshot.put("动作", plan.getOperationType());
        snapshot.put("执行时间", executionTime);
        snapshot.put("生效窗口", formatWindow(startDate, endDate));
        snapshot.put("执行星期", formatWeek(enabledWeek));
        // 持续验证：仅勾选时出现，避免给历史计划（null）平白加一行"持续验证：否"
        if (plan.getVerifyAfterExecute() != null && plan.getVerifyAfterExecute() == 1) {
            snapshot.put("持续验证", "是");
        }
        return snapshot;
    }

    /**
     * 快照压缩为一行文本（新增/启用时直接作为操作内容），如：
     * 计划类型：定时任务；周期类型：自定义；目标：场景 服贸会全部；动作：开启；执行时间：19:00:00；…
     */
    public String describe(Map<String, String> snapshot) {
        return snapshot.entrySet().stream()
                .map(e -> e.getKey() + "：" + display(e.getValue()))
                .collect(Collectors.joining("；"));
    }

    /**
     * 快照差异（旧值 → 新值），无变化返回"无字段变更"。
     * 取两侧键的并集逐项比较：像"持续验证"这种只在勾选时出现的键也能正确报出"空 → 是 / 是 → 空"。
     */
    public String diff(Map<String, String> before, Map<String, String> after) {
        Set<String> keys = new LinkedHashSet<>(before == null ? java.util.Collections.emptySet() : before.keySet());
        if (after != null) {
            keys.addAll(after.keySet());
        }
        List<String> changes = new ArrayList<>();
        for (String key : keys) {
            String oldValue = before == null ? null : before.get(key);
            String newValue = after == null ? null : after.get(key);
            if (!Objects.equals(oldValue, newValue)) {
                changes.add(key + "：" + display(oldValue) + " → " + display(newValue));
            }
        }
        return changes.isEmpty() ? "无字段变更" : String.join("；", changes);
    }

    /**
     * 执行星期展示：1,2,3 → 周一、周二、周三；七天全选 → 每天；空 → 不限
     */
    public static String formatWeek(String enabledWeek) {
        if (StringUtils.isBlank(enabledWeek)) {
            return "不限";
        }
        Set<Integer> days = new LinkedHashSet<>();
        for (String s : enabledWeek.split(",")) {
            if (StringUtils.isBlank(s)) {
                continue;
            }
            try {
                days.add(Integer.parseInt(s.trim()));
            } catch (NumberFormatException e) {
                // 非数字（如直接存"每天"）忽略，最后原样返回
            }
        }
        if (days.isEmpty()) {
            return enabledWeek;
        }
        if (days.size() == 7) {
            return "每天";
        }
        return days.stream().filter(d -> d >= 1 && d <= 7).map(d -> WEEK_NAMES[d])
                .collect(Collectors.joining("、"));
    }

    /**
     * 生效窗口展示：起止同一天显示单日，都为空显示不限
     */
    public static String formatWindow(String startDate, String endDate) {
        if (StringUtils.isBlank(startDate) && StringUtils.isBlank(endDate)) {
            return "不限";
        }
        if (Objects.equals(startDate, endDate)) {
            return nullToEmpty(startDate);
        }
        return nullToEmpty(startDate) + "~" + nullToEmpty(endDate);
    }

    public static String display(String value) {
        return StringUtils.isBlank(value) ? "空" : value;
    }

    private static String nullToEmpty(String value) {
        return value == null ? "" : value;
    }

    /**
     * 按 relType 反查关联目标名称；relIds 非法/getter 异常时返回原 relIds 由调用方兜底
     */
    private String resolveTargetNames(LightingPlan plan) {
        if (StringUtils.isEmpty(plan.getRelIds())) {
            return null;
        }
        List<Long> relIds = new ArrayList<>();
        for (String s : plan.getRelIds().split(",")) {
            if (StringUtils.isBlank(s)) {
                continue;
            }
            try {
                relIds.add(Long.parseLong(s.trim()));
            } catch (NumberFormatException e) {
                return plan.getRelIds();
            }
        }
        if (relIds.isEmpty()) {
            return null;
        }
        if (LightingPlan.REL_TYPE_AREA.equals(plan.getRelType())) {
            return areaService.listByIds(relIds).stream()
                    .map(LightingArea::getAreaName)
                    .collect(Collectors.joining("、"));
        }
        if (LightingPlan.REL_TYPE_CIRCUIT.equals(plan.getRelType())) {
            return circuitService.listByIds(relIds).stream()
                    .map(c -> {
                        StringBuilder sb = new StringBuilder();
                        if (c.getAreaCode() != null) {
                            sb.append(c.getAreaCode()).append("-");
                        }
                        sb.append(c.getCircuitName());
                        return sb.toString();
                    })
                    .collect(Collectors.joining("、"));
        }
        if (LightingPlan.REL_TYPE_SCENE.equals(plan.getRelType())) {
            return sceneService.listByIds(relIds).stream()
                    .map(LightingScene::getSceneName)
                    .collect(Collectors.joining("、"));
        }
        return plan.getRelIds();
    }
}
