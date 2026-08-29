from __future__ import annotations
import html, ipaddress, json, re, socket, time
from datetime import datetime, timezone
from pathlib import Path
from urllib.parse import urljoin, urlparse
from urllib.request import Request, urlopen
from urllib.robotparser import RobotFileParser
from bs4 import BeautifulSoup

ROOT=Path(__file__).resolve().parents[1]
CFG=ROOT/'config'/'sources.json'; DATA=ROOT/'data'
STATE=DATA/'full_crawl_state.json'; DB=DATA/'full_catalog.json'; STATUS=DATA/'full_crawl_status.json'
UA='XiaoXiaoShuoPagesCrawler/2.0 (+authorized-full-catalog)'
IX_BOOK=re.compile(r'^/read/\d+/?$'); IT_BOOK=re.compile(r'^/youshengxiaoshuo/\d+/?$')

def clean(v): return re.sub(r'\s+',' ',v or '').strip()
def host(u): return (urlparse(u).hostname or '').lower()
def safe(u):
    p=urlparse(u)
    if p.scheme not in ('http','https') or not p.hostname: return False
    try: infos=socket.getaddrinfo(p.hostname,None,type=socket.SOCK_STREAM)
    except socket.gaierror: return False
    for x in infos:
        ip=ipaddress.ip_address(x[4][0])
        if ip.is_private or ip.is_loopback or ip.is_link_local or ip.is_reserved or ip.is_multicast or ip.is_unspecified: return False
    return True

def fetch(u):
    req=Request(u,headers={'User-Agent':UA,'Accept':'text/html,application/xhtml+xml'})
    with urlopen(req,timeout=25) as r:
        raw=r.read(); ct=r.headers.get('Content-Type','')
    m=re.search(r'charset=([\w-]+)',ct,re.I)
    for enc in ([m.group(1)] if m else [])+['utf-8','gb18030','gbk']:
        try:return raw.decode(enc)
        except Exception:pass
    return raw.decode('utf-8','replace')

def robots_ok(base,u):
    try:
        p=urlparse(base); rp=RobotFileParser(); rp.set_url(f'{p.scheme}://{p.netloc}/robots.txt'); rp.read(); return rp.can_fetch(UA,u)
    except Exception:return True

def book_links(page,base,kind):
    soup=BeautifulSoup(page,'html.parser'); out=[]; seen=set()
    pat=IX_BOOK if kind=='ixdzs' else IT_BOOK
    for a in soup.find_all('a',href=True):
        u=urljoin(base,a['href']).split('#')[0]; p=urlparse(u)
        if host(u)!=host(base) or not pat.match(p.path): continue
        if u not in seen: seen.add(u); out.append(u)
    return out

def meta_desc(soup):
    d=soup.find('meta',attrs={'name':re.compile('description',re.I)})
    return clean(d.get('content',''))[:600] if d else ''

def parse_book(u,kind):
    text=fetch(u); soup=BeautifulSoup(text,'html.parser'); full='\n'.join(soup.stripped_strings)
    h=soup.find('h1'); title=clean(h.get_text(' ',strip=True)) if h else clean(soup.title.get_text()).split('_')[0] if soup.title else u
    if kind=='itingshu': title=re.sub(r'有声小说$','',title).strip()
    ma=re.search(r'作者\s*[:：]?\s*([^\n|]{1,80})',full,re.I); author=clean(ma.group(1)) if ma else ''
    mk=re.search(r'(?:类型|分类)\s*[:：]?\s*([^\n|]{1,50})',full,re.I); category=clean(mk.group(1)) if mk else ''
    intro=meta_desc(soup)
    if not intro:
        candidates=[]
        for n in soup.select('.intro,.book-intro,.book-describe,.des,.description,#intro'):
            t=clean(n.get_text(' ',strip=True))
            if len(t)>30:candidates.append(t)
        intro=max(candidates,key=len,default='')[:600]
    img=soup.select_one('img.cover,.cover img,#fmimg img,.book-cover img')
    cover=urljoin(u,img.get('src')) if img and img.get('src') else ''
    chapter_count=0; last=''
    if kind=='ixdzs':
        m=re.search(r'目录\s*共\s*(\d+)\s*章',full); chapter_count=int(m.group(1)) if m else 0
        for a in soup.find_all('a',href=True):
            if re.search(r'/read/\d+/p\d+\.html$',urlparse(urljoin(u,a['href'])).path):
                t=clean(a.get_text(' ',strip=True))
                if t: last=last or t
        media='text'
    else:
        nums=[]
        for a in soup.find_all('a',href=True):
            m=re.search(r'/play/\d+_(\d+)_\.html$',urlparse(urljoin(u,a['href'])).path)
            if m:
                nums.append(int(m.group(1))); t=clean(a.get_text(' ',strip=True)); last=last or t
        if nums: chapter_count=max(nums)
        else:
            m=re.search(r'(?:连载至|更新[:：]?)\s*第?\s*(\d+)\s*集',full); chapter_count=int(m.group(1)) if m else 0
        media='audio-metadata'
    return {'title':title,'author':author,'kind':category,'intro':intro,'cover':cover,'source_url':u,'source_host':host(u),'chapter_count':chapter_count,'last_chapter':last,'media':media,'indexed_at':datetime.now(timezone.utc).isoformat(timespec='seconds')}

def loadj(path,default):
    try:return json.loads(path.read_text(encoding='utf-8'))
    except Exception:return default

def ixdzs_pages(st,n):
    cat=max(1,int(st.get('category',1))); page=max(1,int(st.get('page',1))); out=[]
    for _ in range(n):
        if cat>11: st.update({'done':True,'category':cat,'page':page}); break
        u=f'https://ixdzs8.com/sort/{cat}/' if page==1 else f'https://ixdzs8.com/sort/{cat}/index-0-0-0-{page}.html'
        out.append((u,cat,page)); page+=1
    st.update({'category':cat,'page':page}); return out

def itingshu_pages(st,n):
    page=max(1,int(st.get('page',1))); out=[]
    for _ in range(n):
        if page>3363: st.update({'done':True,'page':page}); break
        u='https://www.itingshu.net/yousheng/all.html' if page==1 else f'https://www.itingshu.net/yousheng/all/lastupdate/1/{page}.html'
        out.append((u,1,page)); page+=1
    st.update({'page':page}); return out

def build_page(rows):
    rows=sorted(rows,key=lambda x:x.get('indexed_at',''),reverse=True)
    cards=[]
    for b in rows:
        cover=html.escape(b.get('cover','')); title=html.escape(b.get('title','')); author=html.escape(b.get('author','')); intro=html.escape(b.get('intro','')); kind=html.escape(b.get('kind','')); url=html.escape(b.get('source_url','')); cnt=int(b.get('chapter_count') or 0); media='文本' if b.get('media')=='text' else '有声目录'
        cards.append(f'<article class="book-card"><img src="{cover}" loading="lazy"><div><h3>{title}</h3><p>{author} · {kind} · {media} · {cnt}章/集</p><p>{intro}</p><a href="{url}" rel="nofollow">源站详情</a></div></article>')
    body=''.join(cards)
    ROOT.joinpath('all-catalog.html').write_text(f'''<!doctype html><html lang="zh-CN"><head><meta charset="utf-8"><meta name="viewport" content="width=device-width,initial-scale=1"><title>小小说 · 全站索引库</title><style>body{{font-family:system-ui,"PingFang SC";max-width:1100px;margin:auto;padding:28px;background:#f5f3ec;color:#1b2823}}.book-card{{display:grid;grid-template-columns:90px 1fr;gap:18px;background:#fff;border:1px solid #e5e0d5;border-radius:18px;padding:16px;margin:12px 0}}img{{width:90px;height:120px;object-fit:cover;border-radius:10px;background:#eee}}h3{{margin:0 0 7px}}p{{color:#68736e}}a{{color:#176b55}}@media(max-width:600px){{.book-card{{grid-template-columns:65px 1fr}}img{{width:65px;height:90px}}}}</style></head><body><h1>小小说 · 全站索引库</h1><p>已索引 {len(rows)} 本。爱下电子书按文本小说索引；爱听书按公开有声书目录索引。安全验证/403 内容不会绕过。</p>{body}</body></html>''',encoding='utf-8')

def main():
    DATA.mkdir(parents=True,exist_ok=True)
    cfg=loadj(CFG,{'sources':[]}); state=loadj(STATE,{'sources':{}}); old=loadj(DB,{'books':[]}); byurl={x.get('source_url'):x for x in old.get('books',[]) if x.get('source_url')}
    report=[]
    for s in cfg.get('sources',[]):
        u=str(s.get('url') or '').strip(); name=str(s.get('name') or u); h=host(u)
        if s.get('authorized') is not True: report.append({'name':name,'status':'跳过：未授权'}); continue
        if not safe(u): report.append({'name':name,'status':'跳过：地址解析失败'}); continue
        if h.endswith('ixdzs8.com'): kind='ixdzs'
        elif h.endswith('itingshu.net'): kind='itingshu'
        else: report.append({'name':name,'status':'当前全站适配器未支持'}); continue
        ss=state.setdefault('sources',{}).setdefault(h,{})
        count=max(1,min(int(s.get('catalog_pages_per_run',60)),200)); delay=max(.35,float(s.get('delay_seconds',.8)))
        pages=ixdzs_pages(ss,count) if kind=='ixdzs' else itingshu_pages(ss,count)
        pages_ok=books_seen=books_new=errors=0
        for page_url,cat,page_no in pages:
            if not robots_ok(u,page_url): errors+=1; continue
            try: raw=fetch(page_url); urls=book_links(raw,page_url,kind)
            except Exception: errors+=1; continue
            if not urls:
                if kind=='ixdzs': ss.update({'category':cat+1,'page':1})
                else: ss['done']=True
                continue
            pages_ok+=1; books_seen+=len(urls)
            for bu in urls:
                if bu in byurl: continue
                try:
                    byurl[bu]=parse_book(bu,kind); books_new+=1
                except Exception: errors+=1
                time.sleep(delay)
        report.append({'name':name,'status':f'本轮页面 {pages_ok}/{len(pages)}，发现 {books_seen}，新增 {books_new}，错误 {errors}','cursor':dict(ss)})
    rows=list(byurl.values()); DB.write_text(json.dumps({'books':rows},ensure_ascii=False,indent=2),encoding='utf-8'); STATE.write_text(json.dumps(state,ensure_ascii=False,indent=2),encoding='utf-8'); build_page(rows)
    STATUS.write_text(json.dumps({'generated_at':datetime.now(timezone.utc).isoformat(timespec='seconds'),'books':len(rows),'sources':report},ensure_ascii=False,indent=2),encoding='utf-8')
    print(json.dumps({'books':len(rows),'sources':report},ensure_ascii=False))
if __name__=='__main__': main()
