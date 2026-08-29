(()=>{
  if(window.__DAO_MOTION_V62)return;
  window.__DAO_MOTION_V62=true;
  const reduced=window.matchMedia&&window.matchMedia('(prefers-reduced-motion: reduce)').matches;
  const wait=(fn,n=0)=>{
    const dao=document.querySelector('.daoist-stage');
    if(dao)return fn(dao);
    if(n<80)setTimeout(()=>wait(fn,n+1),100);
  };

  const css=`
  .daoist-stage .dao-reactor{position:absolute;inset:0;width:100%;height:100%;transform-origin:50% 80%;will-change:transform;transition:filter .18s ease}
  .daoist-stage .dao-reactor>svg{width:100%;height:100%;overflow:visible;display:block}
  .daoist-stage.dao-mouse-alert .dao-reactor{filter:drop-shadow(0 42px 32px rgba(0,0,0,.5))}
  .daoist-stage.dao-mouse-alert .dao-head{animation:daoV62Head .72s ease-in-out infinite alternate!important}
  .daoist-stage.dao-mouse-alert .dao-sleeve-left{animation:daoV62GuardL .82s ease-in-out infinite alternate!important}
  .daoist-stage.dao-mouse-alert .dao-sleeve-right{animation:daoV62GuardR .78s ease-in-out infinite alternate!important}
  .daoist-stage.dao-startled .dao-sleeve-left{animation:daoV62StartleL .62s cubic-bezier(.18,.85,.25,1) 1!important}
  .daoist-stage.dao-startled .dao-sleeve-right{animation:daoV62StartleR .62s cubic-bezier(.18,.85,.25,1) 1!important}
  .daoist-stage.dao-startled .dao-head{animation:daoV62StartleHead .62s cubic-bezier(.18,.85,.25,1) 1!important}
  .daoist-stage.dao-petting .dao-sleeve-right{animation:daoV62Pet 1.25s ease-in-out 2!important;transform-origin:228px 355px}
  .daoist-stage.dao-petting .dao-head{animation:daoV62PetHead 2.5s ease-in-out 1!important;transform-origin:170px 190px}
  .dao-v62-bubble{position:absolute;z-index:40;left:52%;top:88px;max-width:190px;padding:8px 11px;border:1px solid rgba(231,190,116,.22);border-radius:12px 12px 12px 4px;background:rgba(13,9,7,.91);backdrop-filter:blur(8px);color:#dcc6a5;font-size:10px;line-height:1.55;letter-spacing:.05em;opacity:0;transform:translate(26px,8px) scale(.96);transition:.2s ease;pointer-events:none;box-shadow:0 15px 38px rgba(0,0,0,.3)}
  .dao-v62-bubble.show{opacity:1;transform:translate(30px,0) scale(1)}
  .dao-v62-bubble:after{content:"";position:absolute;left:12px;bottom:-6px;width:10px;height:10px;background:rgba(13,9,7,.91);border-left:1px solid rgba(231,190,116,.18);border-bottom:1px solid rgba(231,190,116,.18);transform:rotate(-45deg)}
  .daoist-slot{overflow:visible!important}
  .dao-crane{position:absolute;z-index:25;left:50%;top:36px;width:220px;height:175px;margin-left:-110px;opacity:0;pointer-events:none;transform-origin:50% 55%;filter:drop-shadow(0 16px 14px rgba(0,0,0,.28))}
  .dao-crane svg{width:100%;height:100%;overflow:visible;display:block}
  .dao-crane .crane-wing-l,.dao-crane .crane-wing-r{transform-box:fill-box;will-change:transform}
  .dao-crane .crane-wing-l{transform-origin:88% 60%}.dao-crane .crane-wing-r{transform-origin:12% 60%}
  .dao-crane.flying .crane-wing-l,.dao-crane.departing .crane-wing-l{animation:craneWingL .32s ease-in-out infinite alternate}
  .dao-crane.flying .crane-wing-r,.dao-crane.departing .crane-wing-r{animation:craneWingR .32s ease-in-out infinite alternate}
  .dao-crane.landing .crane-wing-l{animation:craneWingL .58s ease-in-out infinite alternate}.dao-crane.landing .crane-wing-r{animation:craneWingR .58s ease-in-out infinite alternate}
  .dao-crane.resting .crane-wing-l,.dao-crane.resting .crane-wing-r{transition:transform .65s ease;transform:rotate(0deg)}
  .dao-crane.nuzzle .crane-neck{animation:craneNuzzle 1.1s ease-in-out 2;transform-box:fill-box;transform-origin:45% 88%}
  .dao-crane.nuzzle .crane-head{animation:craneHeadNuzzle 1.1s ease-in-out 2;transform-box:fill-box;transform-origin:45% 85%}
  .dao-crane .touch-spark{opacity:0;transform-box:fill-box;transform-origin:center}
  .dao-crane.nuzzle .touch-spark{animation:craneSpark 1.25s ease-in-out 2}
  @keyframes daoV62Head{from{transform:translate(var(--gaze-x,0),var(--gaze-y,0)) rotate(-.6deg)}to{transform:translate(var(--gaze-x,0),var(--gaze-y,0)) rotate(.7deg)}}
  @keyframes daoV62GuardL{from{transform:rotate(0)}to{transform:rotate(7deg) translate(4px,-6px)}}
  @keyframes daoV62GuardR{from{transform:rotate(0)}to{transform:rotate(-9deg) translate(-5px,-10px)}}
  @keyframes daoV62StartleL{0%{transform:rotate(0)}24%{transform:rotate(15deg) translate(8px,-15px)}55%{transform:rotate(8deg) translate(4px,-7px)}100%{transform:rotate(1deg)}}
  @keyframes daoV62StartleR{0%{transform:rotate(0)}24%{transform:rotate(-18deg) translate(-9px,-20px)}55%{transform:rotate(-9deg) translate(-4px,-9px)}100%{transform:rotate(-1deg)}}
  @keyframes daoV62StartleHead{0%{transform:translateY(0) rotate(0)}24%{transform:translateY(-9px) rotate(5deg)}55%{transform:translateY(-2px) rotate(-2deg)}100%{transform:none}}
  @keyframes daoV62Pet{0%,100%{transform:rotate(0) translate(0,0)}28%{transform:rotate(-12deg) translate(-8px,9px)}55%{transform:rotate(-15deg) translate(-12px,15px)}78%{transform:rotate(-10deg) translate(-7px,9px)}}
  @keyframes daoV62PetHead{0%,100%{transform:rotate(0)}45%,65%{transform:rotate(3.5deg) translateY(2px)}}
  @keyframes craneWingL{from{transform:rotate(8deg) translateY(1px)}to{transform:rotate(-34deg) translateY(-7px)}}
  @keyframes craneWingR{from{transform:rotate(-8deg) translateY(1px)}to{transform:rotate(34deg) translateY(-7px)}}
  @keyframes craneNuzzle{0%,100%{transform:rotate(0)}48%{transform:rotate(14deg) translate(4px,3px)}}
  @keyframes craneHeadNuzzle{0%,100%{transform:translate(0,0)}48%{transform:translate(8px,6px)}}
  @keyframes craneSpark{0%,100%{opacity:0;transform:scale(.5)}42%,58%{opacity:.9;transform:scale(1.2)}}
  @media(max-width:940px){.dao-crane{width:180px;height:145px;margin-left:-90px}.dao-v62-bubble{top:72px;max-width:155px;font-size:9px}}
  @media(prefers-reduced-motion:reduce){.dao-crane{display:none}.daoist-stage .dao-reactor{transform:none!important}.daoist-stage *{animation:none!important}}
  `;
  const style=document.createElement('style');style.id='dao-motion-v62-style';style.textContent=css;document.head.appendChild(style);

  wait((dao)=>{
    const slot=dao.closest('.daoist-slot')||dao.parentElement;
    let reactor=dao.querySelector('.dao-reactor');
    if(!reactor){
      const svg=dao.querySelector(':scope > svg')||dao.querySelector('svg');
      if(svg){reactor=document.createElement('div');reactor.className='dao-reactor';svg.parentNode.insertBefore(reactor,svg);reactor.appendChild(svg)}
    }
    if(!reactor)return;

    let bubble=dao.querySelector('.dao-v62-bubble');
    if(!bubble){bubble=document.createElement('div');bubble.className='dao-v62-bubble';bubble.textContent='贫道看见你了。';dao.appendChild(bubble)}
    const oldBubble=dao.querySelector('.daoist-scare-bubble');if(oldBubble)oldBubble.remove();

    let targetX=0,targetY=0,targetR=0,targetFear=0,curX=0,curY=0,curR=0,curFear=0,lastX=0,lastY=0,lastT=performance.now(),startleAt=0,bubbleTimer=0;
    const clamp=(v,a,b)=>Math.max(a,Math.min(b,v));
    const hideBubbleLater=(ms=650)=>{clearTimeout(bubbleTimer);bubbleTimer=setTimeout(()=>bubble.classList.remove('show'),ms)};
    const startle=(dir)=>{
      const now=performance.now();if(now-startleAt<900)return;startleAt=now;
      dao.classList.remove('dao-startled');void dao.offsetWidth;dao.classList.add('dao-startled');
      bubble.textContent='呀！道友慢些……';bubble.classList.add('show');hideBubbleLater(900);
      const bx=curX,by=curY,br=curR;
      if(reactor.animate)reactor.animate([
        {transform:`translate3d(${bx}px,${by}px,0) rotate(${br}deg) scale(1)`},
        {transform:`translate3d(${bx-dir*38}px,${by-11}px,0) rotate(${br-dir*7}deg) scale(.96,1.025)`,offset:.23},
        {transform:`translate3d(${bx-dir*22}px,${by+2}px,0) rotate(${br-dir*3}deg) scale(1.012,.99)`,offset:.55},
        {transform:`translate3d(${bx}px,${by}px,0) rotate(${br}deg) scale(1)`}
      ],{duration:640,easing:'cubic-bezier(.18,.85,.25,1)'});
      setTimeout(()=>dao.classList.remove('dao-startled'),680);
    };
    const react=(x,y,speed)=>{
      const r=dao.getBoundingClientRect();
      if(!r.width||!r.height)return;
      const cx=r.left+r.width*.5,cy=r.top+r.height*.43,dx=x-cx,dy=y-cy,dist=Math.hypot(dx,dy);
      const radius=clamp(Math.max(r.width*1.9,430),430,620);
      targetFear=clamp(1-dist/radius,0,1);
      const dir=dx>=0?1:-1;
      const strength=targetFear*targetFear;
      targetX=clamp(-dir*(18+62*strength),-78,78)*targetFear;
      targetY=-clamp(4+22*strength,0,24)*targetFear;
      targetR=clamp(-dir*(2+7*strength),-8.5,8.5)*targetFear;
      dao.style.setProperty('--gaze-x',`${clamp(dx*.025,-8,8).toFixed(1)}px`);
      dao.style.setProperty('--gaze-y',`${clamp(dy*.018,-5,5).toFixed(1)}px`);
      dao.classList.toggle('dao-mouse-alert',targetFear>.08);
      if(targetFear>.18){
        bubble.textContent=targetFear>.72?'道友……莫再靠近了。':targetFear>.4?'贫道正在看着你。':'贫道感应到你了。';
        bubble.classList.add('show');hideBubbleLater(480);
      }
      if((speed>1.15&&targetFear>.18)||(speed>.72&&targetFear>.55))startle(dir);
    };
    const onMove=(e)=>{
      if(e.pointerType&&e.pointerType!=='mouse')return;
      const now=performance.now(),dt=Math.max(8,now-lastT),speed=Math.hypot(e.clientX-lastX,e.clientY-lastY)/dt;
      lastX=e.clientX;lastY=e.clientY;lastT=now;react(e.clientX,e.clientY,speed);
    };
    const onLeave=()=>{targetX=0;targetY=0;targetR=0;targetFear=0;dao.classList.remove('dao-mouse-alert');hideBubbleLater(100)};
    if('PointerEvent' in window)window.addEventListener('pointermove',onMove,{passive:true});
    else window.addEventListener('mousemove',onMove,{passive:true});
    document.documentElement.addEventListener('mouseleave',onLeave);
    slot&&slot.addEventListener('pointerdown',(e)=>{if(e.pointerType==='touch'||e.pointerType==='pen')startle(1)},{passive:true});

    const frame=()=>{
      curX+=(targetX-curX)*.17;curY+=(targetY-curY)*.17;curR+=(targetR-curR)*.17;curFear+=(targetFear-curFear)*.14;
      if(Math.abs(curX)<.03)curX=0;if(Math.abs(curY)<.03)curY=0;if(Math.abs(curR)<.02)curR=0;
      reactor.style.transform=`translate3d(${curX.toFixed(2)}px,${curY.toFixed(2)}px,0) rotate(${curR.toFixed(2)}deg) scale(${(1-curFear*.018).toFixed(4)})`;
      requestAnimationFrame(frame);
    };
    if(!reduced)requestAnimationFrame(frame);

    const crane=document.createElement('div');crane.className='dao-crane';crane.setAttribute('aria-hidden','true');
    crane.innerHTML=`<svg viewBox="0 0 240 180">
      <defs>
        <linearGradient id="craneWhite62" x1="0" y1="0" x2="1" y2="1"><stop offset="0" stop-color="#fffdf6"/><stop offset=".55" stop-color="#d7d5cc"/><stop offset="1" stop-color="#858982"/></linearGradient>
        <linearGradient id="craneWing62" x1="0" y1="0" x2="1" y2="1"><stop offset="0" stop-color="#faf8f0"/><stop offset=".58" stop-color="#c9cac4"/><stop offset="1" stop-color="#626963"/></linearGradient>
        <filter id="craneShadow62" x="-30%" y="-30%" width="160%" height="180%"><feDropShadow dx="0" dy="5" stdDeviation="4" flood-color="#000" flood-opacity=".35"/></filter>
      </defs>
      <g filter="url(#craneShadow62)">
        <g class="crane-wing-l"><path d="M111 94 C80 54 38 45 13 62 C41 71 58 90 76 111 C88 118 102 113 116 104Z" fill="url(#craneWing62)"/><path d="M75 74 C52 62 35 63 23 68 M86 84 C61 75 48 78 36 85" fill="none" stroke="#858a84" stroke-width="1.2" opacity=".55"/></g>
        <g class="crane-wing-r"><path d="M114 95 C146 54 190 47 220 65 C191 73 173 91 151 112 C138 118 124 112 109 104Z" fill="url(#craneWing62)"/><path d="M151 77 C176 65 193 66 207 72 M142 87 C168 77 183 80 197 87" fill="none" stroke="#858a84" stroke-width="1.2" opacity=".55"/></g>
        <path d="M69 111 C53 116 44 128 35 141 C54 137 72 136 89 128Z" fill="#4a514d"/><path d="M77 116 C64 127 60 143 58 157 C72 144 87 136 99 128Z" fill="#6d736e"/>
        <ellipse cx="112" cy="106" rx="43" ry="28" fill="url(#craneWhite62)"/>
        <path class="crane-neck" d="M137 98 C159 88 164 63 151 43 C144 32 143 23 151 17" fill="none" stroke="#ebe9e1" stroke-width="15" stroke-linecap="round"/>
        <g class="crane-head"><ellipse cx="153" cy="18" rx="12" ry="10" fill="#f1efe7"/><path d="M146 11 C151 5 160 5 165 11 C160 10 153 11 146 11Z" fill="#b33a31"/><circle cx="158" cy="17" r="2.1" fill="#161817"/><path d="M164 18 L199 21 L164 24Z" fill="#c9a35f"/></g>
        <path d="M100 129 L96 162 M123 130 L127 163" stroke="#6e6759" stroke-width="2.4"/><path d="M96 162 l-8 5 M96 162 l6 6 M127 163 l-7 6 M127 163 l8 5" stroke="#6e6759" stroke-width="1.6" stroke-linecap="round"/>
        <g class="touch-spark" transform="translate(164 38)"><circle r="9" fill="none" stroke="#e7c984" stroke-width="1" opacity=".7"/><path d="M0-14V-8 M0 8V14 M-14 0H-8 M8 0H14" stroke="#e7c984" stroke-width="1.2"/></g>
      </g>
    </svg>`;
    slot.appendChild(crane);

    let craneTimer=0,craneBusy=false;
    const animate=(el,frames,opts)=>new Promise(resolve=>{
      if(!el.animate){el.style.transform=frames[frames.length-1].transform||'';el.style.opacity=frames[frames.length-1].opacity??1;return setTimeout(resolve,opts.duration||0)}
      const a=el.animate(frames,opts);a.onfinish=()=>resolve();a.oncancel=()=>resolve();
    });
    const say=(text,ms=1200)=>{bubble.textContent=text;bubble.classList.add('show');hideBubbleLater(ms)};
    const craneCycle=async()=>{
      if(craneBusy||document.hidden)return scheduleCrane(4500);
      craneBusy=true;crane.className='dao-crane flying';crane.style.opacity='1';
      say('白鹤来了。',1200);
      await animate(crane,[
        {transform:'translate3d(430px,-120px,0) scale(.52) rotate(8deg)',opacity:0},
        {transform:'translate3d(255px,20px,0) scale(.66) rotate(-4deg)',opacity:1,offset:.34},
        {transform:'translate3d(145px,120px,0) scale(.75) rotate(3deg)',opacity:1,offset:.68},
        {transform:'translate3d(76px,245px,0) scale(.82) rotate(-2deg)',opacity:1}
      ],{duration:3900,easing:'cubic-bezier(.18,.62,.22,1)',fill:'forwards'});
      crane.className='dao-crane landing';
      await animate(crane,[
        {transform:'translate3d(76px,245px,0) scale(.82) rotate(-2deg)'},
        {transform:'translate3d(58px,320px,0) scale(.86) rotate(1deg)'}
      ],{duration:1050,easing:'cubic-bezier(.2,.75,.25,1)',fill:'forwards'});
      crane.className='dao-crane resting';
      await new Promise(r=>setTimeout(r,700));
      dao.classList.add('dao-petting');crane.classList.add('nuzzle');say('乖，歇一歇。',1500);
      await new Promise(r=>setTimeout(r,2500));
      dao.classList.remove('dao-petting');crane.classList.remove('nuzzle');
      await new Promise(r=>setTimeout(r,500));
      crane.className='dao-crane departing';say('去吧，山高云阔。',1400);
      await animate(crane,[
        {transform:'translate3d(58px,320px,0) scale(.86) rotate(1deg)',opacity:1},
        {transform:'translate3d(-40px,190px,0) scale(.78) rotate(-7deg)',opacity:1,offset:.35},
        {transform:'translate3d(-240px,15px,0) scale(.64) rotate(-12deg)',opacity:.95,offset:.72},
        {transform:'translate3d(-470px,-130px,0) scale(.48) rotate(-15deg)',opacity:0}
      ],{duration:3300,easing:'cubic-bezier(.25,.65,.2,1)',fill:'forwards'});
      crane.style.opacity='0';craneBusy=false;scheduleCrane(18000+Math.random()*9000);
    };
    const scheduleCrane=(ms)=>{clearTimeout(craneTimer);craneTimer=setTimeout(craneCycle,ms)};
    if(!reduced)scheduleCrane(2600);
    document.addEventListener('visibilitychange',()=>{if(!document.hidden&&!craneBusy)scheduleCrane(3500)});
  });
})();