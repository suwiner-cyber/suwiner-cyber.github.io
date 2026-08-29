from __future__ import annotations
import json,re,time,unicodedata
from datetime import datetime,timezone
from pathlib import Path
from urllib.parse import urljoin,urlparse
from urllib.request import Request,urlopen
from urllib.error import HTTPError,URLError
from bs4 import BeautifulSoup
import crawl_v4

ROOT=Path(__file__).resolve().parents[1]; CFG=ROOT/'config'/'sources.json'; V4=ROOT/'data'/'v4'; V5=ROOT/'data'/'v5'; BOOKS=V5/'books'; STATE=V5/'state.json'; INDEX=V5/'index.json'; ERR=V5/'errors.json'
HEAD={'User-Agent':'Mozilla/5.0 AppleWebKit/537.36 Chrome/139 Safari/537.36','Accept-Language':'zh-CN,zh;q=0.9','X-XiaoXiaoShuo-Crawler':'catalog-v5'}; SHARD=200
PROFILES={
 'tadu.com':{'name':'塔读文学','kind':'formula','seed':'https://www.tadu.com/book/rank/list/0-potential-0-0-{page}','book':r'^/book/\d+/?$'},
 'www.99csw.com':{'name':'九九藏书网','kind':'follow','seed':'https://www.99csw.com/book/','book':r'^/book/\d+/?$'},
 'www.kanunu8.com':{'name':'努努书坊','kind':'follow','seed':'https://www.kanunu8.com/zt/','book':r'^/\d+/[A-Za-z0-9_-]+/?$'},
 'www.xs8.cn':{'name':'言情小说吧','kind':'follow','seed':'https://www.xs8.cn/','book':r'^/(?:book|chapter|info)/[^?#]+$'},
 'www.1qxs.com':{'name':'一起小说','kind':'follow','seed':'https://www.1qxs.com/','book':r'^/(?:book|novel|info)/[^?#]+$'},
 'www.zwxiaoshuo.com':{'name':'滋味小说网','kind':'formula','seed':'https://www.zwxiaoshuo.com/topweekvisit-{page}.html','book':r'^/(?:book|xs|novel)-?\d+.*\.html$'}
}

def now():return datetime.now(timezone.utc).isoformat(timespec='seconds')
def load(p,d):
 try:return json.loads(p.read_text(encoding='utf-8'))
 except Exception:return d
def save(p,o):
 p.parent.mkdir(parents=True,exist_ok=True);q=p.with_suffix(p.suffix+'.tmp');q.write_text(json.dumps(o,ensure_ascii=False,indent=2),encoding='utf-8');q.replace(p)
def host(u):return (urlparse(u).hostname or '').lower()
def clean(s):return re.sub(r'\s+',' ',s or '').strip()
def han(s):return len(re.findall(r'[\u3400-\u9fff]',s or ''))>=1
def norm(s):
 s=unicodedata.normalize('NFKC',s or '').lower();s=re.sub(r'[_\-—·•|｜（）()\[\]【】《》〈〉“”"\'：:，,。.!！?？\s]+','',s);return s
def author_norm(a):return norm(re.sub(r'_(?:爱下电子书|九九藏书网|努努书坊|塔读文学|言情小说吧|滋味小说网)$','',a or ''))
def key(b):
 t=norm(b.get('title',''));a=author_norm(b.get('author',''));return (t+'|'+a) if t and a else (t+'|'+host(b.get('source_url','')) if t else b.get('source_url',''))
def fetch(u,ref=None):
 last=None
 for n in range(3):
  try:
   h=dict(HEAD);h.update({'Referer':ref} if ref else {});r=urlopen(Request(u,headers=h),timeout=25);raw=r.read();ct=r.headers.get('Content-Type','');r.close();m=re.search(r'charset=([\w.-]+)',ct,re.I)
   for e in ([m.group(1)] if m else [])+['utf-8','gb18030','gbk']:
    try:return raw.decode(e)
    except Exception:pass
   return raw.decode('utf-8','replace')
  except (HTTPError,URLError,TimeoutError,OSError) as e:last=e;time.sleep(min(2**n,4))
 raise RuntimeError(str(last))
def parent(a):
 for x in a.parents:
  if getattr(x,'name','') in ('li','article','tr','dd','div'):
   t=clean(x.get_text(' ',strip=True))
   if 10<=len(t)<=2500:return x
 return a.parent
def meta_from_anchor(a,u,name):
 box=parent(a);title=clean(a.get_text(' ',strip=True));raw=clean(box.get_text(' ',strip=True)) if box else title
 am=re.search(r'作者\s*[:：]?\s*([^\s·|]{1,40})',raw);author=clean(am.group(1)) if am else ''
 km=re.search(r'(?:分类|类型)\s*[:：]?\s*([^\s·|]{1,30})',raw);kind=clean(km.group(1)) if km else ''
 status='已完结' if ('已完结' in raw or '已完成' in raw or '完本' in raw) else ('连载中' if '连载' in raw else '')
 cm=re.search(r'第\s*(\d+)\s*[章节集]',raw);img=box.find('img') if box else None;cover=urljoin(u,(img.get('data-src') or img.get('src'))) if img and (img.get('data-src') or img.get('src')) else ''
 desc='';ps=[clean(x.get_text(' ',strip=True)) for x in box.find_all('p')] if box else []
 if ps:desc=max(ps,key=len)[:700]
 return {'id':'ext-'+str(abs(hash(u))),'title':title,'author':author,'kind':kind,'status':status,'intro':desc,'cover':cover,'source_url':u,'source_host':host(u),'source_name':name,'media':'text','chapter_count':int(cm.group(1)) if cm else 0,'last_chapter':'','detail_complete':False,'indexed_at':now(),'catalog_only':True}
def v4_rows():
 out=[]
 for p in sorted((V4/'books').glob('*.json')):out.extend(load(p,{}).get('books',[]))
 return out
def merge(a,b):
 score=lambda x:sum(bool(x.get(k)) for k in ('author','kind','status','intro','cover','chapter_count','last_chapter'))
 best=b if score(b)>score(a) else a;other=a if best is b else b;z=dict(other);z.update({k:v for k,v in best.items() if v not in ('',None,0,False)});src=list(dict.fromkeys((a.get('sources') or [a.get('source_url')])+(b.get('sources') or [b.get('source_url')])));z['sources']=[x for x in src if x];return z
def crawl_external(cfg,state,errors):
 rows=[];reports=[]
 for s in cfg.get('sources',[]):
  h=host(s.get('url',''))
  if not s.get('catalog_allowed') or h not in PROFILES or s.get('authorized') is True:continue
  p=PROFILES[h];st=state.setdefault(h,{'page':1,'queue':[p['seed']] if p['kind']=='follow' else [],'seen':[],'done':False});limit=max(1,min(int(s.get('catalog_pages_per_run',20)),60));seen=set(st.get('seen',[]));found=pages=0
  while pages<limit and not st.get('done'):
   if p['kind']=='formula':u=p['seed'].format(page=int(st.get('page',1)));st['page']=int(st.get('page',1))+1
   else:
    q=st.setdefault('queue',[])
    if not q:st['done']=True;break
    u=q.pop(0)
   if u in seen:continue
   seen.add(u);pages+=1
   try:text=fetch(u,s.get('url'));soup=BeautifulSoup(text,'html.parser')
   except Exception as e:errors.append({'time':now(),'source':p['name'],'url':u,'error':str(e)[:300]});continue
   page_books=0
   for a in soup.find_all('a',href=True):
    x=urljoin(u,a['href']).split('#')[0]
    if host(x)!=h:continue
    path=urlparse(x).path
    if re.match(p['book'],path,re.I):
     title=clean(a.get_text(' ',strip=True))
     if han(title) and len(title)<=100:rows.append(meta_from_anchor(a,x,p['name']));found+=1;page_books+=1
    elif p['kind']=='follow' and len(st.get('queue',[]))<500 and (('page' in x.lower()) or path.startswith('/book/') or path.startswith('/files/') or path.startswith('/zt/')) and x not in seen:st['queue'].append(x)
   if p['kind']=='formula' and page_books==0 and int(st.get('page',1))>3:st['done']=True
   time.sleep(max(.5,float(s.get('delay_seconds',1))))
  st['seen']=list(seen)[-5000:];st['updated_at']=now();reports.append({'name':p['name'],'pages':pages,'discovered':found,'done':bool(st.get('done'))})
 return rows,reports
def write(rows,reports):
 BOOKS.mkdir(parents=True,exist_ok=True);keep=set();sh=[]
 for i in range(0,len(rows),SHARD):
  n=f'{i//SHARD+1:05d}.json';keep.add(n);chunk=rows[i:i+SHARD];save(BOOKS/n,{'books':chunk});sh.append({'file':'books/'+n,'count':len(chunk)})
 for p in BOOKS.glob('*.json'):
  if p.name not in keep:p.unlink()
 counts={}
 for b in rows:counts[b.get('source_name','未知')]=counts.get(b.get('source_name','未知'),0)+1
 save(INDEX,{'version':5,'generated_at':now(),'total':len(rows),'shard_size':SHARD,'shards':sh,'source_counts':counts,'sources':reports})
def main():
 crawl_v4.main();V5.mkdir(parents=True,exist_ok=True);cfg=load(CFG,{'sources':[]});state=load(STATE,{'sources':{}});errors=load(ERR,{'errors':[]}).get('errors',[])
 rows=[b for b in v4_rows() if han(b.get('title',''))];ext,reports=crawl_external(cfg,state.setdefault('sources',{}),errors);rows+=ext
 d={};removed_nonzh=removed_dup=0
 for b in rows:
  if not han(b.get('title','')):removed_nonzh+=1;continue
  k=key(b)
  if k in d:d[k]=merge(d[k],b);removed_dup+=1
  else:d[k]=b
 out=sorted(d.values(),key=lambda x:x.get('indexed_at',''),reverse=True);write(out,reports);save(STATE,state);save(ERR,{'generated_at':now(),'errors':errors[-300:]});save(V5/'status.json',{'generated_at':now(),'total':len(out),'removed_non_chinese':removed_nonzh,'removed_duplicates':removed_dup,'sources':reports});print(json.dumps({'version':5,'total':len(out),'removed_duplicates':removed_dup,'sources':reports},ensure_ascii=False))
if __name__=='__main__':main()
