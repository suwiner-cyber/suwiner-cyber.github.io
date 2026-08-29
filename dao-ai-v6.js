(()=>{
  const hero=document.querySelector('.hero');
  const master=document.querySelector('.daoist-stage');
  if(!hero)return;

  const section=document.createElement('section');
  section.className='dao-ai-section';
  section.id='ask-master';
  section.innerHTML=`
    <div class="dao-ai-panel" id="daoAiPanel">
      <div class="dao-ai-head">
        <div class="dao-ai-title"><div class="dao-ai-mark">道</div><div><b>问道 · 与道长对话</b><small>结合灵签 · 星运 · 生辰 · 农家历</small></div></div>
        <div style="display:flex;align-items:center;gap:10px"><div class="dao-ai-status"><i></i><span id="daoStatusText">静候垂询</span></div><button class="dao-voice" id="daoVoiceBtn" type="button">语音：关</button></div>
      </div>
      <div class="dao-chat" id="daoChat" aria-live="polite"></div>
      <div class="dao-quick" id="daoQuick">
        <button type="button">我最近的事业怎么样？</button><button type="button">感情上我该主动吗？</button><button type="button">今天适合做什么？</button><button type="button">帮我结合刚才的签解读</button><button type="button">我的生辰运势怎么看？</button>
      </div>
      <form class="dao-input-row" id="daoForm"><input id="daoInput" maxlength="120" autocomplete="off" placeholder="向道长问一句，例如：我最近工作很迷茫，该怎么做？"><button class="dao-send" type="submit" aria-label="发送"><svg viewBox="0 0 24 24" fill="none"><path d="M4 12.5 20 4l-5.5 16-3.1-6.1L4 12.5Z" stroke="currentColor" stroke-width="1.6" stroke-linejoin="round"/><path d="m11.4 13.9 3.7-4.1" stroke="currentColor" stroke-width="1.4" stroke-linecap="round"/></svg></button></form>
      <div class="dao-ai-note">本功能为本地规则与上下文推理互动，不是云端大模型。内容仅供文化娱乐与自我反思，不替代专业意见。</div>
    </div>`;
  const divider=hero.nextElementSibling;
  hero.parentNode.insertBefore(section,divider||null);

  const bubble=document.createElement('div');
  bubble.className='dao-float-bubble';
  bubble.id='daoFloatBubble';
  bubble.textContent='贫道在此。心中若有一事，可直言相问。';
  const slot=document.querySelector('.daoist-slot')||hero;
  slot.appendChild(bubble);

  const chat=document.getElementById('daoChat'),form=document.getElementById('daoForm'),input=document.getElementById('daoInput'),quick=document.getElementById('daoQuick'),panel=document.getElementById('daoAiPanel'),voiceBtn=document.getElementById('daoVoiceBtn'),statusText=document.getElementById('daoStatusText');
  let voiceOn=false,history=[];

  function addMsg(role,text){
    const d=document.createElement('div');d.className='dao-msg '+role;
    d.innerHTML=`<div class="bubble"><span class="who">${role==='master'?'道长':'访客'}</span>${escapeHtml(text)}</div>`;
    chat.appendChild(d);chat.scrollTop=chat.scrollHeight;
    history.push({role,text});if(history.length>12)history=history.slice(-12);
  }
  function escapeHtml(s){return s.replace(/[&<>"']/g,c=>({'&':'&amp;','<':'&lt;','>':'&gt;','"':'&quot;',"'":'&#039;'}[c]))}
  function typing(on){
    panel.classList.toggle('thinking',on);statusText.textContent=on?'凝神思量':'静候垂询';if(master)master.classList.toggle('dao-thinking',on);
    const old=document.getElementById('daoTyping');if(old)old.remove();
    if(on){const d=document.createElement('div');d.className='dao-msg master';d.id='daoTyping';d.innerHTML='<div class="bubble"><span class="who">道长</span><span class="dao-typing"><i></i><i></i><i></i></span></div>';chat.appendChild(d);chat.scrollTop=chat.scrollHeight}
  }
  function speak(text){if(!voiceOn||!('speechSynthesis'in window))return;try{speechSynthesis.cancel();const u=new SpeechSynthesisUtterance(text);u.lang='zh-CN';u.rate=.88;u.pitch=.88;u.volume=.9;speechSynthesis.speak(u)}catch(e){}}
  function actSpeak(text){if(!master)return;master.classList.remove('dao-thinking');master.classList.add('dao-speaking');setTimeout(()=>master.classList.remove('dao-speaking'),Math.min(5200,1800+text.length*28))}
  function showBubble(text){bubble.textContent=text.length>46?text.slice(0,46)+'…':text;bubble.classList.add('show');setTimeout(()=>bubble.classList.remove('show'),4800)}

  function ctx(){
    const get=id=>document.getElementById(id)?.textContent?.trim()||'';
    const topic=document.getElementById('topic')?.value||'';
    const wish=document.getElementById('wish')?.value?.trim()||'';
    const birthDate=document.getElementById('birthDate')?.value||'';
    const birthTime=document.getElementById('birthTime')?.value||'';
    const hasLot=document.getElementById('interpret')?.classList.contains('show');
    return {topic,wish,birthDate,birthTime,hasLot,lotName:get('lotName'),lotRank:get('lotRank'),lotTitle:get('lotTitle'),lotBrief:get('lotBrief'),zName:get('zName'),zScore:get('zScore'),zSummary:get('zSummary'),lunar:get('lunarText'),term:get('solarTerm'),yi:[...document.querySelectorAll('#yiTags .tag')].map(x=>x.textContent).join('、'),ji:[...document.querySelectorAll('#jiTags .tag')].map(x=>x.textContent).join('、'),farm:get('farmTip'),baziText:document.getElementById('baziResult')?.classList.contains('empty')?'':get('baziResult')};
  }

  const phrases={
    career:['事业之事，先看“能否形成结果”，再看“是否值得长期投入”。','若局势未明，先完成一个可验证的小节点，比反复推演更有用。','眼下若感到阻滞，不妨先减去一件最耗能却最无结果的事。'],
    love:['情缘之事，不宜只凭猜测。真正有价值的，是双方能否清楚表达与回应。','若你想主动，可先从一次轻而真诚的沟通开始，不必把结果压得太重。','关系若长期只靠一方维持，就应把“是否双向”作为重要判断。'],
    money:['财运首先不是求快，而是守住风险边界与现金流。','凡涉及大额金钱，宁可慢半步，多核对一次，也胜过情绪决策。','把不可控的收益想象先放下，先看你能控制的支出、储备与能力。'],
    health:['身心若有持续不适，现实中的专业检查应优先于任何运势解读。','先把睡眠、饮食与恢复节奏稳住，再谈其他改变。','疲惫时硬撑往往不是勇敢，而是让判断变差。'],
    study:['学业最怕目标太大而每天没有闭环。先完成一小段，再复盘。','把错题与薄弱点分类，比单纯增加数量更有效。','今天若只能做一件事，就做最能降低未来难度的那件事。']
  };

  function choose(arr,seed){return arr[Math.abs(seed)%arr.length]}
  function hash(s){let h=0;for(let i=0;i<s.length;i++)h=((h<<5)-h+s.charCodeAt(i))|0;return h}
  function lastUser(){return [...history].reverse().find(x=>x.role==='user')?.text||''}

  function answer(q){
    const c=ctx(),s=q.toLowerCase(),seed=hash(q+Date.now().toString().slice(0,-4));
    let intro='你这一问，宜先把“想要什么”与“能做什么”分开看。';
    let core='';
    if(/事业|工作|职业|升职|老板|项目|创业|辞职/.test(s))core=choose(phrases.career,seed);
    else if(/感情|爱情|对象|喜欢|表白|恋爱|婚姻|复合/.test(s))core=choose(phrases.love,seed);
    else if(/钱|财运|投资|收入|赚钱|股票|基金|生意/.test(s))core=choose(phrases.money,seed);
    else if(/身体|健康|睡眠|生病|焦虑|累|疲惫/.test(s))core=choose(phrases.health,seed);
    else if(/学习|考试|学业|读书|考研|考公/.test(s))core=choose(phrases.study,seed);
    else if(/今天|宜|忌|适合/.test(s))core=`今天农家历显示，宜：${c.yi||'整理、静心'}；忌：${c.ji||'冲动、急躁'}。可把它当作提醒：先做确定性高的事，少做情绪化决定。`;
    else if(/签|解签|刚才/.test(s)&&c.hasLot)core=`你刚才所得“${c.lotName}”为${c.lotRank}，签题“${c.lotTitle}”。${c.lotBrief||'其意更偏向稳住当下，再看时机。'}`;
    else if(/星座|星运/.test(s))core=`你当前查看的是${c.zName||'所选星座'}，综合指数约${c.zScore||'—'}。${c.zSummary||'今天更适合用小步行动验证判断。'}`;
    else if(/八字|生辰|五行/.test(s))core=c.birthDate?`你已填写生辰 ${c.birthDate}${c.birthTime?' '+c.birthTime:''}。页面里的八字属于简化娱乐排盘，可重点参考“五行倾向”和分项建议，不宜把它当作确定命运。`:'你还没有填写生辰。若愿意，可先在“生辰”区域填写出生日期与大概时辰，再来问我结合解读。';
    else core='若一件事让你反复纠结，通常不是缺一个“答案”，而是还没把风险、底线和下一步分清。先选一个最小可执行动作，会比继续空想更接近答案。';

    const context=[];
    if(c.topic)context.push(`你当前所问主题是“${c.topic}”`);
    if(c.wish)context.push(`心中所念是“${c.wish}”`);
    if(c.hasLot&&!/签|刚才/.test(s))context.push(`刚抽到的是“${c.lotName}·${c.lotRank}”`);
    if(c.zName&&!/星座|星运/.test(s))context.push(`当前星座页为${c.zName}`);
    let tail='';
    if(context.length)tail=`结合页面当前信息：${context.join('，')}。`;
    if(/怎么办|怎么做|建议|该不该|可以吗|能不能/.test(s))tail+='若要落到现实行动，我建议你先写下“最坏结果能否承受、最小一步是什么、多久复盘一次”这三项。';
    else tail+='把这段话当作自我整理的镜子即可，真正决定仍应依据现实信息。';
    return `${intro}${core}${tail}`;
  }

  function submit(q){q=(q||'').trim();if(!q)return;addMsg('user',q);input.value='';typing(true);const delay=520+Math.min(900,q.length*14);setTimeout(()=>{typing(false);const text=answer(q);addMsg('master',text);actSpeak(text);showBubble(text);speak(text)},delay)}
  form.addEventListener('submit',e=>{e.preventDefault();submit(input.value)});
  quick.addEventListener('click',e=>{const b=e.target.closest('button');if(b)submit(b.textContent)});
  voiceBtn.addEventListener('click',()=>{voiceOn=!voiceOn;voiceBtn.classList.toggle('on',voiceOn);voiceBtn.textContent='语音：'+(voiceOn?'开':'关');if(!voiceOn&&'speechSynthesis'in window)speechSynthesis.cancel()});

  addMsg('master','贫道已候在此。你可以直接问事业、情缘、财运、今天宜忌，也可以先抽签或填写生辰，再让我结合页面结果与你细说。');
  setTimeout(()=>{if(master){master.classList.add('dao-greeting');setTimeout(()=>master.classList.remove('dao-greeting'),2600)}bubble.classList.add('show');setTimeout(()=>bubble.classList.remove('show'),4200)},900);
})();