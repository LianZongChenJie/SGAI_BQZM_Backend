import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

/**
 * 达梦(DM8)数据库 SQL 执行工具
 *
 * 由于项目使用的是达梦数据库（国产数据库），通用的 SQL 客户端工具（Navicat/MySQL 等）
 * 默认连不上达梦，导致"用脚本方式访问数据库总是失败"。
 * 本工具通过达梦 JDBC 驱动直接连接数据库执行 SQL，可规避上述问题。
 *
 * 连接信息来自 Nacos 配置 bqzm-dev.yaml（本机开发环境）：
 *   url: jdbc:dm://127.0.0.1:5236?schema=BQZM&compatibleMode=mysql&ignoreCase=true&ENCODING=utf-8
 *   user: SYSDBA
 *   password: Liming@2026
 *
 * 用法：
 *   1) 编译： javac -encoding UTF-8 -cp "<达梦驱动jar路径>" DmSqlRunner.java
 *   2) 执行 SQL 文件： java -cp ".;<达梦驱动jar路径>" DmSqlRunner <sql文件路径>
 *      例如： java -cp ".;DmJdbcDriver18-8.1.3.140.jar" DmSqlRunner ../db/dm/lighting_circuit_backfill_all_duration.sql
 *   3) 交互执行单条 SQL： java -cp ".;<达梦驱动jar路径>" DmSqlRunner
 *
 * 达梦驱动 jar 位置（Maven 仓库）：
 *   C:\Users\8823\.m2\repository\com\dameng\DmJdbcDriver18\8.1.3.140\DmJdbcDriver18-8.1.3.140.jar
 * 或本机达梦安装目录： D:\soft\dmdbms\drivers\jdbc\DmJdbcDriver8.jar
 */
public class DmSqlRunner {

    // ============ 连接配置（可按需修改） ============
    // 本机开发库
    private static final String URL_LOCAL =
            "jdbc:dm://127.0.0.1:5236?schema=BQZM&compatibleMode=mysql&ignoreCase=true&ENCODING=utf-8";
    // 51 服务器库
    private static final String URL_51 =
            "jdbc:dm://192.168.204.51:5238?schema=BQZM&compatibleMode=mysql&ignoreCase=true&ENCODING=utf-8";
    // 109 服务器库（开发库，51 已迁移/不可达）
    private static final String URL_109 =
            "jdbc:dm://192.168.204.109:5238?schema=BQZM&compatibleMode=mysql&ignoreCase=true&ENCODING=utf-8";
    private static final String USER = "SYSDBA";
    private static final String PASSWORD = "Liming@2026";
    // 生产库（2026-09-18 用户提供，需内网/VPN 可达）
    // 连不上时启动 JVM 加参数：-Djava.net.preferIPv4Stack=true（避免域名解析走 IPv6）
    // 注意：URL 里不能带 /DMSERVER 路径段，否则驱动会把它当成模式名报"无效的模式名[DMSERVER]"
    private static final String URL_PROD =
            "jdbc:dm://10.168.56.103:5236?schema=BQZM&compatibleMode=mysql&ignoreCase=true&ENCODING=utf-8";
    private static final String USER_PROD = "bqzm";
    private static final String PASSWORD_PROD = "Bqzm@123456";
    // ================================================

    public static void main(String[] args) {
        // 解析可选参数 -db=local|51|109|prod，默认 local
        String targetDb = "local";
        String sqlFile = null;
        for (String arg : args) {
            if (arg.startsWith("-db=")) {
                targetDb = arg.substring(4);
            } else {
                sqlFile = arg;
            }
        }

        if (sqlFile == null) {
            // 交互模式
            interactiveMode(targetDb, resolveUrl(targetDb));
            return;
        }

        final String url = resolveUrl(targetDb);
        System.out.println("[目标库] " + targetDb + " -> " + url);
        try {
            List<String> statements = readSqlFile(sqlFile);
            if (statements.isEmpty()) {
                System.out.println("[提示] 文件中未解析到可执行的 SQL 语句（可能只有注释）: " + sqlFile);
                return;
            }
            System.out.println("[解析] 从 " + sqlFile + " 解析出 " + statements.size() + " 条 SQL 语句");
            execute(targetDb, url, statements);
        } catch (Exception e) {
            System.err.println("[错误] 执行失败: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private static String resolveUrl(String targetDb) {
        if ("51".equalsIgnoreCase(targetDb)) {
            return URL_51;
        }
        if ("109".equalsIgnoreCase(targetDb)) {
            return URL_109;
        }
        if ("prod".equalsIgnoreCase(targetDb)) {
            return URL_PROD;
        }
        return URL_LOCAL;
    }

    /** 各目标库的账号（默认 SYSDBA，生产用 bqzm） */
    private static String resolveUser(String targetDb) {
        return "prod".equalsIgnoreCase(targetDb) ? USER_PROD : USER;
    }

    private static String resolvePassword(String targetDb) {
        return "prod".equalsIgnoreCase(targetDb) ? PASSWORD_PROD : PASSWORD;
    }

    // ==================== 生产库只读保护 ====================
    // ★ 生产库【只能查询，禁止任何变更】（用户 2026-09-18 明确要求，最高优先级）
    // 这里做硬拦截：-db=prod 时，只允许查询类语句；任何 DML/DDL 在提交给数据库之前就被拒绝。
    private static final java.util.regex.Pattern READ_ONLY_START =
            java.util.regex.Pattern.compile("(?is)^\\s*(select|with|show|explain|desc|describe)\\b");
    /** SELECT ... INTO 会建表，也算变更 */
    private static final java.util.regex.Pattern READ_ONLY_FORBIDDEN =
            java.util.regex.Pattern.compile("(?is)\\binto\\b");

    private static boolean isReadOnlyTarget(String targetDb) {
        return "prod".equalsIgnoreCase(targetDb);
    }

    /** 校验语句是否允许在该目标库执行；不允许时返回拒绝原因，允许时返回 null */
    private static String checkReadOnly(String targetDb, String sql) {
        if (!isReadOnlyTarget(targetDb)) {
            return null;
        }
        if (!READ_ONLY_START.matcher(sql).find()) {
            return "非查询语句（仅允许 SELECT / WITH / SHOW / EXPLAIN / DESC）";
        }
        if (READ_ONLY_FORBIDDEN.matcher(sql).find()) {
            return "含有 INTO（SELECT ... INTO 会建表，属变更）";
        }
        return null;
    }

    /** 交互模式：循环执行用户输入的单条 SQL */
    private static void interactiveMode(String targetDb, String url) {
        System.out.println("=== 达梦数据库交互执行工具 ===");
        System.out.println("连接: " + url);
        System.out.println("输入 SQL 并按回车执行，输入 exit 退出。");
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(System.in, StandardCharsets.UTF_8))) {
            while (true) {
                System.out.print("SQL> ");
                String sql = reader.readLine();
                if (sql == null || sql.trim().equalsIgnoreCase("exit") || sql.trim().equalsIgnoreCase("quit")) {
                    break;
                }
                if (sql.trim().isEmpty()) {
                    continue;
                }
                execute(targetDb, url, java.util.Collections.singletonList(sql));
            }
        } catch (Exception e) {
            System.err.println("[错误] " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * 读取 SQL 文件并按分号拆分，过滤注释和空行。
     * 注意：达梦数据库中的 PL/SQL（BEGIN...END;）内部含分号，
     * 简单拆分可能把一条匿名块拆成多条，遇到此类脚本请改用 disql 或手动处理。
     */
    private static List<String> readSqlFile(String filePath) throws Exception {
        StringBuilder sb = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(new FileInputStream(filePath), StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line).append("\n");
            }
        }
        return splitSql(sb.toString());
    }

    /** 按分号拆分 SQL，并过滤注释(-- 开头)与空语句 */
    private static List<String> splitSql(String content) {
        List<String> statements = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        String[] lines = content.split("\n");
        for (String rawLine : lines) {
            String line = rawLine.trim();
            // 整行是注释则跳过
            if (line.isEmpty() || line.startsWith("--") || line.startsWith("/*")) {
                continue;
            }
            current.append(rawLine).append("\n");
            // 以分号结尾则视为一条完整语句
            if (rawLine.trim().endsWith(";")) {
                String stmt = current.toString().trim();
                if (!stmt.isEmpty()) {
                    statements.add(stripTrailingSemicolon(stmt));
                }
                current.setLength(0);
            }
        }
        // 处理无分号结尾的剩余语句
        String rest = current.toString().trim();
        if (!rest.isEmpty()) {
            statements.add(stripTrailingSemicolon(rest));
        }
        return statements;
    }

    private static String stripTrailingSemicolon(String s) {
        String t = s.trim();
        if (t.endsWith(";")) {
            return t.substring(0, t.length() - 1);
        }
        return t;
    }

    /** 在单连接中依次执行所有语句，并打印结果 */
    private static void execute(String targetDb, String url, List<String> statements) {
        try (Connection conn = DriverManager.getConnection(url, resolveUser(targetDb), resolvePassword(targetDb))) {
            System.out.println("[连接成功] 已连接达梦数据库");
            if (isReadOnlyTarget(targetDb)) {
                System.out.println("[只读模式] 生产库：仅允许查询（SELECT/WITH/SHOW/EXPLAIN/DESC），任何变更语句会被直接拒绝");
            }
            try (Statement stmt = conn.createStatement()) {
                for (String sql : statements) {
                    System.out.println("\n[执行] " + sql);
                    String refuseReason = checkReadOnly(targetDb, sql);
                    if (refuseReason != null) {
                        System.err.println("[已拒绝] 生产库只读，禁止变更：" + refuseReason);
                        continue;
                    }
                    boolean hasResult;
                    try {
                        hasResult = stmt.execute(sql);
                    } catch (Exception e) {
                        System.err.println("[执行失败] " + e.getMessage());
                        continue; // 一条失败继续执行下一条
                    }
                    if (hasResult) {
                        try (ResultSet rs = stmt.getResultSet()) {
                            printResultSet(rs);
                        }
                    } else {
                        int updateCount = stmt.getUpdateCount();
                        System.out.println("[影响行数] " + updateCount);
                    }
                }
            }
            System.out.println("\n[完成] 全部执行完毕");
        } catch (Exception e) {
            System.err.println("[连接失败] " + e.getMessage());
            e.printStackTrace();
        }
    }

    /** 打印查询结果集（含表头） */
    private static void printResultSet(ResultSet rs) throws Exception {
        ResultSetMetaData meta = rs.getMetaData();
        int columnCount = meta.getColumnCount();
        // 表头
        StringBuilder header = new StringBuilder();
        for (int i = 1; i <= columnCount; i++) {
            header.append(meta.getColumnLabel(i)).append("\t");
        }
        System.out.println(header.toString().trim());
        // 数据行
        while (rs.next()) {
            StringBuilder row = new StringBuilder();
            for (int i = 1; i <= columnCount; i++) {
                row.append(rs.getString(i)).append("\t");
            }
            System.out.println(row.toString().trim());
        }
    }
}
