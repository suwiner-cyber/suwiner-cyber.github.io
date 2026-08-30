from __future__ import annotations
import re,time
from urllib.error import HTTPError,URLError
from urllib.request import Request,urlopen
import mirror_text_universal as core

MOJIBAKE_RE=re.compile(r'(?:\ufffd{1,}|锟斤拷|���|□□|鈥|馃|鎴|銆|闂|鐨|鏄)')
COMMON=set('的了是在我有和不人这中大为上个国到说们地也子时道出而要于就下得可你年生自会那后能对着事其里所去行过家十用发天如然作方成者多日都三小军二无同么经法当起与好看学进种将还分此心前面又定见只主没公从文已外情高两')


def enc_label(v):
    x=(v or '').strip().lower().replace('"','').replace("'",'')
    if x in {'gb2312','gb_2312','x-gbk','cp936','ms936'}: return 'gb18030'
    if x in {'big5-hkscs','big5hkscs'}: return 'big5'
    if x in {'utf8','utf_8'}: return 'utf-8'
    return x


def sniff_meta(raw:bytes):
    head=raw[:16384].decode('latin1','ignore')
    m=re.search(r'<meta[^>]+charset\s*=\s*["\']?\s*([a-z0-9._-]+)',head,re.I)
    if not m:
        m=re.search(r'<meta[^>]+content\s*=\s*["\'][^"\']*charset\s*=\s*([a-z0-9._-]+)',head,re.I)
    return enc_label(m.group(1)) if m else ''


def score_text(s:str):
    if not s: return -10**12
    rep=s.count('\ufffd')
    ctrl=sum(1 for ch in s if ord(ch)<32 and ch not in '\r\n\t')
    han=sum(1 for ch in s if '\u3400'<=ch<='\u9fff')
    common=sum(1 for ch in s if ch in COMMON)
    moj=len(MOJIBAKE_RE.findall(s))
    return han*2+common*3-rep*250-ctrl*40-moj*20


def smart_decode(raw:bytes,ct:str,url:str):
    m=re.search(r'charset=([\w.-]+)',ct or '',re.I)
    header=enc_label(m.group(1)) if m else ''
    meta=sniff_meta(raw)
    cht='/cht/' in (url or '').lower()
    encs=[header,meta,'utf-8']+(['big5','gb18030','gbk'] if cht else ['gb18030','gbk','big5'])
    seen=[]
    for e in encs:
        e=enc_label(e)
        if e and e not in seen: seen.append(e)
    best='';best_score=-10**18
    for e in seen:
        try:
            s=raw.decode(e,'strict')
        except Exception:
            continue
        sc=score_text(s)+(50000 if e==header and header else 0)+(80000 if e==meta and meta else 0)
        if sc>best_score:
            best,best_score=s,sc
    if best:
        return best
    for e in seen:
        try:
            s=raw.decode(e,'replace')
        except Exception:
            continue
        sc=score_text(s)
        if sc>best_score:
            best,best_score=s,sc
    return best or raw.decode('utf-8','replace')


def fetch(u,ref=None):
    last=None
    for n in range(4):
        h=dict(core.HEAD)
        h['X-XiaoXiaoShuo-Crawler']='authorized-text-mirror/6.0'
        if ref: h['Referer']=ref
        try:
            with urlopen(Request(u,headers=h),timeout=30) as r:
                raw=r.read();ct=r.headers.get('Content-Type','')
            return smart_decode(raw,ct,u)
        except HTTPError as e:
            last=e
            if e.code not in (408,425,429,500,502,503,504): break
        except (URLError,TimeoutError,OSError) as e:
            last=e
        time.sleep(min(2**n,5))
    raise RuntimeError(str(last))


def garbled(s):
    s=str(s or '')
    if not s: return False
    rep=s.count('\ufffd')
    bad=len(MOJIBAKE_RE.findall(s))
    ctrl=sum(1 for ch in s if ord(ch)<32 and ch not in '\r\n\t')
    return rep>=2 or bad>=2 or ctrl>=8 or (rep>0 and rep/max(1,len(s))>.001)


def purge_corrupt_cache():
    st=core.load(core.STATE,{'books':{},'book_cursor':0})
    affected=removed=0
    if not core.OUT.exists(): return 0,0
    for p in core.OUT.glob('*.json'):
        doc=core.load(p,{})
        chapters=doc.get('chapters',[]) if isinstance(doc,dict) else []
        if not chapters: continue
        good=[x for x in chapters if not garbled(x.get('content',''))]
        if len(good)==len(chapters): continue
        removed+=len(chapters)-len(good);affected+=1
        doc['chapters']=good;doc['complete']=False;doc['encoding_repair_required']=True;doc['updated_at']=core.now()
        core.save(p,doc)
        bid=doc.get('book_id') or p.stem
        bs=st.setdefault('books',{}).setdefault(bid,{})
        bs['complete']=False;bs['synced_count']=len(good);bs['encoding_repair_required']=True;bs['updated_at']=core.now()
    core.save(core.STATE,st)
    return affected,removed


def main():
    core.fetch=fetch
    affected,removed=purge_corrupt_cache()
    print({'encoding_repair_books':affected,'garbled_chapters_removed':removed})
    core.main()

if __name__=='__main__':
    main()
