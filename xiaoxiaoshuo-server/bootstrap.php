<?php
declare(strict_types=1);

function app_data_dir(): string {
    $dir = __DIR__ . '/data';
    if (!is_dir($dir)) { @mkdir($dir, 0750, true); }
    return $dir;
}
function db(): PDO {
    static $pdo = null;
    if ($pdo instanceof PDO) return $pdo;
    $pdo = new PDO('sqlite:' . app_data_dir() . '/xiaoxiaoshuo.sqlite');
    $pdo->setAttribute(PDO::ATTR_ERRMODE, PDO::ERRMODE_EXCEPTION);
    $pdo->setAttribute(PDO::ATTR_DEFAULT_FETCH_MODE, PDO::FETCH_ASSOC);
    $pdo->exec('PRAGMA journal_mode=WAL; PRAGMA foreign_keys=ON;');
    $pdo->exec('CREATE TABLE IF NOT EXISTS users (id INTEGER PRIMARY KEY AUTOINCREMENT, username TEXT NOT NULL UNIQUE, password_hash TEXT NOT NULL, created_at INTEGER NOT NULL, last_login INTEGER NOT NULL DEFAULT 0, disabled INTEGER NOT NULL DEFAULT 0)');
    $pdo->exec('CREATE TABLE IF NOT EXISTS tokens (token_hash TEXT PRIMARY KEY, user_id INTEGER NOT NULL, expires_at INTEGER NOT NULL, created_at INTEGER NOT NULL, FOREIGN KEY(user_id) REFERENCES users(id) ON DELETE CASCADE)');
    $pdo->exec('CREATE TABLE IF NOT EXISTS drafts (user_id INTEGER NOT NULL, draft_id TEXT NOT NULL, title TEXT NOT NULL, content TEXT NOT NULL, words INTEGER NOT NULL DEFAULT 0, updated_at INTEGER NOT NULL, PRIMARY KEY(user_id,draft_id), FOREIGN KEY(user_id) REFERENCES users(id) ON DELETE CASCADE)');
    $pdo->exec('CREATE TABLE IF NOT EXISTS book_cache (user_id INTEGER NOT NULL, book_key TEXT NOT NULL, title TEXT NOT NULL, author TEXT NOT NULL DEFAULT "", intro TEXT NOT NULL DEFAULT "", cover TEXT NOT NULL DEFAULT "", source_name TEXT NOT NULL DEFAULT "", source_url TEXT NOT NULL DEFAULT "", chapters_json TEXT NOT NULL DEFAULT "[]", updated_at INTEGER NOT NULL, PRIMARY KEY(user_id,book_key), FOREIGN KEY(user_id) REFERENCES users(id) ON DELETE CASCADE)');
    $pdo->exec('CREATE TABLE IF NOT EXISTS chapter_cache (user_id INTEGER NOT NULL, book_key TEXT NOT NULL, chapter_index INTEGER NOT NULL, chapter_name TEXT NOT NULL DEFAULT "", content TEXT NOT NULL, updated_at INTEGER NOT NULL, PRIMARY KEY(user_id,book_key,chapter_index), FOREIGN KEY(user_id) REFERENCES users(id) ON DELETE CASCADE)');
    return $pdo;
}
function json_input(): array {
    $raw = file_get_contents('php://input') ?: '';
    $x = json_decode($raw, true);
    return is_array($x) ? $x : [];
}
function respond(array $data, int $status=200): never {
    http_response_code($status); header('Content-Type: application/json; charset=utf-8'); header('Cache-Control: no-store'); echo json_encode($data, JSON_UNESCAPED_UNICODE|JSON_UNESCAPED_SLASHES); exit;
}
function clean_username(string $u): string { return trim($u); }
function valid_username(string $u): bool { return (bool)preg_match('/^[A-Za-z0-9_\x{4e00}-\x{9fff}.-]{3,32}$/u', $u); }
function valid_password(string $p): bool { return strlen($p) >= 8 && strlen($p) <= 128 && preg_match('/[A-Za-z]/', $p) && preg_match('/[0-9]/', $p); }
function bearer_token(): string {
    $h = $_SERVER['HTTP_AUTHORIZATION'] ?? '';
    if (preg_match('/^Bearer\s+(.+)$/i', trim($h), $m)) return trim($m[1]);
    return '';
}
function current_user(): array {
    $token = bearer_token(); if ($token === '') respond(['ok'=>false,'message'=>'请先登录'],401);
    $hash = hash('sha256',$token); $q=db()->prepare('SELECT u.* FROM tokens t JOIN users u ON u.id=t.user_id WHERE t.token_hash=? AND t.expires_at>? AND u.disabled=0'); $q->execute([$hash,time()]); $u=$q->fetch(); if(!$u) respond(['ok'=>false,'message'=>'登录已失效，请重新登录'],401); return $u;
}
function issue_token(int $uid): string {
    $token = bin2hex(random_bytes(32)); $now=time(); $q=db()->prepare('INSERT INTO tokens(token_hash,user_id,expires_at,created_at) VALUES(?,?,?,?)'); $q->execute([hash('sha256',$token),$uid,$now+60*60*24*90,$now]); return $token;
}
function book_key(string $title): string { return hash('sha256', trim($title)); }

function admin_config_file(): string { return app_data_dir().'/admin.json'; }
function admin_config(): array {
    static $cfg = null;
    if (is_array($cfg)) return $cfg;
    $envUser = getenv('XXS_ADMIN_USER') ?: '';
    $envHash = getenv('XXS_ADMIN_PASSWORD_HASH') ?: '';
    if ($envHash !== '') return $cfg=['username'=>$envUser !== '' ? $envUser : 'admin','password_hash'=>$envHash,'created_at'=>0];
    $file=admin_config_file();
    if (is_file($file)) {
        $x=json_decode((string)@file_get_contents($file),true);
        if(is_array($x) && !empty($x['username']) && !empty($x['password_hash'])) return $cfg=$x;
    }
    return $cfg=[];
}
function admin_is_configured(): bool { $c=admin_config(); return !empty($c['username']) && !empty($c['password_hash']); }
function admin_user(): string { $c=admin_config(); return (string)($c['username'] ?? 'admin'); }
function admin_password_hash(): string { $c=admin_config(); return (string)($c['password_hash'] ?? ''); }
function save_admin_config(string $username,string $password): bool {
    $username=trim($username);
    if(!valid_username($username) || !valid_password($password)) return false;
    $data=['username'=>$username,'password_hash'=>password_hash($password,PASSWORD_DEFAULT),'created_at'=>time(),'updated_at'=>time()];
    $tmp=admin_config_file().'.tmp';
    if(@file_put_contents($tmp,json_encode($data,JSON_UNESCAPED_UNICODE|JSON_PRETTY_PRINT),LOCK_EX)===false) return false;
    @chmod($tmp,0640);
    return @rename($tmp,admin_config_file());
}
function change_admin_password(string $password): bool {
    if(!valid_password($password)) return false;
    $c=admin_config(); if(!$c) return false;
    $c['password_hash']=password_hash($password,PASSWORD_DEFAULT);$c['updated_at']=time();
    return @file_put_contents(admin_config_file(),json_encode($c,JSON_UNESCAPED_UNICODE|JSON_PRETTY_PRINT),LOCK_EX)!==false;
}
function admin_ok(): bool {
    if (session_status() !== PHP_SESSION_ACTIVE) session_start();
    return !empty($_SESSION['xxs_admin']);
}
function admin_session_start(): void {
    if(session_status()===PHP_SESSION_ACTIVE)return;
    @ini_set('session.cookie_httponly','1');
    @ini_set('session.cookie_samesite','Strict');
    if(!empty($_SERVER['HTTPS']) && $_SERVER['HTTPS']!=='off') @ini_set('session.cookie_secure','1');
    session_start();
}
function csrf_token(): string {
    admin_session_start();
    if(empty($_SESSION['xxs_csrf'])) $_SESSION['xxs_csrf']=bin2hex(random_bytes(24));
    return (string)$_SESSION['xxs_csrf'];
}
function csrf_ok(string $token): bool { admin_session_start(); return $token!=='' && !empty($_SESSION['xxs_csrf']) && hash_equals((string)$_SESSION['xxs_csrf'],$token); }
function h(string $s): string { return htmlspecialchars($s, ENT_QUOTES|ENT_SUBSTITUTE, 'UTF-8'); }
function bytes_human(int $n): string { $u=['B','KB','MB','GB'];$i=0;$v=max(0,$n);while($v>=1024&&$i<count($u)-1){$v/=1024;$i++;}return ($i===0?(string)(int)$v:number_format($v,1)).' '.$u[$i]; }
function server_base_url(): string {
    $https=!empty($_SERVER['HTTPS'])&&$_SERVER['HTTPS']!=='off';$scheme=$https?'https':'http';$host=$_SERVER['HTTP_HOST']??'localhost';$path=rtrim(str_replace('\\','/',dirname($_SERVER['SCRIPT_NAME']??'/')),'/');
    return $scheme.'://'.$host.($path===''?'':$path);
}
