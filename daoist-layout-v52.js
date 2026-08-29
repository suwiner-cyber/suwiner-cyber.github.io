(()=>{
  const hero=document.querySelector('.hero');
  if(!hero)return;
  const mount=()=>{
    const dao=hero.querySelector('.daoist-stage');
    const card=hero.querySelector('.oracle-card');
    if(!dao||!card)return false;
    if(dao.parentElement&&dao.parentElement.classList.contains('daoist-slot'))return true;
    const slot=document.createElement('div');
    slot.className='daoist-slot';
    slot.setAttribute('aria-hidden','true');
    hero.insertBefore(slot,card);
    slot.appendChild(dao);
    return true;
  };
  if(mount())return;
  const observer=new MutationObserver(()=>{if(mount())observer.disconnect()});
  observer.observe(hero,{childList:true,subtree:true});
  setTimeout(()=>{mount();observer.disconnect()},2500);
})();