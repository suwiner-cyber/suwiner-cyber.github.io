from __future__ import annotations
import hashlib,json,re
from pathlib import Path
from urllib.parse import urlparse,urldefrag

ROOT=Path(__file__).resolve().parents[1]; V5=ROOT/'data'/'v5'; BOOKS=V5/'books'; INDEX=V5/'index.json'; CH=V5/'chapters'; TOC=V5/'toc'; SHARD=200
STABLE_HOSTS={'tadu.com','kanunu8.com','99csw.com','xs8.cn','1qxs.com','zwxiaoshuo.com','ilwxs.com','ipaoshubaxs.net','aaaks.com','69shuba.com','piaotia.com','hetushu.com'}

def load(p,d):
 try:return json.loads(p.read_text(encoding='utf-8'))
 except Exception:return d
def save(p,o):
 p.parent.mkdir(parents=True,exist_ok=True);q=p.with_suffix(p.suffix+'.tmp');q.write_text(json.dumps(o,ensure_ascii=False,indent=2),encoding='utf-8');q.replace(p)
def host(u):return (urlparse(u).hostname or '').lower().removeprefix('www.')
def canon(u):
 u=urldefrag(u or '')[0];p=urlparse(u);net=p.netloc.lower();
 if net.startswith('www.'):net=net[4:]
 path=re.sub(r'/+$','',p.path) or '/'
 return p._replace(netloc=net,path=path,query='',fragment='').geturl()
def stable(u):return 'ext-'+hashlib.sha1(canon(u).encode()).hexdigest()[:20]
def target_id(b):
 u=b.get('source_url','');h=host(u);p=urlparse(u).path
 if h=='ixdzs8.com':
  m=re.search(r'/read/(\d+)',p);return 'ix-'+m.group(1) if m else (b.get('id') or stable(u))
 if h in STABLE_HOSTS:return stable(u)
 return b.get('id') or stable(u)
def merge_docs(a,b):
 out=dict(a); seen={x.get('url') for x in out.get('chapters',[]) if x.get('url')}; arr=list(out.get('chapters',[]))
 for x in b.get('chapters',[]):
  if x.get('url') not in seen:arr.append(x);seen.add(x.get('url'))
 arr.sort(key=lambda x:(int(x.get('n') or 0),x.get('url','')));out['chapters']=arr;out['complete']=bool(a.get('complete') or b.get('complete'));return out
def migrate_dir(folder,mapping):
 if not folder.exists():return 0
 moved=0
 for old,new in mapping.items():
  if old==new:continue
  src=folder/f'{old}.json';dst=folder/f'{new}.json'
  if not src.exists():continue
  doc=load(src,{})
  doc['book_id']=new
  if dst.exists():doc=merge_docs(load(dst,{}),doc)
  save(dst,doc);src.unlink();moved+=1
 return moved
def main():
 idx=load(INDEX,{});rows=[]
 for s in idx.get('shards',[]):rows.extend(load(V5/s['file'],{}).get('books',[]))
 mapping={}
 for b in rows:
  old=b.get('id');new=target_id(b);b['id']=new
  if old and old!=new:mapping[old]=new
 moved=migrate_dir(CH,mapping)+migrate_dir(TOC,mapping)
 keep=set();sh=[]
 for i in range(0,len(rows),SHARD):
  n=f'{i//SHARD+1:05d}.json';keep.add(n);chunk=rows[i:i+SHARD];save(BOOKS/n,{'books':chunk});sh.append({'file':'books/'+n,'count':len(chunk)})
 for p in BOOKS.glob('*.json'):
  if p.name not in keep:p.unlink()
 idx.update({'shards':sh,'total':len(rows),'shard_size':SHARD,'id_scheme':'stable-v3'});save(INDEX,idx)
 print(json.dumps({'books':len(rows),'ids_changed':len(mapping),'files_migrated':moved},ensure_ascii=False))
if __name__=='__main__':main()
