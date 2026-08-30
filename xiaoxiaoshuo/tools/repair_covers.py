from __future__ import annotations
import json,re,time
from datetime import datetime,timezone
from pathlib import Path
from urllib.parse import urljoin,urlparse
from urllib.request import Request,urlopen
from urllib.error import HTTPError,URLError
from bs4 import BeautifulSoup

ROOT=Path(__file__).resolve().parents[1];V5=ROOT/'data'/'v5';INDEX=V5/'index.json';STATE=V5/'cover_repair_state.json'
UA='Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 Chrome/139 Safari/537.36 XiaoXiaoShuoCoverRepair/1.0'
AUDIO=re.compile(r'\.(?:mp3|m4a|aac|ogg|wav|flac)(?:$|\?)',re.I)
IMG=re.compile(r'\.(?:jpe?g|png|webp|gif)(?:$|\?)',re.I)

def load(p,d):
 try:return json.loads(p.read_text(encoding='utf-8'))
 except Exception:return d
def save(p,o):
 p.parent.mkdir(parents=True,exist_ok=True);q=p.with_suffix(p.suffix+'.tmp');q.write_text(json.dumps(o,ensure_ascii=False,indent=2),encoding='utf-8');q.replace(p)
def now():return datetime.now(timezone.utc).isoformat(timespec='seconds')
def fetch(u):
 last=None
 for n in range(3):
  try:
   with urlopen(Request(u,headers={'User-Agent':UA,'Accept-Language':'zh-CN,zh;q=0.9'}),timeout=25) as r:raw=r.read();ct=r.headers.get('Content-Type','')
   m=re.search(r'charset=([\w.-]+)',ct,re.I)
   for e in ([m.group(1)] if m else [])+['utf-8','gb18030','gbk']:
    try:return raw.decode(e)
    except Exception:pass
  except (HTTPError,URLError,TimeoutError,OSError) as e:last=e;time.sleep(1+n)
 raise RuntimeError(str(last))
def good(u,base):
 if not u:return ''
 u=urljoin(base,u.strip())
 if not u.startswith(('http://','https://')) or AUDIO.search(u):return ''
 return u
def cover_from(html,base):
 s=BeautifulSoup(html,'html.parser')
 for attr,val in [('property','og:image'),('name','twitter:image'),('itemprop','image')]:
  n=s.find('meta',attrs={attr:val})
  if n:
   u=good(n.get('content',''),base)
   if u:return u
 sels=('img.cover','.cover img','.book-cover img','#bookimg img','.bookimg img','.pic img','.book_pic img','.novel-cover img','.detail-cover img','.book-info img')
 for sel in sels:
  n=s.select_one(sel)
  if n:
   for a in ('data-src','data-original','data-lazy-src','src'):
    u=good(n.get(a,''),base)
    if u:return u
 for n in s.find_all('img'):
  text=' '.join([str(n.get('class','')),n.get('alt',''),n.get('title','')]).lower()
  if any(x in text for x in ('cover','book','封面','小说')):
   for a in ('data-src','data-original','src'):
    u=good(n.get(a,''),base)
    if u:return u
 return ''
def main():
 idx=load(INDEX,{});shards=idx.get('shards',[]);st=load(STATE,{'shard':0,'offset':0});si=int(st.get('shard',0));off=int(st.get('offset',0));checked=filled=0;budget=100
 while si<len(shards) and checked<budget:
  path=V5/shards[si]['file'];d=load(path,{'books':[]});rows=d.get('books',[]);changed=False
  while off<len(rows) and checked<budget:
   b=rows[off];off+=1
   if b.get('cover') or b.get('media') not in ('text','',None):continue
   u=b.get('source_url','')
   if not u:continue
   checked+=1
   try:
    c=cover_from(fetch(u),u)
    if c:
     b['cover']=c;b['cover_source']='source-page';b['cover_updated_at']=now();filled+=1;changed=True
   except Exception:pass
   time.sleep(.35)
  if changed:save(path,d)
  if off>=len(rows):si+=1;off=0
 st={'shard':0 if si>=len(shards) else si,'offset':0 if si>=len(shards) else off,'updated_at':now(),'last_checked':checked,'last_filled':filled,'cycle_complete':si>=len(shards)};save(STATE,st)
 print(json.dumps({'checked':checked,'filled':filled,'state':st},ensure_ascii=False))
if __name__=='__main__':main()
