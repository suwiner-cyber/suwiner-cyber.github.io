# 小小说私人同步服务

运行环境：PHP 8.0+，启用 `pdo_sqlite`、`sqlite3`、`openssl`。无需 MySQL。

## 部署

1. 将整个 `xiaoxiaoshuo-server` 目录上传到站点目录，例如 `/www/wwwroot/example.com/xiaoxiaoshuo`。
2. PHP-FPM/Apache 用户必须能写入本目录下的 `data/`。程序第一次请求时会自动创建 SQLite 数据库和表。
3. 推荐 HTTPS。客户端服务器地址填写到目录层级，例如：`https://example.com/xiaoxiaoshuo`，不要填写 `api.php`。
4. Apache 已提供 `.htaccess` 防止数据库文件被下载。Nginx 需要额外加入：

```nginx
location ~* /xiaoxiaoshuo/data/ { deny all; }
```

5. 设置后台管理员环境变量，不要把管理员密码写进代码：

```bash
# 生成管理员密码 hash
php -r "echo password_hash('你的后台强密码', PASSWORD_DEFAULT), PHP_EOL;"

# PHP-FPM / Web 环境变量
XXS_ADMIN_USER=admin
XXS_ADMIN_PASSWORD_HASH=上一步生成的hash
```

在宝塔面板可通过 PHP-FPM 配置、站点环境变量或服务器启动配置设置。修改环境变量后重载 PHP-FPM。

后台入口：`https://你的域名/xiaoxiaoshuo/admin.php`

## 客户端功能

- 注册：密码至少 8 位，并且必须同时包含英文字母和数字；客户端和服务端双重校验。
- 登录：Token 有效期 90 天。
- “记住用户名和密码”：密码由 Android Keystore AES-GCM 加密后保存在手机，不明文保存。
- 码字：草稿本机自动保存；登录后同时同步私人服务器。
- 小说缓存：仅当前登录账号自己的私人离线缓存，可跨设备取回，不做公开书库或公共分发。
- 管理后台：用户数量、草稿数量、私人缓存书籍/章节统计；支持禁用/启用用户、清除某用户缓存、删除用户。

## 数据位置

`data/xiaoxiaoshuo.sqlite`

建议把 `data/` 加入服务器自动备份，并限制 Web 直接访问。

## API

- `POST api.php?action=register`
- `POST api.php?action=login`
- `GET api.php?action=me`
- `POST api.php?action=draft_save`
- `GET api.php?action=draft_list`
- `GET api.php?action=draft_get&id=...`
- `POST api.php?action=draft_delete`
- `POST api.php?action=cache_book`
- `POST/GET api.php?action=cache_chapter`
- `GET api.php?action=cache_stats`
- `POST api.php?action=cache_delete_book`

除注册/登录外，其余私人接口通过 `Authorization: Bearer <token>` 验证账号。
