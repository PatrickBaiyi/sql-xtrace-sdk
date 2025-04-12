# SQL Tracing SDK

A SDK for automatically adding trace IDs to SQL SELECT statements.

## Features

• Automatically adds `xTraceId` field to all SELECT statements executed via MyBatis
• Configurable support for COUNT(*) and GROUP BY queries
• Easy integration with Spring Boot applications
• Thread-local context management for trace IDs
• Automatic inspection and page size limitation for large data queries
• Supports obtaining trace IDs from multiple sources (MDC, thread-local variables)

## Installation

Add the dependency to your Maven project: Latest version 1.0.3.2

```xml
<dependency>
    <groupId>com.annto</groupId>
    <artifactId>sql-xtrace-sdk</artifactId>
    <version>1.0.3.2</version>
</dependency>
```

## Usage

### Configuration

Configure the SQL Tracing SDK in `application.properties` or `application.yml`:

```properties
# Enable/disable SQL tracing feature (default: true)
sql.xtrace.enabled=true

# Enable tracing for COUNT(*) queries (default: false)
sql.xtrace.enable-count-queries=false

# Enable tracing for GROUP BY queries (default: false)
sql.xtrace.enable-group-by-queries=false

# Custom trace ID field name (default: xTraceId)
sql.xtrace.trace-id-field-name=xTraceId

# Limit maximum query page size (default: 20000)
sql.xtrace.max-page-size=20000
```

### Initialize Trace ID

You can initialize the trace ID at the start of request processing in your application:

```java
// At the start of request processing
String traceId = SqlTraceSDK.initTrace(); // Generates and sets a unique trace ID

// Or use a specific trace ID
SqlTraceSDK.initTrace("custom-trace-id-12345");

// Get current trace ID
String currentTraceId = SqlTraceSDK.getCurrentTraceId();

// Clear trace ID after request processing
SqlTraceSDK.clearTrace();
```

### Web Application Integration

For web applications, create an interceptor to automatically set trace IDs for each request:

```java
@Component
public class TraceIdInterceptor implements HandlerInterceptor {
    
    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        // Generate trace ID for each request
        String traceId = SqlTraceSDK.initTrace();
        // Optional: Add trace ID to response headers
        response.addHeader("X-Trace-ID", traceId);
        return true;
    }
    
    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, 
                                Object handler, Exception ex) {
        // Clear trace ID after request completion
        SqlTraceSDK.clearTrace();
    }
}
```

Register the interceptor:

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

## How It Works

1. The SDK intercepts all query requests through MyBatis interceptors
2. For SELECT statements, the interceptor parses SQL and adds a trace ID field
3. The interceptor obtains trace IDs from multiple sources (midea-apm-traceid, tid, traceId or thread-local variables)
4. Modified SQL gets executed, allowing you to see trace IDs in query results
5. Configurable handling for COUNT(*) and GROUP BY queries
6. Solves issues like "order by name asc,2 desc"
7. Automatically limits query page size to prevent performance issues from oversized queries

## Notes

• Ensure the SQL Tracing SDK is included as a dependency in your web application
• Verify SQL compatibility in test environments before production use
• All SQL will automatically include trace ID field - ensure your application handles this additional field
• Currently does not support UNION statements
• The SDK automatically skips modification for SQL statements already containing trace ID fields to avoid duplication

## Currently supported SQL formats:
```SQL
SQL: select count(*), name , 'test-trace-id-1234567' as xTraceId from users group by name limit 1000
SQL: select max(*) from users
SQL: select min(salary) from users
SQL: select avg(salary) from users
SQL: select sum(salary) from users
SQL: select count(*), max(salary) , min(salary), avg(salary) from users
SQL: select department, count(*), max(salary) , 'test-trace-id-1234567' as xTraceId from users group by department limit 1000
SQL: select count(*), max(salary) , min(salary), avg(salary) from users
SQL: select * , 'test-trace-id-1234567' as xTraceId from users limit 1000
SQL: select * , 'test-trace-id-1234567' as xTraceId from users where id > 100 limit 1000
SQL: select * , 'test-trace-id-1234567' as xTraceId from users order by id limit 1000
SQL: select * , 'test-trace-id-1234567' as xTraceId from users limit 10
SQL: select * , 'test-trace-id-1234567' as xTraceId from users where id > 100 order by name limit 10
SQL: select name , case  when count(*) > 10 then 'High' else 'Low' end as volume , 'test-trace-id-1234567' as xTraceId from orders group by name limit 1000
SQL: select name , (count(*) + sum(amount)) / 2 as avg_metric , 'test-trace-id-1234567' as xTraceId from orders group by name limit 1000
SQL: select CONCAT('Total: ', count(*)) as display_count, name , 'test-trace-id-1234567' as xTraceId from orders group by name limit 1000
SQL: select name , ( select count(*) from orders where orders.user_id = users.id ) as order_count , 'test-trace-id-1234567' as xTraceId from users limit 1000
SQL: select name, count(*) as count , 'test-trace-id-1234567' as xTraceId from orders group by name limit 1000 union (select name, count(*) as count , 'test-trace-id-1234567' as xTraceId from returns group by name limit 1000)
SQL: select * , 'test-trace-id-1234567' as xTraceId from ( select * from users ) u limit 1000
SQL: select * , 'test-trace-id-1234567' as xTraceId from users where id in ( select * from temp_users ) limit 1000
SQL: select * , 'test-trace-id-1234567' as xTraceId from users u1 where exists ( select * from users u2 where u2.id = u1.id ) limit 1000
SQL: select * , 'test-trace-id-1234567' as xTraceId from ( select * from users order by id limit 10 ) u limit 1000
SQL: select count(*) from users
SQL: select count(1) from users
SQL: select count(id) from users
SQL: select max(salary) from employees
SQL: select min(age) from users
SQL: select avg(score) from students
SQL: select sum(amount) from orders
SQL: select count(*) as total from users
SQL: select max(salary) as highest_salary from employees
SQL: select min(age) as youngest from users
SQL: select distinct * , 'test-trace-id-1234567' as xTraceId from users limit 1000
SQL: select distinct id, name , 'test-trace-id-1234567' as xTraceId from users limit 1000
SQL: select distinct department, name, salary , 'test-trace-id-1234567' as xTraceId from users limit 1000
SQL: select distinct department, count(*) , 'test-trace-id-1234567' as xTraceId from users group by department limit 1000
SQL: select * , 'test-trace-id-1234567' as xTraceId from users where DATE(created_at) = CURDATE() limit 1000
SQL: select * , 'test-trace-id-1234567' as xTraceId from users where YEAR(created_at) = 2023 limit 1000
SQL: select *, CONCAT(first_name, ' ', last_name) as full_name , 'test-trace-id-1234567' as xTraceId from users limit 1000
SQL: select * , case  when salary > 10000 then 'High' else 'Low' end as salary_grade , 'test-trace-id-1234567' as xTraceId from users limit 1000
SQL: select * , 'test-trace-id-1234567' as xTraceId from users where id in (1, 2, 3) and status = 'active' limit 1000
SQL: select * , 'test-trace-id-1234567' as xTraceId from users where created_at between '2023-01-01' and '2023-12-31' limit 1000
SQL: select * , 'test-trace-id-1234567' as xTraceId from users limit 1000 union (select * , 'test-trace-id-1234567' as xTraceId from temp_users limit 1000)
SQL: select *, name , 'test-trace-id-1234567' as xTraceId from users limit 1000 union (select *, name , 'test-trace-id-1234567' as xTraceId from temp_users limit 1000)
SQL: select name, age , 'test-trace-id-1234567' as xTraceId from users limit 1000 union (select name, age , 'test-trace-id-1234567' as xTraceId from temp_users limit 1000)
SQL: select * , 'test-trace-id-1234567' as xTraceId from users limit 1000 union all (select * , 'test-trace-id-1234567' as xTraceId from temp_users limit 1000)
SQL: select * , 'test-trace-id-1234567' as xTraceId from users limit 1000 union distinct (select * , 'test-trace-id-1234567' as xTraceId from temp_users limit 1000)
SQL: (SELECT * FROM users) UNION (SELECT * FROM temp_users)
SQL: (SELECT *,name FROM users) UNION (SELECT *,name FROM temp_users)
SQL: (SELECT name FROM users) UNION (SELECT name FROM temp_users)
SQL: select * , 'test-trace-id-1234567' as xTraceId from users where id < 100 limit 1000 union (select * , 'test-trace-id-1234567' as xTraceId from users where id > 200 limit 1000)
SQL: select * , 'test-trace-id-1234567' as xTraceId from users u join orders o on u.id = o.user_id limit 1000
SQL: select * , 'test-trace-id-1234567' as xTraceId from users u left join orders o on u.id = o.user_id limit 1000
SQL: select * , 'test-trace-id-1234567' as xTraceId from users u right join orders o on u.id = o.user_id limit 1000
SQL: select * , 'test-trace-id-1234567' as xTraceId from users u inner join orders o on u.id = o.user_id limit 1000
SQL: select * , 'test-trace-id-1234567' as xTraceId from users u left join orders o on u.id = o.user_id limit 1000
SQL: select * , 'test-trace-id-1234567' as xTraceId from users u cross join orders limit 1000
SQL: WITH cte AS (SELECT * FROM users) SELECT * FROM cte
SQL: WITH cte AS (SELECT * FROM users) SELECT * FROM cte WHERE id > 100
SQL: WITH cte1 AS (SELECT * FROM users), cte2 AS (SELECT * FROM orders) SELECT * FROM cte1 JOIN cte2 ON cte1.id = cte2.user_id
SQL: WITH RECURSIVE cte AS (SELECT * FROM users WHERE id = 1 UNION ALL SELECT * FROM users WHERE id > 1) SELECT * FROM cte
SQL: select *, row_number() over (order by salary) as row_num , 'test-trace-id-1234567' as xTraceId from users limit 1000
SQL: select *, rank() over (partition by department order by salary) as salary_rank , 'test-trace-id-1234567' as xTraceId from users limit 1000
SQL: select *, dense_rank() over (order by salary) as dense_rank , 'test-trace-id-1234567' as xTraceId from users limit 1000
SQL: select *, first_value(salary) over (partition by department order by salary) as first_salary , 'test-trace-id-1234567' as xTraceId from users limit 1000
SQL: select *, lag(salary) over (order by id) as prev_salary , 'test-trace-id-1234567' as xTraceId from users limit 1000
SQL: select count(*) from users
SQL: select count(id) from users
SQL: select count(DISTINCT id) from users
SQL: select count(1) from users
SQL: select count(*) as total from users
SQL: select count(*), department , 'test-trace-id-1234567' as xTraceId from users group by department limit 1000
```