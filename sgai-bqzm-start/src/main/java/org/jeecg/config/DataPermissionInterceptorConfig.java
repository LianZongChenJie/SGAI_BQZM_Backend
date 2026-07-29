package org.jeecg.config;

import com.baomidou.mybatisplus.extension.plugins.MybatisPlusInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.DataPermissionInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.InnerInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.PaginationInnerInterceptor;
import org.jeecg.modules.bems.permission.handler.BemsDataPermissionHandler;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;

import javax.annotation.PostConstruct;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

/**
 * 数据权限拦截器配置（MyBatis-Plus 标准模式）
 *
 * <p>实现原理：
 * <ul>
 *   <li>使用 MyBatis-Plus 标准 DataPermissionInterceptor 插件</li>
 *   <li>注入自定义的 BemsDataPermissionHandler 处理器</li>
 *   <li>通过 @PostConstruct 在 Bean 初始化后添加到拦截器链</li>
 *   <li>使用反射重新排序拦截器，确保分页拦截器在最后执行</li>
 * </ul>
 *
 * <p>拦截器顺序（调整后）：
 * <ol>
 *   <li>租户拦截器（TenantLineInnerInterceptor）</li>
 *   <li>动态表名拦截器（DynamicTableNameInnerInterceptor）</li>
 *   <li>乐观锁拦截器（OptimisticLockerInnerInterceptor）</li>
 *   <li>数据权限拦截器（DataPermissionInterceptor）- 新增</li>
 *   <li>分页拦截器（PaginationInnerInterceptor）- 移至最后</li>
 * </ol>
 *
 * <p>为什么分页拦截器要在最后？
 * <ul>
 *   <li>数据权限需要先修改 SQL，添加权限条件</li>
 *   <li>分页拦截器需要基于修改后的 SQL 进行 COUNT 查询</li>
 *   <li>如果分页拦截器先执行，COUNT 查询不会包含权限条件，导致数据不一致</li>
 * </ul>
 *
 * <p>优势：
 * <ul>
 *   <li>标准化：使用官方插件，兼容性更好</li>
 *   <li>无反射修改 BoundSql：不再使用反射修改 SQL 字符串</li>
 *   <li>简化：只需返回 Expression，框架处理 SQL 修改</li>
 *   <li>维护性：代码更简洁，易于维护</li>
 *   <li>正确的执行顺序：确保数据权限在分页前生效</li>
 * </ul>
 *
 * @author bems
 * @date 2026-03-16
 */
@Configuration
public class DataPermissionInterceptorConfig {

    @Autowired
    private MybatisPlusInterceptor mybatisPlusInterceptor;

    @Autowired
    private BemsDataPermissionHandler dataPermissionHandler;

    /**
     * 在 Bean 初始化后执行
     * 将数据权限拦截器添加到拦截器链
     * 调整拦截器顺序，将分页拦截器移到最后
     */
    @PostConstruct
    public void addDataPermissionInterceptor() {
        try {
            // 创建 DataPermissionInterceptor 并注入自定义处理器
            DataPermissionInterceptor interceptor = new DataPermissionInterceptor(dataPermissionHandler);

            // 添加到拦截器链
            mybatisPlusInterceptor.addInnerInterceptor(interceptor);

            // 重新排序拦截器：将 PaginationInnerInterceptor 移到最后
            reorderInterceptors();

            // 输出所有拦截器及其顺序
            printAllInterceptors();

        } catch (Exception e) {
            System.err.println("========================================");
            System.err.println("数据权限拦截器配置失败");
            System.err.println("错误信息: " + e.getMessage());
            e.printStackTrace();
            System.err.println("========================================");
        }
    }

    /**
     * 重新排序拦截器，将 PaginationInnerInterceptor 移到最后
     * 这样可以确保数据权限拦截器在分页之前执行，正确处理 COUNT 查询
     */
    private void reorderInterceptors() {
        try {
            // 使用反射访问私有字段 interceptors
            Field interceptorsField = MybatisPlusInterceptor.class.getDeclaredField("interceptors");
            interceptorsField.setAccessible(true);

            @SuppressWarnings("unchecked")
            List<InnerInterceptor> interceptors = (List<InnerInterceptor>) interceptorsField.get(mybatisPlusInterceptor);

            // 找到 PaginationInnerInterceptor 的位置
            int paginationIndex = -1;
            InnerInterceptor paginationInterceptor = null;

            for (int i = 0; i < interceptors.size(); i++) {
                InnerInterceptor innerInterceptor = interceptors.get(i);
                if (innerInterceptor instanceof PaginationInnerInterceptor) {
                    paginationIndex = i;
                    paginationInterceptor = innerInterceptor;
                    break;
                }
            }

            // 如果找到了分页拦截器且不在最后位置，则移动到最后
            if (paginationIndex >= 0 && paginationIndex < interceptors.size() - 1) {
                // 创建新的拦截器列表
                List<InnerInterceptor> newInterceptors = new ArrayList<>();

                // 添加除分页拦截器外的所有拦截器
                for (int i = 0; i < interceptors.size(); i++) {
                    if (i != paginationIndex) {
                        newInterceptors.add(interceptors.get(i));
                    }
                }

                // 将分页拦截器添加到最后
                newInterceptors.add(paginationInterceptor);

                // 更新 interceptors 字段
                interceptorsField.set(mybatisPlusInterceptor, newInterceptors);

                System.out.println("========================================");
                System.out.println("拦截器顺序已调整");
                System.out.println("分页拦截器已移至最后位置");
                System.out.println("========================================");
            }

        } catch (Exception e) {
            System.err.println("========================================");
            System.err.println("拦截器重新排序失败");
            System.err.println("错误信息: " + e.getMessage());
            e.printStackTrace();
            System.err.println("========================================");
        }
    }

    /**
     * 输出所有拦截器及其执行顺序
     */
    private void printAllInterceptors() {
        try {
            // 使用反射访问私有字段 interceptors
            Field interceptorsField = MybatisPlusInterceptor.class.getDeclaredField("interceptors");
            interceptorsField.setAccessible(true);

            @SuppressWarnings("unchecked")
            List<InnerInterceptor> interceptors = (List<InnerInterceptor>) interceptorsField.get(mybatisPlusInterceptor);

            System.out.println("========================================");
            System.out.println("数据权限拦截器已注册（MyBatis-Plus 标准模式）");
            System.out.println("拦截器类型: DataPermissionInterceptor");
            System.out.println("处理器类型: BemsDataPermissionHandler");
            System.out.println("----------------------------------------");
            System.out.println("当前拦截器总数: " + interceptors.size());
            System.out.println("拦截器列表（按执行顺序）:");
            for (int i = 0; i < interceptors.size(); i++) {
                InnerInterceptor innerInterceptor = interceptors.get(i);
                String className = innerInterceptor.getClass().getSimpleName();
                String fullClassName = innerInterceptor.getClass().getName();
                System.out.println("  " + (i + 1) + ". " + className +
                                 " (" + fullClassName + ")");
            }
            System.out.println("========================================");

        } catch (Exception e) {
            System.err.println("========================================");
            System.err.println("数据权限拦截器已注册，但无法获取拦截器列表");
            System.err.println("错误信息: " + e.getMessage());
            e.printStackTrace();
            System.err.println("========================================");
        }
    }
}
