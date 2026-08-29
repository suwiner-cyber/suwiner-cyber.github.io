from __future__ import annotations
import json,re,time
from datetime import datetime,timezone
from pathlib import Path
from urllib.parse import urljoin,urlparse
from urllib.request import Request,urlopen
from urllib.error import HTTPError,URLError
from bs4 import BeautifulSoup

ROOT=Path(__file__).resolve().parents[1];V5=ROOT/'data'/'v5';CFG=ROOT/'config'/'sources.json';OUT=V5/'chapters';TOC=V5/'toc';STATE=V5/'extra_mirror_state.json';ERR=V5/'extra_mirror_errors.json'
HEAD={'User-Agent':'Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 Chrome/139 Safari/537.36','Accept-Language':'zh-CN,zh;q=0.9','X-XiaoXiaoShuo-Crawler':'authorized-reader-extra/2.0'}
PROFILES={
 'tadu.com':{'chapter':[r'/book/\d+/\d+',r'/chapter/[^?#]+']},
 'kanunu8.com':{'chapter':[r'/[^/]+/\d+\.html$',r'/book\d+/[^/]+/\d+\.html$',r'/files/[^/]+/\d+\.html$']},
 '99csw.com':{'chapter':[r'^/book/\d+/\d+\.(?:htm|html)$']},
 'xs8.cn':{'chapter':[r'/(?:chapter|read)/[^?#]+']},
 '1qxs.com':{'chapter':[r'/xs_\d+/\d+/\d+\.html$',r'/xs_\d+/\d+\.html$',r'/chapter/[^?#]+']},
 'zwxiaoshuo.com':{'chapter':[r'/(?:chapter|read)-?\d+.*\.html$',r'/\d+/\d+\.html$']}
}

def now():return datetime.now(timezone.utc).isoformat(timespec='seconds')
def load(p,d):
 try:return json.loads(p.read_text(encoding='utf-8'))
 except Exception:return d
def save(p,o):
 p.parent.mkdir(parents=True,exist_ok=True);q=p.with_suffix(p.suffix+'.tmp');q.write_text(json.dumps(o,ensure_ascii=False,indent=2),encoding='utf-8');q.replace(p)
def clean(s):return re.sub(r'\s+',' ',s or '').strip()
def host(u):return (urlparse(u).hostname or '').lower().removeprefix('www.')
def same(a,b):return host(a)==host(b)
def fetch(u,ref=None):
 last=None
 for n in range(4):
  try:
   h=dict(HEAD);h.update({'Referer':ref} if ref else {})
   with urlopen(Request(u,headers=h),timeout=30) as r:raw=r.read();ct=r.headers.get('Content-Type','')
   m=re.search(r'charset=([\w.-]+)',ct,re.I)
   for e in ([m.group(1)] if m else [])+['utf-8','gb18030','gbk']:
    try:return raw.decode(e)
    except Exception:pass
   return raw.decode('utf-8','replace')
  except HTTPError as e:
   last=e
   if e.code not in (408,425,429,500,502,503,504):break
  except (URLError,TimeoutError,OSError) as e:last=e
  time.sleep(min(2**n,6))
 raise RuntimeError(str(last))
def rows():
 idx=load(V5/'index.json',{});out=[]
 for s in idx.get('shards',[]):out.extend(load(V5/s['file'],{}).get('books',[]))
 return out
def allowed():
 c=load(CFG,{'sources':[]});return {host(x.get('url','')) for x in c.get('sources',[]) if x.get('authorized') is True and x.get('fulltext_allowed') is True}
def is_chapter(path,profile):return any(re.search(p,path,re.I) for p in profile['chapter'])
def discover(book):
 bid=book.get('id');cached=load(TOC/f'{bid}.json',{}) if bid else {}
 cached_rows=cached.get('chapters',[])
 if cached_rows:
  return [(int(x.get('n') or i+1),clean(x.get('title')) or f'第{i+1}章',x.get('url')) for i,x in enumerate(cached_rows) if x.get('url')]
 u=book.get('source_url','');h=host(u);p=PROFILES.get(h)
 if not p:return []
 s=BeautifulSoup(fetch(u,u),'html.parser');d=[];seen=set()
 for a in s.find_all('a',href=True):
  x=urljoin(u,a['href']).split('#')[0]
  if not same(x,u) or not is_chapter(urlparse(x).path,p) or x in seen:continue
  seen.add(x);d.append((len(d)+1,clean(a.get_text(' ',strip=True)) or f'第{len(d)+1}章',x))
 return d
def extract(u):
 s=BeautifulSoup(fetch(u,u),'html.parser')
 for bad in s.select('script,style,noscript,iframe,ins,.ads,.ad,.advertisement,.recommend,.footer,.header,.nav,.copyright,.tips'):bad.decompose()
 title='';h=s.find('h1') or s.find('h2')
 if h:title=clean(h.get_text(' ',strip=True))
 best='';selectors=('#content','.content','#chaptercontent','.chapter-content','.read-content','.article-content','.novel-content','.text','.txtnav','.read_txt','article')
 for sel in selectors:
  n=s.select_one(sel)
  if not n:continue
  t='\n'.join(clean(x) for x in n.stripped_strings if clean(x))
  if len(t)>len(best):best=t
 if len(best)<120:
  candidates=[]
  for n in s.find_all(['div','article','section']):
   t='\n'.join(clean(x) for x in n.stripped_strings if clean(x))
   if 120<=len(t)<=60000:candidates.append(t)
  if candidates:best=max(candidates,key=len)
 bad_markers=('登录后阅读','请登录','VIP章节','付费阅读','验证码','安全验证')
 if any(x in best[:1000] for x in bad_markers):return title,''
 return title,best if len(best)>=120 else ''
def main():
 OUT.mkdir(parents=True,exist_ok=True);TOC.mkdir(parents=True,exist_ok=True);st=load(STATE,{'books':{}});errs=load(ERR,{'errors':[]}).get('errors',[]);hosts=allowed();count=0
 for b in rows():
  h=host(b.get('source_url',''))
  if h not in hosts or h not in PROFILES or b.get('media')!='text':continue
  bid=b.get('id') or str(abs(hash(b.get('source_url',''))));bs=st['books'].setdefault(bid,{'complete':False})
  if bs.get('complete'):continue
  path=OUT/f'{bid}.json';doc=load(path,{'book_id':bid,'title':b.get('title',''),'chapters':[],'complete':False});have={x.get('url') for x in doc.get('chapters',[]) if x.get('url')}
  try:links=discover(b)
  except Exception as e:bs['error']=str(e)[:300];errs.append({'time':now(),'book':b.get('title'),'url':b.get('source_url'),'error':str(e)[:300]});continue
  if not links:bs['error']='no public chapter links';continue
  budget=20;added=0
  for n,title,u in links:
   if u in have or budget<=0:continue
   try:
    t,c=extract(u)
    if not c:continue
    doc['chapters'].append({'n':n,'title':t or title,'url':u,'content':c});have.add(u);budget-=1;added+=1;time.sleep(.6)
   except Exception as e:errs.append({'time':now(),'book':b.get('title'),'url':u,'error':str(e)[:300]});break
  doc['chapters'].sort(key=lambda x:(x.get('n',0),x.get('url','')));bs['next']=len(doc['chapters'])+1
  if links and len(have)>=len(links):bs['complete']=True;doc['complete']=True
  doc['updated_at']=now();save(path,doc);count+=1
  if count>=20:break
 save(STATE,st);save(ERR,{'generated_at':now(),'errors':errs[-1000:]});print(json.dumps({'mirrored_books_this_run':count},ensure_ascii=False))
if __name__=='__main__':main()
