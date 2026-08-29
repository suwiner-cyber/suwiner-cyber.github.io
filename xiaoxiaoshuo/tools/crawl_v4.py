from __future__ import annotations
import json,re,time
from collections import Counter
from datetime import datetime,timezone
from pathlib import Path
from urllib.error import HTTPError,URLError
from urllib.parse import urljoin,urlparse
from urllib.request import Request,urlopen
from urllib.robotparser import RobotFileParser
from bs4 import BeautifulSoup

ROOT=Path(__file__).resolve().parents[1]; DATA=ROOT/'data'/'v4'; BOOKS=DATA/'books'
CFG=ROOT/'config'/'sources.json'; STATE=DATA/'state.json'; INDEX=DATA/'index.json'; STATUS=DATA/'status.json'; ERR=DATA/'errors.json'
HEAD={'User-Agent':'Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 Chrome/139 Safari/537.36','Accept':'text/html,application/xhtml+xml','Accept-Language':'zh-CN,zh;q=0.9','X-XiaoXiaoShuo-Crawler':'authorized-indexer/4.0'}
IX=re.compile(r'^/read/(\d+)/?$'); IT=re.compile(r'^/youshengxiaoshuo/(\d+)/?$')
CATS=[(1,'玄幻奇幻'),(10,'传统武侠'),(2,'修真仙侠'),(3,'都市青春'),(4,'军事历史'),(5,'网游竞技'),(6,'科幻灵异'),(7,'言情穿越'),(8,'耽美同人'),(9,'台言古言'),(0,'其他')]
ROBOTS={}; SHARD=200

def now(): return datetime.now(timezone.utc).isoformat(timespec='seconds')
def clean(s): return re.sub(r'\s+',' ',s or '').strip()
def load(p,d):
 try:return json.loads(p.read_text(encoding='utf-8'))
 except Exception:return d
def save(p,o):
 p.parent.mkdir(parents=True,exist_ok=True); q=p.with_suffix(p.suffix+'.tmp'); q.write_text(json.dumps(o,ensure_ascii=False,indent=2),encoding='utf-8'); q.replace(p)
def host(u): return (urlparse(u).hostname or '').lower()
def fetch(u,ref=None):
 last=None
 for n in range(3):
  h=dict(HEAD); h.update({'Referer':ref} if ref else {})
  try:
   with urlopen(Request(u,headers=h),timeout=30) as r: raw=r.read(); ct=r.headers.get('Content-Type','')
   m=re.search(r'charset=([\w.-]+)',ct,re.I)
   for e in ([m.group(1)] if m else [])+['utf-8','gb18030','gbk']:
    try:return raw.decode(e)
    except Exception:pass
   return raw.decode('utf-8','replace')
  except HTTPError as e:
   last=e
   if e.code not in (408,425,429,500,502,503,504):break
  except (URLError,TimeoutError,OSError) as e:last=e
  time.sleep(min(2**n,5))
 raise RuntimeError(f'fetch failed: {u}: {last}')
def robots(base,u):
 h=host(base)
 if h not in ROBOTS:
  p=urlparse(base); rp=RobotFileParser(); rp.set_url(f'{p.scheme}://{p.netloc}/robots.txt')
  try:rp.read();ROBOTS[h]=rp
  except Exception:ROBOTS[h]=None
 return True if ROBOTS[h] is None else ROBOTS[h].can_fetch('*',u)
def img(li,base):
 n=li.find('img') if li else None; s=(n.get('data-src') or n.get('src')) if n else ''
 return urljoin(base,s) if s else ''
def container(a): return a.find_parent('li') or a.find_parent('article') or a.find_parent('dd') or a.parent
def other_author(li,title_url,title):
 if not li:return ''
 for a in li.find_all('a',href=True):
  t=clean(a.get_text(' ',strip=True)); u=urljoin(title_url,a['href']); p=urlparse(u).path
  if not t or t==title or u==title_url or IX.match(p) or IT.match(p) or '/play/' in p or p.endswith('.html'):continue
  if len(t)<=80 and not re.fullmatch(r'[\d.]+(?:万)?',t):return t
 return ''
def intro(li,title,author):
 if not li:return ''
 ps=[clean(x.get_text(' ',strip=True)) for x in li.find_all('p')]; ps=[x for x in ps if len(x)>=25 and title not in x[:50]]
 if ps:return max(ps,key=len)[:700]
 raw=clean(li.get_text(' ',strip=True)).replace(title,' ').replace(author,' '); raw=re.sub(r'最新[:：]?.*$','',raw); return clean(raw)[:700]

def parse_ix(text,url,cat,kind):
 s=BeautifulSoup(text,'html.parser'); g={}
 for a in s.find_all('a',href=True):
  u=urljoin(url,a['href']).split('#')[0]
  if host(u)==host(url) and IX.match(urlparse(u).path):g.setdefault(u,[]).append(a)
 out=[]
 for u,aa in g.items():
  labs=[clean(a.get_text(' ',strip=True)) for a in aa]; labs=[x for x in labs if x and not re.fullmatch(r'[\d.]+(?:万)?',x)]
  if not labs:continue
  title=max(labs,key=lambda x:(not x.startswith('第'),len(x))); a=next((x for x in aa if clean(x.get_text(' ',strip=True))==title),aa[0]); li=container(a); raw=clean(li.get_text(' ',strip=True)); au=other_author(li,u,title)
  cm=re.search(r'第\s*(\d+)\s*章',raw); lm=re.search(r'最新[:：]?\s*(第.+?章)',raw); wm=re.search(r'(\d+(?:\.\d+)?)万字',raw)
  out.append({'id':'ix-'+IX.match(urlparse(u).path).group(1),'title':title,'author':au,'kind':kind,'status':'已完结' if '已完结' in raw else ('连载中' if '连载中' in raw else ''),'intro':intro(li,title,au),'cover':img(li,url),'source_url':u,'source_host':host(u),'source_name':'爱下电子书','media':'text','chapter_count':int(cm.group(1)) if cm else 0,'last_chapter':clean(lm.group(1)) if lm else '','word_count_wan':float(wm.group(1)) if wm else None,'detail_complete':False,'indexed_at':now()})
 mx=1
 for a in s.find_all('a',href=True):
  m=re.search(r'index-\d+-\d+-\d+-(\d+)\.html$',urlparse(urljoin(url,a['href'])).path)
  if m:mx=max(mx,int(m.group(1)))
 return out,mx

def parse_it(text,url):
 s=BeautifulSoup(text,'html.parser'); g={}
 for a in s.find_all('a',href=True):
  u=urljoin(url,a['href']).split('#')[0]
  if host(u)==host(url) and IT.match(urlparse(u).path):g.setdefault(u,[]).append(a)
 out=[]
 for u,aa in g.items():
  labs=[clean(a.get_text(' ',strip=True)) for a in aa]; labs=[x for x in labs if x and not re.fullmatch(r'[\d.]+(?:万)?',x) and not re.match(r'^(?:连载至)?第?\d+集$',x)]
  if not labs:continue
  title=max(labs,key=len); a=next((x for x in aa if clean(x.get_text(' ',strip=True))==title),aa[0]); li=container(a); raw=clean(li.get_text(' ',strip=True))
  am=re.search(r'作者[:：]\s*([^\s]+)',raw); nm=re.search(r'演播[:：]\s*(.+?)(?:\s+作者[:：]|\s+连载|$)',raw); cm=re.search(r'第\s*(\d+)\s*集',raw); au=clean(am.group(1)) if am else other_author(li,u,title)
  out.append({'id':'it-'+IT.match(urlparse(u).path).group(1),'title':title,'author':au,'narrator':clean(nm.group(1))[:180] if nm else '','kind':'有声小说','status':'已完结' if ('已完本' in raw or '完结' in raw) else ('连载中' if '连载' in raw else ''),'intro':intro(li,title,au),'cover':img(li,url),'source_url':u,'source_host':host(u),'source_name':'爱听书','media':'audio-metadata','chapter_count':int(cm.group(1)) if cm else 0,'last_chapter':('第'+cm.group(1)+'集') if cm else '','detail_complete':False,'indexed_at':now()})
 m=re.search(r'共\s*(\d+)\s*页',clean(s.get_text(' ',strip=True))); return out,int(m.group(1)) if m else 3363

def existing():
 rows=[]
 for p in sorted(BOOKS.glob('*.json')):
  x=load(p,{}).get('books',[]); rows.extend(v for v in x if isinstance(v,dict) and v.get('source_url'))
 return rows
def write(rows,reports):
 BOOKS.mkdir(parents=True,exist_ok=True); keep=set(); shards=[]
 for i in range(0,len(rows),SHARD):
  name=f'{i//SHARD+1:05d}.json'; keep.add(name); chunk=rows[i:i+SHARD]; save(BOOKS/name,{'books':chunk}); shards.append({'file':'books/'+name,'count':len(chunk)})
 for p in BOOKS.glob('*.json'):
  if p.name not in keep:p.unlink()
 counts=Counter(x.get('source_name','未知') for x in rows); save(INDEX,{'version':4,'generated_at':now(),'total':len(rows),'shard_size':SHARD,'shards':shards,'source_counts':dict(counts),'sources':reports})
def error(log,name,url,e):
 log.append({'time':now(),'source':name,'url':url,'error':str(e)[:500]}); del log[:-300]

def main():
 DATA.mkdir(parents=True,exist_ok=True); BOOKS.mkdir(parents=True,exist_ok=True); cfg=load(CFG,{'sources':[]}); st=load(STATE,{'version':4,'sources':{}})
 if st.get('version')!=4:st={'version':4,'sources':{}}
 ss=st['sources']; elog=load(ERR,{'errors':[]}).get('errors',[]); rows=existing(); by={x['source_url']:x for x in rows}; bootstrap=not rows; reports=[]
 for src in cfg.get('sources',[]):
  if src.get('authorized') is not True:continue
  h=host(src.get('url','')); configured=max(1,int(src.get('catalog_pages_per_run',60))); limit=min(configured,12) if bootstrap else min(max(configured,60),120); delay=max(.35,float(src.get('delay_seconds',.8))); ok=seen=added=attempt=0
  if h.endswith('ixdzs8.com'):
   cur=ss.setdefault('ixdzs8.com',{'category_index':0,'page':1})
   while attempt<limit and not cur.get('done'):
    ci=int(cur.get('category_index',0)); page=int(cur.get('page',1))
    if ci>=len(CATS):cur['done']=True;break
    cat,kind=CATS[ci]; u=f'https://ixdzs8.com/sort/{cat}/' if page==1 else f'https://ixdzs8.com/sort/{cat}/index-0-0-0-{page}.html'; attempt+=1
    try:
     if not robots(src['url'],u):raise RuntimeError('robots denied')
     items,mx=parse_ix(fetch(u,src['url']),u,cat,kind)
    except Exception as e:error(elog,src.get('name','爱下电子书'),u,e);break
    if not items:cur.update({'category_index':ci+1,'page':1});continue
    ok+=1;seen+=len(items)
    for x in items:
     if x['source_url'] not in by:by[x['source_url']]=x;rows.append(x);added+=1
    cur.update({'category_index':ci+1,'page':1} if page>=mx else {'page':page+1});cur['updated_at']=now();time.sleep(delay)
   reports.append({'name':src.get('name','爱下电子书'),'pages':f'{ok}/{attempt}','discovered':seen,'added':added,'cursor':dict(cur)})
  elif h.endswith('itingshu.net'):
   cur=ss.setdefault('www.itingshu.net',{'page':1,'total_pages':3363})
   while attempt<limit and not cur.get('done'):
    page=int(cur.get('page',1)); total=int(cur.get('total_pages',3363))
    if page>total:cur['done']=True;break
    u='https://www.itingshu.net/yousheng/all.html' if page==1 else f'https://www.itingshu.net/yousheng/all/lastupdate/1/{page}.html';attempt+=1
    try:
     if not robots(src['url'],u):raise RuntimeError('robots denied')
     items,total=parse_it(fetch(u,src['url']),u)
    except Exception as e:error(elog,src.get('name','爱听书'),u,e);break
    if not items:error(elog,src.get('name','爱听书'),u,'no book links');break
    ok+=1;seen+=len(items)
    for x in items:
     if x['source_url'] not in by:by[x['source_url']]=x;rows.append(x);added+=1
    cur.update({'page':page+1,'total_pages':total,'updated_at':now()});time.sleep(delay)
   reports.append({'name':src.get('name','爱听书'),'pages':f'{ok}/{attempt}','discovered':seen,'added':added,'cursor':dict(cur)})
 write(rows,reports);st['updated_at']=now();save(STATE,st);save(ERR,{'generated_at':now(),'errors':elog});save(STATUS,{'version':4,'generated_at':now(),'total':len(rows),'bootstrap':bootstrap,'sources':reports,'error_count':len(elog)});print(json.dumps({'version':4,'total':len(rows),'bootstrap':bootstrap,'sources':reports},ensure_ascii=False))
if __name__=='__main__':main()
