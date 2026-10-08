# 可复现压测

`load_test.py` 是标准库实现的并发 HTTP 客户端，不依赖 JMeter。压测前先启动 Demo：

```bash
bash deploy/build.sh
bash deploy/start-services.sh
```

准备一个包含访问令牌的文件（每行一个 Token），或者设置 `ACCESS_TOKEN`。令牌必须是通过当前网关登录得到的 JWT，旧课程 JMX 中的 `user-info` Header 已不能替代 JWT。

优惠券领取示例（网关地址通常是本机 `10010`）：

```bash
python3 bench/load_test.py \
  --url http://127.0.0.1:10010/prs/user-coupons/$COUPON_ID/receive \
  --count 10000 --concurrency 500 --tokens-file tokens.txt \
  --output coupon-run.json
```

学习进度示例：

```bash
python3 bench/load_test.py \
  --url http://127.0.0.1:10010/ls/learning-records \
  --count 10000 --concurrency 500 --token "$ACCESS_TOKEN" \
  --body-file bench/learning-progress.json --output learning-run.json
```

输出 JSON 包含请求数、并发数、有效测试窗口、QPS、HTTP 状态分布、传输错误和 P50/P95/P99 延迟。QPS 是压测客户端观察到的请求吞吐，不等于业务成功数；应同时记录 2xx、业务拒绝和错误数。

优惠券测试结束后必须等待 Outbox 和 RabbitMQ 消费完成，再执行 [`sql_metrics.sql`](sql_metrics.sql)。以 `coupon_receive_record` 的事件 ID 和最终 `user_coupon` 记录校验重复发券；不能只根据接口返回判断。

学习进度写库下降比例必须有同一批请求的基线：

```text
(baseline_db_writes - optimized_db_writes) / baseline_db_writes * 100%
```

基线和优化组必须固定硬件、请求数、并发、用户数、课表/小节和播放轨迹。建议通过数据库审计、MyBatis SQL 日志采样或 Mapper 计数器统计写入次数；不要把 HTTP 请求数直接当成数据库写入次数。

本仓库为基线提供了 `us.learning.progress-direct-write` 开关：默认 `false` 使用 Redis 合并写；压测容器可设置环境变量 `US_LEARNING_PROGRESS_DIRECT_WRITE=true` 运行直接写库基线。该开关只用于对比，不应作为生产配置。

每次结果还应保存：机器配置、服务版本、Redis/MQ/MySQL 配置、库存、Token 数量、测试时长、错误率和 MQ 最终状态。没有这些条件，QPS 和“下降 X%”只能作为一次环境内的观测值。
