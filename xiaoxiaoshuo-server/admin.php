<?php
declare(strict_types=1);
require __DIR__.'/bootstrap.php';
if (session_status() !== PHP_SESSION_ACTIVE) session_start();

$error='';
if(isset($_POST['logout'])){session_destroy();header('Location: admin.php');exit;}
if(!admin_ok() && isset($_POST['username'],$_POST['password'])){
    $hash=admin_password_hash();
    if($hash==='' ){$error='服务器未配置 XXS_ADMIN_PASSWORD_HASH，请先按 README 设置管理员密码。';}
    elseif(hash_equals(admin_user(),(string)$_POST['username']) && password_verify((string)$_POST['password'],$hash)){$_SESSION['xxs_admin']=1;header('Location: admin.php');exit;}
    else $error='管理员账号或密码错误';
}
if(admin_ok() && isset($_POST['action'])){
    $action=(string)$_POST['action'];$id=(int)($_POST['id']??0);
    if($action==='disable_user')db()->prepare('UPDATE users SET disabled=1 WHERE id=?')->execute([$id]);
    if($action==='enable_user')db()->prepare('UPDATE users SET disabled=0 WHERE id=?')->execute([$id]);
    if($action==='delete_user')db()->prepare('DELETE FROM users WHERE id=?')->execute([$id]);
    if($action==='clear_cache'){$uid=(int)($_POST['user_id']??0);$pdo=db();$pdo->beginTransaction();$pdo->prepare('DELETE FROM chapter_cache WHERE user_id=?')->execute([$uid]);$pdo->prepare('DELETE FROM book_cache WHERE user_id=?')->execute([$uid]);$pdo->commit();}
    header('Location: admin.php');exit;
}
?><!doctype html><html lang="zh-CN"><head><meta charset="utf-8"><meta name="viewport" content="width=device-width,initial-scale=1"><title>小小说管理后台</title><style>
body{margin:0;background:#f4f5f2;color:#26332d;font:14px/1.55 system-ui,-apple-system,"Segoe UI",sans-serif}.wrap{max-width:1180px;margin:32px auto;padding:0 20px}.card{background:#fff;border-radius:18px;padding:22px;margin-bottom:18px;box-shadow:0 5px 22px rgba(30,50,40,.06)}h1{margin:0 0 6px;color:#315847}h2{margin:0 0 16px}input,button{border:1px solid #dce5df;border-radius:10px;padding:10px 12px;font-size:14px}input{width:100%;box-sizing:border-box;margin:6px 0 12px}button{background:#315847;color:white;cursor:pointer}.danger{background:#a54c48}.muted{color:#7e857f}.grid{display:grid;grid-template-columns:repeat(4,1fr);gap:12px}.stat{padding:16px;background:#f5f8f6;border-radius:14px}.num{font-size:26px;font-weight:800;color:#315847}table{width:100%;border-collapse:collapse}th,td{text-align:left;padding:11px 8px;border-bottom:1px solid #edf0ed;vertical-align:top}.inline{display:inline}.inline button{padding:6px 9px;margin:2px}.top{display:flex;align-items:center;justify-content:space-between}@media(max-width:760px){.grid{grid-template-columns:1fr 1fr}.table-wrap{overflow:auto}}
</style></head><body><div class="wrap">
<?php if(!admin_ok()): ?><div class="card" style="max-width:420px;margin:80px auto"><h1>小小说管理后台</h1><p class="muted">用户、草稿与私人离线缓存管理</p><?php if($error):?><p style="color:#a54c48"><?=h($error)?></p><?php endif?><form method="post"><input name="username" placeholder="管理员账号" required><input name="password" type="password" placeholder="管理员密码" required><button style="width:100%">登录后台</button></form></div>
<?php else:
$pdo=db();$users=(int)$pdo->query('SELECT COUNT(*) FROM users')->fetchColumn();$drafts=(int)$pdo->query('SELECT COUNT(*) FROM drafts')->fetchColumn();$books=(int)$pdo->query('SELECT COUNT(*) FROM book_cache')->fetchColumn();$chapters=(int)$pdo->query('SELECT COUNT(*) FROM chapter_cache')->fetchColumn();
$rows=$pdo->query('SELECT u.id,u.username,u.created_at,u.last_login,u.disabled,(SELECT COUNT(*) FROM drafts d WHERE d.user_id=u.id) drafts,(SELECT COUNT(*) FROM book_cache b WHERE b.user_id=u.id) books,(SELECT COUNT(*) FROM chapter_cache c WHERE c.user_id=u.id) chapters FROM users u ORDER BY u.id DESC')->fetchAll();
?>
<div class="top"><div><h1>小小说管理后台</h1><div class="muted">私人账号与缓存管理</div></div><form method="post"><button name="logout" value="1">退出</button></form></div>
<div class="card grid"><div class="stat"><div class="num"><?=$users?></div>注册用户</div><div class="stat"><div class="num"><?=$drafts?></div>码字草稿</div><div class="stat"><div class="num"><?=$books?></div>私人缓存书籍</div><div class="stat"><div class="num"><?=$chapters?></div>缓存章节</div></div>
<div class="card"><h2>用户管理</h2><div class="table-wrap"><table><thead><tr><th>ID</th><th>用户名</th><th>注册/登录</th><th>草稿</th><th>缓存</th><th>状态</th><th>操作</th></tr></thead><tbody>
<?php foreach($rows as $r):?><tr><td><?=$r['id']?></td><td><strong><?=h($r['username'])?></strong></td><td><?=date('Y-m-d',$r['created_at'])?><br><span class="muted"><?=$r['last_login']?date('Y-m-d H:i',$r['last_login']):'未登录'?></span></td><td><?=$r['drafts']?></td><td><?=$r['books']?> 本 / <?=$r['chapters']?> 章</td><td><?=$r['disabled']?'已禁用':'正常'?></td><td>
<form class="inline" method="post"><input type="hidden" name="id" value="<?=$r['id']?>"><button name="action" value="<?=$r['disabled']?'enable_user':'disable_user'?>"><?=$r['disabled']?'启用':'禁用'?></button></form>
<form class="inline" method="post" onsubmit="return confirm('确定清空该用户全部服务器小说缓存？')"><input type="hidden" name="user_id" value="<?=$r['id']?>"><button name="action" value="clear_cache">清缓存</button></form>
<form class="inline" method="post" onsubmit="return confirm('确定删除该用户及全部数据？不可恢复。')"><input type="hidden" name="id" value="<?=$r['id']?>"><button class="danger" name="action" value="delete_user">删除用户</button></form>
</td></tr><?php endforeach?></tbody></table></div></div>
<?php endif?></div></body></html>
