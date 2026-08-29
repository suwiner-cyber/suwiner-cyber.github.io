from __future__ import annotations
import hashlib, html, ipaddress, json, re, socket, time
from datetime import datetime, timezone
from pathlib import Path
from urllib.parse import urljoin, urlparse
from urllib.request import Request, urlopen
from urllib.robotparser import RobotFileParser
from bs4 import BeautifulSoup

ROOT=Path(__file__).resolve().parents[1]; CFG=ROOT/'config'/'sources.json'; DATA=ROOT/'data'; BDIR=ROOT/'books'; CDIR=ROOT/'chapters'
UA='XiaoXiaoShuoPagesCrawler/1.0 (+authorized-static-library)'
CH=re.compile(r'(?:第\s*[0-9一二三四五六七八九十百千万零〇两]+\s*[章节回卷集篇部]|chapter\s*\d+)',re.I)
BOOK=re.compile(r'/(?:book|novel|info|detail|xiaoshuo|shu)(?:/|_|-|\?|$)',re.I); CHP=re.compile(r'/(?:chapter|chap|read|content|view)(?:/|_|-|\?|$)',re.I)
NEXT=re.compile(r'下一页|下页|next|more',re.I); TOC=re.compile(r'目录|章节|全部章节|catalog|chapters',re.I); SKIP=re.compile(r'\.(?:jpg|png|gif|webp|svg|css|js|mp3|mp4|zip|rar|pdf|apk)(?:$|\?)',re.I)

def clean(s): return re.sub(r'\s+',' ',s or '').strip()
def sid(s): return hashlib.sha1(s.encode()).hexdigest()[:16]
def same(a,b): return (urlparse(a).hostname or '').lower()==(urlparse(b).hostname or '').lower()
def safe(url):
 p=urlparse(url)
 if p.scheme not in ('http','https') or not p.hostname:return False
 try: infos=socket.getaddrinfo(p.hostname,None,type=socket.SOCK_STREAM)
 except socket.gaierror:return False
 for x in infos:
  ip=ipaddress.ip_address(x[4][0])
  if ip.is_private or ip.is_loopback or ip.is_link_local or ip.is_reserved or ip.is_multicast or ip.is_unspecified:return False
 return True

def fetch(url):
 req=Request(url,headers={'User-Agent':UA,'Accept':'text/html,application/xhtml+xml'})
 with urlopen(req,timeout=20) as r: raw=r.read(); ct=r.headers.get('Content-Type','')
 m=re.search(r'charset=([\w-]+)',ct,re.I); encs=([m.group(1)] if m else [])+['utf-8','gb18030','gbk']
 for e in encs:
  try:return raw.decode(e)
  except Exception:pass
 return raw.decode('utf-8','replace')

def robots(base,url):
 try:
  p=urlparse(base); rp=RobotFileParser(); rp.set_url(f'{p.scheme}://{p.netloc}/robots.txt'); rp.read(); return rp.can_fetch(UA,url)
 except Exception:return True

def links(soup,base):
 out=[]
 for a in soup.find_all('a',href=True):
  u=urljoin(base,a['href']).split('#')[0]
  if same(base,u) and not SKIP.search(u):out.append((clean(a.get_text(' ',strip=True)),u))
 return out

def title(soup,url):
 for sel in ('h1','.book-name','.bookname','.title','#info h1'):
  n=soup.select_one(sel)
  if n and clean(n.get_text()):return clean(n.get_text())
 return clean(soup.title.get_text()).split('_')[0].split('-')[0] if soup.title else url

def meta(soup,base):
 txt='\n'.join(soup.stripped_strings); author=kind=intro=cover=''
 m=re.search(r'(?:作者|author)\s*[:：]?\s*([^|\n]{1,60})',txt,re.I); author=clean(m.group(1)) if m else ''
 m=re.search(r'(?:分类|类型|题材|genre|category)\s*[:：]?\s*([^|\n]{1,40})',txt,re.I); kind=clean(m.group(1)) if m else ''
 d=soup.find('meta',attrs={'name':re.compile('description',re.I)}); intro=clean(d.get('content',''))[:500] if d else ''
 i=soup.select_one('img.cover,.cover img,#fmimg img,.book-cover img'); cover=urljoin(base,i.get('src')) if i and i.get('src') else ''
 return author,kind,intro,cover

def ischap(t,u): return bool(CH.search(t) or CHP.search(urlparse(u).path))
def discover(entry,max_pages,max_books,delay):
 q=[entry]; seen=set(); out=[]; have=set()
 while q and len(seen)<max_pages and len(out)<max_books:
  u=q.pop(0)
  if u in seen or not same(entry,u):continue
  seen.add(u)
  if not robots(entry,u):continue
  try:s=BeautifulSoup(fetch(u),'html.parser')
  except Exception:continue
  for t,v in links(s,u):
   if ischap(t,v):continue
   if BOOK.search(urlparse(v).path):
    if v not in have:have.add(v);out.append(v)
   elif v not in seen and len(q)+len(seen)<max_pages:q.append(v)
  time.sleep(delay)
 return out

def book(url,delay,max_toc,max_ch):
 s=BeautifulSoup(fetch(url),'html.parser'); a,k,i,c=meta(s,url); ll=links(s,url); direct=[(t,u) for t,u in ll if ischap(t,u)]; tc=[u for t,u in ll if TOC.search(t)]
 q=[tc[0] if tc else url]; seen=set(); have=set(); ch=[]
 while q and len(seen)<max_toc and len(ch)<max_ch:
  u=q.pop(0)
  if u in seen:continue
  seen.add(u)
  try:x=BeautifulSoup(fetch(u),'html.parser')
  except Exception:continue
  for t,v in links(x,u):
   if ischap(t,v) and v not in have:have.add(v);ch.append({'title':t or f'第{len(ch)+1}章','url':v})
   elif NEXT.search(t) and v not in seen:q.append(v)
  time.sleep(delay)
 if not ch:ch=[{'title':t,'url':u} for t,u in direct]
 return {'title':title(s,url),'author':a,'kind':k,'intro':i,'cover':c,'book_url':url,'chapters':ch}

def content(url):
 s=BeautifulSoup(fetch(url),'html.parser')
 for x in s(['script','style','nav','footer','header','form']):x.decompose()
 ns=[s.select_one(z) for z in ('#content','.content','.chapter-content','.read-content','article','#chaptercontent','.txtnav')]; ns=[n for n in ns if n]
 n=max(ns,key=lambda z:len(clean(z.get_text(' ',strip=True))),default=s.body or s)
 return '\n'.join(clean(x) for x in n.stripped_strings if clean(x))[:2000000]

def build(books,states):
 DATA.mkdir(parents=True,exist_ok=True);BDIR.mkdir(parents=True,exist_ok=True);CDIR.mkdir(parents=True,exist_ok=True); rows=[]; total=withc=0
 for b in books:
  bid=sid(b['book_url']); last=b['chapters'][-1]['title'] if b['chapters'] else ''; ls=[]
  for ch in b['chapters']:
   cid=sid(ch['url']); f=CDIR/f'{cid}.html'; txt=''
   if not f.exists():
    try:txt=content(ch['url'])
    except Exception:txt=''
    if txt:
     ps=''.join(f'<p>{html.escape(p)}</p>' for p in txt.split('\n') if p.strip()); f.write_text(f'<!doctype html><html><head><meta charset="utf-8"></head><body><h1>{html.escape(ch["title"])}</h1><div id="content">{ps}</div></body></html>',encoding='utf-8')
   if f.exists():withc+=1
   ls.append(f'<li><a class="chapter-link" href="../chapters/{cid}.html">{html.escape(ch["title"])}</a></li>')
  total+=len(b['chapters']); page=f'books/{bid}.html'; (BDIR/f'{bid}.html').write_text(f'<!doctype html><html><head><meta charset="utf-8"></head><body><h1>{html.escape(b["title"])}</h1><div class="author">{html.escape(b["author"])}</div><div class="kind">{html.escape(b["kind"])}</div><img class="cover" src="{html.escape(b["cover"])}"><div class="intro">{html.escape(b["intro"])}</div><div class="latest">{html.escape(last)}</div><a class="toc-link" href="#chapters">目录</a><div id="chapters"><ul>{"".join(ls)}</ul></div></body></html>',encoding='utf-8')
  rows.append({'title':b['title'],'author':b['author'],'kind':b['kind'],'intro':b['intro'],'cover':b['cover'],'last_chapter':last,'page':page})
 DATA.joinpath('books.json').write_text(json.dumps({'books':rows},ensure_ascii=False,indent=2),encoding='utf-8')
 cards=''.join(f'<div class="book-card"><img src="{html.escape(x["cover"])}"><div class="book-name">{html.escape(x["title"])}</div><div class="book-author">{html.escape(x["author"])}</div><div class="book-intro">{html.escape(x["intro"])}</div><div class="book-kind">{html.escape(x["kind"])}</div><div class="book-last">{html.escape(x["last_chapter"])}</div><a class="book-link" href="{html.escape(x["page"])}">打开</a></div>' for x in rows)
 ROOT.joinpath('catalog.html').write_text(f'<!doctype html><html lang="zh-CN"><head><meta charset="utf-8"></head><body><main id="books">{cards}</main></body></html>',encoding='utf-8')
 st={'generated_at':datetime.now(timezone.utc).isoformat(timespec='seconds'),'message':'更新完成' if states else '部署完成，等待添加授权小说站。','books':len(rows),'chapters':total,'chapters_with_content':withc,'sources':states}; DATA.joinpath('status.json').write_text(json.dumps(st,ensure_ascii=False,indent=2),encoding='utf-8')

def main():
 cfg=json.loads(CFG.read_text(encoding='utf-8')); allb=[]; states=[]
 for s in cfg.get('sources',[]):
  name=str(s.get('name') or s.get('url') or 'source'); u=str(s.get('url') or '').strip()
  if s.get('authorized') is not True:states.append({'name':name,'url':u,'status':'跳过：未授权'});continue
  if not safe(u):states.append({'name':name,'url':u,'status':'跳过：地址不安全或无法解析'});continue
  d=max(.2,float(s.get('delay_seconds',.5)))
  try:
   urls=discover(u,int(s.get('max_pages',80)),int(s.get('max_books',5000)),d); n=0
   for bu in urls:
    try:
     b=book(bu,d,int(s.get('max_toc_pages',100)),int(s.get('max_chapters',10000)))
     if b['chapters']:allb.append(b);n+=1
    except Exception:pass
   states.append({'name':name,'url':u,'status':f'完成：{n} 本'})
  except Exception as e:states.append({'name':name,'url':u,'status':'失败：'+str(e)[:100]})
 build(list({b['book_url']:b for b in allb}.values()),states)
if __name__=='__main__':main()
