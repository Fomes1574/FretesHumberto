/* Regras puras, compartilhadas pelo protótipo e pelos testes. */
const Core=(()=>{
const MAX=99999999999;
const clone=x=>JSON.parse(JSON.stringify(x));
const pad=n=>String(n).padStart(2,'0');
const iso=d=>`${d.getFullYear()}-${pad(d.getMonth()+1)}-${pad(d.getDate())}`;
const pd=s=>new Date(s+'T12:00:00');
const validDate=s=>typeof s==='string'&&/^\d{4}-\d{2}-\d{2}$/.test(s)&&!Number.isNaN(pd(s).getTime())&&iso(pd(s))===s;
const validMonth=s=>typeof s==='string'&&/^\d{4}-(0[1-9]|1[0-2])$/.test(s);
const addD=(s,n)=>{const d=pd(s);d.setDate(d.getDate()+n);return iso(d)};
const monthRange=s=>{const d=pd(s);return[`${s.slice(0,7)}-01`,iso(new Date(d.getFullYear(),d.getMonth()+1,0,12))]};
const addM=(s,n)=>{const d=pd(s),day=d.getDate();d.setDate(1);d.setMonth(d.getMonth()+n);d.setDate(Math.min(day,new Date(d.getFullYear(),d.getMonth()+1,0).getDate()));return iso(d)};
const weekStart=s=>addD(s,-((pd(s).getDay()+6)%7));
const daysBetween=(a,b)=>{const utc=s=>{const[y,m,d]=s.split('-').map(Number);return Date.UTC(y,m-1,d)};return Math.round((utc(b)-utc(a))/86400000)};
const roundDiv=(a,b)=>{a=BigInt(a);b=BigInt(b);if(b<=0n)throw Error('Divisor inválido');const sign=a<0n?-1n:1n;const v=a*sign;return Number(sign*((v+b/2n)/b))};
function parseScaled(value){
 if(value==null)return NaN;let s=String(value).trim().replace(/^R\$\s*/,'').replace(/[\s\u00a0]/g,'');if(!s||!/^[\d.,]+$/.test(s))return NaN;
 let int,frac='';
 if(s.includes(',')){if((s.match(/,/g)||[]).length!==1)return NaN;const parts=s.split(',');int=parts[0]||'0';frac=parts[1];if(int.includes('.')){if(!/^\d{1,3}(\.\d{3})+$/.test(int))return NaN;int=int.replace(/\./g,'')}}
 else if(s.includes('.')){const parts=s.split('.');if(parts.length>2){if(!/^\d{1,3}(\.\d{3})+$/.test(s))return NaN;int=s.replace(/\./g,'')}else{[int,frac]=parts;if(!int)int='0';if(frac.length===3)return NaN}}
 else int=s;
 if(!/^\d+$/.test(int)||!/^\d*$/.test(frac))return NaN;
 const n=BigInt(int)*100n+BigInt(frac.slice(0,2).padEnd(2,'0'));return n<=BigInt(MAX)?Number(n):NaN;
}
const num=v=>parseScaled(v)/100;
const integer=v=>/^\d+$/.test(String(v).trim())&&Number.isSafeInteger(Number(v))?Number(v):NaN;
const money=n=>Number.isSafeInteger(n)&&n>=0&&n<=MAX;
function calc({fare,km100,kml100,price,minutes=0,cost=0}){if(![fare,km100,kml100,price].every(x=>money(x)&&x>0)||!money(cost)||!Number.isSafeInteger(minutes)||minutes<0)return null;const fuel=roundDiv(BigInt(km100)*BigInt(price),kml100),res=fare-fuel-cost;return{fare,fuel,res,km100,kml100,price,minutes,cost,perKm:roundDiv(BigInt(res)*100n,km100),perHour:minutes>0?roundDiv(BigInt(res)*60n,minutes):null,liters100:roundDiv(BigInt(km100)*100n,kml100)}}
function fuel(value,aux,mode){if(!(money(value)&&value>0&&money(aux)&&aux>0))return null;const c=mode==='preco'?{litros100:roundDiv(BigInt(value)*100n,aux),preco:aux}:{litros100:aux,preco:roundDiv(BigInt(value)*100n,aux)};return c.litros100>0&&c.preco>0?c:null}
function defaults(){return{version:3,settings:{metaDia:25000,metaMes:550000,kml:8.5,preco:379,fs:18,manualPrice:{data:'1900-01-01',preco:379,stamp:''},lastBackup:null},goalsDay:[{desde:'1900-01-01',valor:25000}],goalsMonth:{},monthBase:550000,lanc:[],rec:[],occ:{},revisions:{},checked:{},demo:true}}
function normalize(raw){
 if(!raw||typeof raw!=='object'||(raw.version!=null&&![2,3].includes(raw.version))||!raw.settings||!Array.isArray(raw.lanc)||raw.lanc.length>50000)throw Error('Cópia incompatível.');
 const base=defaults(),s=clone(raw),cfg=s.settings;
 for(const key of ['metaDia','metaMes','preco'])if(!money(cfg[key])||(key!=='metaMes'&&cfg[key]===0))throw Error('Configuração inválida.');
 if(!(Number.isFinite(cfg.kml)&&cfg.kml>0&&cfg.kml<=1000))throw Error('Consumo inválido.');
 s.settings={...base.settings,...cfg};if(![18,20,23].includes(s.settings.fs))s.settings.fs=18;
 if(!s.settings.manualPrice||!validDate(s.settings.manualPrice.data)||!money(s.settings.manualPrice.preco)||s.settings.manualPrice.preco<=0||typeof s.settings.manualPrice.stamp!=='string')throw Error('Referência de preço inválida.');
 s.version=3;s.goalsDay=s.goalsDay||[{desde:'1900-01-01',valor:cfg.metaDia}];s.goalsMonth=s.goalsMonth||{};s.monthBase=s.monthBase??cfg.metaMes;s.rec=s.rec||[];s.occ=s.occ||{};s.revisions=s.revisions||{};s.checked=s.checked||{};s.demo=!!s.demo;
 if(!Array.isArray(s.goalsDay)||!Array.isArray(s.rec)||s.rec.length>200||!money(s.monthBase)||!s.occ||Array.isArray(s.occ)||!s.goalsMonth||Array.isArray(s.goalsMonth)||!s.revisions||Array.isArray(s.revisions)||!s.checked||Array.isArray(s.checked))throw Error('Estrutura inválida.');
 const ids=new Set();for(const l of s.lanc){if(!l||typeof l.id!=='string'||ids.has(l.id)||!['viagem','abast','despesa'].includes(l.tipo)||!validDate(l.data)||!money(l.valor)||l.valor===0||typeof l.desc!=='string'||l.desc.length>1000)throw Error('Lançamento inválido.');ids.add(l.id);if(l.tipo==='abast'){if(l.litros100==null&&Number.isFinite(l.litros))l.litros100=Math.round(l.litros*100);if(!money(l.litros100)||l.litros100===0||!money(l.preco)||l.preco===0)throw Error('Abastecimento inválido.')}if(l.min!=null&&(!Number.isSafeInteger(l.min)||l.min<0))throw Error('Duração inválida.');if(l.km!=null&&(!Number.isFinite(l.km)||l.km<0))throw Error('Distância inválida.');}
 for(const g of s.goalsDay)if(!validDate(g.desde)||!money(g.valor)||g.valor<=0)throw Error('Meta inválida.');s.goalsDay.sort((a,b)=>a.desde.localeCompare(b.desde));
 for(const[m,v]of Object.entries(s.goalsMonth))if(!validMonth(m)||!money(v))throw Error('Meta mensal inválida.');
 const rids=new Set();for(const r of s.rec){if(typeof r.id!=='string'||rids.has(r.id)||!validDate(r.inicio)||!Array.isArray(r.versions)||!r.versions.length)throw Error('Repetição inválida.');rids.add(r.id);for(const v of r.versions)if(!validMonth(v.desde)||typeof v.nome!=='string'||!v.nome.trim()||v.nome.length>160||!money(v.valor)||v.valor<=0||!Number.isInteger(v.dia)||v.dia<1||v.dia>31||(v.fim&&!validDate(v.fim))||typeof v.ativa!=='boolean')throw Error('Repetição inválida.');}
 for(const[k,o]of Object.entries(s.occ)){if(!o||!rids.has(o.recId)||k!==o.recId+'|'+o.mes||!validMonth(o.mes)||!['gerada','pulada','prevista'].includes(o.status)||(o.lancId&&!ids.has(o.lancId))||(o.status==='gerada'&&!o.lancId))throw Error('Ocorrência inválida.');if(o.override&&(!validDate(o.override.data)||!money(o.override.valor)||o.override.valor===0||typeof o.override.desc!=='string'||o.override.desc.length>160||typeof o.override.cat!=='string'))throw Error('Alteração mensal inválida.');}
 for(const l of s.lanc)if(l.occKey&&(!s.occ[l.occKey]||s.occ[l.occKey].lancId!==l.id))throw Error('Despesa mensal inválida.');
 for(const[d,v]of Object.entries(s.revisions))if(!validDate(d)||!Number.isSafeInteger(v)||v<0)throw Error('Revisão inválida.');
 for(const[d,v]of Object.entries(s.checked))if(!validDate(d)||!v||!Number.isSafeInteger(v.revision)||typeof v.stamp!=='string')throw Error('Conferência inválida.');
 return s;
}
const dayGoal=(s,d)=>{let v=s.goalsDay[0]?.valor||s.settings.metaDia;for(const g of s.goalsDay)if(g.desde<=d)v=g.valor;return v};
const monthGoal=(s,m)=>s.goalsMonth[m]??s.monthBase;
const setDayGoal=(s,d,v)=>{s.goalsDay=s.goalsDay.filter(g=>g.desde!==d);s.goalsDay.push({desde:d,valor:v});s.goalsDay.sort((a,b)=>a.desde.localeCompare(b.desde));s.settings.metaDia=v};
const touch=(s,d)=>{s.revisions[d]=(s.revisions[d]||0)+1};
function summary(s,a,b,selected=null){const o={fretes:0,nF:0,comb:0,nA:0,litros100:0,outras:0,km:0,porDia:{},trab:new Set(),cats:{}};for(const l of s.lanc){if(selected?!selected.includes(l.data):(l.data<a||l.data>b))continue;const d=o.porDia[l.data]||(o.porDia[l.data]={f:0,c:0,o:0});if(l.tipo==='viagem'){o.fretes+=l.valor;o.nF++;d.f+=l.valor;o.trab.add(l.data);o.km+=l.km||0}else if(l.tipo==='abast'){o.comb+=l.valor;o.nA++;o.litros100+=l.litros100||0;d.c+=l.valor}else{o.outras+=l.valor;d.o+=l.valor;o.cats[l.cat||'Outros']=(o.cats[l.cat||'Outros']||0)+l.valor}}o.lucro=o.fretes-o.comb-o.outras;o.dias=Object.fromEntries(Object.entries(o.porDia).map(([k,v])=>[k,v.f-v.c-v.o]));o.metaOk=[...o.trab].filter(d=>o.dias[d]>=dayGoal(s,d)).length;return o}
function price(s){const candidates=s.lanc.filter(l=>l.tipo==='abast').map(l=>({data:l.data,preco:l.preco,stamp:l.editadoEm||l.criadoEm||l.id}));if(s.settings.manualPrice)candidates.push(s.settings.manualPrice);candidates.sort((a,b)=>a.data.localeCompare(b.data)||String(a.stamp||'').localeCompare(String(b.stamp||'')));return candidates.at(-1)?.preco||s.settings.preco}
const recVersion=(r,m)=>r.versions.filter(v=>v.desde<=m).sort((a,b)=>a.desde.localeCompare(b.desde)).at(-1);
const dueDate=(m,day)=>`${m}-${pad(Math.min(day,m.endsWith('-02')?28:pd(monthRange(m+'-01')[1]).getDate()))}`;
 function generate(s,today,makeId){let changed=false;for(const r of s.rec){let m=r.inicio.slice(0,7),last=today.slice(0,7),guard=0;while(m<=last&&guard++<2400){const key=r.id+'|'+m,v=recVersion(r,m),o=s.occ[key];if(v&&(v.ativa||o?.override)&&o?.status!=='pulada'&&o?.status!=='gerada'){const date=o?.override?.data||dueDate(m,v.dia);if(date>=r.inicio&&date<=today&&(!v.fim||date<=v.fim)){const l={id:makeId(),tipo:'despesa',data:date,valor:o?.override?.valor??v.valor,desc:o?.override?.desc??v.nome,cat:o?.override?.cat??v.cat??'Outros',occKey:key,criadoEm:new Date().toISOString()};s.lanc.push(l);s.occ[key]={recId:r.id,mes:m,status:'gerada',lancId:l.id,...(o?.override?{override:o.override}:{})};touch(s,date);changed=true}}m=addM(m+'-01',1).slice(0,7)}}return changed}
function removeEntry(s,id){const i=s.lanc.findIndex(l=>l.id===id);if(i<0)return null;const l=s.lanc[i],occ=l.occKey?clone(s.occ[l.occKey]):null;s.lanc.splice(i,1);if(l.occKey)s.occ[l.occKey]={...s.occ[l.occKey],status:'pulada',lancId:null};touch(s,l.data);return{l:clone(l),occ}}
function restoreEntry(s,removed){if(!removed||s.lanc.some(l=>l.id===removed.l.id))return;s.lanc.push(clone(removed.l));if(removed.occ)s.occ[removed.l.occKey]=clone(removed.occ);touch(s,removed.l.data)}
function saveEntry(s,l){const i=s.lanc.findIndex(x=>x.id===l.id),old=i>=0?s.lanc[i]:null;if(old)touch(s,old.data);if(i>=0)s.lanc[i]=clone(l);else s.lanc.push(clone(l));if(!old||old.data!==l.data)touch(s,l.data);if(l.occKey){const o=s.occ[l.occKey];o.override={data:l.data,valor:l.valor,desc:l.desc,cat:l.cat};o.status='gerada'}}
function validRange(a,b,today){if(!validDate(a)||!validDate(b)||a>b)return'Confira as datas inicial e final.';if(b>today)return'A data final não pode ser no futuro.';if(b>=addM(a,6))return'Escolha um intervalo de até 6 meses.';return''}
function previousRange(mode,a,b,today){const partial=b>=today&&a<=today;const end=partial?today:b;if(mode==='dia')return[addD(a,-1),addD(a,-1)];if(mode==='semana')return[addD(a,-7),addD(end,-7)];if(mode==='mes'){const[pa,pb]=monthRange(addM(a,-1));return[pa,partial?`${pa.slice(0,7)}-${pad(Math.min(pd(today).getDate(),pd(pb).getDate()))}`:pb]}const count=daysBetween(a,end)+1;return[addD(a,-count),addD(a,-1)]}
return{MAX,clone,pad,iso,pd,validDate,validMonth,addD,addM,monthRange,weekStart,daysBetween,roundDiv,parseScaled,num,integer,money,calc,fuel,defaults,normalize,dayGoal,monthGoal,setDayGoal,touch,summary,price,recVersion,dueDate,generate,removeEntry,restoreEntry,saveEntry,validRange,previousRange};
})();
