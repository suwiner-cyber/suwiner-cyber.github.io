(()=>{
  if(window.__XQ_THREE_V71)return;window.__XQ_THREE_V71=true;
  const reduce=window.matchMedia&&window.matchMedia('(prefers-reduced-motion: reduce)').matches;
  const waitSlot=(n=0)=>{const slot=document.querySelector('.daoist-slot');if(slot)return boot(slot);if(n<100)setTimeout(()=>waitSlot(n+1),100)};
  async function boot(slot){
    const stage=document.createElement('div');stage.className='three-v7-stage';stage.innerHTML='<div class="three-v7-loading">正在加载 WebGL 3D 道长与仙鹤…</div><div class="three-v7-badge"><b>WEBGL 3D</b> · GLB 骨骼动画</div><div class="three-v7-fallback">3D 模型加载失败，已自动保留原人物兜底。</div>';slot.appendChild(stage);
    const setLoading=t=>{const e=stage.querySelector('.three-v7-loading');if(e)e.textContent=t};
    try{
      setLoading('正在加载 Three.js 3D 引擎…');
      const THREE=await import('three');
      setLoading('正在加载 GLB 模型解析器…');
      const {GLTFLoader}=await import('https://cdn.jsdelivr.net/npm/three@0.179.1/examples/jsm/loaders/GLTFLoader.js');
      const renderer=new THREE.WebGLRenderer({antialias:true,alpha:true,powerPreference:'high-performance'});
      renderer.setPixelRatio(Math.min(devicePixelRatio||1,1.8));renderer.outputColorSpace=THREE.SRGBColorSpace;renderer.toneMapping=THREE.ACESFilmicToneMapping;renderer.toneMappingExposure=1.12;renderer.shadowMap.enabled=true;renderer.shadowMap.type=THREE.PCFSoftShadowMap;stage.prepend(renderer.domElement);
      const scene=new THREE.Scene();
      const camera=new THREE.PerspectiveCamera(28,1,.1,100);camera.position.set(0,1.72,5.45);camera.lookAt(0,1.55,0);
      scene.add(new THREE.HemisphereLight(0xf1d5a1,0x12181a,1.7));
      const key=new THREE.DirectionalLight(0xffd7a0,3.3);key.position.set(-2.2,4.6,3.7);key.castShadow=true;key.shadow.mapSize.set(1024,1024);scene.add(key);
      const rim=new THREE.DirectionalLight(0x7998a6,1.7);rim.position.set(3.2,2.5,-2.4);scene.add(rim);
      const warm=new THREE.PointLight(0xc46b31,18,5,2);warm.position.set(-1.6,.9,2.1);scene.add(warm);
      const ground=new THREE.Mesh(new THREE.CircleGeometry(1.35,64),new THREE.MeshStandardMaterial({color:0x17110d,roughness:.96,metalness:0,transparent:true,opacity:.82}));ground.rotation.x=-Math.PI/2;ground.position.y=.025;ground.receiveShadow=true;scene.add(ground);
      const loader=new GLTFLoader();loader.setCrossOrigin('anonymous');
      const load=(url,label)=>new Promise((resolve,reject)=>{setLoading(`正在加载${label}…`);loader.load(url,resolve,p=>{if(p.total)setLoading(`${label} ${Math.min(99,Math.round(p.loaded/p.total*100))}%`)},reject)});
      const humanGLTF=await load('https://threejs.org/examples/models/gltf/Soldier.glb','3D 道长骨骼');
      const birdGLTF=await load('https://threejs.org/examples/models/gltf/Flamingo.glb','3D 仙鹤骨骼');
      const humanRoot=new THREE.Group();humanRoot.position.set(-.05,0,0);scene.add(humanRoot);
      const human=humanGLTF.scene;human.scale.setScalar(1.08);human.rotation.y=.06;humanRoot.add(human);
      const skinMat=new THREE.MeshStandardMaterial({color:0xb47d5f,roughness:.78});
      human.traverse(o=>{if(o.isMesh){o.castShadow=true;o.receiveShadow=true;if(/head|face|skin|hand/i.test(o.name)){o.material=skinMat.clone()}else{o.material=o.material.clone();if(o.material.color)o.material.color.lerp(new THREE.Color(0x233237),.76);o.material.roughness=.78;o.material.metalness=.015}}});
      const robeMat=new THREE.MeshPhysicalMaterial({color:0x20343a,roughness:.84,metalness:.02,clearcoat:.06,side:THREE.DoubleSide});
      const robe=new THREE.Mesh(new THREE.CylinderGeometry(.45,.65,1.58,56,1,true),robeMat);robe.position.set(0,.83,.03);robe.castShadow=true;humanRoot.add(robe);
      const robeFront=new THREE.Mesh(new THREE.ConeGeometry(.55,1.18,56,1,true),new THREE.MeshPhysicalMaterial({color:0x304950,roughness:.75,metalness:.015,side:THREE.DoubleSide,transparent:true,opacity:.9}));robeFront.position.set(0,.59,.22);robeFront.rotation.x=-.08;humanRoot.add(robeFront);
      const sash=new THREE.Mesh(new THREE.TorusGeometry(.43,.047,12,64),new THREE.MeshStandardMaterial({color:0x9c7448,roughness:.62,metalness:.16}));sash.rotation.x=Math.PI/2;sash.position.y=1.18;humanRoot.add(sash);
      const bun=new THREE.Mesh(new THREE.SphereGeometry(.14,28,20),new THREE.MeshStandardMaterial({color:0x101212,roughness:.94}));bun.scale.y=1.22;bun.position.set(0,2.86,-.015);humanRoot.add(bun);
      const pin=new THREE.Mesh(new THREE.CylinderGeometry(.018,.018,.42,16),new THREE.MeshStandardMaterial({color:0xb58a54,roughness:.55,metalness:.35}));pin.rotation.z=Math.PI/2;pin.position.set(0,2.86,.01);humanRoot.add(pin);
      const beard=new THREE.Mesh(new THREE.ConeGeometry(.105,.55,24),new THREE.MeshStandardMaterial({color:0x242423,roughness:1}));beard.position.set(0,2.18,.23);beard.rotation.x=.12;humanRoot.add(beard);
      const sleeveMat=new THREE.MeshStandardMaterial({color:0x2a3f45,roughness:.86});const sleeveL=new THREE.Mesh(new THREE.ConeGeometry(.22,.62,24),sleeveMat);sleeveL.position.set(-.49,1.48,.04);sleeveL.rotation.z=-.58;humanRoot.add(sleeveL);const sleeveR=sleeveL.clone();sleeveR.position.x=.49;sleeveR.rotation.z=.58;humanRoot.add(sleeveR);
      const mixer=new THREE.AnimationMixer(human);if(humanGLTF.animations.length){const clip=humanGLTF.animations.find(a=>/idle/i.test(a.name))||humanGLTF.animations[0];mixer.clipAction(clip).play()}
      let rightArm=null,headBone=null;human.traverse(o=>{if(!rightArm&&o.isBone&&/right.*arm|arm.*r/i.test(o.name))rightArm=o;if(!headBone&&o.isBone&&/head/i.test(o.name))headBone=o});
      const craneRoot=new THREE.Group();craneRoot.visible=false;scene.add(craneRoot);const bird=birdGLTF.scene;bird.scale.setScalar(.0088);bird.rotation.y=-Math.PI/2;craneRoot.add(bird);
      bird.traverse(o=>{if(o.isMesh){o.castShadow=true;o.material=o.material.clone();if(o.material.color)o.material.color.set(0xf1efe7);o.material.roughness=.72}});
      const redCrown=new THREE.Mesh(new THREE.SphereGeometry(.055,20,14),new THREE.MeshStandardMaterial({color:0xa62f27,roughness:.65}));redCrown.position.set(.13,.29,.02);craneRoot.add(redCrown);
      const wingMat=new THREE.MeshStandardMaterial({color:0x1c2221,side:THREE.DoubleSide,roughness:.92});const wingL=new THREE.Mesh(new THREE.PlaneGeometry(.34,.18),wingMat);wingL.position.set(-.11,.06,.08);wingL.rotation.set(-.2,.2,-.32);craneRoot.add(wingL);const wingR=wingL.clone();wingR.position.z=-.08;wingR.rotation.z=.32;craneRoot.add(wingR);
      const birdMixer=new THREE.AnimationMixer(bird);if(birdGLTF.animations.length)birdMixer.clipAction(birdGLTF.animations[0]).play();
      let mouseX=0,mouseY=0,targetX=0,targetZ=0,lastX=0,lastY=0,lastT=performance.now(),fear=0,startled=0,petting=0,cranePhase='wait',phaseT=0;
      const clamp=(v,a,b)=>Math.max(a,Math.min(b,v));
      const showSpeech=text=>{const b=document.querySelector('.dao-v62-bubble')||document.querySelector('.dao-float-bubble');if(b){b.textContent=text;b.classList.add('show');setTimeout(()=>b.classList.remove('show'),1200)}};
      const onMove=e=>{if(e.pointerType&&e.pointerType!=='mouse')return;const r=slot.getBoundingClientRect();if(!r.width)return;const nx=(e.clientX-r.left)/r.width*2-1,ny=(e.clientY-r.top)/r.height*2-1;mouseX=nx;mouseY=ny;const dist=Math.hypot(nx*.8,ny*.75);fear=clamp(1-dist,0,1);targetX=clamp(-nx*.54*fear,-.48,.48);targetZ=clamp(ny*.14*fear,-.12,.12);const t=performance.now(),speed=Math.hypot(e.clientX-lastX,e.clientY-lastY)/Math.max(8,t-lastT);lastX=e.clientX;lastY=e.clientY;lastT=t;if(speed>.8&&fear>.38){startled=1;showSpeech('呀！道友慢些……')}};window.addEventListener('pointermove',onMove,{passive:true});
      const startCrane=()=>{if(cranePhase!=='wait')return;cranePhase='in';phaseT=0;craneRoot.visible=true;craneRoot.position.set(3.6,3.35,-.35);showSpeech('白鹤归来。')};setTimeout(startCrane,2600);
      const clock=new THREE.Clock();let total=0;
      function craneStep(dt){phaseT+=dt;if(cranePhase==='wait')return;if(cranePhase==='in'){const u=clamp(phaseT/4.1,0,1),e=1-Math.pow(1-u,3);craneRoot.position.set(3.6*(1-e)+.62*e,3.35*(1-e)+.48*e,-.35*(1-e)+.22*e);craneRoot.rotation.z=Math.sin(u*Math.PI*4)*.08;if(u>=1){cranePhase='rest';phaseT=0;showSpeech('来，歇一歇。')}}else if(cranePhase==='rest'){craneRoot.position.y=.48+Math.sin(total*2.4)*.012;if(phaseT>1.3){cranePhase='pet';phaseT=0;petting=1;showSpeech('乖。')}}else if(cranePhase==='pet'){craneRoot.position.x=.58+Math.sin(phaseT*3.2)*.018;if(phaseT>2.8){cranePhase='out';phaseT=0;petting=0;showSpeech('去吧，山高云阔。')}}else if(cranePhase==='out'){const u=clamp(phaseT/3.5,0,1),e=u*u;craneRoot.position.set(.62*(1-e)-3.9*e,.48*(1-e)+3.15*e,.22*(1-e)-.5*e);craneRoot.rotation.z=-u*.18;if(u>=1){craneRoot.visible=false;cranePhase='wait';phaseT=0;setTimeout(startCrane,17000+Math.random()*8000)}}}
      function resize(){const r=stage.getBoundingClientRect(),w=Math.max(1,r.width),h=Math.max(1,r.height);renderer.setSize(w,h,false);camera.aspect=w/h;camera.updateProjectionMatrix()}const ro=new ResizeObserver(resize);ro.observe(stage);resize();
      renderer.render(scene,camera);document.body.classList.add('three-ready');setLoading('真实 3D 已加载');
      function animate(){const dt=Math.min(clock.getDelta(),.05);total+=dt;mixer.update(dt*.65);birdMixer.update(dt*1.2);craneStep(dt);humanRoot.position.x+=(targetX-humanRoot.position.x)*.08;humanRoot.position.z+=(targetZ-humanRoot.position.z)*.06;humanRoot.rotation.z+=((-mouseX*fear*.075)-humanRoot.rotation.z)*.08;humanRoot.rotation.y+=((mouseX*fear*.14+.04)-humanRoot.rotation.y)*.08;if(startled>0){startled=Math.max(0,startled-dt*2.1);humanRoot.position.y=Math.sin(startled*Math.PI*5)*.055*startled;humanRoot.scale.set(1-startled*.018,1+startled*.026,1-startled*.018)}else{humanRoot.position.y=Math.sin(total*1.2)*.008;humanRoot.scale.lerp(new THREE.Vector3(1,1,1),.08)}if(headBone){headBone.rotation.y+=(clamp(mouseX*.26,-.22,.22)-headBone.rotation.y)*.05;headBone.rotation.x+=(clamp(-mouseY*.09,-.08,.08)-headBone.rotation.x)*.05}if(rightArm){rightArm.rotation.z+=((petting?-.78:-.08)-rightArm.rotation.z)*.055;rightArm.rotation.x+=((petting?-.38:0)-rightArm.rotation.x)*.055}robe.rotation.z=Math.sin(total*.8)*.008;sleeveL.rotation.x=Math.sin(total*1.1)*.02;sleeveR.rotation.x=-Math.sin(total*1.05)*.02;renderer.render(scene,camera);requestAnimationFrame(animate)}if(!reduce)requestAnimationFrame(animate);
    }catch(err){console.error('Xuanqian WebGL 3D load failed',err);stage.classList.add('failed');setLoading('WebGL 3D 加载失败 · 使用旧版兜底');}
  }
  waitSlot();
})();