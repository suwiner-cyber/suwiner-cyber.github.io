<?php
declare(strict_types=1);
require __DIR__.'/bootstrap.php';
header('Access-Control-Allow-Origin: *');
header('Access-Control-Allow-Headers: Authorization, Content-Type');
header('Access-Control-Allow-Methods: GET, POST, OPTIONS');
if ($_SERVER['REQUEST_METHOD']==='OPTIONS') respond(['ok'=>true]);
$action = $_GET['action'] ?? '';

if ($action==='register') {
    $in=json_input(); $u=clean_username((string)($in['username']??'')); $p=(string)($in['password']??'');
    if(!valid_username($u)) respond(['ok'=>false,'message'=>'用户名需3-32位，仅支持中英文、数字、下划线、点和横线'],422);
    if(!valid_password($p)) respond(['ok'=>false,'message'=>'密码至少8位，并且必须同时包含英文字母和数字'],422);
    try{$q=db()->prepare('INSERT INTO users(username,password_hash,created_at,last_login) VALUES(?,?,?,0)');$q->execute([$u,password_hash($p,PASSWORD_DEFAULT),time()]);respond(['ok'=>true,'message'=>'注册成功']);}
    catch(PDOException $e){if(str_contains($e->getMessage(),'UNIQUE'))respond(['ok'=>false,'message'=>'用户名已经存在'],409);throw $e;}
}
if ($action==='login') {
    $in=json_input(); $u=clean_username((string)($in['username']??'')); $p=(string)($in['password']??'');
    $q=db()->prepare('SELECT * FROM users WHERE username=? LIMIT 1');$q->execute([$u]);$row=$q->fetch();
    if(!$row||$row['disabled']||!password_verify($p,$row['password_hash']))respond(['ok'=>false,'message'=>'用户名或密码错误'],401);
    db()->prepare('UPDATE users SET last_login=? WHERE id=?')->execute([time(),$row['id']]);$token=issue_token((int)$row['id']);respond(['ok'=>true,'message'=>'登录成功','token'=>$token,'username'=>$row['username']]);
}
if ($action==='logout') {
    $token=bearer_token(); if($token!=='')db()->prepare('DELETE FROM tokens WHERE token_hash=?')->execute([hash('sha256',$token)]);respond(['ok'=>true]);
}
if ($action==='me') {$u=current_user();respond(['ok'=>true,'username'=>$u['username'],'createdAt'=>(int)$u['created_at']]);}

if ($action==='draft_save') {
    $u=current_user();$in=json_input();$id=preg_replace('/[^A-Za-z0-9_-]/','',(string)($in['id']??''));if($id==='')respond(['ok'=>false,'message'=>'草稿ID无效'],422);
    $title=trim((string)($in['title']??'未命名作品'));$content=(string)($in['content']??'');$words=max(0,(int)($in['words']??0));$updated=max(time(),(int)($in['updatedAt']??0));
    $q=db()->prepare('INSERT INTO drafts(user_id,draft_id,title,content,words,updated_at) VALUES(?,?,?,?,?,?) ON CONFLICT(user_id,draft_id) DO UPDATE SET title=excluded.title,content=excluded.content,words=excluded.words,updated_at=excluded.updated_at');$q->execute([$u['id'],$id,$title,$content,$words,$updated]);respond(['ok'=>true]);
}
if ($action==='draft_list') {
    $u=current_user();$q=db()->prepare('SELECT draft_id AS id,title,words,updated_at AS updatedAt FROM drafts WHERE user_id=? ORDER BY updated_at DESC');$q->execute([$u['id']]);respond(['ok'=>true,'items'=>$q->fetchAll()]);
}
if ($action==='draft_get') {
    $u=current_user();$id=preg_replace('/[^A-Za-z0-9_-]/','',(string)($_GET['id']??''));$q=db()->prepare('SELECT draft_id AS id,title,content,words,updated_at AS updatedAt FROM drafts WHERE user_id=? AND draft_id=?');$q->execute([$u['id'],$id]);$r=$q->fetch();respond($r?['ok'=>true,'item'=>$r]:['ok'=>false,'message'=>'草稿不存在'],$r?200:404);
}
if ($action==='draft_delete') {
    $u=current_user();$in=json_input();$id=preg_replace('/[^A-Za-z0-9_-]/','',(string)($in['id']??''));db()->prepare('DELETE FROM drafts WHERE user_id=? AND draft_id=?')->execute([$u['id'],$id]);respond(['ok'=>true]);
}

if ($action==='cache_book') {
    $u=current_user();$in=json_input();$title=trim((string)($in['title']??''));if($title==='')respond(['ok'=>false,'message'=>'书名不能为空'],422);$key=book_key($title);$chapters=json_encode($in['chapters']??[],JSON_UNESCAPED_UNICODE|JSON_UNESCAPED_SLASHES);
    $q=db()->prepare('INSERT INTO book_cache(user_id,book_key,title,author,intro,cover,source_name,source_url,chapters_json,updated_at) VALUES(?,?,?,?,?,?,?,?,?,?) ON CONFLICT(user_id,book_key) DO UPDATE SET author=excluded.author,intro=excluded.intro,cover=excluded.cover,source_name=excluded.source_name,source_url=excluded.source_url,chapters_json=excluded.chapters_json,updated_at=excluded.updated_at');$q->execute([$u['id'],$key,$title,(string)($in['author']??''),(string)($in['intro']??''),(string)($in['cover']??''),(string)($in['sourceName']??''),(string)($in['sourceUrl']??''),$chapters,time()]);respond(['ok'=>true]);
}
if ($action==='cache_chapter' && $_SERVER['REQUEST_METHOD']==='POST') {
    $u=current_user();$in=json_input();$title=trim((string)($in['title']??''));$idx=(int)($in['chapterIndex']??-1);$content=(string)($in['content']??'');if($title===''||$idx<0||trim($content)==='')respond(['ok'=>false,'message'=>'章节参数无效'],422);$key=book_key($title);$q=db()->prepare('INSERT INTO chapter_cache(user_id,book_key,chapter_index,chapter_name,content,updated_at) VALUES(?,?,?,?,?,?) ON CONFLICT(user_id,book_key,chapter_index) DO UPDATE SET chapter_name=excluded.chapter_name,content=excluded.content,updated_at=excluded.updated_at');$q->execute([$u['id'],$key,$idx,(string)($in['chapterName']??''),$content,time()]);respond(['ok'=>true]);
}
if ($action==='cache_chapter' && $_SERVER['REQUEST_METHOD']==='GET') {
    $u=current_user();$title=trim((string)($_GET['title']??''));$idx=(int)($_GET['chapterIndex']??-1);$q=db()->prepare('SELECT chapter_name,content FROM chapter_cache WHERE user_id=? AND book_key=? AND chapter_index=?');$q->execute([$u['id'],book_key($title),$idx]);$r=$q->fetch();respond($r?['ok'=>true,'chapterName'=>$r['chapter_name'],'content'=>$r['content']]:['ok'=>false,'message'=>'缓存不存在'],$r?200:404);
}
if ($action==='cache_stats') {
    $u=current_user();$q=db()->prepare('SELECT COUNT(*) FROM book_cache WHERE user_id=?');$q->execute([$u['id']]);$books=(int)$q->fetchColumn();$q=db()->prepare('SELECT COUNT(*) FROM chapter_cache WHERE user_id=?');$q->execute([$u['id']]);$chapters=(int)$q->fetchColumn();respond(['ok'=>true,'books'=>$books,'chapters'=>$chapters]);
}
if ($action==='cache_delete_book') {
    $u=current_user();$in=json_input();$title=trim((string)($in['title']??''));$key=book_key($title);$pdo=db();$pdo->beginTransaction();$pdo->prepare('DELETE FROM chapter_cache WHERE user_id=? AND book_key=?')->execute([$u['id'],$key]);$pdo->prepare('DELETE FROM book_cache WHERE user_id=? AND book_key=?')->execute([$u['id'],$key]);$pdo->commit();respond(['ok'=>true]);
}
respond(['ok'=>false,'message'=>'未知接口'],404);
