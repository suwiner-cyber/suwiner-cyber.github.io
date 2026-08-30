#!/usr/bin/env python3
import json, re
from collections import defaultdict
from datetime import datetime, timezone
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
DATA = ROOT / 'data' / 'v5'
OUT = ROOT / 'data' / 'recommendations.json'
RULES = [
 ('玄幻',['玄幻','奇幻','魔法']),('都市',['都市','职场','生活','娱乐']),
 ('仙侠',['仙侠','修仙','修真','洪荒']),('武侠',['武侠','国术','江湖']),
 ('历史',['历史','架空','穿越','古代','争霸']),('科幻',['科幻','末世','星际','未来','机甲']),
 ('悬疑',['悬疑','推理','灵异','惊悚']),('言情',['言情','婚恋','爱情','女频','甜宠']),
 ('游戏',['游戏','网游','电竞']),('轻小说',['轻小说','同人','二次元'])]
BAD = ('default','nocover','no-cover','placeholder','logo','avatar')

def good_cover(u):
    return isinstance(u,str) and u.startswith(('http://','https://')) and not any(x in u.lower() for x in BAD)

def category(b):
    t=' '.join(str(b.get(k) or '') for k in ('kind','title','intro','status')).lower()
    for name, words in RULES:
        if any(w.lower() in t for w in words): return name
    return '其他'

def score(b):
    n=max(0,int(b.get('chapter_count') or 0)); s=min(n,2500)*.34
    if b.get('detail_complete'): s+=420
    if good_cover(str(b.get('cover') or '')): s+=520
    if re.search(r'完结|完本|已完结',str(b.get('status') or '')): s+=120
    try:
        ts=datetime.fromisoformat(str(b.get('indexed_at') or '').replace('Z','+00:00'))
        age=max(0,(datetime.now(timezone.utc)-ts.astimezone(timezone.utc)).total_seconds()/86400)
        s+=max(0,260-age*3.5)
    except Exception: pass
    return s

def compact(b,cat):
    return {'id':b.get('id'),'title':str(b.get('title') or '').strip(),'author':str(b.get('author') or '未知作者').strip(),
      'category':cat,'status':str(b.get('status') or '').strip(),'intro':str(b.get('intro') or '').strip()[:180],
      'cover':str(b.get('cover') or '').strip(),'chapter_count':int(b.get('chapter_count') or 0),
      'source_name':str(b.get('source_name') or '').strip(),'source_host':str(b.get('source_host') or '').strip(),
      'source_url':str(b.get('source_url') or '').strip(),'_score':score(b)}

def pick(rows,limit,quota=3):
    out=[]; counts=defaultdict(int); titles=set()
    for r in sorted(rows,key=lambda x:x['_score'],reverse=True):
        src=r.get('source_host') or r.get('source_name') or 'unknown'; title=r['title']
        if not title or title in titles or counts[src]>=quota: continue
        out.append({k:v for k,v in r.items() if k!='_score'}); titles.add(title); counts[src]+=1
        if len(out)>=limit: break
    return out

def main():
    idx=json.loads((DATA/'index.json').read_text(encoding='utf-8')); books=[]
    for sh in idx.get('shards',[]):
        p=DATA/sh['file']
        if not p.exists(): continue
        try: payload=json.loads(p.read_text(encoding='utf-8'))
        except Exception: continue
        for b in payload.get('books',[]):
            if not b.get('id') or not str(b.get('title') or '').strip(): continue
            if not b.get('source_name') and not b.get('source_host'): continue
            if not b.get('detail_complete') or not good_cover(str(b.get('cover') or '')): continue
            if int(b.get('chapter_count') or 0)<8: continue
            books.append(compact(b,category(b)))
    by=defaultdict(list)
    for r in books: by[r['category']].append(r)
    mix=[]
    for cat,_ in RULES: mix.extend(sorted(by.get(cat,[]),key=lambda x:x['_score'],reverse=True)[:8])
    mix.extend(sorted(books,key=lambda x:x['_score'],reverse=True)[:40])
    cats={cat:pick(by.get(cat,[]),24,4) for cat,_ in RULES if by.get(cat)}
    cats['完本']=pick([r for r in books if re.search(r'完结|完本|已完结',r.get('status',''))],24,4)
    cats['全部']=pick(books,30,4)
    result={'schema':1,'generated_at':datetime.now(timezone.utc).isoformat(timespec='seconds'),'refresh_policy':'12h_snapshot',
      'source_mode':'external_source_index_snapshot','candidate_count':len(books),'featured':pick(mix,12,2),
      'category_order':['玄幻','都市','仙侠','武侠','历史','科幻','悬疑','言情','游戏','轻小说','完本','全部'],'categories':cats}
    OUT.write_text(json.dumps(result,ensure_ascii=False,indent=2)+'\n',encoding='utf-8')
    print('recommendations:',len(result['featured']),'featured from',len(books),'candidates')
if __name__=='__main__': main()
