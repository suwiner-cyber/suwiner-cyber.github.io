from __future__ import annotations
import argparse, hashlib, json, re, time, unicodedata
from collections import deque
from datetime import datetime, timezone
from pathlib import Path
from urllib.error import HTTPError, URLError
from urllib.parse import urljoin, urlparse, urldefrag
from urllib.request import Request, urlopen
from bs4 import BeautifulSoup

ROOT = Path(__file__).resolve().parents[1]
CFG = ROOT / 'config' / 'sources.json'
V4 = ROOT / 'data' / 'v4'
V5 = ROOT / 'data' / 'v5'
BOOKS = V5 / 'books'
TOC = V5 / 'toc'
INDEX = V5 / 'index.json'
STATE = V5 / 'six_full_state.json'
STATUS = V5 / 'six_full_status.json'
ERRORS = V5 / 'six_full_errors.json'
SHARD = 200
UA = 'Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 Chrome/139 Safari/537.36'
HEAD = {'User-Agent': UA, 'Accept-Language': 'zh-CN,zh;q=0.9', 'X-XiaoXiaoShuo-Crawler': 'six-full/2.0'}

PROFILES = {
    'www.tadu.com': {
        'name': '塔读文学',
        'seeds': ['https://www.tadu.com/store', 'https://www.tadu.com/book/rank/list/0-potential-0-0-1'],
        'book_path': [r'^/book/\d+/?$'],
        'chapter_path': [r'^/book/\d+/\d+/?$'],
        'nav_tokens': ('/store', '/book/rank/', '/rank', '/channel'),
    },
    'www.kanunu8.com': {
        'name': '努努书坊',
        'seeds': ['https://www.kanunu8.com/', 'https://www.kanunu8.com/genres.html'],
        'book_path': [r'^/book\d+/[^/]+/?$', r'^/book\d+/[^/]+/index\.html$', r'^/files/[^/]+/?$'],
        'chapter_path': [r'^/book\d+/[^/]+/\d+\.html$', r'^/files/[^/]+/\d+\.html$'],
        'nav_tokens': ('genres', '/book', '/files/', '/zt/', '/sort', '/list', '/writer', '/author'),
    },
    'www.99csw.com': {
        'name': '九九藏书网',
        'seeds': ['https://www.99csw.com/book/index.php'],
        'book_path': [r'^/book/\d+/?$', r'^/book/\d+/index\.(?:htm|html)$'],
        'chapter_path': [r'^/book/\d+/\d+\.(?:htm|html)$'],
        'nav_tokens': ('/book/', '/author/', '/wenku/', '/top/'),
    },
    'www.xs8.cn': {
        'name': '言情小说吧',
        'seeds': ['https://www.xs8.cn/'],
        'book_path': [r'^/bookquery/[0-9a-z]+/?$', r'^/book/[^?#]+$'],
        'chapter_path': [r'^/(?:chapter|read)/[^?#]+$'],
        'nav_tokens': ('/bookquery/', '/book/', '/category', '/store', '/rank', '/all'),
    },
    'www.1qxs.com': {
        'name': '一七小说',
        'seeds': ['https://www.1qxs.com/all.html', 'https://www.1qxs.com/'],
        'book_path': [r'^/xs_\d+/?$', r'^/xs_\d+/\d+/?$', r'^/(?:book|novel|info)/[^?#]+$'],
        'chapter_path': [r'^/xs_\d+/\d+/\d+\.html$', r'^/xs_\d+/\d+\.html$', r'^/chapter/[^?#]+$'],
        'nav_tokens': ('/all', '/sort', '/class', '/rank', '/quanben', '/xs_', '/book'),
    },
    'www.zwxiaoshuo.com': {
        'name': '滋味小说网',
        'seeds': ['https://www.zwxiaoshuo.com/', 'https://www.zwxiaoshuo.com/topweekvisit-1.html'],
        'book_path': [r'^/(?:book|xs|novel)-?\d+.*\.html$', r'^/book/\d+/?$'],
        'chapter_path': [r'^/(?:chapter|read)-?\d+.*\.html$', r'^/\d+/\d+\.html$'],
        'nav_tokens': ('top', 'full', 'update', 'author', 'list', 'sort', 'book', 'xs', 'novel'),
    },
}

BAD_PATH = re.compile(r'\.(?:jpg|jpeg|png|gif|webp|svg|css|js|ico|zip|rar|7z|pdf|mp3|m4a|mp4|woff2?)(?:$|\?)', re.I)
BAD_WORDS = ('login', 'logout', 'register', 'signup', 'account', 'usercenter', 'pay', 'payment', 'vip', 'download', 'app', 'feedback', 'help', 'contact', 'javascript:')
CHAPTER_TEXT = re.compile(r'^(?:第\s*[0-9零一二三四五六七八九十百千万两〇]+\s*[章节回卷]|楔子|序章|前言|后记|尾声|番外)', re.I)


def now():
    return datetime.now(timezone.utc).isoformat(timespec='seconds')


def load(path, default):
    try:
        return json.loads(path.read_text(encoding='utf-8'))
    except Exception:
        return default


def save(path, obj):
    path.parent.mkdir(parents=True, exist_ok=True)
    tmp = path.with_suffix(path.suffix + '.tmp')
    tmp.write_text(json.dumps(obj, ensure_ascii=False, indent=2), encoding='utf-8')
    tmp.replace(path)


def host(url):
    return (urlparse(url).hostname or '').lower()


def clean(text):
    return re.sub(r'\s+', ' ', text or '').strip()


def han(text):
    return bool(re.search(r'[\u3400-\u9fff]', text or ''))


def norm(text):
    text = unicodedata.normalize('NFKC', text or '').lower()
    return re.sub(r'[^0-9a-z\u3400-\u9fff]+', '', text)


def stable_id(url):
    return 'ext-' + hashlib.sha1(url.encode('utf-8')).hexdigest()[:20]


def dedupe_key(book):
    title = norm(book.get('title', ''))
    author = norm(re.sub(r'[_-](?:爱下电子书|九九藏书网|努努书坊|塔读文学|言情小说吧|滋味小说网|一七小说)$', '', book.get('author', '')))
    return title + '|' + author if title and author else (title + '|' + host(book.get('source_url', '')) if title else book.get('source_url', ''))


def fetch(url, referer=None):
    last = None
    for attempt in range(4):
        headers = dict(HEAD)
        if referer:
            headers['Referer'] = referer
        try:
            with urlopen(Request(url, headers=headers), timeout=30) as response:
                raw = response.read()
                content_type = response.headers.get('Content-Type', '')
            m = re.search(r'charset=([\w.-]+)', content_type, re.I)
            encodings = ([m.group(1)] if m else []) + ['utf-8', 'gb18030', 'gbk']
            for enc in encodings:
                try:
                    return raw.decode(enc)
                except Exception:
                    pass
            return raw.decode('utf-8', 'replace')
        except HTTPError as exc:
            last = exc
            if exc.code not in (408, 425, 429, 500, 502, 503, 504):
                break
        except (URLError, TimeoutError, OSError) as exc:
            last = exc
        time.sleep(min(2 ** attempt, 6))
    raise RuntimeError(str(last))


def canonical(url):
    url = urldefrag(url)[0]
    p = urlparse(url)
    if p.scheme not in ('http', 'https'):
        return ''
    return p._replace(fragment='').geturl()


def matches(path, patterns):
    return any(re.search(pattern, path, re.I) for pattern in patterns)


def chapterish(anchor_text, path, profile):
    return bool(CHAPTER_TEXT.search(clean(anchor_text))) or matches(path, profile['chapter_path'])


def navigable(url, anchor_text, source_host, profile):
    if not url or host(url) != source_host or BAD_PATH.search(url):
        return False
    low = url.lower()
    if any(word in low for word in BAD_WORDS):
        return False
    path = urlparse(url).path
    if matches(path, profile['chapter_path']) or chapterish(anchor_text, path, profile):
        return False
    if matches(path, profile['book_path']):
        return True
    if any(token in low for token in profile['nav_tokens']):
        return True
    text = clean(anchor_text)
    if text.isdigit() or text in ('下一页', '下页', '下一章', '更多', '全部', '书库', '分类', '排行', '全本', '最近更新'):
        return True
    if han(text) and len(text) <= 80 and path.count('/') <= 4:
        return True
    return False


def chapter_links(soup, page_url, source_host, profile):
    result = []
    seen = set()
    for a in soup.find_all('a', href=True):
        url = canonical(urljoin(page_url, a.get('href', '')))
        if not url or host(url) != source_host:
            continue
        path = urlparse(url).path
        title = clean(a.get_text(' ', strip=True))
        if url in seen:
            continue
        if matches(path, profile['chapter_path']) or CHAPTER_TEXT.search(title):
            if title and len(title) <= 160:
                seen.add(url)
                result.append({'n': len(result) + 1, 'title': title, 'url': url})
    return result


def detect_book(soup, page_url, profile):
    p = urlparse(page_url)
    path_match = matches(p.path, profile['book_path'])
    chapters = chapter_links(soup, page_url, p.hostname or '', profile)
    text = clean(soup.get_text(' ', strip=True))
    signals = sum((
        bool(re.search(r'(?:作者|作\s*者)\s*[:：]', text)),
        bool(re.search(r'(?:内容简介|作品简介|小说简介)', text)),
        bool(re.search(r'(?:目录|正文).{0,15}(?:\d+\s*章|章节)', text)),
        len(chapters) >= 3,
    ))
    return path_match or signals >= 2, chapters


def best_title(soup, page_url):
    for selector in ('h1', '.book-title', '.title h1', '#book_name', '.bookname h1'):
        node = soup.select_one(selector)
        if node:
            text = clean(node.get_text(' ', strip=True))
            if han(text) and len(text) <= 120:
                return re.sub(r'[_\-|｜].*$', '', text).strip()
    title = clean(soup.title.get_text(' ', strip=True)) if soup.title else ''
    title = re.sub(r'[_\-|｜].*$', '', title).strip()
    return title[:120]


def parse_book(soup, page_url, profile, chapters):
    full = clean(soup.get_text('\n', strip=True))
    title = best_title(soup, page_url)
    if not han(title):
        return None
    author = ''
    for pattern in (r'作者\s*[:：]\s*([^\n|｜]{1,60})', r'([^\s]{1,40})\s*著'):
        m = re.search(pattern, full)
        if m:
            author = clean(m.group(1))[:60]
            break
    kind = ''
    m = re.search(r'(?:分类|类型)\s*[:：]\s*([^\n|｜]{1,40})', full)
    if m:
        kind = clean(m.group(1))[:40]
    status = '已完结' if re.search(r'已完结|完本|已完成|全文完结', full) else ('连载中' if re.search(r'连载|更新中', full) else '')
    intro = ''
    for selector in ('.book-intro', '.bookintro', '.intro', '.book-desc', '.description', '#intro', '.abstract'):
        node = soup.select_one(selector)
        if node:
            candidate = clean(node.get_text(' ', strip=True))
            if len(candidate) >= 20:
                intro = candidate[:800]
                break
    if not intro:
        m = re.search(r'(?:内容简介|作品简介|小说简介)\s*[:：]?\s*(.{20,800}?)(?:目录|正文|最新章节|本书作者|$)', full, re.S)
        if m:
            intro = clean(m.group(1))[:800]
    cover = ''
    for selector in ('img.cover', '.cover img', '.book-cover img', '#bookimg img', '.bookimg img'):
        node = soup.select_one(selector)
        if node:
            src = node.get('data-src') or node.get('src')
            if src:
                cover = urljoin(page_url, src)
                break
    return {
        'id': stable_id(page_url), 'title': title, 'author': author, 'kind': kind, 'status': status,
        'intro': intro, 'cover': cover, 'source_url': page_url, 'source_host': host(page_url),
        'source_name': profile['name'], 'media': 'text', 'chapter_count': len(chapters),
        'last_chapter': chapters[-1]['title'] if chapters else '', 'detail_complete': True,
        'indexed_at': now(), 'catalog_only': False,
    }


def load_existing_books():
    rows = []
    idx = load(INDEX, {})
    if idx.get('shards'):
        for shard in idx.get('shards', []):
            rows.extend(load(V5 / shard['file'], {}).get('books', []))
        return rows
    idx4 = load(V4 / 'index.json', {})
    for shard in idx4.get('shards', []):
        rows.extend(load(V4 / shard['file'], {}).get('books', []))
    return rows


def merge_book(old, new):
    merged = dict(old)
    for field in ('author', 'kind', 'status', 'intro', 'cover', 'chapter_count', 'last_chapter'):
        if new.get(field) and (not old.get(field) or field in ('chapter_count', 'last_chapter')):
            merged[field] = new[field]
    sources = list(dict.fromkeys((old.get('sources') or [old.get('source_url')]) + (new.get('sources') or [new.get('source_url')])))
    merged['sources'] = [x for x in sources if x]
    return merged


def write_library(rows, source_reports):
    dedup = {}
    for book in rows:
        if not han(book.get('title', '')):
            continue
        key = dedupe_key(book)
        if key in dedup:
            dedup[key] = merge_book(dedup[key], book)
        else:
            dedup[key] = book
    rows = sorted(dedup.values(), key=lambda x: (x.get('indexed_at', ''), x.get('title', '')), reverse=True)
    BOOKS.mkdir(parents=True, exist_ok=True)
    keep = set()
    shards = []
    for start in range(0, len(rows), SHARD):
        name = f'{start // SHARD + 1:05d}.json'
        keep.add(name)
        chunk = rows[start:start + SHARD]
        save(BOOKS / name, {'books': chunk})
        shards.append({'file': 'books/' + name, 'count': len(chunk)})
    for file in BOOKS.glob('*.json'):
        if file.name not in keep:
            file.unlink()
    counts = {}
    for book in rows:
        counts[book.get('source_name', '未知')] = counts.get(book.get('source_name', '未知'), 0) + 1
    save(INDEX, {
        'version': 6, 'generated_at': now(), 'total': len(rows), 'shard_size': SHARD,
        'shards': shards, 'source_counts': counts, 'six_source_status': source_reports,
    })
    return len(rows)


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument('--pages-per-source', type=int, default=250)
    args = parser.parse_args()
    V5.mkdir(parents=True, exist_ok=True)
    TOC.mkdir(parents=True, exist_ok=True)
    cfg = load(CFG, {'sources': []})
    state = load(STATE, {'version': 2, 'sources': {}})
    errors = load(ERRORS, {'errors': []}).get('errors', [])
    rows = load_existing_books()
    by_url = {b.get('source_url'): b for b in rows if b.get('source_url')}
    reports = []

    for src in cfg.get('sources', []):
        source_host = host(src.get('url', ''))
        profile = PROFILES.get(source_host)
        if not profile or src.get('authorized') is not True or src.get('catalog_allowed') is not True:
            continue
        ss = state['sources'].setdefault(source_host, {
            'queue': list(profile['seeds']), 'seen': [], 'done': False, 'blocked': False,
            'consecutive_errors': 0,
        })
        queue = deque(ss.get('queue') or profile['seeds'])
        seen = set(ss.get('seen', []))
        if ss.get('done') and queue:
            ss['done'] = False
        pages = books_found = books_added = toc_written = success = failures = 0
        limit = max(20, min(args.pages_per_source, 500))

        while queue and pages < limit:
            page_url = queue.popleft()
            if page_url in seen:
                continue
            seen.add(page_url)
            pages += 1
            try:
                html = fetch(page_url, src.get('url'))
                soup = BeautifulSoup(html, 'html.parser')
                success += 1
                ss['consecutive_errors'] = 0
            except Exception as exc:
                failures += 1
                ss['consecutive_errors'] = int(ss.get('consecutive_errors', 0)) + 1
                errors.append({'time': now(), 'source': profile['name'], 'url': page_url, 'error': str(exc)[:500]})
                if ss['consecutive_errors'] >= 12 and success == 0:
                    ss['blocked'] = True
                    break
                continue

            is_book, chapters = detect_book(soup, page_url, profile)
            if is_book:
                book = parse_book(soup, page_url, profile, chapters)
                if book:
                    books_found += 1
                    if page_url not in by_url:
                        rows.append(book)
                        by_url[page_url] = book
                        books_added += 1
                    else:
                        by_url[page_url].update({k: v for k, v in book.items() if v not in ('', None, 0, False)})
                    if chapters:
                        save(TOC / f"{book['id']}.json", {'book_id': book['id'], 'title': book['title'], 'chapters': chapters, 'updated_at': now()})
                        toc_written += 1
            else:
                for a in soup.find_all('a', href=True):
                    target = canonical(urljoin(page_url, a.get('href', '')))
                    text = clean(a.get_text(' ', strip=True))
                    if navigable(target, text, source_host, profile) and target not in seen and target not in queue:
                        queue.append(target)
            time.sleep(max(0.35, float(src.get('delay_seconds', 1.0))))

        ss['queue'] = list(queue)
        ss['seen'] = sorted(seen)
        ss['done'] = not bool(queue) and not ss.get('blocked', False)
        ss['updated_at'] = now()
        reports.append({
            'name': profile['name'], 'host': source_host, 'pages_this_run': pages,
            'successful_pages': success, 'failed_pages': failures, 'books_found': books_found,
            'books_added': books_added, 'toc_written': toc_written, 'remaining_queue': len(queue),
            'seen_pages': len(seen), 'done': bool(ss.get('done')), 'blocked': bool(ss.get('blocked')),
        })

    total = write_library(rows, reports)
    save(STATE, state)
    save(ERRORS, {'generated_at': now(), 'errors': errors[-2000:]})
    active = [r for r in reports if not r['done'] and not r['blocked']]
    blocked = [r for r in reports if r['blocked']]
    status = {
        'version': 2, 'generated_at': now(), 'total_books': total,
        'all_done': bool(reports) and not active and not blocked,
        'continue_needed': bool(active), 'blocked_sources': [r['name'] for r in blocked],
        'sources': reports,
    }
    save(STATUS, status)
    print(json.dumps(status, ensure_ascii=False))

if __name__ == '__main__':
    main()
