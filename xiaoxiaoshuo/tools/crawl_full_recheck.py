from __future__ import annotations
import crawl_six_full as core

# Runtime corrections for known live catalog/detail URL patterns.
# Keeping these outside the crawler core makes adapter hot-fixes small and auditable.
core.PROFILES['www.99csw.com'].update({
    'seeds': [
        'https://www.99csw.com/book/',
        'https://www.99csw.com/book/index.php',
    ],
    'book_path': [
        r'^/book/\d+/?$',
        r'^/book/\d+/index\.(?:htm|html)$',
    ],
    'chapter_path': [
        r'^/book/\d+/\d+\.(?:htm|html)$',
    ],
    'nav_tokens': ('/book/', '/author/', '/wenku/', '/top/', 'page='),
})

core.PROFILES['www.1qxs.com'].update({
    'seeds': [
        'https://www.1qxs.com/all.html',
        'https://www.1qxs.com/',
    ],
    'book_path': [
        r'^/xs_\d+/\d+\.html$',
        r'^/xs_\d+/\d+/?$',
        r'^/(?:book|novel|info)/[^?#]+$',
    ],
    'chapter_path': [
        r'^/xs_\d+/\d+/\d+\.html$',
        r'^/chapter/[^?#]+$',
        r'^/read/[^?#]+$',
    ],
    'nav_tokens': ('/all', '/sort', '/class', '/rank', '/quanben', '/xs_', '/book', 'page'),
})

if __name__ == '__main__':
    core.main()
