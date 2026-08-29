from __future__ import annotations
import html, ipaddress, json, re, socket, time
from datetime import datetime, timezone
from pathlib import Path
from urllib.error import HTTPError, URLError
from urllib.parse import urljoin, urlparse
from urllib.request import Request, urlopen
from urllib.robotparser import RobotFileParser
from bs4 import BeautifulSoup

ROOT=Path(__file__).resolve().parents[1]; DATA=ROOT/'data'; CFG=ROOT/'config'/'sources.json'
DB=DATA/'full_catalog.json'; STATE=DATA/'full_crawl_state.json'; STATUS=DATA/'full_crawl_status.json'; ERR=DATA/'crawl_errors.json'
UA='Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 Chrome/139 Safari/537.36'
HEAD={'User-Agent':UA,'Accept':'text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8','Accept-Language':'zh-CN,zh;q=0.9','X-XiaoXiaoShuo-Crawler':'authorized-indexer/3.0'}
IX_BOOK=re.compile(r'^/read/(\d+)/?$',re.I); IT_BOOK=re.compile(r'^/youshengxiaoshuo/(\d+)/?$',re.I)
IX_CH=re.compile(r'^/read/(\d+)/p(\d+)\.html$',re.I); IT_CH=re.compile(r'^/play/(\d+)_(\d+)_?\.html$',re.I)
IX_CATS=[1,2,3,4,5,6,7,8,9,10,0]; CHALLENGE=('正在验证浏览器','安全验证','challenge=','captcha','cf-chl-')
ROBOTS={}

def now(): return datetime.now(timezone.utc).isoformat(timespec='seconds')
def clean(v): return re.sub(r'\s+',' ',v or '').strip()
def host(u): return (urlparse(u).hostname or '').lower()
def load(p,d):
 try:return json.loads(p.read_text(encoding='utf-8'))
 except Exception:return d
def save(p,o):
 p.parent.mkdir(parents=True,exist_ok=True); t=p.with_suffix(p.suffix+'.tmp'); t.write_text(json.dumps(o,ensure_ascii=False,indent=2),encoding='utf-8'); t.replace(p)
def safe(u):
 p=urlparse(u)
 if p.scheme not in ('http','https') or not p.hostname:return False
 try: infos=socket.getaddrinfo(p.hostname,None,type=socket.SOCK_STREAM)
 except socket.gaierror:return False
 for x in infos:
  ip=ipaddress.ip_address(x[4][0])
  if ip.is_private or ip.is_loopback or ip.is_link_local or ip.is_reserved or ip.is_multicast or ip.is_unspecified:return False
 return True

def decode(raw,ct):
 m=re.search(r'charset=([\w.-]+)',ct or '',re.I)
 for enc in ([m.group(1)] if m else [])+['utf-8','gb18030','gbk']:
  try:return raw.decode(enc)
  except Exception:pass
 return raw.decode('utf-8','replace')

def fetch(u,referer=None,retries=3):
 last=None
 for i in range(retries):
  h=dict(HEAD)
  if referer:h['Referer']=referer
  try:
   with urlopen(Request(u,headers=h),timeout=25) as r:
    s=decode(r.read(),r.headers.get('Content-Type',''))
    if len(s)<80: raise RuntimeError('response too short')
    return s
  except HTTPError as e:
   last=e
   if e.code not in (408,425,429,500,502,503,504):break
  except (URLError,TimeoutError,OSError,RuntimeError) as e:last=e
  time.sleep(min(2**i,5))
 raise RuntimeError(f'fetch failed: {u}: {last}')

def robots_ok(base,u):
 h=host(base)
 if h not in ROBOTS:
  p=urlparse(base); rp=RobotFileParser(); rp.set_url(f'{p.scheme}://{p.netloc}/robots.txt')
  try:rp.read();ROBOTS[h]=rp
  except Exception:ROBOTS[h]=None
 return True if ROBOTS[h] is None else ROBOTS[h].can_fetch('*',u)
def challenged(s):
 x=s.lower(); return any(k.lower() in x for k in CHALLENGE)
def links(soup,base):
 for a in soup.find_all('a',href=True):yield a,urljoin(base,a.get('href','')).split('#')[0]

def parse_author(s):
 m=re.search(r'作者\s*[:：]\s*([^\n|]{1,100})',s,re.I)
 if not m:return ''
 return re.split(r'\s+(?:演播|状态|更新|类型|分类)\s*[:：]',clean(m.group(1)),maxsplit=1)[0][:80]
def parse_kind(s):
 m=re.search(r'(?:类型|分类)\s*[:：]\s*([^\n|]{1,80})',s,re.I)
 return re.split(r'\s+(?:状态|作者|演播|更新)\s*[:：]',clean(m.group(1)),maxsplit=1)[0][:60] if m else ''
def cover(soup,base):
 for sel in ('img.cover','.cover img','#fmimg img','.book-cover img','img[alt*="封面"]'):
  n=soup.select_one(sel)
  if n and n.get('src'):return urljoin(base,n.get('src'))
 n=soup.find('meta',attrs={'property':'og:image'}); return urljoin(base,n.get('content')) if n and n.get('content') else ''
def meta_desc(soup):
 for attrs in ({'name':re.compile('description',re.I)},{'property':'og:description'}):
  n=soup.find('meta',attrs=attrs)
  if n and clean(n.get('content','')):return clean(n.get('content',''))[:700]
 return ''

def catalog_items(text,base,kind):
 soup=BeautifulSoup(text,'html.parser'); pat=IX_BOOK if kind=='ix' else IT_BOOK; groups={}
 for a,u in links(soup,base):
  if host(u)!=host(base) or not pat.match(urlparse(u).path):continue
  g=groups.setdefault(u,{'labels':[],'nodes':[]}); lab=clean(a.get_text(' ',strip=True))
  if lab:g['labels'].append(lab)
  g['nodes'].append(a)
 rows=[]
 for u,g in groups.items():
  def score(v):return (not bool(re.fullmatch(r'[\d.]+(?:万)?',v)) and not bool(re.search(r'^(?:连载至|更新|最新|第\d+[章节集]?)',v)),len(v))
  title=max(g['labels'],key=score,default=u); node=max(g['nodes'],key=lambda n:len(clean(n.get_text(' ',strip=True))),default=None); parent=None
  if node:
   for x in node.parents:
    if getattr(x,'name','') in ('li','div','article','dd','section'):
     t=clean(x.get_text(' ',strip=True))
     if 20<=len(t)<=2500:parent=x;break
  raw=clean(parent.get_text(' ',strip=True)) if parent else title; cnt=0
  if kind=='it':
   m=re.search(r'(?:连载至)?第?\s*(\d+)\s*集',raw);cnt=int(m.group(1)) if m else 0
  img=''
  if parent:
   n=parent.find('img'); img=urljoin(base,n.get('src')) if n and n.get('src') else ''
  rows.append({'title':title,'author':parse_author(raw),'kind':parse_kind(raw),'status':'已完结' if '已完结' in raw or re.search(r'\b完结\b',raw) else ('连载中' if '连载' in raw else ''),'intro':'','cover':img,'source_url':u,'source_host':host(u),'chapter_count':cnt,'last_chapter':'','media':'text' if kind=='ix' else 'audio-metadata','content_access':'not-yet-enriched','detail_complete':False,'indexed_at':now()})
 return rows

def detail(u,kind,referer):
 text=fetch(u,referer)
 if challenged(text):raise RuntimeError('detail page returned browser challenge')
 soup=BeautifulSoup(text,'html.parser'); full='\n'.join(clean(x) for x in soup.stripped_strings if clean(x)); h=soup.find('h1'); title=clean(h.get_text(' ',strip=True)) if h else u
 if kind=='it':title=re.sub(r'有声小说$','',title).strip()
 intro=meta_desc(soup); cnt=0; last=''; first=''; download=''; narrator=''
 if kind=='ix':
  m=re.search(r'目录\s*共\s*(\d+)\s*章',full);cnt=int(m.group(1)) if m else 0
  m=re.search(r'内容简介\s*(.+?)\s*目录\s*共',full,re.S);intro=clean(m.group(1))[:700] if m else intro
  for a,x in links(soup,u):
   lab=clean(a.get_text(' ',strip=True)); cm=IX_CH.match(urlparse(x).path)
   if cm and not last:last=lab
   if lab=='立即阅读':first=x
   if x.lower().endswith('.zip') or 'TXT下载' in lab:download=x
  access='challenge-protected' if first else 'unknown'
 else:
  nums=[]
  for a,x in links(soup,u):
   m=IT_CH.match(urlparse(x).path)
   if m:nums.append(int(m.group(2)));last=last or clean(a.get_text(' ',strip=True))
  if nums:cnt=max(nums)
  if not cnt:
   m=re.search(r'(?:连载至|更新\s*[:：]?)\s*第?\s*(\d+)\s*集',full);cnt=int(m.group(1)) if m else 0
  m=re.search(r'演播\s*[:：]\s*([^\n|]{1,160})',full);narrator=clean(m.group(1))[:160] if m else ''
  access='metadata-only'
 return {'title':title,'author':parse_author(full),'narrator':narrator,'kind':parse_kind(full),'status':'已完结' if '已完结' in full[:1200] or re.search(r'\b完结\b',full[:1200]) else ('连载中' if '连载' in full[:1200] else ''),'intro':intro,'cover':cover(soup,u),'source_url':u,'source_host':host(u),'chapter_count':cnt,'last_chapter':last,'first_chapter_url':first,'download_url':download,'media':'text' if kind=='ix' else 'audio-metadata','content_access':access,'detail_complete':True,'indexed_at':now()}

def page_max(text,kind):
 if kind=='it':
  m=re.search(r'共\s*(\d+)\s*页',text)
  if m:return int(m.group(1))
 soup=BeautifulSoup(text,'html.parser'); vals=[]
 for _,u in links(soup,'https://placeholder.invalid/'):
  p=urlparse(u).path; m=re.search(r'index-\d+-\d+-\d+-(\d+)\.html$',p) if kind=='ix' else re.search(r'/lastupdate/\d+/(\d+)\.html$',p)
  if m:vals.append(int(m.group(1)))
 return max(vals) if vals else None
def ix_url(cat,page):return f'https://ixdzs8.com/sort/{cat}/' if page==1 else f'https://ixdzs8.com/sort/{cat}/index-0-0-0-{page}.html'
def it_url(page):return 'https://www.itingshu.net/yousheng/all.html' if page==1 else f'https://www.itingshu.net/yousheng/all/lastupdate/1/{page}.html'
def err(log,name,url,stage,e):
 log.append({'time':now(),'source':name,'url':url,'stage':stage,'error':str(e)[:500]})
 if len(log)>300:del log[:-300]

def build(rows):
 rows=sorted(rows,key=lambda x:(x.get('indexed_at',''),x.get('title','')),reverse=True); cards=[]
 for b in rows:
  q=lambda x:html.escape(str(x or '')); media='文本小说' if b.get('media')=='text' else '有声书目录'
  cards.append(f'<article class="book-card"><img src="{q(b.get("cover"))}" loading="lazy"><div><h3>{q(b.get("title"))}</h3><p>{q(b.get("author"))} · {q(b.get("kind"))} · {q(b.get("status"))} · {media} · {int(b.get("chapter_count") or 0)}章/集</p><p>{q(b.get("intro"))}</p><a href="{q(b.get("source_url"))}" rel="nofollow">源站详情</a></div></article>')
 ROOT.joinpath('all-catalog.html').write_text(f'<!doctype html><html lang="zh-CN"><head><meta charset="utf-8"><meta name="viewport" content="width=device-width,initial-scale=1"><title>小小说全站索引</title><style>body{{font-family:system-ui,"PingFang SC";max-width:1100px;margin:auto;padding:28px;background:#f5f3ec;color:#1b2823}}.book-card{{display:grid;grid-template-columns:90px 1fr;gap:18px;background:#fff;border:1px solid #e5e0d5;border-radius:18px;padding:16px;margin:12px 0}}img{{width:90px;height:120px;object-fit:cover;border-radius:10px;background:#eee}}p{{color:#68736e;line-height:1.6}}a{{color:#176b55}}</style></head><body><h1>小小说 · 全站索引库</h1><p>已索引 {len(rows)} 本；详情或章节遇到安全验证时会保留目录记录，不再整本丢弃。</p>{"".join(cards)}</body></html>',encoding='utf-8')

def main():
 DATA.mkdir(parents=True,exist_ok=True); cfg=load(CFG,{'sources':[]}); state=load(STATE,{'version':3,'sources':{}}); old=load(DB,{'books':[]}); elog=load(ERR,{'errors':[]}).get('errors',[]); by={b.get('source_url'):b for b in old.get('books',[]) if b.get('source_url')}; report=[]
 for src in cfg.get('sources',[]):
  base=str(src.get('url') or '').strip(); name=str(src.get('name') or base); h=host(base)
  if src.get('authorized') is not True:report.append({'name':name,'status':'跳过：未授权'});continue
  if not safe(base):report.append({'name':name,'status':'跳过：地址解析失败'});continue
  kind='ix' if h.endswith('ixdzs8.com') else ('it' if h.endswith('itingshu.net') else '')
  if not kind:report.append({'name':name,'status':'未适配'});continue
  st=state.setdefault('sources',{}).setdefault(h,{'page':1,'category_index':0}); pages=max(1,min(int(src.get('catalog_pages_per_run',20)),80)); enrich=max(0,min(int(src.get('detail_pages_per_run',40)),200)); delay=max(.35,float(src.get('delay_seconds',.7))); ok=attempt=seen=new=upd=ce=de=used=0
  while attempt<pages and not st.get('done'):
   if kind=='ix':
    ci=int(st.get('category_index',0));page=int(st.get('page',1))
    if ci>=len(IX_CATS):st['done']=True;break
    cat=IX_CATS[ci];url=ix_url(cat,page)
   else:
    page=int(st.get('page',1));total=int(st.get('total_pages',3363))
    if page>total:st['done']=True;break
    url=it_url(page);cat=None
   attempt+=1
   if not robots_ok(base,url):ce+=1;err(elog,name,url,'robots','robots denied');break
   try:
    raw=fetch(url,base)
    if challenged(raw):raise RuntimeError('catalog returned browser challenge')
    items=catalog_items(raw,url,kind);mx=page_max(raw,kind)
   except Exception as e:ce+=1;err(elog,name,url,'catalog',e);break
   if items:ok+=1;seen+=len(items)
   for basic in items:
    bu=basic['source_url'];prev=by.get(bu)
    if prev is None:by[bu]=basic;prev=basic;new+=1
    if used<enrich and (src.get('refresh_existing') is True or not prev.get('detail_complete')):
     used+=1
     try:by[bu]={**prev,**detail(bu,kind,url)};upd+=1
     except Exception as e:de+=1;err(elog,name,bu,'detail',e)
     time.sleep(delay)
   if kind=='ix':
    if (mx and page>=mx) or (not items and page>1):st['category_index']=int(st.get('category_index',0))+1;st['page']=1
    else:st['page']=page+1
   else:
    if mx:st['total_pages']=mx
    if not items and page>1:st['done']=True
    else:st['page']=page+1
   st['updated_at']=now();time.sleep(delay)
  report.append({'name':name,'url':base,'status':f'页面 {ok}/{attempt}，发现 {seen}，新增 {new}，详情成功 {upd}，目录错误 {ce}，详情错误 {de}','cursor':dict(st)})
 rows=list(by.values());save(DB,{'generated_at':now(),'books':rows});save(STATE,state);save(ERR,{'generated_at':now(),'errors':elog});build(rows);save(STATUS,{'generated_at':now(),'books':len(rows),'sources':report,'error_count':len(elog),'message':'全站索引更新完成' if rows else '本轮仍为 0 本，请查看 crawl_errors.json'});print(json.dumps({'books':len(rows),'sources':report,'errors':len(elog)},ensure_ascii=False))
if __name__=='__main__':main()
