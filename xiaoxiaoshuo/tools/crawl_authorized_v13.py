from __future__ import annotations
import argparse, hashlib, json, re, time, unicodedata
from collections import deque
from datetime import datetime, timezone
from pathlib import Path
from urllib.error import HTTPError, URLError
from urllib.parse import urljoin, urlparse, urldefrag
from urllib.request import Request, urlopen
from bs4 import BeautifulSoup

ROOT=Path(__file__).resolve().parents[1]
CFG=ROOT/'config'/'sources.json'; V5=ROOT/'data'/'v5'; V4=ROOT/'data'/'v4'
BOOKS=V5/'books'; TOC=V5/'toc'; INDEX=V5/'index.json'; STATE=V5/'authorized_v13_state.json'; STATUS=V5/'authorized_v13_status.json'; ERR=V5/'authorized_v13_errors.json'
SHARD=200
HEAD={'User-Agent':'Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 Chrome/139 Safari/537.36','Accept-Language':'zh-CN,zh;q=0.9','X-XiaoXiaoShuo-Crawler':'authorized-v13/1.0'}
AUDIO=('有声','听书','音频','audiobook','audio')
BAD_EXT=re.compile(r'\.(?:jpg|jpeg|png|gif|webp|svg|css|js|ico|zip|rar|7z|pdf|mp3|m4a|mp4|woff2?)(?:$|\?)',re.I)
BAD_URL=('login','logout','register','signup','account','usercenter','payment','feedback','contact','javascript:','/app/')
CHAPTER_TEXT=re.compile(r'(?:^|\s)(?:第\s*[0-9零一二三四五六七八九十百千万两〇]+\s*[章节回卷部集]|楔子|序章|前言|后记|尾声|番外)',re.I)
NAV_TEXT={'下一页','下页','后一页','更多','全部','书库','分类','排行','全本','完本','最近更新','今日更新','上架新书','小说','男生小说','女生小说'}

P={
 'tadu.com':dict(name='塔读文学',seeds=['https://www.tadu.com/store','https://www.tadu.com/book/rank/list/0-potential-0-0-1'],book=[r'^/book/\d+/?$'],chapter=[r'^/book/\d+/\d+/?$'],nav=('/store','/book/rank/','/rank','/channel')),
 'kanunu8.com':dict(name='努努书坊',seeds=['https://www.kanunu8.com/','https://www.kanunu8.com/genres.html'],book=[r'^/book\d+/[^/]+/?$',r'^/book\d+/[^/]+/index\.html$',r'^/files/[^/]+/?$'],chapter=[r'^/book\d+/[^/]+/\d+\.html$',r'^/files/[^/]+/\d+\.html$'],nav=('genres','/book','/files/','/zt/','/sort','/list','/writer','/author')),
 '99csw.com':dict(name='九九藏书网',seeds=['https://www.99csw.com/book/','https://www.99csw.com/book/index.php'],book=[r'^/book/\d+/?$',r'^/book/\d+/index\.(?:htm|html)$'],chapter=[r'^/book/\d+/\d+\.(?:htm|html)$'],nav=('/book/','/author/','/wenku/','/top/','index.php')),
 'xs8.cn':dict(name='言情小说吧',seeds=['https://www.xs8.cn/'],book=[r'^/bookquery/[0-9a-z]+/?$',r'^/book/[^?#]+$'],chapter=[r'^/(?:chapter|read)/[^?#]+$'],nav=('/bookquery/','/book/','/category','/store','/rank','/all')),
 '1qxs.com':dict(name='一七小说',seeds=['https://www.1qxs.com/','https://www.1qxs.com/rk/1/0/1','https://www.1qxs.com/rk/4/0/1'],book=[r'^/xs/\d+/?$',r'^/xs_\d+/\d+\.html$'],chapter=[r'^/xs/\d+/\d+/?$',r'^/xs_\d+/\d+/\d+\.html$'],nav=('/list/','/rk/','/fl/','/xs/','/all','/sort','/class','/rank','/quanben')),
 'zwxiaoshuo.com':dict(name='滋味小说网',seeds=['https://www.zwxiaoshuo.com/','https://www.zwxiaoshuo.com/topweekvisit-1.html'],book=[r'^/(?:book|xs|novel)-?\d+.*\.html$',r'^/book/\d+/?$'],chapter=[r'^/(?:chapter|read)-?\d+.*\.html$',r'^/\d+/\d+\.html$'],nav=('top','full','update','author','list','sort','book','xs','novel')),
 'ilwxs.com':dict(name='乐文小说',seeds=['https://www.ilwxs.com/'],book=[r'^/info-\d+\.html$',r'^/shu/\d+/?$'],chapter=[r'^/shu/\d+/\d+\.html$'],nav=('/info-','/shu/','/sort/','/list/','/rank/','/quanben','/top/')),
 'ipaoshubaxs.net':dict(name='泡书吧小说网',seeds=['https://www.ipaoshubaxs.net/'],book=[r'^/book/\d+\.html$',r'^/info-\d+\.html$',r'^/shu/\d+/?$',r'^/\d+/?$'],chapter=[r'^/shu/\d+/\d+\.html$',r'^/book/\d+/\d+\.html$',r'^/\d+/\d+/\d+\.html$'],nav=('/book/','/info-','/shu/','/sort/','/list/','/rank/','/quanben','/top/')),
 'aaaks.com':dict(name='AAAKS小说',seeds=['https://www.aaaks.com/'],book=[r'^/(?:book|novel|info)[-/]?\d+.*$',r'^/\d+/?$'],chapter=[r'^/(?:chapter|read)[-/]?\d+.*$',r'^/\d+/\d+(?:\.html)?$'],nav=('/book','/novel','/sort','/list','/rank','/all','page')),
 '69shuba.com':dict(name='69书吧',seeds=['https://www.69shuba.com/','https://www.69shuba.com/book/'],book=[r'^/book/\d+\.htm$',r'^/book/\d+/?$'],chapter=[r'^/txt/\d+/\d+/?$',r'^/book/\d+/\d+\.html$'],nav=('/book/','/txt/','/sort/','/rank/','/quanben','/list/','/category')),
 'piaotia.com':dict(name='飘天文学',seeds=['https://www.piaotia.com/'],book=[r'^/bookinfo/\d+/\d+\.html$',r'^/html/\d+/\d+/?$'],chapter=[r'^/html/\d+/\d+/\d+\.html$'],nav=('/bookinfo/','/html/','/sort/','/top/','/quanben','/list/','articlelist')),
 'hetushu.com':dict(name='和图书',seeds=['https://www.hetushu.com/','https://www.hetushu.com/book/'],book=[r'^/book/\d+/index\.html$',r'^/book/\d+/?$'],chapter=[r'^/book/\d+/\d+\.html$'],nav=('/book/','/author/','/sort/','/top/','/quanben','/novel/')),
}

def now():return datetime.now(timezone.utc).isoformat(timespec='seconds')
def load(p,d):
 try:return json.loads(p.read_text(encoding='utf-8'))
 except Exception:return d
def save(p,o):
 p.parent.mkdir(parents=True,exist_ok=True);q=p.with_suffix(p.suffix+'.tmp');q.write_text(json.dumps(o,ensure_ascii=False,indent=2),encoding='utf-8');q.replace(p)
def clean(s):return re.sub(r'\s+',' ',s or '').strip()
def han(s):return bool(re.search(r'[\u3400-\u9fff]',s or ''))
def norm(s):return re.sub(r'[^0-9a-z\u3400-\u9fff]+','',unicodedata.normalize('NFKC',s or '').lower())
def family(u):
 h=(urlparse(u or '').hostname or '').lower()
 for x in ('www.','m.','wap.','read.'):
  if h.startswith(x):h=h[len(x):]
 return h
def canon(u):
 u=urldefrag(u or '')[0];p=urlparse(u)
 if p.scheme not in ('http','https'):return ''
 return p._replace(fragment='').geturl()
def stable(u):return 'ext-'+hashlib.sha1(u.encode('utf-8')).hexdigest()[:20]
def matches(path,arr):return any(re.search(x,path,re.I) for x in arr)
def same_site(a,b):return family(a)==family(b)
def is_audio(b):
 s=' '.join(str(b.get(k,'') or '') for k in ('title','kind','source_name','media')).lower();return b.get('media') not in ('',None,'text') or any(x in s for x in AUDIO)

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
  time.sleep(min(2**n,6))
 raise RuntimeError(str(last))

def bookish(url,profile):return matches(urlparse(url).path,profile['book'])
def chapterish(url,title,profile):return matches(urlparse(url).path,profile['chapter']) or bool(CHAPTER_TEXT.search(clean(title)))
def good_link(url):return bool(url) and not BAD_EXT.search(url) and not any(x in url.lower() for x in BAD_URL)

def links(soup,page,profile):
 books=[];chap=[];nav=[];sb=set();sc=set();sn=set()
 for a in soup.find_all('a',href=True):
  u=canon(urljoin(page,a.get('href','')));t=clean(a.get_text(' ',strip=True))
  if not good_link(u) or not same_site(page,u):continue
  if chapterish(u,t,profile):
   if u not in sc and t and len(t)<=180:sc.add(u);chap.append({'n':len(chap)+1,'title':t,'url':u})
   continue
  if bookish(u,profile):
   if u not in sb:sb.add(u);books.append(u)
   continue
  low=u.lower();path=urlparse(u).path
  if any(x in low for x in profile['nav']) or t in NAV_TEXT or re.search(r'(?:page|p)=\d+',urlparse(u).query,re.I) or (han(t) and len(t)<=50 and path.count('/')<=3):
   if u not in sn:sn.add(u);nav.append(u)
 return books,chap,nav

def title_of(soup):
 for sel in ('h1','.book-title','.bookname h1','.title h1','#book_name','h2'):
  n=soup.select_one(sel)
  if n:
   t=clean(n.get_text(' ',strip=True));
   if han(t) and len(t)<=120:return re.sub(r'[_\-|｜].*$','',t).strip()
 t=clean(soup.title.get_text(' ',strip=True)) if soup.title else ''
 return re.sub(r'[_\-|｜].*$','',t).strip()[:120]
def detect(soup,u,profile,chap):
 text=clean(soup.get_text(' ',strip=True));signals=sum((bool(re.search(r'作者\s*[:：]',text)),bool(re.search(r'内容简介|作品简介|小说简介|全文阅读',text)),len(chap)>=3,bool(re.search(r'字数|文章状态|写作状态|章节数',text))))
 return bookish(u,profile) or signals>=2

def parse_book(soup,u,profile,chap):
 full=clean(soup.get_text('\n',strip=True));title=title_of(soup)
 if not han(title) or len(title)<1:return None
 author=''
 for pat in (r'作者\s*[:：]?\s*([^\n|｜]{1,60})',r'作\s*者\s*[:：]?\s*([^\n|｜]{1,60})',r'([^\s]{1,40})\s*著'):
  m=re.search(pat,full)
  if m:author=clean(m.group(1));break
 author=re.sub(r'\s*(?:写作状态|文章状态|更新时间|分类|类型)[:：].*$','',author)[:60]
 kind='';m=re.search(r'(?:分类|类型|类\s*别)\s*[:：]?\s*([^\n|｜]{1,40})',full)
 if m:kind=clean(m.group(1))[:40]
 status='已完结' if re.search(r'已完结|已完成|完本|全书完',full) else ('连载中' if re.search(r'连载|更新中',full) else '')
 intro=''
 for sel in ('.book-intro','.bookintro','.intro','.book-desc','.description','#intro','.abstract','.bookinfo_intro'):
  n=soup.select_one(sel)
  if n:
   x=clean(n.get_text(' ',strip=True))
   if len(x)>=20:intro=x[:1000];break
 if not intro:
  m=re.search(r'(?:内容简介|作品简介|小说简介)\s*[:：]?\s*(.{20,1000}?)(?:目录|正文|最新章节|作者其他作品|$)',full,re.S)
  if m:intro=clean(m.group(1))[:1000]
 cover=''
 for sel,attr in (("meta[property='og:image']",'content'),("meta[name='twitter:image']",'content'),('img.cover','src'),('.cover img','src'),('.book-cover img','src'),('#bookimg img','src'),('.bookimg img','src')):
  n=soup.select_one(sel)
  if n:
   x=n.get(attr) or n.get('data-src') or n.get('data-original')
   if x:cover=urljoin(u,x);break
 return {'id':stable(u),'title':title,'author':author,'kind':kind,'status':status,'intro':intro,'cover':cover,'source_url':u,'source_host':family(u),'source_name':profile['name'],'source_names':[profile['name']],'sources':[u],'read_sources':[u],'media':'text','chapter_count':len(chap),'last_chapter':chap[-1]['title'] if chap else '','detail_complete':True,'indexed_at':now(),'catalog_only':False}

def dedupe_key(b):
 t=norm(b.get('title',''));a=norm(b.get('author',''))
 return t+'|'+a if t and a else (t+'|'+family(b.get('source_url','')) if t else b.get('source_url',''))
def merge(a,b):
 o=dict(a)
 for f in ('author','kind','status','intro','cover','last_chapter'):
  if b.get(f) and not o.get(f):o[f]=b[f]
 o['chapter_count']=max(int(o.get('chapter_count') or 0),int(b.get('chapter_count') or 0))
 for f,vals in (('sources',[a.get('source_url'),b.get('source_url')]+list(a.get('sources') or [])+list(b.get('sources') or [])),('read_sources',list(a.get('read_sources') or [])+list(b.get('read_sources') or [])+[b.get('source_url')]),('source_names',list(a.get('source_names') or [a.get('source_name')])+list(b.get('source_names') or [b.get('source_name')]))):
  o[f]=[x for i,x in enumerate(vals) if x and x not in vals[:i]]
 return o
def existing():
 out=[];idx=load(INDEX,{})
 for s in idx.get('shards',[]):out.extend(load(V5/s.get('file',''),{}).get('books',[]))
 if out:return out
 idx=load(V4/'index.json',{})
 for s in idx.get('shards',[]):out.extend(load(V4/s.get('file',''),{}).get('books',[]))
 return out

def write(rows,reports):
 d={}
 for b in rows:
  if is_audio(b) or not han(b.get('title','')):continue
  k=dedupe_key(b);d[k]=merge(d[k],b) if k in d else b
 rows=sorted(d.values(),key=lambda x:(x.get('indexed_at',''),x.get('title','')),reverse=True);BOOKS.mkdir(parents=True,exist_ok=True);keep=set();sh=[]
 for i in range(0,len(rows),SHARD):
  n=f'{i//SHARD+1:05d}.json';keep.add(n);save(BOOKS/n,{'books':rows[i:i+SHARD]});sh.append({'file':'books/'+n,'count':len(rows[i:i+SHARD])})
 for p in BOOKS.glob('*.json'):
  if p.name not in keep:p.unlink()
 counts={}
 for b in rows:
  names=b.get('source_names') or [b.get('source_name','未知')]
  for n in set(x for x in names if x):counts[n]=counts.get(n,0)+1
 save(INDEX,{'version':13,'generated_at':now(),'total':len(rows),'shard_size':SHARD,'shards':sh,'source_counts':counts,'source_status':reports})
 return len(rows),counts

def main():
 ap=argparse.ArgumentParser();ap.add_argument('--pages-per-source',type=int,default=120);args=ap.parse_args();V5.mkdir(parents=True,exist_ok=True);TOC.mkdir(parents=True,exist_ok=True)
 cfg=load(CFG,{'sources':[]});st=load(STATE,{'version':13,'sources':{}});errs=load(ERR,{'errors':[]}).get('errors',[]);rows=existing();by_url={b.get('source_url'):b for b in rows if b.get('source_url')};reports=[]
 for src in cfg.get('sources',[]):
  h=family(src.get('url',''));profile=P.get(h)
  if not profile or src.get('authorized') is not True or src.get('catalog_allowed') is not True:continue
  ss=st['sources'].setdefault(h,{'queue':list(profile['seeds']),'seen':[],'books_seen':0,'done':False,'blocked':False,'needs_adapter':False})
  if not ss.get('queue') and not ss.get('done'):ss['queue']=list(profile['seeds'])
  q=deque(ss.get('queue') or []);seen=set(ss.get('seen') or []);pages=ok=fail=found=added=tocs=0;limit=max(20,min(args.pages_per_source,500))
  while q and pages<limit:
   u=q.popleft()
   if u in seen:continue
   seen.add(u);pages+=1
   try:html=fetch(u,src.get('url'));soup=BeautifulSoup(html,'html.parser');ok+=1
   except Exception as e:
    fail+=1;errs.append({'time':now(),'source':profile['name'],'url':u,'error':str(e)[:400]});continue
   burls,chap,nav=links(soup,u,profile)
   if detect(soup,u,profile,chap):
    b=parse_book(soup,u,profile,chap)
    if b:
     found+=1;ss['books_seen']=int(ss.get('books_seen',0))+1
     if u not in by_url:rows.append(b);by_url[u]=b;added+=1
     else:by_url[u].update({k:v for k,v in b.items() if v not in ('',None,0,False,[])})
     if chap:save(TOC/f"{b['id']}.json",{'book_id':b['id'],'title':b['title'],'chapters':chap,'updated_at':now()});tocs+=1
   for x in burls+nav:
    if x not in seen and x not in q:q.append(x)
   time.sleep(max(.25,float(src.get('delay_seconds',.8))))
  cumulative=int(ss.get('books_seen',0));empty=not bool(q)
  ss['needs_adapter']=bool(empty and cumulative==0 and ok>0)
  ss['blocked']=bool(empty and cumulative==0 and ok==0 and fail>0)
  ss['done']=bool(empty and cumulative>0 and not ss['blocked'])
  if (ss['needs_adapter'] or ss['blocked']) and not q:ss['queue']=list(profile['seeds'])
  else:ss['queue']=list(q)
  ss['seen']=sorted(seen);ss['updated_at']=now()
  reports.append({'name':profile['name'],'host':h,'pages_this_run':pages,'successful_pages':ok,'failed_pages':fail,'books_found':found,'books_added':added,'toc_written':tocs,'books_seen_total':cumulative,'remaining_queue':len(q),'seen_pages':len(seen),'done':ss['done'],'blocked':ss['blocked'],'needs_adapter':ss['needs_adapter']})
 total,counts=write(rows,reports);save(STATE,st);save(ERR,{'generated_at':now(),'errors':errs[-2500:]})
 unresolved=[r for r in reports if not r['done']];status={'version':13,'generated_at':now(),'total_books':total,'source_counts':counts,'all_done':bool(reports) and not unresolved,'continue_needed':bool(unresolved),'unresolved_sources':[r['name'] for r in unresolved],'sources':reports};save(STATUS,status);print(json.dumps(status,ensure_ascii=False))
if __name__=='__main__':main()
