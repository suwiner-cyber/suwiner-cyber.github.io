from __future__ import annotations
import json,re,time,unicodedata
from datetime import datetime,timezone
from pathlib import Path
from urllib.parse import urljoin,urlparse
from urllib.request import Request,urlopen
from urllib.error import HTTPError,URLError
from bs4 import BeautifulSoup

ROOT=Path(__file__).resolve().parents[1]; CFG=ROOT/'config'/'sources.json'; V5=ROOT/'data'/'v5'; BOOKS=V5/'books'; INDEX=V5/'index.json'; STATE=V5/'extra_state.json'; ERR=V5/'extra_errors.json'; SHARD=200
HEAD={'User-Agent':'Mozilla/5.0 AppleWebKit/537.36 Chrome/139 Safari/537.36','Accept-Language':'zh-CN,zh;q=0.9','X-XiaoXiaoShuo-Crawler':'authorized-extra/1.0'}
PROFILES={
 'www.tadu.com':{'name':'塔读文学','seeds':['https://www.tadu.com/book/rank/list/0-potential-0-0-1'],'book':[r'^/book/\d+/?$']},
 'www.kanunu8.com':{'name':'努努书坊','seeds':['https://www.kanunu8.com/','https://www.kanunu8.com/zt/'],'book':[r'^/(?:book\d+|files|\d+)/.+\.html$',r'^/\d+/[A-Za-z0-9_-]+/?$']},
 'www.99csw.com':{'name':'九九藏书网','seeds':['https://www.99csw.com/book/'],'book':[r'^/book/\d+/?$',r'^/book/\d+/index\.htm$']},
 'www.xs8.cn':{'name':'言情小说吧','seeds':['https://www.xs8.cn/'],'book':[r'^/(?:book|info)/[^?#]+$']},
 'www.1qxs.com':{'name':'一七小说','seeds':['https://www.1qxs.com/'],'book':[r'^/xs_\d+/\d+\.html$',r'^/(?:book|novel|info)/[^?#]+$']},
 'www.zwxiaoshuo.com':{'name':'滋味小说网','seeds':['https://www.zwxiaoshuo.com/','https://www.zwxiaoshuo.com/topweekvisit-1.html'],'book':[r'^/book-\d+.*\.html$',r'^/(?:book|xs|novel)-?\d+.*\.html$']}
}

def now():return datetime.now(timezone.utc).isoformat(timespec='seconds')
def load(p,d):
 try:return json.loads(p.read_text(encoding='utf-8'))
 except Exception:return d
def save(p,o):p.parent.mkdir(parents=True,exist_ok=True);p.write_text(json.dumps(o,ensure_ascii=False,indent=2),encoding='utf-8')
def clean(s):return re.sub(r'\s+',' ',s or '').strip()
def host(u):return (urlparse(u).hostname or '').lower()
def han(s):return bool(re.search(r'[\u3400-\u9fff]',s or ''))
def norm(s):
 s=unicodedata.normalize('NFKC',s or '').lower();return re.sub(r'[^0-9a-z\u3400-\u9fff]+','',s)
def dkey(b):
 t=norm(b.get('title',''));a=norm(re.sub(r'[_-](?:爱下电子书|九九藏书网|努努书坊|塔读文学|言情小说吧|滋味小说网|一七小说)$','',b.get('author','')))
 return t+'|'+a if t and a else t+'|'+host(b.get('source_url',''))
def fetch(u,ref=None):
 last=None
 for n in range(3):
  try:
   h=dict(HEAD); h.update({'Referer':ref} if ref else {})
   with urlopen(Request(u,headers=h),timeout=25) as r:raw=r.read();ct=r.headers.get('Content-Type','')
   m=re.search(r'charset=([\w.-]+)',ct,re.I)
   for e in ([m.group(1)] if m else [])+['utf-8','gb18030','gbk']:
    try:return raw.decode(e)
    except Exception:pass
   return raw.decode('utf-8','replace')
  except (HTTPError,URLError,TimeoutError,OSError) as e:last=e;time.sleep(min(2**n,4))
 raise RuntimeError(str(last))
def existing():
 out=[];idx=load(INDEX,{})
 for s in idx.get('shards',[]):out.extend(load(V5/s['file'],{}).get('books',[]))
 return out
def parent(a):
 for x in a.parents:
  if getattr(x,'name','') in ('li','article','tr','dd','div','section'):
   t=clean(x.get_text(' ',strip=True))
   if 8<=len(t)<=2200:return x
 return a.parent
def meta(a,u,name):
 box=parent(a);title=clean(a.get_text(' ',strip=True));raw=clean(box.get_text(' ',strip=True)) if box else title
 am=re.search(r'作者\s*[:：]?\s*([^\s|·]{1,50})',raw);km=re.search(r'(?:分类|类型)\s*[:：]?\s*([^\s|·]{1,30})',raw);cm=re.search(r'第\s*(\d+)\s*[章节]',raw)
 im=box.find('img') if box else None;cover=urljoin(u,(im.get('data-src') or im.get('src'))) if im and (im.get('data-src') or im.get('src')) else ''
 ps=[clean(x.get_text(' ',strip=True)) for x in box.find_all('p')] if box else [];desc=max(ps,key=len)[:700] if ps else ''
 return {'id':'ext-'+str(abs(hash(u))),'title':title,'author':clean(am.group(1)) if am else '','kind':clean(km.group(1)) if km else '','status':'已完结' if re.search(r'完结|完本|已完成',raw) else ('连载中' if '连载' in raw else ''),'intro':desc,'cover':cover,'source_url':u,'source_host':host(u),'source_name':name,'media':'text','chapter_count':int(cm.group(1)) if cm else 0,'last_chapter':'','detail_complete':False,'indexed_at':now(),'catalog_only':False}
def is_book(path,profile):return any(re.match(p,path,re.I) for p in profile['book'])
def interesting(x,h):
 p=urlparse(x);path=p.path.lower()
 if host(x)!=h:return False
 if re.search(r'/(?:login|user|account|pay|vip|help|download|app|author)(?:/|$)',path):return False
 return any(k in path for k in ('book','novel','xs_','sort','list','rank','top','class','category','zt','files')) or path in ('/','')
def rewrite(rows):
 d={}
 for b in rows:
  if not han(b.get('title','')):continue
  k=dkey(b)
  if k not in d:d[k]=b
  else:
   old=d[k];src=list(dict.fromkeys((old.get('sources') or [old.get('source_url')])+(b.get('sources') or [b.get('source_url')])));old['sources']=[x for x in src if x]
   for f in ('author','kind','status','intro','cover','chapter_count','last_chapter'):
    if not old.get(f) and b.get(f):old[f]=b[f]
 rows=list(d.values());BOOKS.mkdir(parents=True,exist_ok=True);keep=set();sh=[]
 for i in range(0,len(rows),SHARD):
  n=f'{i//SHARD+1:05d}.json';keep.add(n);chunk=rows[i:i+SHARD];save(BOOKS/n,{'books':chunk});sh.append({'file':'books/'+n,'count':len(chunk)})
 for p in BOOKS.glob('*.json'):
  if p.name not in keep:p.unlink()
 counts={}
 for b in rows:counts[b.get('source_name','未知')]=counts.get(b.get('source_name','未知'),0)+1
 old=load(INDEX,{});old.update({'version':5,'generated_at':now(),'total':len(rows),'shard_size':SHARD,'shards':sh,'source_counts':counts});save(INDEX,old)
def main():
 cfg=load(CFG,{'sources':[]});st=load(STATE,{'sources':{}});errs=load(ERR,{'errors':[]}).get('errors',[]);rows=existing();known={b.get('source_url') for b in rows};reports=[]
 for s in cfg.get('sources',[]):
  h=host(s.get('url',''));p=PROFILES.get(h)
  if not p or s.get('authorized') is not True or s.get('catalog_allowed') is not True:continue
  ss=st['sources'].setdefault(h,{'queue':list(p['seeds']),'seen':[],'done':False});seen=set(ss.get('seen',[]));q=ss.setdefault('queue',[]);limit=max(10,min(int(s.get('catalog_pages_per_run',60)),120));pages=found=added=0
  if ss.get('done') and q:ss['done']=False
  while q and pages<limit:
   u=q.pop(0)
   if u in seen:continue
   seen.add(u);pages+=1
   try:soup=BeautifulSoup(fetch(u,s.get('url')),'html.parser')
   except Exception as e:errs.append({'time':now(),'source':p['name'],'url':u,'error':str(e)[:300]});continue
   for a in soup.find_all('a',href=True):
    x=urljoin(u,a['href']).split('#')[0];path=urlparse(x).path
    if host(x)!=h:continue
    title=clean(a.get_text(' ',strip=True))
    if is_book(path,p) and han(title) and 1<len(title)<=100:
     found+=1
     if x not in known:rows.append(meta(a,x,p['name']));known.add(x);added+=1
    elif interesting(x,h) and x not in seen and x not in q and len(q)<5000:q.append(x)
   time.sleep(max(.5,float(s.get('delay_seconds',1))))
  ss['seen']=list(seen)[-20000:];ss['done']=not bool(q);ss['updated_at']=now();reports.append({'name':p['name'],'pages':pages,'found':found,'added':added,'queue':len(q),'done':ss['done']})
 rewrite(rows);save(STATE,st);save(ERR,{'generated_at':now(),'errors':errs[-500:]});print(json.dumps({'total':len(rows),'sources':reports},ensure_ascii=False))
if __name__=='__main__':main()
