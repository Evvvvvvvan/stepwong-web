# Stepwong Web

一个非前后端分离的 Spring Boot 项目，包含：

- 用户注册 / 登录 / 退出
- MySQL 存储
- 账号密码列表管理
- 账号密码按登录用户隔离
- 第三方账号密码 AES-GCM 加密入库
- 登录密码 BCrypt 哈希入库
- 每个账号单独设置步数范围
- 每个账号单独设置是否每天定时执行
- 手动执行
- 执行日志
- Zepp Life 登录、app_token 获取、步数上报
- 登录 token / app_token 加密缓存
- `/api/**` JSON 接口

## 技术栈

- Java 8
- Spring Boot 2.7.18
- Spring MVC
- Spring Security
- Spring Data JPA
- Thymeleaf
- MySQL 8

## 目录说明

```text
src/main/java/com/example/stepwong
  config        安全配置
  controller    页面控制器
  dto           表单对象和执行结果对象
  entity        JPA 实体
  repository    数据访问层
  security      当前登录用户工具
  service       业务服务、加密服务、定时任务、执行网关
```

## 快速启动

### 1. 初始化数据库

数据库连接已写入 `src/main/resources/application.yml`。首次部署时先执行建表脚本：

```bash
mysql -uroot -p < sql/stepwong_web_schema.sql
```

### 2. 设置加密密钥

`APP_CRYPTO_SECRET` 用于加密保存第三方账号密码，建议至少 32 位。

```bash
export APP_CRYPTO_SECRET="replace-with-a-long-random-secret-32"
```

### 3. 启动项目

```bash
mvn spring-boot:run
```

默认访问地址：

```text
http://localhost:18080
```

## 默认数据库配置

```yaml
url: jdbc:mysql://localhost:3306/stepwong_web?useUnicode=true&characterEncoding=utf8&useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=Asia/Shanghai
username: root
password: root
```

如需切换数据库，直接修改 `src/main/resources/application.yml` 中的 `spring.datasource` 配置。

## 数据隔离设计

1. 登录用户保存在 `app_users`。
2. 账号记录保存在 `step_accounts`，每条记录都有 `owner_id`。
3. 查询、编辑、删除、显示密码、手动执行都使用 `owner_id + id` 查询。
4. 执行日志保存在 `execution_logs`，每条日志都有 `owner_id`。
5. 日志页面只查询当前登录用户的最近 100 条记录。
6. 第三方账号密码使用 `CryptoService` 加密后保存，不明文入库。

## 定时任务说明

`StepScheduleRunner` 每分钟扫描一次到期任务：

```text
auto_enabled = true
run_hour = 当前小时
run_minute = 当前分钟
last_run_date 不是今天
```

命中后调用 `StepExecutionService` 执行，并写入日志。

## Zepp Life 接口说明

默认实现：

```text
com.example.stepwong.service.ZeppLifeStepGateway
```

执行流程：

1. 调用 `https://api-user.zepp.com/v2/registrations/tokens` 获取 access code。
2. 调用 `https://account.huami.com/v2/client/login` 获取 login_token 和 user_id。
3. 调用 `https://account-cn.huami.com/v1/client/app_tokens` 获取 app_token。
4. 调用 `https://api-mifit-cn.huami.com/v1/data/band_data.json` 上报随机步数。
5. login_token / app_token 使用 `CryptoService` 加密后缓存在 `step_accounts`，后续优先复用，失败后自动清空并重新登录。

本地只跑页面流程、不调用外部接口时，可启用测试 profile：

```bash
mvn spring-boot:run -Dspring-boot.run.profiles=local-test
```

## JSON 接口

所有 `/api/**` 接口需要登录态或 Basic Auth。

```text
POST   /api/register              注册登录用户
GET    /api/me                    查询当前登录用户
GET    /api/accounts              查询账号列表
POST   /api/accounts              新增账号
GET    /api/accounts/{id}         查询账号详情
PUT    /api/accounts/{id}         更新账号
DELETE /api/accounts/{id}         删除账号
POST   /api/accounts/{id}/execute 手动执行
POST   /api/accounts/{id}/reveal  显示明文密码
GET    /api/logs                  查询最近 100 条日志
```

新增 / 更新账号请求体示例：

```json
{
  "displayName": "主账号",
  "accountNo": "13800138000",
  "password": "zepp-password",
  "minStep": 18000,
  "maxStep": 25000,
  "autoEnabled": true,
  "runHour": 8,
  "runMinute": 35,
  "enabled": true
}
```

## 执行接口扩展点

执行入口是：

```text
com.example.stepwong.service.StepGateway
```

当前默认接入 Zepp Life。需要本地测试时使用 `local-test` profile 切换到 `LocalTestStepGateway`。

## 常用页面

```text
/login          登录
/register       注册
/accounts       账号密码列表
/accounts/new   添加账号
/logs           执行日志
```

## 数据库配置说明

项目同时保留 `application.yml` 和 `application.properties`，其中 `application.properties` 使用完整的 `spring.datasource.*` 配置，避免 YAML 缩进错误导致启动时读取不到数据库连接。

当前数据库连接：

```properties
spring.datasource.url=jdbc:mysql://localhost:3306/stepwong_web?useUnicode=true&characterEncoding=utf8&useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=Asia/Shanghai
spring.datasource.username=root
spring.datasource.password=root
spring.datasource.driver-class-name=com.mysql.cj.jdbc.Driver
```

## 数据源兜底说明

项目保留 `src/main/resources/application.yml` 和 `src/main/resources/application.properties` 两份配置，同时新增 `DataSourceConfig` 作为兜底数据源配置。即使运行环境没有正确读取资源配置，也会使用当前 MySQL 连接创建数据源，避免出现 `url attribute is not specified`。
