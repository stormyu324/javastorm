# Storm 商城

基于 Spring Boot 的电商网站，包含**商城前台**和**商品管理后台**，重点是让商品上架尽量省事。

## 快速开始

只需要安装 **JDK 21**（不用装 Maven 和数据库，项目自带 Maven Wrapper）。

```bash
git clone https://github.com/stormyu324/javastorm.git
cd javastorm
git checkout claude/determined-gauss-5n658i

# macOS / Linux
./mvnw spring-boot:run
# Windows
mvnw.cmd spring-boot:run
```

看到 `Started ShopApplication` 后打开浏览器：

- 商城首页：http://localhost:8080
- 管理后台：http://localhost:8080/admin （默认账号 `admin` / `admin123`）

首次启动会自动写入几条示例商品（`DEMO_DATA=false` 可关闭）。按 `Ctrl + C` 停止。

## 上架商品的几种方式

| 方式 | 适合场景 | 入口 |
| --- | --- | --- |
| 单个添加 | 日常上新 | 后台 → 商品 → 添加商品 |
| 保存并继续添加 | 连续录入多个商品，自动沿用上一个的分类 | 添加商品页底部按钮 |
| 复制商品 | 同款不同颜色/尺码，复制后改几项即可 | 商品列表 → 复制 |
| 表格批量导入 | 一次上架几十上百个商品，或批量改价、改库存 | 后台 → 批量导入 |
| 批量上架/下架 | 勾选多个商品一键上下架或删除 | 商品列表顶部 |

### 批量导入说明

1. 在「批量导入」页下载 CSV 模板，用 Excel / WPS 填写后另存为 CSV。
2. 列：`名称, SKU, 分类, 售价, 原价, 库存, 状态, 图片, 描述`，其中只有**名称**和**售价**必填。
3. **SKU 相同则更新、不同则新建**，所以同一张表可以反复导入用来改价、补库存。
4. 分类不存在会自动创建；多张图片网址用 `|` 分隔；UTF-8 和 GBK 编码都能识别。
5. 出错的行会列出行号和原因，其他行照常导入。

### 商品图片

- 添加/编辑商品时可一次选择或拖入多张图片，保存到 `./uploads`（`UPLOAD_DIR` 可修改）。
- 也可以直接粘贴网络图片地址；调整地址顺序即可更换主图。

## 其他功能

- 前台：分类浏览、搜索、价格排序、商品详情多图、购物车、下单（自动扣库存，库存不足或已下架时不能下单）
- 后台：数据概览（在售/草稿/库存紧张/待处理订单）、分类管理、订单管理（取消订单自动退回库存）

> 目前没有接入在线支付，订单提交后由商家线下确认。

## 配置（环境变量）

| 变量 | 默认值 | 说明 |
| --- | --- | --- |
| `ADMIN_USERNAME` / `ADMIN_PASSWORD` | `admin` / `admin123` | 后台账号，**上线前一定要修改密码** |
| `DB_URL` / `DB_USERNAME` / `DB_PASSWORD` | 本地 H2 文件 `./data/shop` | 换 MySQL/PostgreSQL 时修改，并在 `pom.xml` 加驱动 |
| `UPLOAD_DIR` | `./uploads` | 图片保存目录 |
| `PORT` | `8080` | 端口 |
| `DEMO_DATA` | `true` | 空库时是否写入示例数据 |

## 打包部署

```bash
./mvnw package
ADMIN_PASSWORD=你的强密码 java -jar target/shop-0.1.0.jar
```

## 测试

```bash
./mvnw test
```
