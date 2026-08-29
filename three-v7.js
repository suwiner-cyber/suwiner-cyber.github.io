(()=>{
  if(window.__XQ_THREE_V80)return;window.__XQ_THREE_V80=true;
  const reduce=window.matchMedia&&window.matchMedia('(prefers-reduced-motion: reduce)').matches;
  const HUMAN_MODELS=[
    'https://api.meshy.ai/v1/models/019da9f6-5577-7976-8b44-9c2746482921/model.glb',
    'https://api.meshy.ai/v1/models/019db082-350d-7243-8e42-6a0f334432ce/model.glb'
  ];
  const BIRD_MODEL='https://threejs.org/examples/models/gltf/Flamingo.glb';
  const waitSlot=(n=0)=>{const slot=document.querySelector('.daoist-slot');if(slot)return boot(slot);if(n<100)setTimeout(()=>waitSlot(n+1),100)};
  async function boot(slot){
    document.body.classList.remove('three-ready');
    slot.querySelectorAll('.three-v7-stage').forEach(e=>e.remove());
    const stage=document.createElement('div');stage.className='three-v7-stage';stage.innerHTML='<div class="three-v7-loading">正在加载写实道长 3D 模型…</div><div class="three-v7-badge"><b>PHOTOREAL 3D</b> · PBR 写实人物</div><div class="three-v7-fallback">写实模型加载失败，暂时保留原人物。</div>';slot.appendChild(stage);
    const loading=stage.querySelector('.three-v7-loading');const setLoading=t=>{if(loading)loading.textContent=t};
    try{
      const THREE=await import('three');
      const [{GLTFLoader},{RoomEnvironment}]=await Promise.all([
        import('https://cdn.jsdelivr.net/npm/three@0.179.1/examples/jsm/loaders/GLTFLoader.js'),
        import('https://cdn.jsdelivr.net/npm/three@0.179.1/examples/jsm/environments/RoomEnvironment.js')
      ]);
      const renderer=new THREE.WebGLRenderer({antialias:true,alpha:true,powerPreference:'high-performance',premultipliedAlpha:true});
      renderer.setPixelRatio(Math.min(window.devicePixelRatio||1,2));renderer.outputColorSpace=THREE.SRGBColorSpace;renderer.toneMapping=THREE.ACESFilmicToneMapping;renderer.toneMappingExposure=1.02;renderer.shadowMap.enabled=true;renderer.shadowMap.type=THREE.PCFSoftShadowMap;stage.prepend(renderer.domElement);
      const scene=new THREE.Scene();
      const pmrem=new THREE.PMREMGenerator(renderer);scene.environment=pmrem.fromScene(new RoomEnvironment(),.035).texture;pmrem.dispose();
      const camera=new THREE.PerspectiveCamera(29,1,.1,60);scene.add(camera);
      scene.add(new THREE.HemisphereLight(0xf3dbc1,0x111819,1.18));
      const key=new THREE.DirectionalLight(0xffd6a4,3.1);key.position.set(-2.8,4.8,4.2);key.castShadow=true;key.shadow.mapSize.set(1024,1024);key.shadow.bias=-.00018;scene.add(key);
      const fill=new THREE.DirectionalLight(0xd7c3a5,1.15);fill.position.set(2.6,2.2,3.6);scene.add(fill);
      const rim=new THREE.DirectionalLight(0x7893a0,2.0);rim.position.set(3.6,3.1,-3.2);scene.add(rim);
      const fire=new THREE.PointLight(0xc66e34,12,4.6,2);fire.position.set(-1.7,1.0,2.4);scene.add(fire);
      const ground=new THREE.Mesh(new THREE.CircleGeometry(1.5,72),new THREE.MeshStandardMaterial({color:0x12100e,roughness:.95,metalness:0,transparent:true,opacity:.72}));ground.rotation.x=-Math.PI/2;ground.position.y=.012;ground.receiveShadow=true;scene.add(ground);
      const loader=new GLTFLoader();loader.setCrossOrigin('anonymous');
      const load=(url,label)=>new Promise((resolve,reject)=>loader.load(url,resolve,p=>{if(p.total)setLoading(`${label} ${Math.min(99,Math.round(p.loaded/p.total*100))}%`)},reject));
      let humanGLTF=null,lastErr=null;
      for(let i=0;i<HUMAN_MODELS.length;i++){
        try{setLoading(i===0?'正在加载写实中国道长…':'正在尝试备用写实道长…');humanGLTF=await load(HUMAN_MODELS[i],'写实道长');break}catch(e){lastErr=e}
      }
      if(!humanGLTF)throw lastErr||new Error('Photoreal Taoist model unavailable');
      const humanRoot=new THREE.Group();scene.add(humanRoot);
      const human=humanGLTF.scene;humanRoot.add(human);
      human.updateMatrixWorld(true);
      const sourceBox=new THREE.Box3().setFromObject(human),sourceSize=sourceBox.getSize(new THREE.Vector3());
      if(!isFinite(sourceSize.y)||sourceSize.y<=0)throw new Error('Invalid Taoist model bounds');
      const desiredHeight=3.1,scale=desiredHeight/sourceSize.y;human.scale.setScalar(scale);human.updateMatrixWorld(true);
      let humanBox=new THREE.Box3().setFromObject(human),center=humanBox.getCenter(new THREE.Vector3());
      human.position.set(-center.x,-humanBox.min.y,-center.z);human.updateMatrixWorld(true);
      humanBox=new THREE.Box3().setFromObject(human);const modelSize=humanBox.getSize(new THREE.Vector3());
      human.traverse(o=>{if(o.isMesh){o.castShadow=true;o.receiveShadow=true;if(Array.isArray(o.material)){o.material.forEach(m=>{if(m){m.envMapIntensity=.82;m.needsUpdate=true}})}else if(o.material){o.material.envMapIntensity=.82;o.material.needsUpdate=true}if(o.material?.map)o.material.map.anisotropy=Math.min(8,renderer.capabilities.getMaxAnisotropy())}});
      let mixer=null;if(humanGLTF.animations?.length){mixer=new THREE.AnimationMixer(human);const idle=humanGLTF.animations.find(a=>/idle|breath|stand/i.test(a.name))||humanGLTF.animations[0];mixer.clipAction(idle).reset().fadeIn(.25).play()}
      let headBone=null,rightArm=null,spineBone=null;
      human.traverse(o=>{if(!o.isBone)return;const n=o.name.toLowerCase();if(!headBone&&/head/.test(n))headBone=o;if(!rightArm&&/(right.*arm|arm.*right|upperarm_r|upperarm\.r)/.test(n))rightArm=o;if(!spineBone&&/(spine2|chest|upperchest)/.test(n))spineBone=o});
      const birdGLTF=await load(BIRD_MODEL,'3D 仙鹤');
      const craneRoot=new THREE.Group();craneRoot.visible=false;scene.add(craneRoot);const bird=birdGLTF.scene;craneRoot.add(bird);
      bird.scale.setScalar(.0086);bird.rotation.y=-Math.PI/2;bird.traverse(o=>{if(o.isMesh){o.castShadow=true;o.receiveShadow=true;o.material=o.material.clone();if(o.material.color)o.material.color.set(0xf2f0e9);o.material.roughness=.72;o.material.envMapIntensity=.7}});
      const crown=new THREE.Mesh(new THREE.SphereGeometry(.052,20,14),new THREE.MeshStandardMaterial({color:0xa72e27,roughness:.67}));crown.position.set(.13,.285,.02);craneRoot.add(crown);
      const birdMixer=new THREE.AnimationMixer(bird);if(birdGLTF.animations?.length)birdMixer.clipAction(birdGLTF.animations[0]).play();
      let mouseX=0,mouseY=0,fear=0,targetX=0,targetZ=0,lastX=0,lastY=0,lastT=performance.now(),startled=0,petting=0,phase='wait',phaseT=0,total=0,craneTimer=0;
      const clamp=(v,a,b)=>Math.max(a,Math.min(b,v));
      const speech=text=>{const b=document.querySelector('.dao-v62-bubble')||document.querySelector('.dao-float-bubble');if(!b)return;b.textContent=text;b.classList.add('show');clearTimeout(b.__hide);b.__hide=setTimeout(()=>b.classList.remove('show'),1300)};
      const onMove=e=>{if(e.pointerType&&e.pointerType!=='mouse')return;const r=slot.getBoundingClientRect();if(!r.width||!r.height)return;const nx=(e.clientX-r.left)/r.width*2-1,ny=(e.clientY-r.top)/r.height*2-1;mouseX=nx;mouseY=ny;const d=Math.hypot(nx*.78,ny*.72);fear=clamp(1-d,0,1);targetX=clamp(-nx*.46*fear,-.42,.42);targetZ=clamp(ny*.11*fear,-.1,.1);const now=performance.now(),speed=Math.hypot(e.clientX-lastX,e.clientY-lastY)/Math.max(8,now-lastT);lastX=e.clientX;lastY=e.clientY;lastT=now;if(speed>.82&&fear>.34){startled=1;speech('呀！道友慢些……')}};window.addEventListener('pointermove',onMove,{passive:true});
      const startCrane=()=>{if(phase!=='wait'||document.hidden)return;phase='in';phaseT=0;craneRoot.visible=true;craneRoot.position.set(3.5,3.15,-.25);speech('白鹤归来。')};
      const scheduleCrane=ms=>{clearTimeout(craneTimer);craneTimer=setTimeout(startCrane,ms)};scheduleCrane(3000);
      const clock=new THREE.Clock();
      function craneStep(dt){phaseT+=dt;if(phase==='wait')return;if(phase==='in'){const u=clamp(phaseT/4,0,1),e=1-Math.pow(1-u,3);craneRoot.position.set(3.5*(1-e)+.72*e,3.15*(1-e)+.5*e,-.25*(1-e)+.18*e);craneRoot.rotation.z=Math.sin(u*Math.PI*5)*.065;if(u>=1){phase='rest';phaseT=0;speech('来，歇一歇。')}}else if(phase==='rest'){craneRoot.position.y=.5+Math.sin(total*2.2)*.012;if(phaseT>1.25){phase='pet';phaseT=0;petting=1;speech('乖。')}}else if(phase==='pet'){craneRoot.position.x=.72+Math.sin(phaseT*3)*.014;if(phaseT>2.7){phase='out';phaseT=0;petting=0;speech('去吧，山高云阔。')}}else if(phase==='out'){const u=clamp(phaseT/3.4,0,1),e=u*u;craneRoot.position.set(.72*(1-e)-3.8*e,.5*(1-e)+3.1*e,.18*(1-e)-.45*e);craneRoot.rotation.z=-u*.16;if(u>=1){craneRoot.visible=false;phase='wait';phaseT=0;scheduleCrane(19000+Math.random()*7000)}}}
      function fitCamera(){const r=stage.getBoundingClientRect(),w=Math.max(1,r.width),h=Math.max(1,r.height);renderer.setSize(w,h,false);camera.aspect=w/h;const vfov=THREE.MathUtils.degToRad(camera.fov),hfov=2*Math.atan(Math.tan(vfov/2)*camera.aspect);const distV=modelSize.y/(2*Math.tan(vfov/2)),distH=modelSize.x/(2*Math.tan(Math.max(.12,hfov)/2));const dist=Math.max(distV,distH)*1.16;camera.position.set(0,modelSize.y*.5,Math.min(13,Math.max(4.8,dist)));camera.lookAt(0,modelSize.y*.5,0);camera.updateProjectionMatrix()}
      new ResizeObserver(fitCamera).observe(stage);fitCamera();renderer.render(scene,camera);document.body.classList.add('three-ready');setLoading('写实道长已加载');
      function animate(){const dt=Math.min(clock.getDelta(),.05);total+=dt;if(mixer)mixer.update(dt*.72);birdMixer.update(dt*1.15);craneStep(dt);humanRoot.position.x+=(targetX-humanRoot.position.x)*.075;humanRoot.position.z+=(targetZ-humanRoot.position.z)*.06;humanRoot.rotation.z+=((-mouseX*fear*.055)-humanRoot.rotation.z)*.075;humanRoot.rotation.y+=((mouseX*fear*.11)-humanRoot.rotation.y)*.07;if(startled>0){startled=Math.max(0,startled-dt*2);humanRoot.position.y=Math.sin(startled*Math.PI*5)*.045*startled;humanRoot.scale.set(1-startled*.012,1+startled*.02,1-startled*.012)}else{humanRoot.position.y=Math.sin(total*.95)*.006;humanRoot.scale.lerp(new THREE.Vector3(1,1,1),.08)}if(headBone){headBone.rotation.y+=(clamp(mouseX*.2,-.18,.18)-headBone.rotation.y)*.045;headBone.rotation.x+=(clamp(-mouseY*.07,-.06,.06)-headBone.rotation.x)*.045}if(rightArm){rightArm.rotation.z+=((petting?-.48:0)-rightArm.rotation.z)*.04;rightArm.rotation.x+=((petting?-.28:0)-rightArm.rotation.x)*.04}else if(spineBone){spineBone.rotation.z+=((petting?.045:0)-spineBone.rotation.z)*.035}renderer.render(scene,camera);requestAnimationFrame(animate)}
      if(!reduce)requestAnimationFrame(animate);
      document.addEventListener('visibilitychange',()=>{if(!document.hidden&&phase==='wait')scheduleCrane(3500)});
    }catch(err){console.error('Photoreal Taoist 3D load failed',err);stage.classList.add('failed');setLoading('写实 3D 模型加载失败 · 已保留旧人物兜底');document.body.classList.remove('three-ready')}
  }
  waitSlot();
})();