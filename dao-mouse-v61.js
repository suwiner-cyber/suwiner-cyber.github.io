(()=>{
  const reduce=window.matchMedia&&window.matchMedia('(prefers-reduced-motion: reduce)').matches;
  const fine=window.matchMedia&&window.matchMedia('(hover: hover) and (pointer: fine)').matches;
  if(reduce)return;
  let dao=null,slot=null,bubble=null,raf=0,last={x:0,y:0,t:performance.now()},fear=0,targetFear=0,startleLock=false,leaveTimer=0;
  const find=()=>{
    dao=document.querySelector('.daoist-stage');
    if(!dao)return false;
    slot=dao.closest('.daoist-slot')||dao.parentElement;
    if(!bubble){bubble=document.createElement('div');bubble.className='daoist-scare-bubble';bubble.textContent='哎……慢些靠近。';dao.appendChild(bubble)}
    return true;
  };
  const clamp=(v,a,b)=>Math.max(a,Math.min(b,v));
  const setState=(px,py,speed)=>{
    if(!dao&& !find())return;
    const r=dao.getBoundingClientRect();
    const cx=r.left+r.width*.5,cy=r.top+r.height*.44;
    const dx=px-cx,dy=py-cy,dist=Math.hypot(dx,dy);
    const radius=Math.max(250,Math.min(430,r.width*1.45));
    targetFear=clamp(1-dist/radius,0,1);
    const dir=dx===0?1:Math.sign(dx);
    const fleeX=clamp(-dx*.09,-34,34)*targetFear;
    const fleeY=clamp((Math.abs(dy)<r.height*.7?-10:2)-dy*.015,-18,7)*targetFear;
    const rot=clamp(-dir*targetFear*5.2,-5.2,5.2);
    dao.style.setProperty('--fear-x',`${fleeX.toFixed(2)}px`);
    dao.style.setProperty('--fear-y',`${fleeY.toFixed(2)}px`);
    dao.style.setProperty('--fear-r',`${rot.toFixed(2)}deg`);
    dao.style.setProperty('--gaze-x',`${clamp(dx*.025,-7,7).toFixed(2)}px`);
    dao.style.setProperty('--gaze-y',`${clamp(dy*.018,-5,5).toFixed(2)}px`);
    dao.style.setProperty('--fear',targetFear.toFixed(3));
    dao.classList.toggle('dao-mouse-alert',targetFear>.12);
    if(slot)slot.classList.toggle('mouse-near',targetFear>.18);
    if(targetFear>.4){
      clearTimeout(leaveTimer);
      bubble.classList.add('show');
      bubble.textContent=speed>1.05?'呀！莫要突然扑来……':targetFear>.72?'道友，靠得有些近了。':'贫道看见你了。';
    }else{
      leaveTimer=setTimeout(()=>bubble&&bubble.classList.remove('show'),260);
    }
    if((speed>1.05&&targetFear>.3)||(speed>.65&&targetFear>.62))startle();
  };
  const startle=()=>{
    if(!dao||startleLock)return;
    startleLock=true;
    dao.classList.remove('dao-startled');void dao.offsetWidth;dao.classList.add('dao-startled');
    if(bubble){bubble.textContent='呀！';bubble.classList.add('show')}
    setTimeout(()=>{dao&&dao.classList.remove('dao-startled');startleLock=false},620);
  };
  const tick=()=>{
    if(!dao)return;
    fear+=(targetFear-fear)*.16;
    if(targetFear<.03&&fear<.04){dao.classList.remove('dao-mouse-alert');dao.classList.add('dao-retreat');setTimeout(()=>dao&&dao.classList.remove('dao-retreat'),700)}
    raf=requestAnimationFrame(tick);
  };
  const onMove=(e)=>{
    if(!fine)return;
    if(!dao&&!find())return;
    const now=performance.now(),dt=Math.max(8,now-last.t),speed=Math.hypot(e.clientX-last.x,e.clientY-last.y)/dt;
    last={x:e.clientX,y:e.clientY,t:now};setState(e.clientX,e.clientY,speed);
  };
  const onLeave=()=>{targetFear=0;if(dao){dao.style.setProperty('--fear-x','0px');dao.style.setProperty('--fear-y','0px');dao.style.setProperty('--fear-r','0deg');dao.style.setProperty('--gaze-x','0px');dao.style.setProperty('--gaze-y','0px');dao.classList.remove('dao-mouse-alert')}if(slot)slot.classList.remove('mouse-near');if(bubble)bubble.classList.remove('show')};
  const touch=()=>{if(!find())return;dao.classList.add('dao-startled');setTimeout(()=>dao&&dao.classList.remove('dao-startled'),620)};
  const init=()=>{if(!find()){setTimeout(init,120);return}document.addEventListener('pointermove',onMove,{passive:true});document.documentElement.addEventListener('mouseleave',onLeave);if(!fine&&slot)slot.addEventListener('pointerdown',touch,{passive:true});raf=requestAnimationFrame(tick)};
  init();
})();