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
function admin_user(): string { return getenv('XXS_ADMIN_USER') ?: 'admin'; }
function admin_password_hash(): string { return getenv('XXS_ADMIN_PASSWORD_HASH') ?: ''; }
function admin_ok(): bool {
    if (session_status() !== PHP_SESSION_ACTIVE) session_start();
    return !empty($_SESSION['xxs_admin']);
}
function h(string $s): string { return htmlspecialchars($s, ENT_QUOTES|ENT_SUBSTITUTE, 'UTF-8'); }
