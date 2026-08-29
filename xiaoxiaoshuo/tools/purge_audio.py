from __future__ import annotations
import json,re
from collections import Counter
from pathlib import Path
from urllib.parse import urlparse

ROOT=Path(__file__).resolve().parents[1]
AUDIO_HOSTS={'itingshu.net'}
AUDIO_WORDS=('爱听书','听书网','有声小说','有声书','音频小说','audio-metadata','audiobook')

def load(p,d):
    try:return json.loads(p.read_text(encoding='utf-8'))
    except Exception:return d

def save(p,o):
    p.parent.mkdir(parents=True,exist_ok=True)
    q=p.with_suffix(p.suffix+'.tmp')
    q.write_text(json.dumps(o,ensure_ascii=False,indent=2),encoding='utf-8')
    q.replace(p)

def host(u):return (urlparse(u or '').hostname or '').lower().removeprefix('www.')

def is_audio_book(b):
    if not isinstance(b,dict):return False
    if host(b.get('source_url','')) in AUDIO_HOSTS:return True
    if host(b.get('source_host','')) in AUDIO_HOSTS:return True
    media=str(b.get('media','')).lower()
    if media and media!='text':return True
    text=' '.join(str(b.get(k,'') or '') for k in ('source_name','kind','title','media')).lower()
    return any(w.lower() in text for w in AUDIO_WORDS)

def purge_version(version):
    base=ROOT/'data'/version; books=base/'books'; idxp=base/'index.json'
    if not idxp.exists():return {'version':version,'removed':0,'kept':0}
    idx=load(idxp,{}); rows=[]
    for s in idx.get('shards',[]):rows.extend(load(base/s.get('file',''),{}).get('books',[]))
    removed=[b for b in rows if is_audio_book(b)]
    kept=[b for b in rows if not is_audio_book(b)]
    removed_ids={str(b.get('id')) for b in removed if b.get('id')}
    shard_size=int(idx.get('shard_size') or 200); keep_files=set(); shards=[]
    books.mkdir(parents=True,exist_ok=True)
    for i in range(0,len(kept),shard_size):
        name=f'{i//shard_size+1:05d}.json'; keep_files.add(name); chunk=kept[i:i+shard_size]
        save(books/name,{'books':chunk}); shards.append({'file':'books/'+name,'count':len(chunk)})
    for p in books.glob('*.json'):
        if p.name not in keep_files:p.unlink()
    counts=Counter(str(b.get('source_name') or '未知') for b in kept)
    idx['total']=len(kept);idx['shards']=shards;idx['source_counts']=dict(counts);idx['text_only']=True
    save(idxp,idx)
    for folder_name in ('chapters','toc'):
        folder=base/folder_name
        if folder.exists():
            for bid in removed_ids:
                p=folder/f'{bid}.json'
                if p.exists():p.unlink()
            for p in folder.glob('it-*.json'):p.unlink()
    return {'version':version,'removed':len(removed),'kept':len(kept)}

def purge_state():
    p=ROOT/'data'/'v4'/'state.json'
    if p.exists():
        d=load(p,{})
        if isinstance(d.get('sources'),dict):
            for k in list(d['sources']):
                if host('https://'+k) in AUDIO_HOSTS:d['sources'].pop(k,None)
        save(p,d)
    for p in (ROOT/'data'/'v5').glob('*state.json'):
        d=load(p,{})
        changed=False
        def walk(x):
            nonlocal changed
            if isinstance(x,dict):
                for k in list(x):
                    v=x[k]
                    if str(k).startswith('it-') or 'itingshu' in str(k).lower():x.pop(k,None);changed=True
                    else:walk(v)
            elif isinstance(x,list):
                old=len(x);x[:]=[v for v in x if 'itingshu' not in str(v).lower() and 'it-' not in str(v).lower()];changed|=(len(x)!=old)
                for v in x:walk(v)
        walk(d)
        if changed:save(p,d)

def main():
    reports=[purge_version('v4'),purge_version('v5')]
    purge_state()
    print(json.dumps({'policy':'text-only-no-audio','reports':reports},ensure_ascii=False))
if __name__=='__main__':main()
