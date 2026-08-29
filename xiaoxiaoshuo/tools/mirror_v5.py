from __future__ import annotations
import json,re,time
from datetime import datetime,timezone
from pathlib import Path
from urllib.parse import urljoin,urlparse
from urllib.request import Request,urlopen
from bs4 import BeautifulSoup

ROOT=Path(__file__).resolve().parents[1];V5=ROOT/'data'/'v5';CFG=ROOT/'config'/'sources.json';OUT=V5/'chapters';STATE=V5/'mirror_state.json'
HEAD={'User-Agent':'Mozilla/5.0 AppleWebKit/537.36 Chrome/139 Safari/537.36','Accept-Language':'zh-CN,zh;q=0.9','X-XiaoXiaoShuo-Crawler':'reader-mirror-v5'}
def load(p,d):
 try:return json.loads(p.read_text(encoding='utf-8'))
 except Exception:return d
def save(p,o):p.parent.mkdir(parents=True,exist_ok=True);p.write_text(json.dumps(o,ensure_ascii=False,indent=2),encoding='utf-8')
def fetch(u,ref=None):
 h=dict(HEAD);h.update({'Referer':ref} if ref else {})
 with urlopen(Request(u,headers=h),timeout=25) as r:raw=r.read();ct=r.headers.get('Content-Type','')
 m=re.search(r'charset=([\w.-]+)',ct,re.I)
 for e in ([m.group(1)] if m else [])+['utf-8','gb18030','gbk']:
  try:return raw.decode(e)
  except Exception:pass
 return raw.decode('utf-8','replace')
def clean(s):return re.sub(r'\s+',' ',s or '').strip()
def rows():
 idx=load(V5/'index.json',{});out=[]
 for s in idx.get('shards',[]):out.extend(load(V5/s['file'],{}).get('books',[]))
 return out
def allowed_hosts():
 c=load(CFG,{'sources':[]});return {urlparse(x.get('url','')).hostname for x in c.get('sources',[]) if x.get('fulltext_allowed') is True}
def chapter_links(book):
 u=book.get('source_url','');text=fetch(u);s=BeautifulSoup(text,'html.parser');bid=re.search(r'/read/(\d+)/?',urlparse(u).path)
 if not bid:return []
 pat=re.compile(rf'^/read/{bid.group(1)}/p(\d+)\.html$');d={}
 for a in s.find_all('a',href=True):
  x=urljoin(u,a['href']).split('#')[0];m=pat.match(urlparse(x).path)
  if m:d[int(m.group(1))]=(clean(a.get_text(' ',strip=True)) or f'第{m.group(1)}章',x)
 return [(n,d[n][0],d[n][1]) for n in sorted(d)]
def parse_chapter(u):
 text=fetch(u);s=BeautifulSoup(text,'html.parser');title='';h=s.find('h1') or s.find('h2')
 if h:title=clean(h.get_text(' ',strip=True))
 best=''
 for sel in ('#content','.content','.chapter-content','#chaptercontent','.read-content','article'):
  n=s.select_one(sel)
  if n:
   for bad in n.select('script,style,ins,iframe,.ads,.ad'):bad.decompose()
   t='\n'.join(clean(x) for x in n.stripped_strings if clean(x))
   if len(t)>len(best):best=t
 if len(best)<80:return title,''
 return title,best
def main():
 OUT.mkdir(parents=True,exist_ok=True);st=load(STATE,{'books':{}});hosts=allowed_hosts();done_books=0
 for b in rows():
  if b.get('media')!='text' or urlparse(b.get('source_url','')).hostname not in hosts:continue
  bid=b.get('id');bs=st['books'].setdefault(bid,{'next':1,'complete':False})
  if bs.get('complete'):continue
  path=OUT/f'{bid}.json';doc=load(path,{'book_id':bid,'title':b.get('title',''),'chapters':[],'complete':False});have={int(x.get('n',0)) for x in doc.get('chapters',[])}
  try:links=chapter_links(b)
  except Exception as e:bs['error']=str(e)[:300];continue
  if not links:bs['error']='no public chapter links';continue
  budget=40
  for n,title,u in links:
   if n in have or n<int(bs.get('next',1)):continue
   if budget<=0:break
   try:
    t,c=parse_chapter(u)
    if not c:continue
    doc['chapters'].append({'n':n,'title':t or title,'url':u,'content':c});doc['chapters'].sort(key=lambda x:x['n']);bs['next']=n+1;budget-=1;time.sleep(.8)
   except Exception as e:bs['error']=str(e)[:300];break
  if links and int(bs.get('next',1))>links[-1][0]:bs['complete']=True;doc['complete']=True
  doc['updated_at']=datetime.now(timezone.utc).isoformat(timespec='seconds');save(path,doc);done_books+=1
  if done_books>=4:break
 save(STATE,st);print(json.dumps({'mirrored_books_this_run':done_books},ensure_ascii=False))
if __name__=='__main__':main()
