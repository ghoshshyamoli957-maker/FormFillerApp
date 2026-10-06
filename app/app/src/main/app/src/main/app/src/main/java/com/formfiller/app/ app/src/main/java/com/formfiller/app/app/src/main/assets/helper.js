(function(){
if(window.__ffLoaded||!window.FF)return;
window.__ffLoaded=true;
var PK=location.hostname+location.pathname;
var st={map:false,rec:FF.load('rec')==='1',choose:null,cur:-1};
function ld(k,d){try{var v=FF.load(k);return v?JSON.parse(v):d;}catch(e){return d;}}
function sv(k,v){FF.save(k,JSON.stringify(v));}
function getData(){try{var d=JSON.parse(FF.getData());return {headers:d.headers||[],rows:d.rows||[]};}catch(e){return {headers:[],rows:[]};}}
function esc(s){return (window.CSS&&CSS.escape)?CSS.escape(s):String(s).replace(/[^a-zA-Z0-9_-]/g,'\\$&');}
function find(s){try{return document.querySelector(s);}catch(e){return null;}}
function sel(el){
 if(el.id&&document.querySelectorAll('#'+esc(el.id)).length===1)return '#'+esc(el.id);
 var n=el.getAttribute('name');
 if(n){var q=el.tagName.toLowerCase()+'[name="'+n.replace(/"/g,'\\"')+'"]';
  if(document.querySelectorAll(q).length===1)return q;}
 var p=[];
 while(el&&el.nodeType===1&&el!==document.body){
  var i=1,s=el;
  while((s=s.previousElementSibling))i++;
  p.unshift(el.tagName.toLowerCase()+':nth-child('+i+')');
  el=el.parentElement;
 }
 return 'body>'+p.join('>');
}
function setVal(el,v){
 v=(v==null)?'':String(v);
 if(el.tagName==='SELECT'){
  for(var i=0;i<el.options.length;i++){var o=el.options[i];
   if(o.value.trim()===v.trim()||o.text.trim()===v.trim()){el.selectedIndex=i;break;}}
 }else if(el.type==='radio'){
  var g=document.querySelectorAll('input[type=radio][name="'+el.name+'"]');
  for(var j=0;j<g.length;j++){if(g[j].value.trim()===v.trim()){g[j].click();break;}}
  return;
 }else if(el.type==='checkbox'){return;}
 else{
  var proto=el.tagName==='TEXTAREA'?HTMLTextAreaElement.prototype:HTMLInputElement.prototype;
  var d=Object.getOwnPropertyDescriptor(proto,'value');
  if(d&&d.set)d.set.call(el,v);else el.value=v;
 }
 el.dispatchEvent(new Event('input',{bubbles:true}));
 el.dispatchEvent(new Event('change',{bubbles:true}));
}
function mk(t,c,x){var e=document.createElement(t);e.style.cssText=c||'';if(x)e.textContent=x;return e;}
function btn(x,f,bg){
 var b=mk('button','margin:2px;padding:6px 10px;border:1px solid #888;border-radius:6px;font-size:14px;color:'+(bg?'#fff':'#000')+';background:'+(bg||'#eee'),x);
 b.type='button';
 b.onclick=function(e){e.preventDefault();f();};
 return b;
}

var root=mk('div','');
root.id='ff-root';
var fab=mk('div','position:fixed;right:12px;bottom:80px;width:52px;height:52px;border-radius:50%;background:#1a73e8;color:#fff;font-size:24px;text-align:center;line-height:52px;box-shadow:0 2px 8px rgba(0,0,0,.4);z-index:2147483647;','📋');
var panel=mk('div','display:none;position:fixed;right:8px;bottom:142px;width:min(94vw,360px);max-height:62vh;background:#fff;color:#000;border:1px solid #999;border-radius:10px;padding:8px;box-shadow:0 2px 12px rgba(0,0,0,.5);overflow:hidden;z-index:2147483647;font:14px sans-serif;');
var bar=mk('div','');
var bar2=mk('div','margin:4px 0;');
var status=mk('div','margin:4px 2px;font-size:13px;color:#333;','প্রস্তুত');
var list=mk('div','display:flex;flex-wrap:wrap;overflow-y:auto;max-height:30vh;');
var dl=mk('input','width:70px;padding:4px;margin:0 4px;');
dl.type='number';dl.value=ld('delay',800);
dl.onchange=function(){sv('delay',parseInt(dl.value)||800);};
bar2.appendChild(mk('span','','ক্লিকের মাঝে বিরতি (ms):'));
bar2.appendChild(dl);
panel.appendChild(bar);panel.appendChild(bar2);panel.appendChild(status);panel.appendChild(list);
root.appendChild(fab);root.appendChild(panel);

function say(s){status.textContent=s;}

function render(){
 bar.innerHTML='';list.innerHTML='';
 var d=getData();
 if(st.choose){
  say('এই ঘরে কোন কলামের ডাটা বসবে?');
  d.headers.forEach(function(h,i){
   list.appendChild(btn((i+1)+'. '+h,function(){var el=st.choose;st.choose=null;addMap(el,i);}));
  });
  list.appendChild(btn('বাতিল',function(){st.choose=null;render();say('বাতিল');},'#888'));
  return;
 }
 bar.appendChild(btn(st.map?'ম্যাপ বন্ধ':'ম্যাপ',function(){
  st.map=!st.map;
  if(st.map){st.rec=false;FF.save('rec','');}
  render();say(st.map?'ম্যাপ চালু: ফর্মের ঘরে ট্যাপ করুন':'ম্যাপ বন্ধ');
 },st.map?'#d93025':'#1a73e8'));
 bar.appendChild(btn(st.rec?'⏹ রেকর্ড বন্ধ':'⏺ রেকর্ড',function(){
  st.rec=!st.rec;
  FF.save('rec',st.rec?'1':'');
  if(st.rec){st.map=false;sv('seq:'+PK,[]);}
  render();say(st.rec?'রেকর্ড চালু: বোতামগুলো ক্রমে ট্যাপ করুন':'রেকর্ড সেভ হয়েছে ('+ld('seq:'+PK,[]).length+'টি ক্লিক)');
 },st.rec?'#d93025':'#e37400'));
 bar.appendChild(btn('▶ ক্লিক চালাও',play,'#0a7'));
 bar.appendChild(btn('ম্যাপ মুছুন',function(){sv('map:'+PK,[]);say('ম্যাপ মোছা হয়েছে');}));
 bar.appendChild(btn('ক্লিক মুছুন',function(){sv('seq:'+PK,[]);say('রেকর্ড মোছা হয়েছে');}));
 bar.appendChild(btn('✕',function(){panel.style.display='none';},'#888'));
 if(!d.rows.length){say('আগে অ্যাপ থেকে এক্সেল ফাইল ইমপোর্ট করুন');return;}
 d.rows.forEach(function(r,i){
  list.appendChild(btn(String(i+1),function(){fill(i);},i===st.cur?'#0a7':null));
 });
}

function addMap(el,col){
 var k='map:'+PK,m=ld(k,[]),s=sel(el);
 m=m.filter(function(e){return e.sel!==s;});
 m.push({sel:s,col:col});
 sv(k,m);
 el.style.outline='2px solid #1a73e8';
 render();
 say('✔ ঘর ম্যাপ হয়েছে (মোট '+m.length+'টি)। আরেকটি ঘরে ট্যাপ করুন');
}

function fill(i){
 var m=ld('map:'+PK,[]);
 if(!m.length){say('আগে ম্যাপ মোডে ঘরগুলো ম্যাপ করুন');return;}
 var row=getData().rows[i];
 var n=0,miss=0;
 m.forEach(function(e){
  var el=find(e.sel);
  if(el){setVal(el,row[e.col]);n++;}else miss++;
 });
 st.cur=i;
 render();
 say('সিরিয়াল '+(i+1)+': '+n+'টি ঘর ভরা হয়েছে'+(miss?', '+miss+'টি ঘর পাওয়া যায়নি':''));
}

function play(){
 var seq=ld('seq:'+PK,[]);
 if(!seq.length){say('এই পেজে কোনো ক্লিক রেকর্ড করা নেই');return;}
 var i=0;
 (function step(){
  if(i>=seq.length){say('✔ সব ক্লিক শেষ');return;}
  var tries=0;
  (function tryFind(){
   var el=find(seq[i].sel);
   if(el){
    el.click();i++;
    setTimeout(step,ld('delay',800));
   }else if(++tries<15)setTimeout(tryFind,300);
   else say('ধাপ '+(i+1)+' এর বোতাম পাওয়া যায়নি');
  })();
 })();
}

document.addEventListener('click',function(e){
 if(!e.isTrusted||root.contains(e.target))return;
 if(st.map){
  e.preventDefault();e.stopPropagation();
  var f=e.target.closest?e.target.closest('input,select,textarea'):null;
  if(!f){panel.style.display='block';say('ফর্মের কোনো ঘরে ট্যাপ করুন');return;}
  st.choose=f;panel.style.display='block';render();
  return;
 }
 if(st.rec){
  var t=(e.target.closest&&e.target.closest('button,a,input[type=submit],input[type=button],[role=button]'))||e.target;
  var s=ld('seq:'+PK,[]);
  s.push({sel:sel(t)});
  sv('seq:'+PK,s);
  say('রেকর্ড: '+s.length+'টি ক্লিক');
 }
},true);

fab.onclick=function(){
 panel.style.display=panel.style.display==='none'?'block':'none';
 if(panel.style.display==='block')render();
};

(document.body||document.documentElement).appendChild(root);
ld('map:'+PK,[]).forEach(function(e){var el=find(e.sel);if(el)el.style.outline='2px solid #1a73e8';});
})();
