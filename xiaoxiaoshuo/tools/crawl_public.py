from __future__ import annotations
import json,re,time
from datetime import datetime,timezone
from pathlib import Path
from urllib.parse import urlencode
from urllib.request import Request,urlopen

ROOT=Path(__file__).resolve().parents[1]; DATA=ROOT/'data'; CFG=ROOT/'config'/'public_sources.json'; DB=DATA/'full_catalog.json'; STATE=DATA/'public_crawl_state.json'; STATUS=DATA/'public_crawl_status.json'
UA='XiaoXiaoShuoPublicCrawler/1.0 (+public-domain-open-license)'

def now():return datetime.now(timezone.utc).isoformat(timespec='seconds')
def load(p,d):
 try:return json.loads(p.read_text(encoding='utf-8'))
 except Exception:return d
def save(p,o):
 p.parent.mkdir(parents=True,exist_ok=True);p.write_text(json.dumps(o,ensure_ascii=False,indent=2),encoding='utf-8')
def getj(url):
 r=Request(url,headers={'User-Agent':UA,'Accept':'application/json'})
 with urlopen(r,timeout=30) as x:return json.loads(x.read().decode('utf-8'))
def clean(s):return re.sub(r'\s+',' ',s or '').strip()

def crawl_gutendex(src,state,by):
 page=max(1,int(state.get('page',1)));limit=max(1,min(int(src.get('pages_per_run',8)),30));added=seen=0
 langs=src.get('languages') or ['zh','en']
 for lang in langs:
  p=page
  for _ in range(limit):
   url=src['url']+'?'+urlencode({'page':p,'languages':lang,'copyright':'false'})
   try:d=getj(url)
   except Exception:break
   rows=d.get('results') or []
   if not rows:break
   for b in rows:
    seen+=1;bid=f'gutenberg:{b.get("id")}';authors=', '.join(clean(a.get('name')) for a in (b.get('authors') or []) if a.get('name'));subjects=b.get('subjects') or []
    fmt=b.get('formats') or {};text_url=fmt.get('text/html') or fmt.get('text/html; charset=utf-8') or fmt.get('text/plain; charset=utf-8') or fmt.get('text/plain') or ''
    cover=fmt.get('image/jpeg','');title=clean(b.get('title')) or bid
    rec={'title':title,'author':authors,'kind':' / '.join(subjects[:3]),'status':'公版/开放授权','intro':'Project Gutenberg public-domain/open-license catalog','cover':cover,'source_url':text_url or f'https://www.gutenberg.org/ebooks/{b.get("id")}', 'source_host':'gutenberg.org','chapter_count':0,'last_chapter':'','media':'text','content_access':'public-domain','detail_complete':True,'indexed_at':now(),'source_key':bid}
    if rec['source_url'] not in by:added+=1
    by[rec['source_url']]=rec
   p+=1
   if not d.get('next'):break
   time.sleep(.2)
 state['page']=p
 return {'seen':seen,'added':added,'cursor':dict(state)}

def crawl_wikisource(src,state,by):
 cont=state.get('cmcontinue');rounds=max(1,min(int(src.get('pages_per_run',8)),30));seen=added=0
 for _ in range(rounds):
  q={'action':'query','format':'json','list':'categorymembers','cmtitle':src.get('category','Category:小说'),'cmnamespace':0,'cmlimit':'500','cmtype':'page'}
  if cont:q['cmcontinue']=cont
  try:d=getj(src['url']+'?'+urlencode(q))
  except Exception:break
  rows=((d.get('query') or {}).get('categorymembers') or [])
  for x in rows:
   seen+=1;title=clean(x.get('title'));url='https://zh.wikisource.org/wiki/'+title.replace(' ','_');rec={'title':title,'author':'','kind':'中文维基文库 · 小说','status':'公版/开放授权','intro':'来自中文维基文库开放文本目录','cover':'','source_url':url,'source_host':'zh.wikisource.org','chapter_count':0,'last_chapter':'','media':'text','content_access':'open-license-or-public-domain','detail_complete':True,'indexed_at':now(),'source_key':'wikisource:'+str(x.get('pageid'))}
   if url not in by:added+=1
   by[url]=rec
  cont=(d.get('continue') or {}).get('cmcontinue')
  if not cont:state['done']=True;break
  time.sleep(.2)
 state['cmcontinue']=cont
 return {'seen':seen,'added':added,'cursor':dict(state)}

def main():
 cfg=load(CFG,{'sources':[]});state=load(STATE,{'sources':{}});db=load(DB,{'books':[]});by={b.get('source_url'):b for b in db.get('books',[]) if b.get('source_url')};report=[]
 for src in cfg.get('sources',[]):
  if not src.get('enabled') or not src.get('public_domain_or_open_license'):continue
  key=src.get('name') or src.get('type');st=state.setdefault('sources',{}).setdefault(key,{})
  try:
   if src.get('type')=='gutendex':r=crawl_gutendex(src,st,by)
   elif src.get('type')=='wikisource-category':r=crawl_wikisource(src,st,by)
   else:r={'seen':0,'added':0,'cursor':dict(st)}
   report.append({'name':key,'status':f'发现 {r["seen"]}，新增 {r["added"]}','cursor':r['cursor']})
  except Exception as e:report.append({'name':key,'status':'失败：'+str(e)[:300]})
 rows=list(by.values());save(DB,{'generated_at':now(),'books':rows});save(STATE,state);save(STATUS,{'generated_at':now(),'books':len(rows),'sources':report});print(json.dumps({'books':len(rows),'sources':report},ensure_ascii=False))
if __name__=='__main__':main()
