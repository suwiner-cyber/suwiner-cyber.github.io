from __future__ import annotations
import argparse, json, sys
from pathlib import Path
import crawl_authorized_v13 as core

ROOT=Path(__file__).resolve().parents[1]
AGG=ROOT/'data'/'v5'/'authorized_v14_status.json'
ADAPTER_VERSION='14.2'

# 13th authorized source.
core.P['ixdzs8.com']={
    'name':'爱下电子书',
    'seeds':['https://ixdzs8.com/','https://ixdzs8.com/sort/0/','https://ixdzs8.com/sort/14/'],
    'book':[r'^/read/\d+/?$'],
    'chapter':[r'^/read/\d+/p\d+\.html$',r'^/read/\d+/\d+(?:\.html)?$'],
    'nav':('/sort/','/rank','/author/','/tags/','/read/','index-'),
}

# Explicit current URL shapes and reliable discovery seeds.
core.P['1qxs.com'].update({
    'seeds':['https://www.1qxs.com/','https://www.1qxs.com/rk/1/0/1','https://www.1qxs.com/rk/4/0/1'],
    'book':[r'^/xs/\d+/?$',r'^/xs_\d+/\d+\.html$'],
    'chapter':[r'^/xs/\d+/\d+/?$',r'^/xs_\d+/\d+/\d+\.html$'],
    'nav':('/list/','/rk/','/fl/','/xs/','/all','/sort','/class','/rank','/quanben'),
})
core.P['69shuba.com'].update({
    'seeds':['https://www.69shuba.com/','https://www.69shuba.com/novels/full','https://www.69shuba.com/book/'],
    'book':[r'^/book/\d+\.(?:htm|html)$',r'^/book/\d+/?$'],
    'chapter':[r'^/txt/\d+/\d+/?$',r'^/book/\d+/\d+\.html$'],
    'nav':('/book/','/txt/','/sort/','/rank/','/quanben','/list/','/category','/novels/full'),
})
core.P['piaotia.com'].update({
    'seeds':['https://www.piaotia.com/','https://www.piaotia.com/bookinfo/','https://www.piaotia.com/html/'],
    'book':[r'^/bookinfo/\d+/\d+\.html$',r'^/html/\d+/\d+/?$'],
    'chapter':[r'^/html/\d+/\d+/\d+\.html$'],
    'nav':('/bookinfo/','/html/','/sort/','/top/','/quanben','/list/','articlelist'),
})
core.P['hetushu.com'].update({
    'seeds':['https://www.hetushu.com/','https://www.hetushu.com/book/'],
    'book':[r'^/book/\d+/index\.html$',r'^/book/\d+/?$'],
    'chapter':[r'^/book/\d+/\d+\.html$'],
    'nav':('/book/','/catalog-','/author/','/sort/','/top/','/quanben','/novel/'),
})
core.P['99csw.com'].update({
    'seeds':['https://www.99csw.com/book/','https://www.99csw.com/book/index.php'],
    'book':[r'^/book/\d+/?$',r'^/book/\d+/index\.(?:htm|html)$'],
    'chapter':[r'^/book/\d+/\d+\.(?:htm|html)$'],
    'nav':('/book/','/author/','/wenku/','/top/','index.php','page='),
})
core.P['ilwxs.com'].update({
    'seeds':['https://www.ilwxs.com/'],
    'book':[r'^/info-\d+\.html$',r'^/shu/\d+/?$',r'^/book/\d+/?$',r'^/book/\d+\.html$'],
    'chapter':[r'^/shu/\d+/\d+\.html$',r'^/book/\d+/\d+\.html$',r'^/read/\d+/\d+\.html$'],
    'nav':('/info-','/shu/','/book/','/sort/','/list/','/rank/','/quanben','/top/','type/'),
})
core.P['ipaoshubaxs.net'].update({
    'seeds':['https://www.ipaoshubaxs.net/'],
    'book':[r'^/book/\d+\.html$',r'^/info-\d+\.html$',r'^/shu/\d+/?$',r'^/\d+/?$'],
    'chapter':[r'^/shu/\d+/\d+\.html$',r'^/book/\d+/\d+\.html$',r'^/\d+/\d+\.html$',r'^/\d+/\d+/\d+\.html$'],
    'nav':('/book/','/info-','/shu/','/sort/','/list/','/rank/','/quanben','/top/','page'),
})

orig_load=core.load
orig_save=core.save

def full_config():
    return orig_load(core.CFG,{'sources':[]})

def match_source(src,selector):
    h=core.family(src.get('url',''))
    s=(selector or '').strip().lower()
    return (not s) or s==h or s==str(src.get('name','')).lower() or s==str(src.get('url','')).lower()

def reset_selected_state_if_needed(host):
    state=orig_load(core.STATE,{'version':13,'sources':{}})
    ss=state.setdefault('sources',{}).get(host,{})
    stale=ss.get('adapter_version')!=ADAPTER_VERSION
    zero_done=bool(ss.get('done')) and int(ss.get('books_seen') or 0)==0
    bad_adapter=bool(ss.get('needs_adapter'))
    if stale or zero_done or bad_adapter:
        profile=core.P[host]
        state['sources'][host]={
            'queue':list(profile['seeds']),
            'seen':[],
            'books_seen':0,
            'done':False,
            'blocked':False,
            'needs_adapter':False,
            'adapter_version':ADAPTER_VERSION,
            'reset_reason':'adapter_changed' if stale else ('zero_book_false_done' if zero_done else 'adapter_retry'),
        }
        orig_save(core.STATE,state)
    elif ss:
        ss['adapter_version']=ADAPTER_VERSION
        state['sources'][host]=ss
        orig_save(core.STATE,state)

def main():
    ap=argparse.ArgumentParser()
    ap.add_argument('--source',required=True,help='source host or configured name')
    ap.add_argument('--pages-per-source',type=int,default=80)
    args=ap.parse_args()

    cfg_all=full_config()
    selected=[s for s in cfg_all.get('sources',[]) if match_source(s,args.source)]
    if not selected:
        raise SystemExit(f'unknown source: {args.source}')
    selected_host=core.family(selected[0].get('url',''))
    if selected_host not in core.P:
        raise SystemExit(f'no adapter for configured source: {selected_host}')

    reset_selected_state_if_needed(selected_host)

    def filtered_load(path,default):
        data=orig_load(path,default)
        if path==core.CFG:
            return {'sources':selected}
        return data

    core.load=filtered_load
    old_argv=sys.argv[:]
    try:
        sys.argv=[old_argv[0],'--pages-per-source',str(args.pages_per_source)]
        core.main()
    finally:
        sys.argv=old_argv
        core.load=orig_load

    one=orig_load(core.STATUS,{})
    # Never accept a successful HTTP crawl with zero books as "done".
    for r in one.get('sources',[]):
        if int(r.get('books_seen_total') or 0)==0:
            r['done']=False
            r['needs_adapter']=True

    agg=orig_load(AGG,{'version':14,'sources':[]})
    reports={r.get('host'):r for r in agg.get('sources',[]) if r.get('host')}
    for r in one.get('sources',[]):
        if r.get('host'):
            reports[r['host']]=r

    configured=[]
    for src in cfg_all.get('sources',[]):
        if src.get('authorized') is not True or src.get('catalog_allowed') is not True:
            continue
        h=core.family(src.get('url',''))
        if h not in core.P:
            continue
        configured.append(h)
        if h not in reports:
            reports[h]={
                'name':core.P[h]['name'],'host':h,'pages_this_run':0,'successful_pages':0,
                'failed_pages':0,'books_found':0,'books_added':0,'toc_written':0,
                'books_seen_total':0,'remaining_queue':None,'seen_pages':0,
                'done':False,'blocked':False,'needs_adapter':False,'pending':True,
            }

    ordered=[reports[h] for h in configured if h in reports]
    unresolved=[r for r in ordered if not r.get('done')]
    idx=orig_load(core.INDEX,{})
    status={
        'version':14,'adapter_version':ADAPTER_VERSION,'generated_at':core.now(),
        'total_books':int(idx.get('total') or 0),'source_counts':idx.get('source_counts') or {},
        'all_done':bool(ordered) and not unresolved,'continue_needed':bool(unresolved),
        'unresolved_sources':[r.get('name') for r in unresolved],
        'zero_book_sources':[r.get('name') for r in ordered if int(r.get('books_seen_total') or 0)==0],
        'last_published_source':selected_host,'sources':ordered,
    }
    orig_save(AGG,status)
    idx['version']=14
    idx['generated_at']=core.now()
    idx['source_status']=ordered
    idx['status_file']='authorized_v14_status.json'
    idx['last_published_source']=selected_host
    orig_save(core.INDEX,idx)
    print(json.dumps(status,ensure_ascii=False))

if __name__=='__main__':
    main()
