from __future__ import annotations
import crawl_six_full as core

# Runtime corrections and adapters for all authorized text-only sources.
core.PROFILES['www.99csw.com'].update({
    'seeds': ['https://www.99csw.com/book/','https://www.99csw.com/book/index.php'],
    'book_path': [r'^/book/\d+/?$',r'^/book/\d+/index\.(?:htm|html)$'],
    'chapter_path': [r'^/book/\d+/\d+\.(?:htm|html)$'],
    'nav_tokens': ('/book/','/author/','/wenku/','/top/','page='),
})
core.PROFILES['www.1qxs.com'].update({
    'seeds': ['https://www.1qxs.com/all.html','https://www.1qxs.com/'],
    'book_path': [r'^/xs_\d+/\d+\.html$',r'^/xs_\d+/\d+/?$',r'^/(?:book|novel|info)/[^?#]+$'],
    'chapter_path': [r'^/xs_\d+/\d+/\d+\.html$',r'^/chapter/[^?#]+$',r'^/read/[^?#]+$'],
    'nav_tokens': ('/all','/sort','/class','/rank','/quanben','/xs_','/book','page'),
})

core.PROFILES.update({
    'www.ilwxs.com': {
        'name':'乐文小说','seeds':['https://www.ilwxs.com/book/','https://www.ilwxs.com/'],
        'book_path':[r'^/book/\d+/?$',r'^/book/\d+\.html$'],
        'chapter_path':[r'^/book/\d+/\d+\.html$',r'^/read/\d+/\d+\.html$'],
        'nav_tokens':('/book/','/sort/','/list/','/rank/','/quanben','page'),
    },
    'www.ipaoshubaxs.net': {
        'name':'泡书吧小说网','seeds':['https://www.ipaoshubaxs.net/','https://www.ipaoshubaxs.net/all.html'],
        'book_path':[r'^/book/\d+/?$',r'^/\d+/\d+/?$',r'^/novel/\d+/?$'],
        'chapter_path':[r'^/book/\d+/\d+\.html$',r'^/\d+/\d+/\d+\.html$',r'^/chapter/\d+\.html$'],
        'nav_tokens':('/all','/book/','/sort/','/list/','/rank/','/quanben','page'),
    },
    'www.aaaks.com': {
        'name':'AAAKS小说','seeds':['https://www.aaaks.com/'],
        'book_path':[r'^/(?:book|novel|info)/\d+/?$',r'^/\d+/?$'],
        'chapter_path':[r'^/(?:chapter|read)/\d+/?$',r'^/\d+/\d+\.html$'],
        'nav_tokens':('/book','/novel','/sort','/list','/rank','/all','page'),
    },
    'www.69shuba.com': {
        'name':'69书吧','seeds':['https://www.69shuba.com/','https://www.69shuba.com/book/'],
        'book_path':[r'^/book/\d+/?$',r'^/txt/\d+/?$'],
        'chapter_path':[r'^/book/\d+/\d+\.html$',r'^/txt/\d+/\d+\.html$'],
        'nav_tokens':('/book/','/txt/','/sort/','/rank/','/quanben','/list/','page'),
    },
    'www.piaotia.com': {
        'name':'飘天文学','seeds':['https://www.piaotia.com/','https://www.piaotia.com/html/'],
        'book_path':[r'^/html/\d+/\d+/?$',r'^/book/\d+/?$'],
        'chapter_path':[r'^/html/\d+/\d+/\d+\.html$',r'^/book/\d+/\d+\.html$'],
        'nav_tokens':('/html/','/book/','/sort/','/top/','/quanben','/list/','page'),
    },
    'www.hetushu.com': {
        'name':'和图书','seeds':['https://www.hetushu.com/','https://www.hetushu.com/book/'],
        'book_path':[r'^/book/\d+/?$',r'^/book/\d+/index\.html$'],
        'chapter_path':[r'^/book/\d+/\d+\.html$'],
        'nav_tokens':('/book/','/author/','/sort/','/top/','/quanben','page'),
    },
})

if __name__ == '__main__':
    core.main()
