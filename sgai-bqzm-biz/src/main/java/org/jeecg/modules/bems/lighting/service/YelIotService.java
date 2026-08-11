package org.jeecg.modules.bems.lighting.service;

import cn.hutool.core.util.StrUtil;
import cn.hutool.http.HttpUtil;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.extern.slf4j.Slf4j;
import org.jeecg.modules.bems.lighting.entity.LightingProgram;
import org.jeecg.modules.bems.lighting.mapper.LightingProgramMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.locks.ReentrantLock;

/**
 * 泛光总控系统 API 服务（获取节目运行状态）
 * 移植自外挂小程序 electric-box-controller 项目，用于查询泛光总控系统当前正在运行的节目，
 * 供场景列表（/bems/lighting/scene/listPage）展示运行中的节目名称。
 */
@Service
@Slf4j
public class YelIotService {

    @Value("${yel.iot.username:}")
    private String userName;

    @Value("${yel.iot.password:}")
    private String password;

    /**
     * 总控系统地址，格式：ip:port，如 127.0.0.1:9999
     */
    @Value("${yel.iot.host:}")
    private String host;

    /**
     * mock 开关：true 时不调用真实接口，返回样例数据（本地开发看不到总控系统时使用）；
     * 部署到服务器后可改为 false 走真实接口
     */
    @Value("${yel.iot.mock:false}")
    private boolean mockEnabled;

    private final LightingProgramMapper programMapper;

    public YelIotService(LightingProgramMapper programMapper) {
        this.programMapper = programMapper;
    }

    /**
     * token 有效期（秒），官方2小时，这里提前5分钟过期更安全
     */
    private static final long TOKEN_EXPIRE_SECONDS = 6900;

    /**
     * 本地缓存的 token
     */
    private volatile String cachedToken;

    /**
     * token 过期时间戳（毫秒）
     */
    private volatile long tokenExpireTime;

    /**
     * 刷新 token 的锁，防止并发重复获取
     */
    private final ReentrantLock tokenLock = new ReentrantLock();

    /**
     * 获取 token
     * 优先从本地缓存取，过期则重新获取
     */
    private String getToken() {
        // 先判断缓存是否有效
        long now = System.currentTimeMillis();
        if (cachedToken != null && !cachedToken.isEmpty() && now < tokenExpireTime) {
            return cachedToken;
        }

        // 缓存失效，加锁获取
        tokenLock.lock();
        try {
            // 双重检查，防止多线程重复获取
            now = System.currentTimeMillis();
            if (cachedToken != null && !cachedToken.isEmpty() && now < tokenExpireTime) {
                return cachedToken;
            }

            HashMap<String, Object> paramMap = new HashMap<>();
            paramMap.put("username", userName);
            paramMap.put("password", password);
            String s = HttpUtil.get("http://" + host + "/yel-iot-platform/open_api/get_token", paramMap);
            if (StrUtil.isEmpty(s)) {
                log.error("泛光总控系统-获取token失败,无返回值");
                return "";
            }
            JSONObject response = JSONObject.parseObject(s);
            if (response != null && response.getInteger("code") != null && response.getInteger("code") == 200) {
                String token = String.valueOf(response.get("data"));
                cachedToken = token;
                tokenExpireTime = now + TOKEN_EXPIRE_SECONDS * 1000;
                log.info("泛光总控系统-token获取成功，有效期{}秒", TOKEN_EXPIRE_SECONDS);
                return token;
            }
            log.error("泛光总控系统-获取token失败：{}", response == null ? "无响应" : response.getString("message"));
            return "";
        } catch (Exception e) {
            log.error("泛光总控系统-获取token异常", e);
            return "";
        } finally {
            tokenLock.unlock();
        }
    }

    /**
     * 获取当前正在运行的节目列表（state=1 表示运行中/开启）
     *
     * @return 运行中的节目（JSONObject 含 id/groupName/state 等字段）；无节目运行、未配置host或查询失败返回空列表
     */
    public List<JSONObject> getRunningGroups() {
        // mock 模式：本地无法访问总控系统时返回样例数据，保证前端能看到节目运行状态字段
        if (mockEnabled) {
            return buildMockRunningGroups();
        }
        if (StrUtil.isEmpty(host)) {
            log.warn("泛光总控系统-host未配置，无法获取节目运行状态（可在 application.yml 配置 yel.iot.host）");
            return Collections.emptyList();
        }
        try {
            String token = getToken();
            if (StrUtil.isEmpty(token)) {
                log.error("泛光总控系统-token为空，无法获取节目运行状态");
                return Collections.emptyList();
            }
            HashMap<String, Object> paramMap = new HashMap<>();
            paramMap.put("token", token);
            String s = HttpUtil.get("http://" + host + "/yel-iot-platform/open_api/get_group_run_state", paramMap);
            if (StrUtil.isEmpty(s)) {
                log.error("泛光总控系统-获取节目运行状态失败：无响应");
                return Collections.emptyList();
            }
            log.info("泛光总控系统-获取节目运行状态成功：{}", s);
            JSONObject jsonObject = JSONObject.parseObject(s);
            // 这个接口 code=500 也可能返回数据，不按 code 判断，直接取 data.state
            if (jsonObject == null || jsonObject.get("data") == null) {
                return Collections.emptyList();
            }
            JSONObject dataObj = jsonObject.getJSONObject("data");
            if (dataObj == null) {
                return Collections.emptyList();
            }
            JSONArray stateArray = dataObj.getJSONArray("state");
            if (stateArray == null || stateArray.isEmpty()) {
                return Collections.emptyList();
            }
            List<JSONObject> all = stateArray.toJavaList(JSONObject.class);
            // 只保留运行中/开启（state=1）的节目
            all.removeIf(st -> st.getInteger("state") == null || st.getInteger("state") != 1);
            return all;
        } catch (Exception e) {
            log.error("泛光总控系统-获取节目运行状态异常。", e);
            return Collections.emptyList();
        }
    }

    /**
     * mock 样例数据：把 lighting_program 里配置了泛光节目ID(groupId) 的节目当作"运行中"返回
     * （id 拼接 yel_ 前缀、state=1、groupName 取节目名），保证前端在本地也能看到 programDetail 有值。
     * 库里没有任何节目时，返回一条固定样例（对应接口文档示例）。
     */
    private List<JSONObject> buildMockRunningGroups() {
        List<JSONObject> result = new ArrayList<>();
        try {
            List<LightingProgram> programs = programMapper.selectList(
                    new LambdaQueryWrapper<LightingProgram>()
                            .isNotNull(LightingProgram::getGroupId)
                            .ne(LightingProgram::getGroupId, ""));
            for (LightingProgram p : programs) {
                JSONObject o = new JSONObject();
                o.put("id", "yel_" + p.getGroupId());
                o.put("groupName", p.getProgramName());
                o.put("state", 1);
                result.add(o);
            }
        } catch (Exception e) {
            log.error("泛光总控系统-mock样例数据构建异常", e);
        }
        if (result.isEmpty()) {
            JSONObject sample = new JSONObject();
            sample.put("id", "yel_170614849494864");
            sample.put("groupName", "前奏-月牙泉篇");
            sample.put("state", 1);
            result.add(sample);
        }
        log.info("泛光总控系统-mock模式，返回{}个运行中的节目样例", result.size());
        return result;
    }
}
