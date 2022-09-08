### 背景
日志的重要性不言而喻，是我们排查问题，解决 BUG 的重要手段之一，但是在高并发环境下，又会存在悖论：
大量打印日志，消耗 I/O，导致 CPU 占用率高；减少日志，性能是下来了，但是排查问题的链路断掉了。

**痛点**：一方面需要借助日志可快速排查问题，另一方面要兼顾性能，二者能否得兼？
那么本文的动态日志调整实现就是为了能解决这个痛点所构思开发的。

### 功能特性
- 低侵入，快速接入：以二方包（jar）的形式介入，只需要配置启用，对业务无感
- 及时响应，随调随改：应对研发不小心在大流量入口链路打印了大量 INFO 日志，能及时调整日志级别
- 阶梯配置支持：默认全局设置兜底，又可以支持局部 Logger 放/限流
- 人性化操作：与操作界面，方便修改

### 使用方法
```java
// spring 容器可直接注册为 bean 使用
DynamicLoggerConfiguration config = new DynamicLoggerConfiguration();
config.init();
```

### 技术实现
slf4j( log4j2/logback/... ) + 配置中心( Apollo )

#### 设计图：
![img](https://user-images.githubusercontent.com/16236899/189108352-69951ee4-1c4c-49d2-a8c5-bfd8ebca6c43.png)

### 关注
- 个人技术博客：https://jifuwei.github.io/
- 原创"干货"，公众号：是咕咕鸡

![qrcode_for_gh_e51f1c0b8df7_258](https://user-images.githubusercontent.com/16236899/189108197-db0b4cee-33f1-4766-bbd2-be9a24624d8e.jpg)