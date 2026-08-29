(()=>{
  document.body.classList.add('v5');
  const hero=document.querySelector('.hero');
  const card=document.querySelector('.oracle-card');
  const stage=document.querySelector('.oracle-stage');
  const bowl=document.getElementById('bowl');
  if(!hero||!card||!stage||!bowl)return;

  ['tl','tr','bl','br'].forEach(c=>{const i=document.createElement('i');i.className='ritual-corner '+c;card.appendChild(i)});

  const rim=document.createElement('div');rim.className='bowl-rim';bowl.appendChild(rim);
  const badge=document.createElement('div');badge.className='bowl-badge';badge.textContent='玄';bowl.appendChild(badge);
  const dust=document.createElement('div');dust.className='ritual-dust';dust.innerHTML='<i></i><i></i><i></i><i></i><i></i>';stage.appendChild(dust);

  const dao=document.createElement('div');
  dao.className='daoist-stage';
  dao.setAttribute('aria-hidden','true');
  dao.innerHTML=`
    <div class="daoist-glow"></div>
    <svg viewBox="0 0 340 680" role="img" aria-label="道长人物装饰">
      <defs>
        <linearGradient id="robeV5" x1="0" x2="1"><stop offset="0" stop-color="#101619"/><stop offset=".24" stop-color="#314249"/><stop offset=".48" stop-color="#5c7173"/><stop offset=".72" stop-color="#29383d"/><stop offset="1" stop-color="#0d1215"/></linearGradient>
        <linearGradient id="robeHiV5" x1="0" y1="0" x2="0.8" y2="1"><stop offset="0" stop-color="#829598" stop-opacity=".74"/><stop offset=".42" stop-color="#455a60"/><stop offset="1" stop-color="#121a1e"/></linearGradient>
        <linearGradient id="skinV5" x1="0" x2="1"><stop offset="0" stop-color="#6c4936"/><stop offset=".26" stop-color="#c49270"/><stop offset=".56" stop-color="#d5aa87"/><stop offset=".82" stop-color="#95654d"/><stop offset="1" stop-color="#493226"/></linearGradient>
        <radialGradient id="faceLightV5" cx="38%" cy="31%" r="70%"><stop offset="0" stop-color="#efd1ac"/><stop offset=".5" stop-color="#b37d5f"/><stop offset="1" stop-color="#50372b"/></radialGradient>
        <linearGradient id="hairV5" x1="0" x2="1"><stop offset="0" stop-color="#090a0a"/><stop offset=".48" stop-color="#343230"/><stop offset=".7" stop-color="#111211"/><stop offset="1" stop-color="#030404"/></linearGradient>
        <linearGradient id="sashV5" x1="0" x2="1"><stop offset="0" stop-color="#493a29"/><stop offset=".5" stop-color="#b48a52"/><stop offset="1" stop-color="#3f3022"/></linearGradient>
        <filter id="softV5" x="-30%" y="-30%" width="160%" height="160%"><feGaussianBlur stdDeviation="4"/></filter>
        <filter id="shadowV5" x="-30%" y="-20%" width="180%" height="180%"><feDropShadow dx="0" dy="10" stdDeviation="10" flood-color="#000" flood-opacity=".55"/></filter>
        <filter id="skinGlowV5" x="-40%" y="-40%" width="180%" height="180%"><feGaussianBlur in="SourceAlpha" stdDeviation="2.8" result="b"/><feSpecularLighting in="b" surfaceScale="4" specularConstant=".35" specularExponent="18" lighting-color="#f3d2a8" result="s"><fePointLight x="110" y="50" z="120"/></feSpecularLighting><feComposite in="s" in2="SourceAlpha" operator="in" result="si"/><feComposite in="SourceGraphic" in2="si" operator="arithmetic" k1="0" k2="1" k3=".32" k4="0"/></filter>
      </defs>
      <ellipse cx="170" cy="640" rx="112" ry="22" fill="#000" opacity=".27" filter="url(#softV5)"/>
      <g class="dao-robe" filter="url(#shadowV5)">
        <path d="M112 292 C92 335 78 415 72 560 C91 604 122 627 170 632 C216 626 251 600 268 557 C260 425 246 339 224 291 C199 273 140 273 112 292Z" fill="url(#robeV5)"/>
        <path d="M116 311 C132 352 143 409 144 594 C129 594 111 588 94 576 C100 445 102 360 116 311Z" fill="url(#robeHiV5)" opacity=".72"/>
        <path d="M219 307 C205 359 199 447 198 607 C222 599 238 584 248 563 C244 442 235 357 219 307Z" fill="#172226" opacity=".8"/>
        <path d="M169 300 C157 390 158 498 170 622" fill="none" stroke="#98a5a0" stroke-opacity=".16" stroke-width="2"/>
        <path d="M103 448 C124 461 144 466 168 465 C194 466 216 459 239 445" fill="none" stroke="url(#sashV5)" stroke-width="14" opacity=".82"/>
        <path d="M160 444 L174 444 L182 528 L165 559 L151 524Z" fill="#473624" opacity=".8"/>
        <path d="M167 448 L173 451 L171 520" stroke="#d3b172" stroke-opacity=".42" stroke-width="2" fill="none"/>
      </g>
      <g class="dao-sleeve-left">
        <path d="M111 305 C73 331 54 386 48 455 C61 476 80 479 98 464 C96 414 104 368 127 329Z" fill="url(#robeV5)" filter="url(#shadowV5)"/>
        <path d="M54 440 C63 452 80 456 96 446" fill="none" stroke="#8a9c9d" stroke-opacity=".21" stroke-width="3"/>
        <path d="M65 458 C57 478 62 500 81 507 C96 511 107 498 103 486 C99 470 91 456 82 449Z" fill="url(#skinV5)" filter="url(#skinGlowV5)"/>
        <path d="M78 477 C85 482 92 486 100 488 M75 486 C83 491 90 495 98 497" stroke="#6e4938" stroke-width="1.1" fill="none" opacity=".65"/>
      </g>
      <g class="dao-sleeve-right">
        <path d="M224 306 C264 328 283 381 292 449 C281 472 258 477 242 462 C240 415 232 368 209 330Z" fill="url(#robeV5)" filter="url(#shadowV5)"/>
        <path d="M245 448 C257 456 274 454 287 441" fill="none" stroke="#8a9c9d" stroke-opacity=".2" stroke-width="3"/>
        <path d="M258 458 C271 454 284 463 287 477 C290 490 281 501 268 500 C255 499 247 489 249 476Z" fill="url(#skinV5)" filter="url(#skinGlowV5)"/>
        <path d="M254 476 C264 475 274 478 282 483 M255 485 C265 484 274 487 280 491" stroke="#6e4938" stroke-width="1.1" fill="none" opacity=".65"/>
      </g>
      <g class="dao-head">
        <path d="M122 166 C119 124 138 86 170 78 C205 82 225 117 219 165 L210 212 C202 244 188 261 169 263 C149 259 136 242 128 211Z" fill="url(#faceLightV5)" filter="url(#skinGlowV5)"/>
        <path d="M123 164 C119 127 126 96 143 82 C157 67 190 66 207 85 C221 103 224 131 218 162 C213 136 202 116 184 105 C166 95 145 103 132 122 C126 132 124 145 123 164Z" fill="url(#hairV5)"/>
        <path d="M141 82 C145 56 160 39 174 39 C192 40 204 59 203 86 C187 76 158 75 141 82Z" fill="#0a0b0b"/>
        <path d="M154 45 C156 24 166 13 176 13 C186 14 193 28 191 47 C181 43 166 42 154 45Z" fill="#171817"/>
        <path d="M155 54 C164 49 183 49 193 56" fill="none" stroke="#8a765c" stroke-width="3" opacity=".5"/>
        <path d="M137 174 C148 167 158 167 167 173" fill="none" stroke="#34271f" stroke-width="3" stroke-linecap="round"/>
        <path d="M176 173 C188 166 198 168 207 175" fill="none" stroke="#34271f" stroke-width="3" stroke-linecap="round"/>
        <ellipse class="dao-eye" cx="154" cy="181" rx="4.2" ry="2.6" fill="#171411"/><ellipse class="dao-eye" cx="193" cy="181" rx="4.2" ry="2.6" fill="#171411"/>
        <circle cx="153" cy="180" r=".8" fill="#eadcc7" opacity=".7"/><circle cx="192" cy="180" r=".8" fill="#eadcc7" opacity=".7"/>
        <path d="M172 178 C169 191 168 202 173 207 C177 209 181 207 183 205" fill="none" stroke="#805946" stroke-width="1.7" opacity=".8"/>
        <path d="M158 220 C168 225 181 225 190 219" fill="none" stroke="#673d34" stroke-width="2" stroke-linecap="round"/>
        <path d="M157 223 C160 241 165 264 169 288 M189 222 C185 244 178 268 171 290" fill="none" stroke="#242321" stroke-width="4" stroke-linecap="round" opacity=".86"/>
        <path d="M139 217 C127 229 119 250 119 279 M204 219 C218 233 224 252 223 280" fill="none" stroke="#1f201f" stroke-width="5" stroke-linecap="round" opacity=".8"/>
        <path d="M128 205 C121 207 115 200 115 188 C115 177 120 171 126 174 M215 205 C223 207 229 199 229 188 C229 177 224 171 218 175" fill="url(#skinV5)"/>
        <path d="M147 155 C160 149 170 151 177 154 M183 154 C192 150 202 152 209 158" fill="none" stroke="#3c2c22" stroke-width="2.5" opacity=".7"/>
        <path d="M134 119 C150 98 178 91 200 107" fill="none" stroke="#78736b" stroke-opacity=".24" stroke-width="2"/>
      </g>
      <path d="M133 273 C148 286 191 288 209 272 L227 316 C202 339 141 341 113 315Z" fill="url(#robeHiV5)"/>
      <path d="M151 286 C158 300 184 300 193 285" fill="none" stroke="#d7b67d" stroke-opacity=".34" stroke-width="2"/>
      <g opacity=".7"><circle cx="111" cy="338" r="3" fill="#d5ad6a"/><circle cx="230" cy="340" r="3" fill="#d5ad6a"/></g>
    </svg>
    <div class="dao-incense"><div class="incense-stick"></div><div class="incense-smoke"></div><div class="incense-pot"></div></div>
    <div class="dao-caption">玄门守签 · 静候一问</div>`;
  hero.appendChild(dao);

  const svgIcon=(type)=>{
    const icons={
      draw:'<svg viewBox="0 0 24 24" fill="none" aria-hidden="true"><path d="M8 4h8M9 4l-2 15h10L15 4M8 8h8" stroke="currentColor" stroke-width="1.6" stroke-linecap="round"/><path d="M10 2v8M12 1v10M14 2v8" stroke="currentColor" stroke-width="1.2" stroke-linecap="round"/></svg>',
      bazi:'<svg viewBox="0 0 24 24" fill="none" aria-hidden="true"><circle cx="12" cy="12" r="8.5" stroke="currentColor" stroke-width="1.4"/><path d="M12 3.5c3 2.4 3 6.1 0 8.5s-3 6.1 0 8.5M12 3.5c-3 2.4-3 6.1 0 8.5s3 6.1 0 8.5" stroke="currentColor" stroke-width="1.2"/><circle cx="12" cy="8" r="1" fill="currentColor"/><circle cx="12" cy="16" r="1" fill="currentColor"/></svg>'
    };return icons[type]||icons.draw;
  };
  const drawBtn=document.getElementById('drawBtn');
  const formatDraw=()=>{if(drawBtn&&!drawBtn.querySelector('svg')){const txt=drawBtn.textContent.trim();drawBtn.innerHTML=svgIcon('draw')+'<span>'+txt+'</span>'}};
  formatDraw();
  if(drawBtn)new MutationObserver(formatDraw).observe(drawBtn,{childList:true,characterData:true,subtree:true});
  const bz=document.getElementById('baziBtn');if(bz&&!bz.querySelector('svg'))bz.innerHTML=svgIcon('bazi')+'<span>生成 · 生辰运势</span>';

  const navIcons=['⌁','◫','✦','☯','卷'];
  document.querySelectorAll('nav a').forEach((a,i)=>{if(!a.querySelector('.nav-mini')){const s=document.createElement('span');s.className='nav-mini';s.textContent=navIcons[i]||'·';s.style.cssText='opacity:.6;margin-right:5px;font-size:11px';a.prepend(s)}});

  let blinkTimer=null;
  const blink=()=>{dao.querySelectorAll('.dao-eye').forEach(e=>{e.style.transformOrigin=e.getAttribute('cx')+'px '+e.getAttribute('cy')+'px';e.style.transform='scaleY(.12)'});setTimeout(()=>dao.querySelectorAll('.dao-eye').forEach(e=>e.style.transform='scaleY(1)'),120);blinkTimer=setTimeout(blink,2800+Math.random()*3200)};
  blinkTimer=setTimeout(blink,2200);

  const reduce=window.matchMedia&&window.matchMedia('(prefers-reduced-motion: reduce)').matches;
  if(!reduce){
    const move=(e)=>{const r=hero.getBoundingClientRect();const x=(e.clientX-r.left)/r.width-.5;const y=(e.clientY-r.top)/r.height-.5;dao.style.marginLeft=(x*5)+'px';dao.style.filter=`drop-shadow(${x*-10}px ${38+y*5}px 30px rgba(0,0,0,.42))`};
    hero.addEventListener('pointermove',move,{passive:true});
    hero.addEventListener('pointerleave',()=>{dao.style.marginLeft='0';dao.style.filter=''});
  }
})();