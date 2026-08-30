#!/usr/bin/env python3
import json, re, urllib.request
from datetime import datetime, timezone
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
OUT = ROOT / 'data' / 'recommendations.json'
RANK_PAGE = 'https://fanqienovel.com/rank'
API = 'https://fanqienovel.com/api/rank/category/list'
HEADERS = {
    'User-Agent': 'Mozilla/5.0 (Linux; Android 14) AppleWebKit/537.36 Chrome/124 Safari/537.36',
    'Accept': 'application/json, text/plain, */*',
    'Referer': 'https://fanqienovel.com/rank',
}

# Only ranking metadata comes from the public ranking page. Reading content is NOT taken from
# this snapshot; the Android client searches its own HEALTHY EXTERNAL source pool by title.
CATEGORIES = [
    ('玄幻', '258', '1'),
    ('都市', '261', '1'),
    ('仙侠', '1140', '1'),
    ('历史', '273', '1'),
    ('科幻', '8', '1'),
    ('悬疑', '539', '1'),
    ('言情', '248', '0'),
    ('游戏', '746', '1'),
    ('同人', '718', '1'),
]

def get_text(url):
    req = urllib.request.Request(url, headers=HEADERS)
    with urllib.request.urlopen(req, timeout=18) as r:
        return r.read().decode('utf-8', 'ignore')

def rank_version():
    try:
        text = get_text(RANK_PAGE)
        m = re.search(r'"rankVersion"\s*:\s*"([^"]+)"', text)
        return m.group(1) if m else ''
    except Exception:
        return ''

def first(d, *keys):
    for k in keys:
        v = d.get(k)
        if v not in (None, '', 0):
            return v
    return ''

def fetch_rank(cat_id, gender, mold, limit=12, rv=''):
    q = (
        f'{API}?app_id=2503&rank_list_type=3&offset=0&limit={limit}'
        f'&category_id={cat_id}&rank_version={rv}&gender={gender}&rankMold={mold}'
    )
    try:
        data = json.loads(get_text(q))
        if data.get('code') != 0:
            return []
        rows = ((data.get('data') or {}).get('book_list') or [])
    except Exception:
        return []
    out=[]
    for i,b in enumerate(rows):
        title=str(first(b,'bookName','book_name','name')).strip()
        if not title: continue
        out.append({
            'rank': i+1,
            'title': title,
            'author': str(first(b,'authorName','author','author_name')).strip() or '未知作者',
            'cover': str(first(b,'thumbUri','thumb_uri','cover','coverUrl')).strip(),
            'intro': str(first(b,'abstract','bookAbstract','bookDesc','description')).strip()[:160],
            'category': str(first(b,'categoryV2','category','category_name')).strip(),
            'word_count': int(first(b,'wordNumber','word_count','wordCount') or 0),
            'latest': str(first(b,'lastChapterTitle','latestChapterTitle','latest_chapter')).strip(),
            'book_id': str(first(b,'bookId','book_id','id')).strip(),
        })
    return out

def dedupe_round_robin(groups, limit=8):
    out=[]; seen=set(); pos=0
    while len(out)<limit:
        changed=False
        for group in groups:
            if pos >= len(group): continue
            changed=True
            b=group[pos]
            key=b['title'].replace(' ','')
            if key not in seen:
                seen.add(key); out.append(b)
                if len(out)>=limit: break
        if not changed: break
        pos+=1
    for i,b in enumerate(out): b['rank']=i+1
    return out

def main():
    rv=rank_version()
    category_data={}
    read_groups=[]; finish_groups=[]; new_groups=[]
    for name,cid,gender in CATEGORIES:
        reading=fetch_rank(cid,gender,2,12,rv)
        finished=fetch_rank(cid,gender,1,8,rv)
        newbooks=fetch_rank(cid,gender,3,8,rv)
        for rows in (reading,finished,newbooks):
            for b in rows:
                if not b.get('category'): b['category']=name
        category_data[name]=reading[:12]
        read_groups.append(reading)
        finish_groups.append(finished)
        new_groups.append(newbooks)
    featured=dedupe_round_robin(read_groups,8)
    finished=dedupe_round_robin(finish_groups,8)
    newbooks=dedupe_round_robin(new_groups,8)
    # A second reading mix acts as the "巅峰榜"; it is intentionally metadata-only.
    peak=dedupe_round_robin([g[1:] for g in read_groups if len(g)>1],8)
    result={
        'schema':3,
        'generated_at':datetime.now(timezone.utc).isoformat(timespec='seconds'),
        'refresh_policy':'12h_snapshot',
        'source_mode':'official_rank_metadata_then_external_source_lookup',
        'content_source_policy':'Android client must resolve a healthy external source before reading',
        'featured':featured,
        'rankings':{'推荐榜':featured,'完本榜':finished,'巅峰榜':peak,'新书榜':newbooks},
        'category_order':[x[0] for x in CATEGORIES],
        'categories':category_data,
    }
    OUT.parent.mkdir(parents=True,exist_ok=True)
    OUT.write_text(json.dumps(result,ensure_ascii=False,indent=2)+'\n',encoding='utf-8')
    print('built',len(featured),'featured; rank_version=',rv or 'blank')

if __name__=='__main__':
    main()
