from __future__ import annotations
import json,re
from pathlib import Path
from urllib.parse import urlparse

ROOT=Path(__file__).resolve().parents[1]
V5=ROOT/'data'/'v5'; INDEX=V5/'index.json'; BOOKS=V5/'books'; SHARD=200

NAV_BAD=('登入/注册','登录/注册','首页 排行','首页 分类','作品信息 首页','爱下电子书 爱下电子书','完结 登入','完结 登录')

def load(p,d):
    try:return json.loads(p.read_text(encoding='utf-8'))
    except Exception:return d

def save(p,o):
    p.parent.mkdir(parents=True,exist_ok=True)
    q=p.with_suffix(p.suffix+'.tmp')
    q.write_text(json.dumps(o,ensure_ascii=False,indent=2),encoding='utf-8')
    q.replace(p)

def clean_author(v):
    s=re.sub(r'\s+',' ',str(v or '')).strip()
    s=re.sub(r'^作者\s*[:：]?\s*','',s)
    for pat in (r'[_-]爱下电子书.*$',r'\s+爱下电子书.*$',r'\s+(?:作品信息|首页|排行|分类|完结|登入/注册|登录/注册).*$'):
        s=re.sub(pat,'',s).strip()
    if len(s)>40:s=s[:40].strip()
    return s

def clean_kind(v):
    s=re.sub(r'\s+',' ',str(v or '')).strip()
    if len(s)>40 or any(x in s for x in NAV_BAD):
        m=re.search(r'(玄幻|奇幻|武侠|仙侠|都市|现实|军事|历史|游戏|体育|科幻|悬疑|轻小说|言情|穿越|古言|现言|青春|校园|同人)',s)
        return m.group(1) if m else ''
    return s

def main():
    idx=load(INDEX,{})
    rows=[]
    for s in idx.get('shards',[]):rows.extend(load(V5/s['file'],{}).get('books',[]))
    out=[];removed=changed=0
    for b in rows:
        u=str(b.get('source_url') or '')
        h=(urlparse(u).hostname or '').lower().replace('www.','')
        p=urlparse(u).path
        # Author/collection pages are discovery pages, not novels.
        if h=='ixdzs8.com' and p.startswith('/author/'):
            removed+=1;continue
        olda=b.get('author','');oldk=b.get('kind','')
        b['author']=clean_author(olda);b['kind']=clean_kind(oldk)
        if b['author']!=olda or b['kind']!=oldk:changed+=1
        out.append(b)
    keep=set();sh=[]
    for i in range(0,len(out),SHARD):
        n=f'{i//SHARD+1:05d}.json';keep.add(n);chunk=out[i:i+SHARD]
        save(BOOKS/n,{'books':chunk});sh.append({'file':'books/'+n,'count':len(chunk)})
    for p in BOOKS.glob('*.json'):
        if p.name not in keep:p.unlink()
    counts={}
    for b in out:
        names=b.get('source_names') or [b.get('source_name','未知')]
        for n in set(x for x in names if x):counts[n]=counts.get(n,0)+1
    idx.update({'total':len(out),'shards':sh,'source_counts':counts,'metadata_sanitized':True})
    save(INDEX,idx)
    print(json.dumps({'books':len(out),'removed_non_books':removed,'cleaned':changed},ensure_ascii=False))

if __name__=='__main__':main()
