# SQL追踪SDK

一个用于自动向SQL SELECT语句添加追踪ID的SDK。

## 功能特点

- 自动向通过MyBatis执行的所有SELECT语句添加`xTraceId`字段
- 可配置对COUNT(*)和GROUP BY查询的支持
- 易于与Spring Boot应用程序集成
- 使用线程本地上下文管理追踪ID
- 自动检查和限制大数据查询的页面大小
- 支持从多种来源获取追踪ID（MDC、线程本地变量）

## 安装

将依赖添加到您的Maven项目中：最新版本为1.0.3.2

```xml
<dependency>
    <groupId>com.annto</groupId>
    <artifactId>sql-xtrace-sdk</artifactId>
    <version>1.0.3.2</version>
</dependency>
```

## 使用方法

### 配置

在`application.properties`或`application.yml`中配置SQL追踪SDK：

```properties
# 启用/禁用SQL追踪功能（默认：true）
sql.xtrace.enabled=true

# 启用对COUNT(*)查询的追踪（默认：false）
sql.xtrace.enable-count-queries=false

# 启用对GROUP BY查询的追踪（默认：false）
sql.xtrace.enable-group-by-queries=false

# 自定义追踪ID字段名（默认：xTraceId）
sql.xtrace.trace-id-field-name=xTraceId

# 限制最大查询页面大小（默认：20000）
sql.xtrace.max-page-size=20000

```

### 初始化追踪ID

您可以在应用程序的请求处理开始时初始化追踪ID：

```java
// 在请求处理开始时
String traceId = SqlTraceSDK.initTrace(); // 生成并设置一个唯一的追踪ID

// 或者使用特定的追踪ID
SqlTraceSDK.initTrace("custom-trace-id-12345");

// 获取当前的追踪ID
String currentTraceId = SqlTraceSDK.getCurrentTraceId();

// 在请求处理结束时清除追踪ID
SqlTraceSDK.clearTrace();
```

### 与Web应用集成

对于Web应用程序，您可以创建一个拦截器来自动为每个请求设置追踪ID：

```java
@Component
public class TraceIdInterceptor implements HandlerInterceptor {
    
    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        // 为每个请求生成一个追踪ID
        String traceId = SqlTraceSDK.initTrace();
        // 可选：将追踪ID添加到响应头中
        response.addHeader("X-Trace-ID", traceId);
        return true;
    }
    
    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, 
                                Object handler, Exception ex) {
        // 请求完成后清除追踪ID
        SqlTraceSDK.clearTrace();
    }
}
```

注册拦截器：

```java
@Configuration
public class WebConfig implements WebMvcConfigurer {
    
    @Autowired
    private TraceIdInterceptor traceIdInterceptor;
    
    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(traceIdInterceptor);
    }
}
```

## 工作原理

1. SDK通过MyBatis拦截器拦截所有查询请求
2. 对于SELECT语句，拦截器解析SQL并添加一个包含追踪ID的字段
3. 拦截器从多种来源获取追踪ID（midea-apm-traceid、tid、traceId或线程本地变量）
4. 修改后的SQL将被执行，让您可以在查询结果中看到追踪ID
5. 可以根据需要配置对COUNT(*)和GROUP BY查询的处理
6. 解决了类似order by name asc,2 desc的问题
7. 自动限制查询页面大小，防止过大的查询导致性能问题

## 注意事项

- 确保SQL追踪SDK作为您的Web应用程序的依赖项
- 在生产环境中使用前，请在测试环境中验证SQL兼容性
- 所有SQL都将自动添加追踪ID字段，请确保您的应用程序代码能够处理这个额外的字段
- 目前不支持union语句
- 对于已经包含追踪ID字段的SQL，SDK会自动跳过修改，避免重复添加