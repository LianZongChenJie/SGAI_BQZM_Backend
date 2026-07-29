package org.jeecg.modules.bems.permission.holder;

import com.alibaba.ttl.TransmittableThreadLocal;
import org.jeecg.modules.bems.permission.vo.UserDataScope;

/**
 * 数据权限上下文持有者
 * 使用 Alibaba TTL 支持线程池场景的ThreadLocal传递
 */
public class DataPermissionHolder {
    private static final ThreadLocal<UserDataScope> DATA_SCOPE_THREAD_LOCAL =
        new TransmittableThreadLocal<>();

    /**
     * 设置数据权限范围
     */
    public static void setDataScope(UserDataScope dataScope) {
        DATA_SCOPE_THREAD_LOCAL.set(dataScope);
    }

    /**
     * 获取数据权限范围
     */
    public static UserDataScope getDataScope() {
        return DATA_SCOPE_THREAD_LOCAL.get();
    }

    /**
     * 清除数据权限范围
     */
    public static void clear() {
        DATA_SCOPE_THREAD_LOCAL.remove();
    }
}
