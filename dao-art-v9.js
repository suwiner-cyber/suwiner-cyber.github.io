(()=>{
  if(window.__DAO_ART_V9)return;window.__DAO_ART_V9=true;
  const reduce=window.matchMedia&&window.matchMedia('(prefers-reduced-motion: reduce)').matches;
  const wait=(n=0)=>{
    const slot=document.querySelector('.daoist-slot');
    if(slot)return init(slot);
    if(n<100)setTimeout(()=>wait(n+1),100);
  };
  function init(slot){
    slot.querySelectorAll('.daoist-stage,.dao-crane,.three-v7-stage').forEach(e=>e.remove());
    document.body.classList.remove('three-ready');
    const dao=document.createElement('div');dao.className='daoist-stage dao-v9';dao.setAttribute('aria-hidden','true');
    dao.innerHTML=`<div class="daoist-glow"></div>
    <svg viewBox="0 0 380 700" role="img" aria-label="精细道长人物装饰">
      <defs>
        <linearGradient id="v9robe" x1="0" x2="1"><stop offset="0" stop-color="#0e1517"/><stop offset=".18" stop-color="#26383c"/><stop offset=".43" stop-color="#586b6d"/><stop offset=".58" stop-color="#405458"/><stop offset=".82" stop-color="#1c292d"/><stop offset="1" stop-color="#080d0f"/></linearGradient>
        <linearGradient id="v9robe2" x1="0" y1="0" x2="1" y2="1"><stop offset="0" stop-color="#839395"/><stop offset=".26" stop-color="#52676a"/><stop offset=".58" stop-color="#26383d"/><stop offset="1" stop-color="#111a1d"/></linearGradient>
        <linearGradient id="v9skin" x1="0" x2="1"><stop offset="0" stop-color="#714a36"/><stop offset=".2" stop-color="#b57d5e"/><stop offset=".48" stop-color="#d5a17b"/><stop offset=".68" stop-color="#bc8264"/><stop offset="1" stop-color="#5a392b"/></linearGradient>
        <radialGradient id="v9face" cx="38%" cy="28%" r="72%"><stop offset="0" stop-color="#e6bb91"/><stop offset=".34" stop-color="#c58e6c"/><stop offset=".7" stop-color="#936048"/><stop offset="1" stop-color="#4a3025"/></radialGradient>
        <linearGradient id="v9hair" x1="0" x2="1"><stop offset="0" stop-color="#060707"/><stop offset=".45" stop-color="#363533"/><stop offset=".67" stop-color="#151615"/><stop offset="1" stop-color="#020303"/></linearGradient>
        <linearGradient id="v9beard" x1="0" x2="1"><stop offset="0" stop-color="#151615"/><stop offset=".42" stop-color="#4c4a45"/><stop offset=".68" stop-color="#242524"/><stop offset="1" stop-color="#090a0a"/></linearGradient>
        <linearGradient id="v9gold" x1="0" x2="1"><stop offset="0" stop-color="#5f4327"/><stop offset=".4" stop-color="#c89a58"/><stop offset=".64" stop-color="#e1bc75"/><stop offset="1" stop-color="#654629"/></linearGradient>
        <filter id="v9shadow" x="-40%" y="-30%" width="180%" height="190%"><feDropShadow dx="0" dy="12" stdDeviation="10" flood-color="#000" flood-opacity=".5"/></filter>
        <filter id="v9soft" x="-40%" y="-40%" width="180%" height="180%"><feGaussianBlur stdDeviation="4"/></filter>
        <filter id="v9skinlight" x="-30%" y="-30%" width="160%" height="160%"><feGaussianBlur in="SourceAlpha" stdDeviation="2" result="b"/><feSpecularLighting in="b" surfaceScale="3" specularConstant=".22" specularExponent="18" lighting-color="#ffe0bc" result="s"><fePointLight x="120" y="60" z="130"/></feSpecularLighting><feComposite in="s" in2="SourceAlpha" operator="in" result="si"/><feComposite in="SourceGraphic" in2="si" operator="arithmetic" k2="1" k3=".26"/></filter>
        <pattern id="v9cloth" width="6" height="6" patternUnits="userSpaceOnUse"><path d="M0 1H6M1 0V6" stroke="#dce5df" stroke-opacity=".045" stroke-width=".5"/></pattern>
      </defs>
      <ellipse cx="190" cy="655" rx="116" ry="20" fill="#000" opacity=".28" filter="url(#v9soft)"/>
      <g class="v9-body" filter="url(#v9shadow)">
        <path d="M123 302 C101 344 89 425 83 580 C104 618 139 641 189 645 C239 640 276 615 297 578 C290 431 278 346 254 301 C224 282 151 281 123 302Z" fill="url(#v9robe)"/>
        <path d="M122 306 C143 352 151 427 149 616 C128 608 110 598 96 579 C101 444 105 353 122 306Z" fill="url(#v9robe2)" opacity=".82"/>
        <path d="M254 304 C236 354 227 431 230 618 C251 608 271 593 285 573 C280 439 272 350 254 304Z" fill="#152126" opacity=".9"/>
        <path d="M186 302 C173 396 173 518 188 642" fill="none" stroke="#cbd4ce" stroke-opacity=".12" stroke-width="1.5"/>
        <path d="M196 304 C211 403 211 516 198 642" fill="none" stroke="#071012" stroke-opacity=".55" stroke-width="1"/>
        <path d="M85 580 C123 594 151 598 190 598 C231 598 264 592 296 576" fill="none" stroke="#899797" stroke-opacity=".12" stroke-width="2"/>
        <path d="M102 347 C137 359 164 364 190 364 C216 364 247 358 279 345" fill="none" stroke="#9ba7a6" stroke-opacity=".11" stroke-width="2"/>
        <path d="M88 580 C109 609 143 627 190 633 C236 626 271 609 291 578" fill="url(#v9cloth)" opacity=".45"/>
        <path d="M111 446 C142 456 162 460 190 460 C219 460 244 454 270 443" fill="none" stroke="url(#v9gold)" stroke-width="15" opacity=".88"/>
        <path d="M172 451 L190 458 L207 451 L210 533 L191 573 L170 533Z" fill="#43311f" opacity=".85"/>
        <path d="M186 458 C189 487 190 522 190 564" fill="none" stroke="#d6b06e" stroke-opacity=".45" stroke-width="2"/>
      </g>
      <g class="v9-sleeve-l">
        <path d="M129 309 C87 329 57 385 49 462 C60 489 82 500 105 482 C104 424 113 365 145 328Z" fill="url(#v9robe)" filter="url(#v9shadow)"/>
        <path d="M59 444 C72 457 88 463 103 457" fill="none" stroke="#a7b1af" stroke-opacity=".18" stroke-width="2.5"/>
        <path d="M73 477 C62 495 66 516 84 525 C99 533 115 524 116 511 C117 498 104 478 95 467Z" fill="url(#v9skin)" filter="url(#v9skinlight)"/>
        <path d="M81 494 C92 500 101 506 110 511 M78 503 C89 510 98 516 107 520" stroke="#6f4633" stroke-width="1.1" fill="none" opacity=".62"/>
        <path d="M71 488 C77 484 82 480 88 478" stroke="#e0b18c" stroke-width="1" fill="none" opacity=".42"/>
      </g>
      <g class="v9-sleeve-r">
        <path d="M251 309 C292 328 321 383 331 456 C322 485 299 496 275 479 C273 420 264 364 235 329Z" fill="url(#v9robe)" filter="url(#v9shadow)"/>
        <path d="M279 447 C294 458 311 458 325 445" fill="none" stroke="#a7b1af" stroke-opacity=".18" stroke-width="2.5"/>
        <path d="M287 475 C298 466 313 472 320 487 C326 501 320 515 307 520 C292 526 280 515 280 500 C280 491 283 482 287 475Z" fill="url(#v9skin)" filter="url(#v9skinlight)"/>
        <path d="M286 493 C297 490 307 492 316 497 M287 503 C297 501 306 503 314 508" stroke="#6f4633" stroke-width="1.1" fill="none" opacity=".62"/>
        <path d="M306 481 C311 483 316 486 320 491" stroke="#e0b18c" stroke-width="1" fill="none" opacity=".42"/>
      </g>
      <g class="v9-head">
        <path d="M142 155 C138 111 156 77 189 71 C225 76 244 111 238 155 L230 207 C224 241 208 259 190 262 C168 259 153 241 146 207Z" fill="url(#v9face)" filter="url(#v9skinlight)"/>
        <path d="M143 153 C139 121 143 93 158 78 C173 63 207 62 224 80 C240 97 243 122 238 153 C232 127 220 107 202 96 C182 85 158 95 148 117 C144 128 143 140 143 153Z" fill="url(#v9hair)"/>
        <path d="M155 84 C158 57 171 41 190 40 C211 42 225 59 225 86 C208 74 174 73 155 84Z" fill="#090a0a"/>
        <path d="M172 45 C173 26 181 13 191 13 C202 13 210 26 209 46 C198 42 183 42 172 45Z" fill="#171817"/>
        <path d="M169 53 C181 47 201 47 214 54" fill="none" stroke="url(#v9gold)" stroke-width="3.2" opacity=".72"/>
        <path d="M156 167 C167 160 179 160 187 166" fill="none" stroke="#33241d" stroke-width="3.4" stroke-linecap="round"/>
        <path d="M195 166 C207 159 220 161 227 168" fill="none" stroke="#33241d" stroke-width="3.4" stroke-linecap="round"/>
        <ellipse class="v9-eye" cx="175" cy="176" rx="4.6" ry="2.8" fill="#151311"/><ellipse class="v9-eye" cx="211" cy="176" rx="4.6" ry="2.8" fill="#151311"/>
        <circle cx="174" cy="175" r="1" fill="#f3e0c9" opacity=".75"/><circle cx="210" cy="175" r="1" fill="#f3e0c9" opacity=".75"/>
        <path d="M191 171 C188 185 187 199 192 205 C196 208 201 207 204 203" fill="none" stroke="#784d3a" stroke-width="1.6"/>
        <path d="M178 218 C187 223 199 223 207 218" fill="none" stroke="#6a3e35" stroke-width="2" stroke-linecap="round"/>
        <path d="M153 196 C157 202 161 205 166 207 M218 206 C223 204 227 200 231 194" fill="none" stroke="#9b6951" stroke-opacity=".45" stroke-width="1"/>
        <path d="M158 188 C165 191 172 193 178 192 M205 192 C214 193 221 190 226 187" fill="none" stroke="#7d5542" stroke-opacity=".28" stroke-width="1"/>
        <path d="M160 151 C173 144 184 146 192 150 M199 150 C210 145 221 147 228 154" fill="none" stroke="#3a2a22" stroke-width="2.4" opacity=".72"/>
        <path d="M147 199 C138 200 133 192 134 181 C135 170 140 165 146 169 M236 199 C244 201 249 192 248 181 C247 170 243 165 238 169" fill="url(#v9skin)"/>
        <path d="M158 121 C170 103 195 94 218 105" fill="none" stroke="#c7bbb0" stroke-opacity=".16" stroke-width="1.4"/>
        <path d="M148 140 C157 130 167 124 178 121" fill="none" stroke="#d7b69a" stroke-opacity=".12" stroke-width="1.1"/>
      </g>
      <g class="v9-beard">
        <path d="M169 218 C170 242 178 270 189 301 C199 273 208 244 210 218 C200 229 180 231 169 218Z" fill="url(#v9beard)"/>
        <path d="M166 220 C152 235 144 258 143 286 C149 272 157 261 166 255 M213 220 C227 235 235 258 236 286 C230 272 222 261 213 255" fill="none" stroke="url(#v9beard)" stroke-width="5.5" stroke-linecap="round"/>
        <path d="M177 226 C181 248 185 272 189 294 M199 226 C196 251 193 273 190 298" fill="none" stroke="#8d8980" stroke-opacity=".22" stroke-width="1.1"/>
      </g>
      <path d="M149 272 C162 286 216 287 231 272 L251 316 C225 339 158 341 130 316Z" fill="url(#v9robe2)"/>
      <path d="M169 284 C180 298 203 298 214 283" fill="none" stroke="#d5af72" stroke-opacity=".32" stroke-width="2"/>
      <g class="v9-highlight" opacity=".8"><circle cx="126" cy="342" r="2.8" fill="#d8ae68"/><circle cx="254" cy="342" r="2.8" fill="#d8ae68"/><path d="M190 335 v18 M181 344 h18" stroke="#d8ae68" stroke-opacity=".32"/></g>
    </svg>
    <div class="v9-dust"><i></i><i></i><i></i><i></i><i></i></div>
    <div class="v9-incense"><div class="smoke"></div><div class="stick"></div><div class="ember"></div><div class="pot"></div></div>
    <div class="v9-caption">玄门守签 · 静候一问</div><div class="v9-speech">贫道在此。</div>`;
    slot.appendChild(dao);

    const crane=document.createElement('div');crane.className='v9-crane';crane.setAttribute('aria-hidden','true');crane.innerHTML=`<svg viewBox="0 0 280 210">
      <defs><linearGradient id="cw9" x1="0" y1="0" x2="1" y2="1"><stop offset="0" stop-color="#fffef9"/><stop offset=".55" stop-color="#e0dfd8"/><stop offset="1" stop-color="#a5aaa4"/></linearGradient><linearGradient id="cb9" x1="0" x2="1"><stop offset="0" stop-color="#1d2321"/><stop offset=".55" stop-color="#4a514d"/><stop offset="1" stop-color="#151918"/></linearGradient><filter id="cs9" x="-30%" y="-30%" width="160%" height="180%"><feDropShadow dx="0" dy="6" stdDeviation="4" flood-color="#000" flood-opacity=".32"/></filter></defs>
      <g filter="url(#cs9)"><g class="wing-l"><path d="M130 111 C95 60 50 45 12 68 C46 77 68 98 88 124 C101 134 117 128 135 116Z" fill="url(#cw9)"/><path d="M94 80 C66 66 43 67 25 75 M105 91 C78 80 58 83 39 93 M114 103 C87 95 69 99 52 108" fill="none" stroke="#8f948f" stroke-width="1.15" opacity=".58"/><path d="M76 87 C56 80 42 82 31 87" fill="none" stroke="#fff" stroke-opacity=".38" class="feather-shine"/></g><g class="wing-r"><path d="M132 111 C168 60 218 47 264 71 C226 78 202 99 179 126 C164 135 148 128 128 116Z" fill="url(#cw9)"/><path d="M171 81 C203 66 228 69 248 77 M160 93 C190 81 213 85 233 95 M151 104 C181 96 201 100 220 110" fill="none" stroke="#8f948f" stroke-width="1.15" opacity=".58"/><path d="M192 88 C215 81 231 83 244 90" fill="none" stroke="#fff" stroke-opacity=".38" class="feather-shine"/></g><path d="M85 130 C64 137 50 151 39 169 C63 163 84 164 104 153Z" fill="url(#cb9)"/><path d="M98 135 C82 150 74 169 72 187 C88 169 104 159 119 151Z" fill="#555d58"/><ellipse cx="135" cy="122" rx="50" ry="31" fill="url(#cw9)"/><path d="M103 120 C114 109 127 105 142 107 C132 118 124 132 122 146 C113 141 107 132 103 120Z" fill="url(#cb9)" opacity=".82"/><path class="neck" d="M164 113 C187 99 192 73 177 50 C168 36 168 25 178 17" fill="none" stroke="#f0efe9" stroke-width="17" stroke-linecap="round"/><g class="head"><ellipse cx="181" cy="17" rx="13" ry="11" fill="#f4f2ec"/><path d="M172 10 C179 3 188 4 195 10 C188 9 180 10 172 10Z" fill="#a62f28"/><path d="M176 12 C181 9 188 10 191 14 C187 15 181 16 176 12Z" fill="#202321"/><circle cx="188" cy="17" r="2.1" fill="#101211"/><circle cx="188.5" cy="16.4" r=".7" fill="#fff"/><path d="M193 18 L235 21 L193 25Z" fill="#c7a05c"/><path d="M201 20 L233 21" stroke="#f1d99b" stroke-opacity=".6"/></g><path d="M118 148 L114 190 M146 149 L151 191" stroke="#756c5d" stroke-width="2.4"/><path d="M114 190 l-9 6 M114 190 l7 7 M151 191 l-8 7 M151 191 l9 5" stroke="#756c5d" stroke-width="1.5" stroke-linecap="round"/><g class="touch-ring" transform="translate(188 45)"><circle r="10" fill="none" stroke="#e4c37b"/><path d="M0-15V-8 M0 8V15 M-15 0H-8 M8 0H15" stroke="#e4c37b"/></g></g></svg>`;slot.appendChild(crane);

    const bubble=dao.querySelector('.v9-speech');let bubbleTimer=0,last={x:0,y:0,t:performance.now()},fear=0,targetFear=0,startledAt=0;
    const clamp=(v,a,b)=>Math.max(a,Math.min(b,v));
    const say=(text,ms=900)=>{bubble.textContent=text;bubble.classList.add('show');clearTimeout(bubbleTimer);bubbleTimer=setTimeout(()=>bubble.classList.remove('show'),ms)};
    const startle=()=>{const now=performance.now();if(now-startledAt<850)return;startledAt=now;dao.classList.remove('startled');void dao.offsetWidth;dao.classList.add('startled');say('呀！道友慢些……',1000);setTimeout(()=>dao.classList.remove('startled'),680)};
    const react=(x,y,speed)=>{const r=dao.getBoundingClientRect();if(!r.width)return;const cx=r.left+r.width*.5,cy=r.top+r.height*.43,dx=x-cx,dy=y-cy,dist=Math.hypot(dx,dy),radius=Math.max(420,r.width*1.65);targetFear=clamp(1-dist/radius,0,1);fear+=(targetFear-fear)*.32;const dir=dx>=0?1:-1,str=fear*fear;dao.style.setProperty('--fear-x',`${(-dir*(12+58*str)*fear).toFixed(1)}px`);dao.style.setProperty('--fear-y',`${(-4-15*str).toFixed(1)}px`);dao.style.setProperty('--fear-r',`${(-dir*(1.5+5.5*str)*fear).toFixed(2)}deg`);dao.style.setProperty('--gaze-x',`${clamp(dx*.02,-6,6).toFixed(1)}px`);dao.style.setProperty('--gaze-y',`${clamp(dy*.014,-4,4).toFixed(1)}px`);dao.classList.toggle('mouse-near',fear>.1);if(fear>.35)say(fear>.72?'道友，莫再靠近了。':'贫道看见你了。',420);if((speed>.95&&fear>.2)||(speed>.62&&fear>.58))startle()};
    const onMove=e=>{if(e.pointerType&&e.pointerType!=='mouse')return;const now=performance.now(),dt=Math.max(8,now-last.t),speed=Math.hypot(e.clientX-last.x,e.clientY-last.y)/dt;last={x:e.clientX,y:e.clientY,t:now};react(e.clientX,e.clientY,speed)};window.addEventListener('pointermove',onMove,{passive:true});document.documentElement.addEventListener('mouseleave',()=>{fear=targetFear=0;dao.style.setProperty('--fear-x','0px');dao.style.setProperty('--fear-y','0px');dao.style.setProperty('--fear-r','0deg');dao.classList.remove('mouse-near');bubble.classList.remove('show')});
    let blinkTimer=0;const blink=()=>{dao.classList.add('v9-blink');setTimeout(()=>dao.classList.remove('v9-blink'),115);blinkTimer=setTimeout(blink,2600+Math.random()*3500)};if(!reduce)blinkTimer=setTimeout(blink,1800);
    let busy=false,craneTimer=0;const animate=(frames,opts)=>new Promise(res=>{if(!crane.animate){setTimeout(res,opts.duration||0);return}const a=crane.animate(frames,opts);a.onfinish=res;a.oncancel=res});
    const cycle=async()=>{if(busy||document.hidden)return schedule(4000);busy=true;crane.className='v9-crane flying';crane.style.opacity='1';say('白鹤归来。',1200);await animate([{transform:'translate3d(470px,-145px,0) scale(.5) rotate(9deg)',opacity:0},{transform:'translate3d(260px,0,0) scale(.66) rotate(-5deg)',opacity:1,offset:.34},{transform:'translate3d(145px,120px,0) scale(.77) rotate(3deg)',opacity:1,offset:.68},{transform:'translate3d(78px,260px,0) scale(.86) rotate(-2deg)',opacity:1}],{duration:3900,easing:'cubic-bezier(.18,.62,.22,1)',fill:'forwards'});crane.className='v9-crane landing';await animate([{transform:'translate3d(78px,260px,0) scale(.86) rotate(-2deg)'},{transform:'translate3d(56px,342px,0) scale(.9) rotate(1deg)'}],{duration:950,easing:'cubic-bezier(.2,.75,.25,1)',fill:'forwards'});crane.className='v9-crane resting';await new Promise(r=>setTimeout(r,700));dao.classList.add('petting');crane.classList.add('nuzzle');say('乖，歇一歇。',1500);await new Promise(r=>setTimeout(r,2300));dao.classList.remove('petting');crane.classList.remove('nuzzle');await new Promise(r=>setTimeout(r,450));crane.className='v9-crane departing';say('去吧，山高云阔。',1300);await animate([{transform:'translate3d(56px,342px,0) scale(.9) rotate(1deg)',opacity:1},{transform:'translate3d(-40px,195px,0) scale(.79) rotate(-8deg)',opacity:1,offset:.35},{transform:'translate3d(-250px,10px,0) scale(.62) rotate(-13deg)',opacity:.96,offset:.72},{transform:'translate3d(-500px,-145px,0) scale(.46) rotate(-16deg)',opacity:0}],{duration:3300,easing:'cubic-bezier(.24,.66,.2,1)',fill:'forwards'});crane.style.opacity='0';busy=false;schedule(18000+Math.random()*9000)};
    const schedule=ms=>{clearTimeout(craneTimer);craneTimer=setTimeout(cycle,ms)};if(!reduce)schedule(2500);document.addEventListener('visibilitychange',()=>{if(!document.hidden&&!busy)schedule(3500)});
  }
  wait();
})();