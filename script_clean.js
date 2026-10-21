
const MP_TEX='...';
const MP_V=`#version 300 es
layout(location=0) in vec2 a_position;
uniform vec2 u_resolution;uniform float u_imageAspect;uniform float u_scale;
out vec2 v_imageUV;
void main(){gl_Position=vec4(a_position,0.0,1.0);vec2 uv=a_position*0.5;
vec2 box;box.x=min(u_resolution.x/u_imageAspect,u_resolution.y)*u_imageAspect;box.y=box.x/u_imageAspect;
v_imageUV=uv*(u_resolution/box)/u_scale;v_imageUV+=0.5;v_imageUV.y=1.0-v_imageUV.y;}`;
const MP_F=`#version 300 es
precision highp float;
uniform sampler2D u_image;uniform vec2 u_resolution;uniform float u_time;uniform vec4 u_colorBack;uniform vec4 u_colorTint;
uniform float u_softness;uniform float u_repetition;uniform float u_shiftRed;uniform float u_shiftBlue;uniform float u_distortion;
uniform float u_contour;uniform float u_angle;uniform vec2 u_tilt;uniform float u_reveal;
in vec2 v_imageUV;out vec4 fragColor;
#define PI 3.14159265358979323846
vec2 rotate(vec2 uv,float th){return mat2(cos(th),sin(th),-sin(th),cos(th))*uv;}
vec3 permute(vec3 x){return mod(((x*34.0)+1.0)*x,289.0);}
float snoise(vec2 v){const vec4 C=vec4(0.211324865405187,0.366025403784439,-0.577350269189626,0.024390243902439);
vec2 i=floor(v+dot(v,C.yy));vec2 x0=v-i+dot(i,C.xx);vec2 i1=(x0.x>x0.y)?vec2(1.0,0.0):vec2(0.0,1.0);
vec4 x12=x0.xyxy+C.xxzz;x12.xy-=i1;i=mod(i,289.0);
vec3 p=permute(permute(i.y+vec3(0.0,i1.y,1.0))+i.x+vec3(0.0,i1.x,1.0));
vec3 m=max(0.5-vec3(dot(x0,x0),dot(x12.xy,x12.xy),dot(x12.zw,x12.zw)),0.0);m=m*m;m=m*m;
vec3 x=2.0*fract(p*C.www)-1.0;vec3 h=abs(x)-0.5;vec3 ox=floor(x+0.5);vec3 a0=x-ox;
m*=1.79284291400159-0.85373472095314*(a0*a0+h*h);
vec3 g;g.x=a0.x*x0.x+h.x*x0.y;g.yz=a0.yz*x12.xz+h.yz*x12.yw;return 130.0*dot(m,g);}
float getColorChanges(float c1,float c2,float stripe,vec3 w,float blur,float bump,float tint){
float ch=mix(c2,c1,smoothstep(0.0,2.0*blur,stripe));float border=w[0];
ch=mix(ch,c2,smoothstep(border,border+2.0*blur,stripe));
bump=smoothstep(0.2,0.8,bump);
border=w[0]+0.4*(1.0-bump)*w[1];ch=mix(ch,c1,smoothstep(border,border+2.0*blur,stripe));
border=w[0]+0.5*(1.0-bump)*w[1];ch=mix(ch,c2,smoothstep(border,border+2.0*blur,stripe));
border=w[0]+w[1];ch=mix(ch,c1,smoothstep(border,border+2.0*blur,stripe));
float gradientT=(stripe-w[0]-w[1])/w[2];float gradient=mix(c1,c2,smoothstep(0.0,1.0,gradientT));
ch=mix(ch,gradient,smoothstep(border,border+0.5*blur,stripe));
ch=mix(ch,1.0-min(1.0,(1.0-ch)/max(tint,0.0001)),u_colorTint.a);return ch;}
float blurEdge3x3(sampler2D tex,vec2 uv,vec2 dudx,vec2 dudy,float radius,float cs){
vec2 r=radius/vec2(textureSize(tex,0));float s=4.0*cs;
s+=2.0*textureGrad(tex,uv+vec2(0.0,-r.y),dudx,dudy).r;s+=2.0*textureGrad(tex,uv+vec2(0.0,r.y),dudx,dudy).r;
s+=2.0*textureGrad(tex,uv+vec2(-r.x,0.0),dudx,dudy).r;s+=2.0*textureGrad(tex,uv+vec2(r.x,0.0),dudx,dudy).r;
s+=textureGrad(tex,uv+vec2(-r.x,-r.y),dudx,dudy).r;s+=textureGrad(tex,uv+vec2(r.x,-r.y),dudx,dudy).r;
s+=textureGrad(tex,uv+vec2(-r.x,r.y),dudx,dudy).r;s+=textureGrad(tex,uv+vec2(r.x,r.y),dudx,dudy).r;return s/16.0;}
void main(){
float t=0.3*(u_time+2.8);vec2 uv=v_imageUV;vec2 dudx=dFdx(v_imageUV);vec2 dudy=dFdy(v_imageUV);
vec4 img=textureGrad(u_image,uv,dudx,dudy);float cycleWidth=u_repetition;
vec2 rotatedUV=uv-vec2(0.5);float angle=(-u_angle+70.0)*PI/180.0;float cosA=cos(angle);float sinA=sin(angle);
rotatedUV=vec2(rotatedUV.x*cosA-rotatedUV.y*sinA,rotatedUV.x*sinA+rotatedUV.y*cosA)+vec2(0.5);
float edge=blurEdge3x3(u_image,uv,dudx,dudy,6.0,img.r);edge=pow(edge,1.6);edge*=smoothstep(0.0,0.4,u_contour);
float opacity=img.g;float depth=img.r;
float diagBLtoTR=rotatedUV.x-rotatedUV.y;float diagTLtoBR=rotatedUV.x+rotatedUV.y;
vec3 color1=vec3(0.98,0.98,1.0);vec3 color2=vec3(0.1,0.1,0.1+0.1*smoothstep(0.7,1.3,diagTLtoBR));
vec2 gradUV=uv-0.5-u_tilt*vec2(0.18,-0.12);float dist=length(gradUV+vec2(0.0,0.2*diagBLtoTR));
gradUV=rotate(gradUV,(0.25-0.2*diagBLtoTR)*PI);float direction=gradUV.x;
float bump=pow(1.8*dist,1.2);bump=1.0-bump;bump*=pow(uv.y,0.3);
float thinStrip1=0.12/cycleWidth*(1.0-0.4*bump);float thinStrip2=0.07/cycleWidth*(1.0+0.4*bump);float wideStrip=1.0-thinStrip1-thinStrip2;
float noise=snoise(uv-t);edge+=(1.0-edge)*u_distortion*noise;
direction+=diagBLtoTR;
direction-=2.0*noise*diagBLtoTR*(smoothstep(0.0,1.0,edge)*(1.0-smoothstep(0.0,1.0,edge)));
direction*=mix(1.0,1.0-edge,smoothstep(0.5,1.0,u_contour));
direction-=1.7*edge*smoothstep(0.5,1.0,u_contour);
direction+=0.2*pow(u_contour,4.0)*(1.0-smoothstep(0.0,1.0,edge));
bump*=clamp(pow(uv.y,0.1),0.3,1.0);direction*=0.1+(1.1-edge)*bump;
direction*=0.4+0.6*(1.0-smoothstep(0.5,1.0,edge));
direction+=0.18*(smoothstep(0.1,0.2,uv.y)*(1.0-smoothstep(0.2,0.4,uv.y)));
direction+=0.03*(smoothstep(0.1,0.2,1.0-uv.y)*(1.0-smoothstep(0.2,0.4,1.0-uv.y)));
direction*=0.5+0.5*pow(uv.y,2.0);direction*=cycleWidth;direction-=t+dot(u_tilt,vec2(0.35,0.2));
float colorDispersion=clamp(1.0-bump,0.0,1.0);float dispersionRed=colorDispersion;
dispersionRed+=0.03*bump*noise;
dispersionRed+=5.0*(smoothstep(-0.1,0.2,uv.y)*(1.0-smoothstep(0.1,0.5,uv.y)))*(smoothstep(0.4,0.6,bump)*(1.0-smoothstep(0.4,1.0,bump)));
dispersionRed-=diagBLtoTR;
float dispersionBlue=colorDispersion*1.3;
dispersionBlue+=(smoothstep(0.0,0.4,uv.y)*(1.0-smoothstep(0.1,0.8,uv.y)))*(smoothstep(0.4,0.6,bump)*(1.0-smoothstep(0.4,0.8,bump)));
dispersionBlue-=0.2*edge;
dispersionRed*=u_shiftRed/20.0;dispersionBlue*=u_shiftBlue/20.0;
float softness=0.05*u_softness;
float blur=softness+0.5*smoothstep(1.0,10.0,u_repetition)*smoothstep(0.0,1.0,edge);
float smallCanvas=1.0-smoothstep(100.0,500.0,min(u_resolution.x,u_resolution.y));
blur+=smallCanvas*smoothstep(0.0,1.0,edge);
float rExtraBlur=softness*(0.05+0.1*(u_shiftRed/20.0)*bump);
float gExtraBlur=softness*0.05/max(0.001,abs(1.0-diagBLtoTR));
vec3 w=vec3(thinStrip1*cycleWidth,thinStrip2*cycleWidth,wideStrip);w[1]-=0.02*smoothstep(0.0,1.0,edge+bump);
float stripeR=fract(direction+dispersionRed);
float r=getColorChanges(color1.r,color2.r,stripeR,w,blur+fwidth(stripeR)+rExtraBlur,bump,u_colorTint.r);
float stripeG=fract(direction);
float g=getColorChanges(color1.g,color2.g,stripeG,w,blur+fwidth(stripeG)+gExtraBlur,bump,u_colorTint.g);
float stripeB=fract(direction-dispersionBlue);
float b=getColorChanges(color1.b,color2.b,stripeB,w,blur+fwidth(stripeB),bump,u_colorTint.b);
vec3 sheen=mix(vec3(1.0),u_colorTint.rgb,0.45*u_colorTint.a);r*=sheen.r;g*=sheen.g;b*=sheen.b;
float pour=u_reveal*1.25-0.1;
opacity*=smoothstep(1.0-pour,1.15-pour,depth)*smoothstep(0.0,0.25,u_reveal);
vec3 color=vec3(r,g,b)*opacity;vec3 back=u_colorBack.rgb*u_colorBack.a;color+=back*(1.0-opacity);opacity+=u_colorBack.a*(1.0-opacity);
color+=1.0/256.0*(fract(sin(dot(0.014*gl_FragCoord.xy,vec2(12.9898,78.233)))*43758.5453123)-0.5);
fragColor=vec4(color,opacity);}`;
function mpLogo(host){
const cv=host.querySelector('canvas');const gl=cv.getContext('webgl2',{alpha:true,premultipliedAlpha:true,antialias:false});
const reduce=matchMedia('(prefers-reduced-motion: reduce)').matches;
const fit=()=>{const r=host.getBoundingClientRect(),d=Math.max(devicePixelRatio||1,2);cv.width=Math.max(1,Math.round(r.width*d));cv.height=Math.max(1,Math.round(r.height*d));};
const img=new Image();let alive=true,raf=0;
if(!gl){img.onload=()=>{fit();const c=cv.getContext('2d'),k=Math.min(cv.width/img.width,cv.height/img.height)*0.9,w=img.width*k,h=img.height*k;const o=document.createElement('canvas');o.width=img.width;o.height=img.height;const x=o.getContext('2d');x.drawImage(img,0,0);const dd=x.getImageData(0,0,o.width,o.height);for(let i=0;i<dd.data.length;i+=4){dd.data[i+3]=dd.data[i+1];dd.data[i]=dd.data[i+1]=dd.data[i+2]=245;}x.putImageData(dd,0,0);c.drawImage(o,(cv.width-w)/2,(cv.height-h)/2,w,h);};img.src=MP_TEX;return()=>{};}
const sh=(ty,s)=>{const q=gl.createShader(ty);gl.shaderSource(q,s);gl.compileShader(q);return q;};
const pr=gl.createProgram();gl.attachShader(pr,sh(gl.VERTEX_SHADER,MP_V));gl.attachShader(pr,sh(gl.FRAGMENT_SHADER,MP_F));gl.linkProgram(pr);
if(!gl.getProgramParameter(pr,gl.LINK_STATUS))return()=>{};
gl.useProgram(pr);
const U={};['u_image','u_resolution','u_time','u_imageAspect','u_scale','u_colorBack','u_colorTint','u_softness','u_repetition','u_shiftRed','u_shiftBlue','u_distortion','u_contour','u_angle','u_tilt','u_reveal'].forEach(k=>U[k]=gl.getUniformLocation(pr,k));
const vao=gl.createVertexArray(),buf=gl.createBuffer();gl.bindVertexArray(vao);gl.bindBuffer(gl.ARRAY_BUFFER,buf);
gl.bufferData(gl.ARRAY_BUFFER,new Float32Array([-1,-1,1,-1,-1,1,1,1]),gl.STATIC_DRAW);gl.enableVertexAttribArray(0);gl.vertexAttribPointer(0,2,gl.FLOAT,false,0,0);
const tex=gl.createTexture();gl.activeTexture(gl.TEXTURE0);gl.bindTexture(gl.TEXTURE_2D,tex);
gl.texParameteri(gl.TEXTURE_2D,gl.TEXTURE_WRAP_S,gl.CLAMP_TO_EDGE);gl.texParameteri(gl.TEXTURE_2D,gl.TEXTURE_WRAP_T,gl.CLAMP_TO_EDGE);gl.texParameteri(gl.TEXTURE_2D,gl.TEXTURE_MAG_FILTER,gl.LINEAR);
gl.uniform1i(U.u_image,0);gl.clearColor(0,0,0,0);
let ready=false,aspect=1,time=0,last=0,t0=-1,reveal=reduce?1:0,tx=0,ty=0,gx=0,gy=0,vis=true;
fit();
img.onload=()=>{if(!alive)return;gl.bindTexture(gl.TEXTURE_2D,tex);gl.pixelStorei(gl.UNPACK_COLORSPACE_CONVERSION_WEBGL,gl.NONE);gl.texImage2D(gl.TEXTURE_2D,0,gl.RGBA,gl.RGBA,gl.UNSIGNED_BYTE,img);gl.generateMipmap(gl.TEXTURE_2D);gl.texParameteri(gl.TEXTURE_2D,gl.TEXTURE_MIN_FILTER,gl.LINEAR_MIPMAP_LINEAR);aspect=img.width/img.height;ready=true;wake();};
img.src=MP_TEX;
const mv=e=>{const r=host.getBoundingClientRect();gx=Math.max(-1.2,Math.min(1.2,((e.clientX-r.left)/r.width)*2-1));gy=Math.max(-1.2,Math.min(1.2,((e.clientY-r.top)/r.height)*2-1));wake();};
const lv=()=>{gx=0;gy=0;wake();};
host.addEventListener('pointermove',mv);host.addEventListener('pointerleave',lv);
const ro=new ResizeObserver(()=>{fit();wake();});ro.observe(host);
const io=new IntersectionObserver(([en])=>{vis=en.isIntersecting;if(vis)wake();});io.observe(host);
function wake(){if(!raf&&alive&&vis)raf=requestAnimationFrame(fr);}
function fr(now){raf=0;if(!alive)return;const dt=last?Math.min(0.05,(now-last)/1000):1/60;last=now;
if(!reduce)time+=dt;
let busy=!reduce;
if(reveal<1&&ready){if(t0<0)t0=now;const k=Math.min(1,(now-t0)/1600);reveal=1-Math.pow(1-k,3);if(!reduce)time+=dt*2.4*Math.pow(1-k,2);busy=true;}
const a=1-Math.exp(-dt*9);tx+=(gx*0.5-tx)*a;ty+=(gy*0.5-ty)*a;
if(Math.abs(gx*0.5-tx)+Math.abs(gy*0.5-ty)>0.0005)busy=true;
gl.viewport(0,0,cv.width,cv.height);gl.clear(gl.COLOR_BUFFER_BIT);
if(ready){gl.useProgram(pr);gl.bindVertexArray(vao);
gl.uniform2f(U.u_resolution,cv.width,cv.height);gl.uniform1f(U.u_time,time);gl.uniform1f(U.u_imageAspect,aspect);gl.uniform1f(U.u_scale,0.92);
gl.uniform4f(U.u_colorBack,0,0,0,0);gl.uniform4f(U.u_colorTint,1,1,1,1);
gl.uniform1f(U.u_softness,0.1);gl.uniform1f(U.u_repetition,2);gl.uniform1f(U.u_shiftRed,0.3);gl.uniform1f(U.u_shiftBlue,0.3);
gl.uniform1f(U.u_distortion,0.07);gl.uniform1f(U.u_contour,0.6);gl.uniform1f(U.u_angle,70);gl.uniform2f(U.u_tilt,tx,ty);gl.uniform1f(U.u_reveal,reveal);
gl.drawArrays(gl.TRIANGLE_STRIP,0,4);}
if(busy&&vis)raf=requestAnimationFrame(fr);else last=0;}
return()=>{alive=false;cancelAnimationFrame(raf);ro.disconnect();io.disconnect();host.removeEventListener('pointermove',mv);host.removeEventListener('pointerleave',lv);gl.getExtension('WEBGL_lose_context')?.loseContext();};
}
const GN_ICONS={home:'M3 11l9-8 9 8v9a1 1 0 0 1-1 1h-5v-6H9v6H4a1 1 0 0 1-1-1z',lesions:'M3 8V5a2 2 0 0 1 2-2h3M16 3h3a2 2 0 0 1 2 2v3M21 16v3a2 2 0 0 1-2 2h-3M8 21H5a2 2 0 0 1-2-2v-3M8 12h8',capture:'M4 8h3l2-3h6l2 3h3a1 1 0 0 1 1 1v9a1 1 0 0 1-1 1H4a1 1 0 0 1-1-1V9a1 1 0 0 1 1-1zM8.5 13a3.5 3.5 0 1 0 7 0a3.5 3.5 0 1 0-7 0',progress:'M5 20v-5M10 20v-8M15 20v-6M20 20V8M3 11l6-5 4 3 7-6',profile:'M8 8a4 4 0 1 0 8 0a4 4 0 1 0-8 0M4 21a8 8 0 0 1 16 0',overview:'M3 3h7v7H3zM14 3h7v7h-7zM3 14h7v7H3zM14 14h7v7h-7z',users:'M5.5 8a3.5 3.5 0 1 0 7 0a3.5 3.5 0 1 0-7 0M2 20a7 7 0 0 1 14 0M17 5a3.5 3.5 0 0 1 0 6M22 20a6 6 0 0 0-4-5.6',models:'M6 6h12v12H6zM9 2v4M15 2v4M9 18v4M15 18v4M2 9h4M2 15h4M18 9h4M18 15h4',aprofile:'M8 8a4 4 0 1 0 8 0a4 4 0 1 0-8 0M4 21a8 8 0 0 1 16 0'};
const GN_LABELS={home:'Home',lesions:'Lesions',capture:'',progress:'Progress',profile:'Profile',overview:'Overview',users:'Users',models:'Models',aprofile:'Profile'};
class Component extends DCLogic {
constructor(p){super(p);this.state={notify:false,sent:false,screen:'login',role:'user',tab:'home',email:'',pw:'',err:false,pf:{name:'Alex Morgan',email:'alex.morgan@example.com',dob:'14 / 03 / 1990',t:[true,true,false]},draft:null,saved:false,ci:0};}
tabs(){return this.state.role==='admin'?['overview','users','models','aprofile']:['home','lesions','capture','progress','profile'];}
mountLogo(){const host=document.querySelector('.dt-logo');if(this.logo&&this.logo.host!==host){this.logo.stop();this.logo=null;}if(host&&!this.logo)this.logo={host,stop:mpLogo(host)};}
componentDidMount(){this.mountLogo();}
componentDidUpdate(){this.mountLogo();}
componentWillUnmount(){if(this.logo)this.logo.stop();}
renderVals(){
const st=this.state,admin=st.role==='admin';
const d=st.draft||st.pf,dirty=!!st.draft;
const TG=[['Monthly skin-check reminder','Reminder to photograph tracked lesions'],['Report ready alerts','Notify me when an analysis finishes'],['Share images with my clinician','Only after you approve each report']];
const edit=(k,val)=>this.setState({draft:{...(this.state.draft||this.state.pf),[k]:val},saved:false,hid:false});
const nm=d.name.trim().split(/\s+/).filter(Boolean);
const pf={name:d.name,email:d.email,dob:d.dob,initials:((nm[0]||'?')[0]+(nm.length>1?nm[nm.length-1][0]:'')).toUpperCase(),clean:!dirty,dim:dirty?1:.4,status:dirty?'You have unsaved changes':(st.saved?'Changes saved':'All changes saved')};
const toggles=TG.map((x,i)=>({label:x[0],desc:x[1],on:d.t[i]?'true':'false',track:d.t[i]?'rgba(255,255,255,.42)':'rgba(255,255,255,.14)',knob:d.t[i]?31:3,flip:()=>{const t=[...(this.state.draft||this.state.pf).t];t[i]=!t[i];edit('t',t);}}));const tabs=this.tabs();
const all=['home','lesions','capture','progress','profile','overview','users','models','aprofile'];
const v={};all.forEach(k=>{v[k]=st.screen==='app'&&st.tab===k;});
return{
pf,toggles,setName:e=>edit('name',e.target.value),setPfEmail:e=>edit('email',e.target.value),setDob:e=>edit('dob',e.target.value),discard:()=>this.setState({draft:null,saved:false}),save:()=>{clearTimeout(this._tt);this.setState({pf:this.state.draft||this.state.pf,draft:null,saved:true});this._tt=setTimeout(()=>this.setState({saved:false}),2800);},
n:{pressed:st.notify?'true':'false',bg:st.notify?'#f5f5f5':'#27272a',ink:st.notify?'#18181b':'#f5f5f5',anim:st.notify?'ring .82s cubic-bezier(.77,0,.175,1) 1':'none',offO:st.notify?0:1,onO:st.notify?1:0},
toggleNotify:()=>this.setState({notify:!this.state.notify}),
tn:(()=>{const prof=st.screen==='app'&&st.tab==='profile',show=prof&&((dirty&&!st.hid)||st.saved),btns=dirty;
let dr=null;
const dismiss=()=>{clearTimeout(this._tt);if(this.state.draft)this.setState({hid:true});else this.setState({saved:false});};
return{y:show?'0px':'140%',op:show?1:0,pe:show?'auto':'none',hid:show?'false':'true',tab:show?'0':'-1',btns,
title:dirty?'Unsaved changes':'Changes saved',desc:dirty?'Save or discard your edits':'Your profile is up to date',
fuse:(!dirty&&st.saved&&show)?'tst-burn 2.8s linear forwards':'none',
down:e=>{if(e.button!==0||e.target.closest('button'))return;dr={id:e.pointerId,y:e.clientY,dy:0};try{e.currentTarget.setPointerCapture(e.pointerId);}catch(x){}e.currentTarget.style.transition='none';},
move:e=>{if(!dr||dr.id!==e.pointerId)return;const d=e.clientY-dr.y;dr.dy=d>0?d:d*.2;e.currentTarget.style.transform='translateY('+dr.dy+'px)';},
up:e=>{if(!dr||dr.id!==e.pointerId)return;const el=e.currentTarget,go=dr.dy>=40;dr=null;el.style.transition='transform .4s cubic-bezier(.23,1,.32,1)';el.style.transform='';if(go)dismiss();}};})(),
car:(()=>{const ci=st.ci,ids=[0,1,2];const o={onScroll:e=>{const t=e.currentTarget,i=Math.round(t.scrollLeft/(t.clientWidth+12));if(i!==this.state.ci)this.setState({ci:i});},
lesions:[{name:'Left forearm',when:'18 Oct',dot:'#86efac'},{name:'Upper back',when:'09 Oct',dot:'#fde68a'},{name:'Right calf',when:'27 Sep',dot:'#86efac'}],
ups:[{day:'28',mon:'Oct',what:'Re-photograph left forearm'},{day:'09',mon:'Nov',what:'Upper back check'},{day:'15',mon:'Nov',what:'Dermatologist visit'}],
dots:ids.map(i=>({label:'Go to card '+(i+1),cur:i===ci?'true':'false',w:i===ci?22:8,bg:i===ci?'#f4f4f5':'rgba(255,255,255,.28)',go:()=>{const tr=document.querySelector('.cs-track');if(tr)tr.scrollTo({left:i*(tr.clientWidth+12),behavior:'smooth'});this.setState({ci:i});}}))};
ids.forEach(i=>{o['s'+i]=i===ci?1:.94;o['o'+i]=i===ci?1:.55;});return o;})(),
s:{login:st.screen==='login',compose:!st.sent,sent:st.sent,app:st.screen==='app',email:st.email,pw:st.pw,err:st.err,roleLabel:admin?'Admin':'Patient'},
v,
nav:{n:tabs.length,i:Math.max(0,tabs.indexOf(st.tab))},
navItems:tabs.map((k,i)=>({i,label:GN_LABELS[k],name:GN_LABELS[k]||'Capture',cur:st.tab===k?'page':'false',d:GN_ICONS[k],pick:()=>this.setState({tab:k})})),
setEmail:e=>this.setState({email:e.target.value}),
setPw:e=>this.setState({pw:e.target.value}),
goCapture:()=>this.setState({tab:'capture'}),
social:()=>{this.setState({screen:'app',role:'user',tab:'home',err:false,sent:false});},
signIn:()=>{if(!this.state.email.trim()){this.setState({err:true});return;}
const e=this.state.email.trim().toLowerCase();
const a=e.startsWith('admin');
this.setState({screen:'app',role:a?'admin':'user',tab:a?'overview':'home',err:false,sent:false});},
signOut:()=>this.setState({screen:'login',pw:'',sent:false,tab:'home'})
};}
}
