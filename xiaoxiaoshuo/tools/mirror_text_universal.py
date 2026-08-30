from __future__ import annotations
import json,re,time
from collections import deque
from datetime import datetime,timezone
from pathlib import Path
from urllib.error import HTTPError,URLError
from urllib.parse import urljoin,urlparse,urldefrag
from urllib.request import Request,urlopen
from bs4 import BeautifulSoup

ROOT=Path(__file__).resolve().parents[1]
V5=ROOT/'data'/'v5'; CFG=ROOT/'config'/'sources.json'; OUT=V5/'chapters'; TOC=V5/'toc'; STATE=V5/'universal_mirror_state.json'; ERR=V5/'universal_mirror_errors.json'; STATUS=V5/'universal_mirror_status.json'
HEAD={'User-Agent':'Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 Chrome/139 Safari/537.36','Accept':'text/html,application/xhtml+xml','Accept-Language':'zh-CN,zh;q=0.9','X-XiaoXiaoShuo-Crawler':'authorized-text-mirror/5.0'}
CHAPTER_WORD=re.compile(r'^(?:第\s*[0-9一二三四五六七八九十百千万两〇零]+\s*[章节回卷部集]|楔子|序章|前言|后记|尾声|番外|正文)',re.I)
TOC_WORDS=('目录','章节目录','全部章节','所有章节','查看目录','正文目录','最新章节','章节列表','完整目录','全部目录')
NEXT_WORDS=('下一页','下页','后一页','更多章节','下一章列表')
AUDIO_WORDS=('有声','听书','音频','audiobook','audio')
PROFILES={
 'ixdzs8.com':{'chapter':[r'^/read/\d+/p\d+\.html$'],'book_prefix':r'^/read/(\d+)/'},
 'tadu.com':{'chapter':[r'^/book/\d+/\d+/?$'],'book_prefix':r'^/book/(\d+)/'},
 'kanunu8.com':{'chapter':[r'/book\d+/[^/]+/\d+\.html$',r'/files/[^/]+/\d+\.html$']},
 '99csw.com':{'chapter':[r'^/book/\d+/\d+\.(?:htm|html)$']},
 'xs8.cn':{'chapter':[r'/(?:chapter|read)/[^?#]+']},
 '1qxs.com':{'chapter':[r'/xs_\d+/\d+/\d+\.html$',r'/xs_\d+/\d+\.html$',r'/chapter/[^?#]+']},
 'zwxiaoshuo.com':{'chapter':[r'/(?:chapter|read)-?\d+.*\.html$',r'/\d+/\d+\.html$']},
 'ilwxs.com':{'chapter':[r'^/book/\d+/\d+\.html$',r'^/read/\d+/\d+\.html$'],'book_prefix':r'^/(?:book|read)/(\d+)/'},
 'ipaoshubaxs.net':{'chapter':[r'^/book/\d+/\d+\.html$',r'^/\d+/\d+/\d+\.html$',r'^/chapter/\d+\.html$',r'^/\d+/\d+\.html$']},
 'aaaks.com':{'chapter':[r'^/(?:chapter|read)/\d+/?$',r'^/\d+/\d+\.html$',r'^/book/\d+/\d+\.html$']},
 '69shuba.com':{'chapter':[r'^/book/\d+/\d+\.html$',r'^/txt/\d+/\d+\.html$',r'^/book/\d+/\d+/?$'],'book_prefix':r'^/(?:book|txt)/(\d+)/'},
 'piaotia.com':{'chapter':[r'^/html/\d+/\d+/\d+\.html$',r'^/book/\d+/\d+\.html$',r'^/html/\d+/\d+\.html$']},
 'hetushu.com':{'chapter':[r'^/book/\d+/\d+\.html$'],'book_prefix':r'^/book/(\d+)/'},
}

def now():return datetime.now(timezone.utc).isoformat(timespec='seconds')
def load(p,d):
 try:return json.loads(p.read_text(encoding='utf-8'))
 except Exception:return d
def save(p,o):
 p.parent.mkdir(parents=True,exist_ok=True);q=p.with_suffix(p.suffix+'.tmp');q.write_text(json.dumps(o,ensure_ascii=False,indent=2),encoding='utf-8');q.replace(p)
def clean(s):return re.sub(r'\s+',' ',s or '').strip()
def host(u):
 h=(urlparse(u or '').hostname or '').lower()
 for x in ('www.','m.','wap.'):
  if h.startswith(x):h=h[len(x):]
 return h
def same(a,b):return host(a)==host(b)
def canon(u):
 u=urldefrag(u or '')[0];p=urlparse(u)
 return u if p.scheme in ('http','https') else ''
def fetch(u,ref=None):
 last=None
 for n in range(4):
  h=dict(HEAD)
  if ref:h['Referer']=ref
  try:
   with urlopen(Request(u,headers=h),timeout=30) as r:raw=r.read();ct=r.headers.get('Content-Type','')
   m=re.search(r'charset=([\w.-]+)',ct,re.I)
   for enc in ([m.group(1)] if m else [])+['utf-8','gb18030','gbk']:
    try:return raw.decode(enc)
    except Exception:pass
   return raw.decode('utf-8','replace')
  except HTTPError as e:
   last=e
   if e.code not in (408,425,429,500,502,503,504):break
  except (URLError,TimeoutError,OSError) as e:last=e
  time.sleep(min(2**n,5))
 raise RuntimeError(str(last))
def rows():
 idx=load(V5/'index.json',{});out=[]
 for s in idx.get('shards',[]):out.extend(load(V5/s.get('file',''),{}).get('books',[]))
 return out
def allowed_hosts():
 c=load(CFG,{'sources':[]})
 return {host(x.get('url')) for x in c.get('sources',[]) if x.get('authorized') is True and x.get('fulltext_allowed') is True and x.get('media_policy','text-only')=='text-only'}
def is_audio_book(b):
 text=' '.join(str(b.get(k,'') or '') for k in ('title','kind','source_name','media')).lower()
 return b.get('media') not in ('',None,'text') or any(w in text for w in AUDIO_WORDS)
def is_chapter(url,profile,title=''):
 path=urlparse(url).path
 if any(re.search(p,path,re.I) for p in profile.get('chapter',[])):return True
 return bool(CHAPTER_WORD.search(clean(title)))
def same_book_scope(book_url,target,profile):
 if not same(book_url,target):return False
 rule=profile.get('book_prefix')
 if not rule:return True
 a=re.search(rule,urlparse(book_url).path,re.I);b=re.search(rule,urlparse(target).path,re.I)
 return True if not a else bool(b and a.group(1)==b.group(1))
def collect_from_page(soup,page_url,book_url,profile,chapters,dirq,seen_dirs):
 for a in soup.find_all('a',href=True):
  target=canon(urljoin(page_url,a.get('href','')));title=clean(a.get_text(' ',strip=True))
  if not target or not same(book_url,target):continue
  if is_chapter(target,profile,title) and same_book_scope(book_url,target,profile):
   if target not in chapters:chapters[target]=title or f'第{len(chapters)+1}章'
   continue
  low=(title+' '+target).lower()
  if any(w in title for w in TOC_WORDS) or any(x in low for x in ('catalog','chapterlist','chapters','directory','list','allchapter','mulu')) or title in NEXT_WORDS:
   if target not in seen_dirs and target not in dirq:dirq.append(target)
def expand_by_neighbor_chapters(book_url,profile,chapters,max_pages=180):
 q=deque(list(chapters.keys())[:3]+list(chapters.keys())[-3:]);seen=set();pages=0
 while q and pages<max_pages:
  u=q.popleft()
  if not u or u in seen:continue
  seen.add(u);pages+=1
  try:s=BeautifulSoup(fetch(u,book_url),'html.parser')
  except Exception:continue
  for a in s.find_all('a',href=True):
   t=clean(a.get_text(' ',strip=True));x=canon(urljoin(u,a.get('href','')))
   if not x or not same_book_scope(book_url,x,profile):continue
   if t in ('上一章','下一章','上章','下章') or is_chapter(x,profile,t):
    if is_chapter(x,profile,t) and x not in chapters:chapters[x]=t or f'第{len(chapters)+1}章'
    if x not in seen:q.append(x)
  time.sleep(.12)
 return pages
def discover_full_toc(book):
 bid=book.get('id');cached=load(TOC/f'{bid}.json',{}) if bid else {};cached_rows=cached.get('chapters',[])
 u=book.get('source_url','');profile=PROFILES.get(host(u))
 if not u or not profile:return cached_rows
 chapters={x.get('url'):clean(x.get('title')) or f"第{x.get('n',1)}章" for x in cached_rows if x.get('url')}
 dirq=deque([u]);seen_dirs=set();pages=0
 while dirq and pages<60:
  page=dirq.popleft()
  if page in seen_dirs:continue
  seen_dirs.add(page);pages+=1
  try:soup=BeautifulSoup(fetch(page,u),'html.parser')
  except Exception:continue
  collect_from_page(soup,page,u,profile,chapters,dirq,seen_dirs);time.sleep(.18)
 if len(chapters)<=3:
  pages+=expand_by_neighbor_chapters(u,profile,chapters)
 out=[{'n':i+1,'title':title or f'第{i+1}章','url':url} for i,(url,title) in enumerate(chapters.items())]
 def numkey(x):
  title=x.get('title','');m=re.search(r'第\s*(\d+)\s*[章节回卷部集]',title)
  if m:return (0,int(m.group(1)))
  nums=[int(n) for n in re.findall(r'\d+',urlparse(x['url']).path)]
  return (1,)+tuple(nums[-3:]) if nums else (2,x['n'])
 out.sort(key=numkey)
 for i,x in enumerate(out):x['n']=i+1
 if bid:save(TOC/f'{bid}.json',{'book_id':bid,'title':book.get('title',''),'chapters':out,'updated_at':now(),'directory_pages_scanned':pages,'complete_directory':len(out)>3})
 return out
def extract(u):
 s=BeautifulSoup(fetch(u,u),'html.parser')
 for bad in s.select('script,style,noscript,iframe,ins,.ads,.ad,.advertisement,.recommend,.footer,.header,.nav,.copyright,.tips,.toolbar,.comment,.comments'):bad.decompose()
 title='';h=s.find('h1') or s.find('h2')
 if h:title=clean(h.get_text(' ',strip=True))
 best=''
 for sel in ('#content','#chaptercontent','#chapter-content','.chapter-content','.read-content','.article-content','.novel-content','.content','.text','.txtnav','.read_txt','.yd_text2','.contentbox','.readcontent','#nr1','.txt','article'):
  n=s.select_one(sel)
  if not n:continue
  t='\n'.join(clean(x) for x in n.stripped_strings if clean(x))
  if len(t)>len(best):best=t
 if len(best)<150:
  candidates=[]
  for n in s.find_all(['div','article','section']):
   t='\n'.join(clean(x) for x in n.stripped_strings if clean(x))
   if 150<=len(t)<=120000:candidates.append(t)
  if candidates:best=max(candidates,key=len)
 blockers=('请登录','登录后阅读','VIP章节','付费阅读','验证码','安全验证','APP用户特权','扫码畅读专属内容')
 if any(x in best[:1500] for x in blockers):return title,''
 return title,best if len(best)>=150 else ''
def main():
 OUT.mkdir(parents=True,exist_ok=True);TOC.mkdir(parents=True,exist_ok=True)
 st=load(STATE,{'books':{},'book_cursor':0});errs=load(ERR,{'errors':[]}).get('errors',[]);hosts=allowed_hosts();allrows=[b for b in rows() if host(b.get('source_url')) in hosts and not is_audio_book(b) and b.get('id')]
 total=len(allrows);start=int(st.get('book_cursor',0))%max(1,total);ordered=allrows[start:]+allrows[:start];processed=0;added_total=0;visited=0
 for b in ordered:
  if processed>=36:break
  visited+=1;bid=b.get('id');bs=st['books'].setdefault(bid,{'cursor':0,'complete':False})
  path=OUT/f'{bid}.json';existing=load(path,{'chapters':[],'complete':False})
  if bs.get('complete') and existing.get('complete'):continue
  try:toc=discover_full_toc(b)
  except Exception as e:errs.append({'time':now(),'book':b.get('title'),'url':b.get('source_url'),'stage':'toc','error':str(e)[:400]});continue
  if not toc:bs['error']='no chapter directory found';continue
  doc=existing if isinstance(existing,dict) else {'book_id':bid,'title':b.get('title',''),'chapters':[],'complete':False};doc.setdefault('book_id',bid);doc.setdefault('title',b.get('title',''));doc.setdefault('chapters',[])
  have={x.get('url') for x in doc.get('chapters',[]) if x.get('url')};budget=120
  for item in toc:
   if budget<=0:break
   url=item.get('url')
   if not url or url in have:continue
   try:
    t,c=extract(url)
    if not c:continue
    doc['chapters'].append({'n':item.get('n'),'title':t or item.get('title'),'url':url,'content':c});have.add(url);added_total+=1;budget-=1;time.sleep(.28)
   except Exception as e:errs.append({'time':now(),'book':b.get('title'),'url':url,'stage':'chapter','error':str(e)[:400]})
  doc['chapters'].sort(key=lambda x:int(x.get('n') or 0));doc['updated_at']=now();doc['toc_count']=len(toc);doc['complete']=len(have)>=len(toc) and len(toc)>0
  bs['complete']=doc['complete'];bs['toc_count']=len(toc);bs['synced_count']=len(have);bs['updated_at']=now();save(path,doc);processed+=1
 st['book_cursor']=(start+max(1,visited))%max(1,total);unfinished=sum(1 for b in allrows if not st['books'].get(b.get('id'),{}).get('complete'))
 st['updated_at']=now();save(STATE,st);save(ERR,{'generated_at':now(),'errors':errs[-2000:]});save(STATUS,{'generated_at':now(),'text_books':total,'processed_this_run':processed,'chapters_added':added_total,'unfinished_books':unfinished,'all_fulltext_done':unfinished==0})
 print(json.dumps({'text_books':total,'processed_books':processed,'chapters_added':added_total,'unfinished_books':unfinished,'all_fulltext_done':unfinished==0},ensure_ascii=False))
if __name__=='__main__':main()
