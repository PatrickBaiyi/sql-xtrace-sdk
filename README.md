# SQL XTrace SDK

MyBatis 拦截器，自动向所有 SELECT 语句注入 traceId 列，用于 SQL 级别的分布式链路追踪。

## 特性

- 自动向所有 SELECT 语句注入 `xTraceId` 字段，无例外（COUNT、GROUP BY、聚合查询均覆盖）
- 支持复杂 SQL：JOIN、子查询、UNION、CTE、窗口函数等
- 通过 Spring Boot AutoConfiguration 零代码接入
- 支持从多个来源获取 traceId（MDC: `midea-apm-traceid` / `tid` / `traceId`，或手动设置）
- 自动防重复注入
- 非 SELECT 语句（INSERT/UPDATE/DELETE）不受影响

## 安装

```xml
<dependency>
    <groupId>com.annto</groupId>
    <artifactId>sql-xtrace-sdk</artifactId>
    <version>1.0.3.2</version>
</dependency>
```

## 配置

在 `application.properties` 或 `application.yml` 中配置：

```properties
# 启用/禁用 SQL 追踪（默认: true）
sql.xtrace.enabled=true

# 注入的 traceId 列别名（默认: xTraceId）
sql.xtrace.trace-id-field-name=xTraceId
```

## 使用方式

### 自动模式（推荐）

如果应用已接入 APM 系统（如 Midea APM、SkyWalking 等），SDK 会自动从 MDC 中获取 traceId，无需任何代码改动。

MDC key 的读取优先级：`midea-apm-traceid` > `tid` > `traceId` > `SqlTraceContext`

### 手动模式

```java
// 生成并设置 traceId
String traceId = SqlTraceSDK.initTrace();

// 或使用指定的 traceId
SqlTraceSDK.initTrace("custom-trace-id-12345");

// 获取当前 traceId
String currentTraceId = SqlTraceSDK.getCurrentTraceId();

// 请求结束后清除
SqlTraceSDK.clearTrace();
```

### Web 应用集成示例

```java
@Component
public class TraceIdInterceptor implements HandlerInterceptor {
    
    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        String traceId = SqlTraceSDK.initTrace();
        response.addHeader("X-Trace-ID", traceId);
        return true;
    }
    
    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, 
                                Object handler, Exception ex) {
        SqlTraceSDK.clearTrace();
    }
}
```

## 工作原理

1. Spring Boot 启动时自动注册 MyBatis 拦截器到所有 `SqlSessionFactory`
2. 拦截器拦截所有 `Executor.query()` 调用
3. 从 MDC 或 `SqlTraceContext` 中获取 traceId
4. 使用 Druid SQL Parser 解析 SELECT 语句，在 select list 末尾追加 `'traceId值' AS xTraceId`
5. 非 SELECT 语句、已包含 traceId 的语句直接跳过

**SQL 改写示例：**

```sql
-- 原始 SQL
SELECT id, name FROM users WHERE status = 'active'

-- 改写后
SELECT id, name, 'abc123def456' AS xTraceId FROM users WHERE status = 'active'
```

## 支持的 SQL 类型

| SQL 类型 | 示例 | 是否注入 |
|----------|------|---------|
| 基础 SELECT | `SELECT id, name FROM users` | Yes |
| SELECT * | `SELECT * FROM users` | Yes |
| COUNT | `SELECT COUNT(*) FROM users` | Yes |
| 聚合函数 | `SELECT MAX(salary), AVG(age) FROM emp` | Yes |
| GROUP BY | `SELECT dept, COUNT(*) FROM emp GROUP BY dept` | Yes |
| JOIN | `SELECT u.id FROM users u JOIN orders o ON ...` | Yes |
| 子查询 | `SELECT * FROM (SELECT ...) t` | Yes |
| UNION | `SELECT ... UNION SELECT ...` | Yes（两侧都注入） |
| CTE | `WITH cte AS (...) SELECT * FROM cte` | Yes |
| 窗口函数 | `SELECT *, ROW_NUMBER() OVER (...) FROM ...` | Yes |
| DISTINCT | `SELECT DISTINCT dept FROM emp` | Yes |
| FOR UPDATE | `SELECT * FROM users FOR UPDATE` | Yes |
| INSERT | `INSERT INTO users ...` | No |
| UPDATE | `UPDATE users SET ...` | No |
| DELETE | `DELETE FROM users ...` | No |

## 注意事项

- 确保应用能处理 SELECT 结果中额外的 traceId 列
- 建议在测试环境验证 SQL 兼容性后再上生产
- SDK 自动检测并跳过已包含 traceId 的 SQL，避免重复注入
