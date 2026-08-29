from __future__ import annotations
import io,json,re,time,zipfile
from datetime import datetime,timezone
from pathlib import Path
from urllib.parse import urljoin,urlparse
from urllib.request import Request,urlopen
from urllib.error import HTTPError,URLError
from bs4 import BeautifulSoup

ROOT=Path(__file__).resolve().parents[1];V5=ROOT/'data'/'v5';CFG=ROOT/'config'/'sources.json';OUT=V5/'chapters';STATE=V5/'mirror_state.json';ERR=V5/'mirror_errors.json'
HEAD={'User-Agent':'Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 Chrome/139 Safari/537.36','Accept-Language':'zh-CN,zh;q=0.9','X-XiaoXiaoShuo-Crawler':'reader-mirror-v6'}
CHAPTER_RE=re.compile(r'(?m)^\s*((?:第\s*[0-9零一二三四五六七八九十百千万两〇]+\s*[章节回卷].{0,100})|(?:楔子|序章|前言|后记|尾声|番外.{0,80}))\s*$')

def now():return datetime.now(timezone.utc).isoformat(timespec='seconds')
def load(p,d):
 try:return json.loads(p.read_text(encoding='utf-8'))
 except Exception:return d
def save(p,o):
 p.parent.mkdir(parents=True,exist_ok=True);q=p.with_suffix(p.suffix+'.tmp');q.write_text(json.dumps(o,ensure_ascii=False,indent=2),encoding='utf-8');q.replace(p)
def host(u):return (urlparse(u).hostname or '').lower().removeprefix('www.')
def fetch_bytes(u,ref=None):
 last=None
 for n in range(4):
  try:
   h=dict(HEAD);h.update({'Referer':ref} if ref else {})
   with urlopen(Request(u,headers=h),timeout=35) as r:return r.read(),r.headers.get('Content-Type','')
  except HTTPError as e:
   last=e
   if e.code not in (408,425,429,500,502,503,504):break
  except (URLError,TimeoutError,OSError) as e:last=e
  time.sleep(min(2**n,6))
 raise RuntimeError(str(last))
def decode(raw,ct=''):
 m=re.search(r'charset=([\w.-]+)',ct or '',re.I)
 for e in ([m.group(1)] if m else [])+['utf-8','gb18030','gbk','big5']:
  try:return raw.decode(e)
  except Exception:pass
 return raw.decode('utf-8','replace')
def fetch(u,ref=None):
 raw,ct=fetch_bytes(u,ref);return decode(raw,ct)
def clean(s):return re.sub(r'\s+',' ',s or '').strip()
def rows():
 idx=load(V5/'index.json',{});out=[]
 for s in idx.get('shards',[]):out.extend(load(V5/s['file'],{}).get('books',[]))
 return out
def allowed_hosts():
 c=load(CFG,{'sources':[]});return {host(x.get('url','')) for x in c.get('sources',[]) if x.get('fulltext_allowed') is True}
def chapter_links(book):
 u=book.get('source_url','');text=fetch(u);s=BeautifulSoup(text,'html.parser');bid=re.search(r'/read/(\d+)/?',urlparse(u).path)
 if not bid:return []
 pat=re.compile(rf'^/read/{bid.group(1)}/p(\d+)\.html$');d={}
 for a in s.find_all('a',href=True):
  x=urljoin(u,a['href']).split('#')[0];m=pat.match(urlparse(x).path)
  if m:d[int(m.group(1))]=(clean(a.get_text(' ',strip=True)) or f'第{m.group(1)}章',x)
 if 1 not in d:d[1]=('第一章',urljoin(u,'p1.html'))
 return [(n,d[n][0],d[n][1]) for n in sorted(d)]
def parse_chapter(u):
 text=fetch(u);s=BeautifulSoup(text,'html.parser');title='';h=s.find('h1') or s.find('h2')
 if h:title=clean(h.get_text(' ',strip=True))
 for bad in s.select('script,style,noscript,iframe,ins,.ads,.ad,.nav,.header,.footer'):bad.decompose()
 best=''
 for sel in ('#content','.content','.chapter-content','#chaptercontent','.read-content','.read-content-jieqi','.page-content','.article-content','article'):
  n=s.select_one(sel)
  if n:
   t='\n'.join(clean(x) for x in n.stripped_strings if clean(x))
   if len(t)>len(best):best=t
 blockers=('正在验证浏览器','安全验证','验证码','请登录后阅读','VIP章节','APP用户特权','扫码畅读专属内容')
 if any(x in best[:1500] for x in blockers) or len(best)<120:return title,''
 return title,best
def split_txt(text,title=''):
 text=text.replace('\r\n','\n').replace('\r','\n');matches=list(CHAPTER_RE.finditer(text));out=[]
 if not matches:
  body=text.strip()
  if len(body)>500:out=[{'n':1,'title':title or '正文','content':body}]
  return out
 for i,m in enumerate(matches):
  start=m.end();end=matches[i+1].start() if i+1<len(matches) else len(text);body=text[start:end].strip();t=clean(m.group(1))
  if len(body)>=80:out.append({'n':len(out)+1,'title':t,'content':body})
 return out
def zip_fulltext(book):
 u=book.get('source_url','');m=re.search(r'/read/(\d+)',urlparse(u).path)
 if not m:return []
 bid=m.group(1);candidates=[f'https://down7.ixdzs8.com/{bid}.zip',f'https://down.ixdzs8.com/{bid}.zip']
 for zurl in candidates:
  try:
   raw,_=fetch_bytes(zurl,u)
   if raw[:2]!=b'PK':continue
   with zipfile.ZipFile(io.BytesIO(raw)) as z:
    names=[n for n in z.namelist() if not n.endswith('/') and (n.lower().endswith('.txt') or '.' not in Path(n).name)]
    if not names:continue
    name=max(names,key=lambda n:z.getinfo(n).file_size);data=z.read(name);text=decode(data)
    ch=split_txt(text,book.get('title',''))
    if ch:
     for x in ch:x['url']=zurl+'#'+str(x['n'])
     return ch
  except Exception:continue
 return []
def main():
 OUT.mkdir(parents=True,exist_ok=True);st=load(STATE,{'books':{}});errs=load(ERR,{'errors':[]}).get('errors',[]);hosts=allowed_hosts();done_books=0
 for b in rows():
  h=host(b.get('source_url',''))
  if b.get('media')!='text' or h not in hosts or h!='ixdzs8.com':continue
  bid=b.get('id');bs=st['books'].setdefault(bid,{'next':1,'complete':False});path=OUT/f'{bid}.json';doc=load(path,{'book_id':bid,'title':b.get('title',''),'chapters':[],'complete':False})
  if doc.get('chapters') and bs.get('complete'):continue
  # Preferred path: site's public TXT/ZIP download, because chapter HTML may return a browser challenge.
  try:full=zip_fulltext(b)
  except Exception as e:full=[];errs.append({'time':now(),'book':b.get('title'),'stage':'zip','error':str(e)[:300]})
  if full:
   doc={'book_id':bid,'title':b.get('title',''),'chapters':full,'complete':True,'updated_at':now(),'source_mode':'public-txt-zip'};save(path,doc);bs.update({'next':len(full)+1,'complete':True,'mode':'public-txt-zip'});done_books+=1
  else:
   try:links=chapter_links(b)
   except Exception as e:bs['error']=str(e)[:300];errs.append({'time':now(),'book':b.get('title'),'stage':'toc','error':str(e)[:300]});continue
   have={x.get('url') for x in doc.get('chapters',[]) if x.get('url')};budget=20
   for n,title,u in links:
    if u in have or budget<=0:continue
    try:
     t,c=parse_chapter(u)
     if not c:continue
     doc['chapters'].append({'n':n,'title':t or title,'url':u,'content':c});have.add(u);budget-=1
    except Exception as e:errs.append({'time':now(),'book':b.get('title'),'stage':'html','url':u,'error':str(e)[:300]});break
   doc['chapters'].sort(key=lambda x:int(x.get('n') or 0));doc['updated_at']=now();doc['source_mode']='chapter-html';save(path,doc);bs['next']=len(doc['chapters'])+1;done_books+=1
  if done_books>=12:break
 save(STATE,st);save(ERR,{'generated_at':now(),'errors':errs[-1000:]});print(json.dumps({'mirrored_ixdzs_books_this_run':done_books},ensure_ascii=False))
if __name__=='__main__':main()
