const {pad,iso,pd,addD,addM,monthRange,weekStart}=Core;
let TODAY=Core.hoje();
const DOW=['Dom','Seg','Ter','Qua','Qui','Sex','Sáb'];
const DOWL=['Domingo','Segunda','Terça','Quarta','Quinta','Sexta','Sábado'];
const MES=['janeiro','fevereiro','março','abril','maio','junho','julho','agosto','setembro','outubro','novembro','dezembro'];
const MESC=['jan','fev','mar','abr','mai','jun','jul','ago','set','out','nov','dez'];
const R=c=>(c/100).toLocaleString('pt-BR',{style:'currency',currency:'BRL'});
const R0=R;
const brNum=(n,d=2)=>n.toLocaleString('pt-BR',{maximumFractionDigits:d,minimumFractionDigits:d});
const inp=n=>Number(n).toFixed(2).replace('.',',');
const dm=s=>s.slice(8,10)+'/'+s.slice(5,7);
const longDate=s=>`${DOWL[pd(s).getDay()]}, ${pd(s).getDate()} de ${MES[pd(s).getMonth()]}`;
const shortDate=s=>`${DOW[pd(s).getDay()]}, ${pd(s).getDate()} ${MESC[pd(s).getMonth()]}`;
const esc=s=>String(s??'').replace(/[&<>"']/g,c=>({'&':'&amp;','<':'&lt;','>':'&gt;','"':'&quot;',"'":'&#39;'}[c]));
const uid=()=>crypto.randomUUID?crypto.randomUUID():Date.now().toString(36)+Math.random().toString(36).slice(2,10);
const parseMoney=Core.parseScaled,parseNum=Core.num;
const val=id=>document.getElementById(id)?.value??'';
const KEY='meusfretes_proto_v2';
const PREVENTIVA=KEY+'_antes_da_restauracao';
let S, lido, loadProblem='', storageProblem='', filaGravacao=Promise.resolve();
function carregar(valor){
 S=valor?Core.normalize(JSON.parse(valor)):Core.normalize({...Core.defaults(),lanc:seed()});
 lido=valor;
 loadProblem='';
}
function load(){
 try{carregar(localStorage.getItem(KEY))}
 catch{loadProblem='Não foi possível abrir os dados salvos. Restaure uma cópia.';S=Core.defaults();S.demo=false}
}
function comGravacao(fn){
 const proxima=filaGravacao.then(()=>navigator.locks?navigator.locks.request(KEY,fn):fn());
 filaGravacao=proxima.catch(()=>{});
 return proxima;
}
function sincronizar(){
 const raw=localStorage.getItem(KEY);
 if(raw!==lido)carregar(raw);
 return raw;
}
function persistir(next,esperado){
 const valid=Core.normalize(next);
 const raw=JSON.stringify(valid);
 if(localStorage.getItem(KEY)!==esperado)throw Error('Os dados mudaram em outra aba. Tente novamente.');
 localStorage.setItem(KEY,raw);
 lido=raw;
 S=valid;
 loadProblem='';
 storageProblem='';
}
async function commit(fn){
 try{return await comGravacao(()=>{
   const raw=sincronizar(),next=Core.clone(S);
   if(fn(next)!==false)persistir(next,raw);
   return true;
 })}
 catch(err){
   storageProblem=err.message==='O lançamento mudou em outra aba.'?err.message:'Não foi possível salvar. Seus dados anteriores foram mantidos.';
   toast(storageProblem);
   return false;
 }
}
window.addEventListener('storage',e=>{
 if(e.key!==KEY&&e.key!==null)return;
 try{sincronizar();applyFs();if(!document.activeElement?.closest('#ajForm'))render(true)}catch{load();render(true)}
});
function resumo(a,b){return Core.summary(S,a,b,rep?.modo==='dois'?rep.dates:null)}
function metaTexto(luc,meta){const df=luc-meta;return df<0?`Faltam <b>${R(-df)}</b> para a meta`:df===0?'Meta atingida':`Meta atingida · <b>${R(df)}</b> acima`}
function dayGoal(d){return Core.dayGoal(S,d)}
function latestPrice(){return Core.price(S)}
async function processRecurring(){
 if(loadProblem||!S.rec.length)return false;
 let mudou=false;
 return await commit(s=>mudou=Core.generate(s,TODAY,uid))&&mudou;
}
async function rollDate(){const d=Core.hoje();if(d!==TODAY){if(rep.ref===TODAY)rep.ref=d;TODAY=d;await processRecurring();render()}}
let tab='hoje',rep={modo:'semana',ref:TODAY},chartSel=null,recMonth=TODAY.slice(0,7);
function nav(){document.getElementById('nav').innerHTML=[['hoje','Hoje',I.home],['hist','Histórico',I.list],['rel','Relatórios',I.chart],['aj','Ajustes',I.gear]].map(([k,t,i])=>`<button type="button" data-tab="${k}" ${tab===k?'aria-current="page"':''}><span class="pill">${i}</span>${t}</button>`).join('')}
function demoBar(){return S.demo?'<div class="demo"><span>Valores de exemplo para testar.</span><button type="button" data-act="limpar">Começar do zero</button></div>':''}
function render(keepScroll=false){const y=window.scrollY;nav();const m=document.getElementById('main');const chartLeft=keepScroll?m.querySelector('.chartScroll')?.scrollLeft:null;m.innerHTML=(loadProblem?`<div class="card err" role="alert">${esc(loadProblem)}</div>`:'')+(tab==='hoje'?vHoje():tab==='hist'?vHist():tab==='rel'?vRel():tab==='contas'?vAccounts():vAj());const chart=m.querySelector('.chartScroll');if(chart)chart.scrollLeft=chartLeft??chart.scrollWidth;if(keepScroll)window.scrollTo(0,y);else window.scrollTo(0,0);startScenes()}
function startScenes(){if(tab==='hoje'){const o=Core.summary(S,TODAY,TODAY);sceneStart('dia',fxFor(o.lucro,dayGoal(TODAY)),o.lucro,null,R)}}
function contaDia(o){
 const pedagio=o.cats['Pedágio']||0;
 const linha=(nome,v,gasto,informado=true)=>`<div class="row"><span>${nome}</span><b class="${gasto?'gasto':'ganho'}">${informado?(gasto&&v>0?'− ':'')+R(v):'Sem registro'}</b></div>`;
 return `<div class="label">TOTAL DO TRABALHO</div><div class="big">${R(o.lucro)}</div>${linha('Valor ganho',o.fretes,false)}${linha('Valor gasto',o.comb+o.outras,true)}${linha('Combustível',o.comb,true,!!o.nA)}${linha('Pedágio',pedagio,true,'Pedágio' in o.cats)}${o.outras>pedagio?linha('Outros gastos',o.outras-pedagio,true):''}`;
}
function entryRow(l){const t={viagem:['Frete','t-viagem'],abast:['Combustível','t-abast'],despesa:['Despesa','t-despesa']}[l.tipo];const det=l.tipo==='viagem'?(l.km?brNum(l.km)+' km':''):l.tipo==='abast'?(l.litros100?brNum(l.litros100/100)+' L · '+R(l.preco)+'/L':''):l.occKey?'mensal':'';return `<div class="entry"><button type="button" class="info" data-act="edit" data-id="${esc(l.id)}"><span class="tag ${t[1]}">${t[0]}</span><span class="sub">${esc(det)}</span><b>${esc(l.desc||(l.tipo==='abast'?'Abastecimento':l.tipo==='viagem'?'Frete':l.cat))}</b></button><span class="v ${l.tipo==='viagem'?'ganho':'gasto'}">${l.tipo==='viagem'?'':'− '}${R(l.valor)}</span><button type="button" class="icon-btn" data-act="del" data-id="${esc(l.id)}" aria-label="Apagar ${esc(l.desc||t[0])} de ${R(l.valor)}">${I.trash}</button></div>`}
function atalhosGastos(dia){
 return `<div class="two"><button class="btn line" data-act="novo" data-tipo="abast" data-d="${dia}">Combustível</button>
 <button class="btn line" data-act="novo" data-tipo="despesa" data-cat="Pedágio" data-d="${dia}">Pedágio</button></div>
 <button class="btn line wide" data-act="novo" data-tipo="despesa" data-d="${dia}">Outro gasto</button>`;
}
function vHoje(){
 const o=Core.summary(S,TODAY,TODAY),meta=dayGoal(TODAY),lista=S.lanc.filter(l=>l.data===TODAY).slice().reverse();
 return `${demoBar()}<div class="top"><div><h1>Meus Fretes</h1><div class="date">${longDate(TODAY)}</div></div></div>
 <h2>Hoje</h2><div class="card total">${contaDia(o)}</div>
 <div class="metaTxt goal-status"><span>Meta do dia <b>${R(meta)}</b></span><b>${o.lucro<meta?'Faltam '+R(meta-o.lucro):'Meta atingida'}</b></div>
 <div class="goal-road">${sceneSVG('dia',AS.dia?.fx??SC.x0,'Meta').replace('viewBox="0 0 360 150"','viewBox="0 34 360 116"')}</div>
 <button class="btn main wide" data-act="novo" data-tipo="viagem">Anotar frete</button>
 ${atalhosGastos(TODAY)}<button class="btn line wide" data-act="calc">Calcular frete</button>
 <div class="card"><h2>Últimos lançamentos</h2>${lista.length?lista.slice(0,3).map(entryRow).join(''):'<div class="empty">Nada anotado ainda hoje.</div>'}
 ${lista.length>3?`<button class="btn line wide" data-act="dia" data-d="${TODAY}">Ver todos</button>`:''}</div>
 <button class="btn line wide" data-act="vermes">Ver relatório do mês</button>`;
}
function vHist(){const days=[...new Set(S.lanc.map(l=>l.data))].sort().reverse();let h=`${demoBar()}<h1>Histórico</h1><p class="date">Toque num dia para ver ou corrigir.</p><div class="two"><button class="btn line" data-act="twoDays">Comparar dois dias</button><button class="btn line" data-act="accounts">Contas do mês</button></div>`;if(!days.length)return h+'<div class="card empty">Ainda não há lançamentos.</div>';let m='';for(const d of days){if(d.slice(0,7)!==m){m=d.slice(0,7);const o=Core.summary(S,...monthRange(d));h+=`<div class="mhead"><span>${MES[pd(d).getMonth()]} ${pd(d).getFullYear()}</span><span>${R(o.lucro)}</span></div>`}const o=Core.summary(S,d,d);h+=`<button type="button" class="day" data-act="dia" data-d="${d}"><span><b>${shortDate(d)}${d===TODAY?' · hoje':''}</b><br><span class="sub">${o.nF} frete${o.nF===1?'':'s'}${o.nA?' · abasteceu':''}</span>${o.nF?`<br><span class="sub">${metaTexto(o.lucro,dayGoal(d))}</span>`:''}</span><span class="v ${o.lucro<0?'neg':''}">${R(o.lucro)}</span></button>`}return h}
function range(){if(rep.modo==='dia')return[rep.ref,rep.ref];if(rep.modo==='semana'){const a=weekStart(rep.ref);return[a,addD(a,6)]}if(rep.modo==='mes')return monthRange(rep.ref);if(rep.modo==='dois')return[...rep.dates].sort();return[rep.de,rep.ate]}
function shift(n){if(rep.modo==='dia')rep.ref=addD(rep.ref,n);else if(rep.modo==='semana')rep.ref=addD(rep.ref,7*n);else if(rep.modo==='mes')rep.ref=addM(rep.ref.slice(0,7)+'-01',n);chartSel=null}
function rotulo(a,b){if(rep.modo==='dia')return longDate(a);if(rep.modo==='semana')return`Semana de ${dm(a)} a ${dm(b)}`;if(rep.modo==='mes')return`${MES[pd(a).getMonth()]} de ${pd(a).getFullYear()}`;return`${dm(a)}/${a.slice(0,4)} ${rep.modo==='dois'?'e':'a'} ${dm(b)}/${b.slice(0,4)}`}
function barChart(a,b,o){const days=rep.modo==='dois'?[...rep.dates].sort():[];if(!days.length)for(let d=a;d<=b&&d<=TODAY;d=addD(d,1))days.push(d);const W=Math.max(340,days.length*42),H=190,top=24,bot=30,n=days.length,gw=W/n,bw=Math.min(34,gw*.68),goals=days.map(dayGoal),vals=days.map(d=>o.dias[d]||0),mx=Math.max(...goals.map(x=>x*1.15),...vals,1),mn=Math.min(0,...vals),sc=(H-top-bot)/(mx-mn),y0=top+mx*sc;let s=`<div class="chartScroll" tabindex="0" aria-label="Gráfico de lucro por dia; deslize para ver os outros dias"><svg style="width:${W}px;max-width:none" viewBox="0 0 ${W} ${H}" role="img" aria-label="Total por dia; os mesmos valores estão na lista de dias">`;if(goals.every(x=>x===goals[0])){const ym=y0-goals[0]*sc;s+=`<line x1="0" x2="${W}" y1="${ym}" y2="${ym}" stroke="#17603F" stroke-dasharray="5 5" stroke-width="1.5"/>`}s+=`<line x1="0" x2="${W}" y1="${y0}" y2="${y0}" stroke="#B8B1A2"/>`;days.forEach((d,i)=>{const v=vals[i],x=i*gw+(gw-bw)/2,h=Math.abs(v)*sc,y=v>=0?y0-h:y0,fill=v<0?'#A9221B':v>=goals[i]?'#17603F':'#7FB394';s+=`<rect data-act="bar" data-d="${d}" x="${i*gw}" y="${top}" width="${gw}" height="${H-top}" fill="transparent"/>`;if(d<=TODAY&&o.porDia[d])s+=`<rect pointer-events="none" x="${x}" y="${y}" width="${bw}" height="${Math.max(h,2)}" rx="3" fill="${fill}" ${chartSel===d?'stroke="#191D1B" stroke-width="2.5"':''}/>`;if(n<=7&&o.porDia[d])s+=`<text pointer-events="none" x="${x+bw/2}" y="${(v>=0?y:y+h)-5}" text-anchor="middle" font-size="12" fill="#191D1B">${Math.round(v/100)}</text>`;s+=`<text pointer-events="none" x="${i*gw+gw/2}" y="${H-8}" text-anchor="middle" font-size="13" fill="#565D59">${n<=7?DOW[pd(d).getDay()]:dm(d)}</text>`});return s+'</svg></div>'}
function listDays(o){return Object.keys(o.porDia).sort().map(d=>`<button class="day" data-act="dia" data-d="${d}"><span>${shortDate(d)}<br><span class="sub">${d.slice(0,4)}</span></span><b class="${o.dias[d]<0?'neg':''}">${R(o.dias[d])}</b></button>`).join('')||'<p class="sub">Sem lançamentos neste período.</p>'}
function vRel(){const[a,b]=range(),o=resumo(a,b),m=rep.modo,partial=a<=TODAY&&b>=TODAY&&m!=='dia'&&m!=='dois';let cmp='';if(m!=='dois'){const[pa,pb]=Core.previousRange(m,a,b,TODAY),prev=Core.summary(S,pa,pb);if(Object.keys(prev.porDia).length){const delta=o.lucro-prev.lucro;cmp=`${R(Math.abs(delta))} ${delta>=0?'a mais':'a menos'} que ${dm(pa)} a ${dm(pb)}${partial?' (mesmos dias)':''}`}}let h=`${demoBar()}<h1 style="margin-bottom:12px">Relatórios</h1><div class="seg" role="group" aria-label="Período">${[['dia','Dia'],['semana','Semana'],['mes','Mês'],['periodo','Período']].map(([k,t])=>`<button type="button" data-act="modo" data-m="${k}" aria-pressed="${m===k}">${t}</button>`).join('')}</div>`;if(['periodo','dois'].includes(m))h+=`<div class="period"><div class="p">${rotulo(a,b)}</div><button class="btn line" data-act="${m==='dois'?'twoDays':'customRange'}">Alterar</button></div>`;else h+=`<div class="period"><button class="icon-btn" data-act="prev" aria-label="Período anterior">${I.left}</button><div class="p">${rotulo(a,b)}${partial?'<span class="badge">Parcial</span>':''}</div><button class="icon-btn" data-act="next" aria-label="Próximo período" ${b>=TODAY?'disabled':''}>${I.right}</button></div>`;h+=`<div class="card total">${contaDia(o)}${cmp?`<div class="cmp">${cmp}</div>`:''}${m==='dia'&&o.nF?`<div class="metaTxt">${metaTexto(o.lucro,dayGoal(a))}</div>`:''}</div><button class="btn line wide" data-act="twoDays">Comparar dois dias</button>`;
 if(m==='dois'){const dates=[...rep.dates].sort(),x=Core.summary(S,dates[0],dates[0]),y=Core.summary(S,dates[1],dates[1]);h+=`<div class="card"><h2>Dois dias escolhidos</h2><div class="row"><span>${dm(dates[0])}</span><b>${R(x.lucro)}</b></div><div class="row"><span>${dm(dates[1])}</span><b>${R(y.lucro)}</b></div><div class="sub">Diferença entre os dias: ${R(Math.abs(y.lucro-x.lucro))}. A soma inclui somente esses dois dias.</div></div>`}
 if(m!=='dia')h+=`<div class="card chart"><h2>Total por dia</h2>${barChart(a,b,o)}<div class="tip">${chartSel&&o.porDia[chartSel]?`${shortDate(chartSel)}: ${R(o.dias[chartSel])}`:''}</div>${chartSel&&o.porDia[chartSel]?`<button class="btn line wide" data-act="dia" data-d="${chartSel}">Abrir este dia</button>`:''}<details><summary>Ver lançamentos por dia</summary>${listDays(o)}</details></div><details class="card"><summary>Metas e média</summary><div class="kpis"><div class="kpi"><div class="n">${o.metaOk} de ${o.trab.size}</div><div class="l">dias trabalhados com a meta do dia atingida</div></div><div class="kpi"><div class="n">${o.trab.size?R(Math.round(o.lucro/o.trab.size)):'—'}</div><div class="l">resultado do período por dia trabalhado</div></div></div></details>`;
 h+=`<details class="card fuelCard"><summary>Combustível no período</summary><div class="kpis"><div class="kpi"><div class="n">${R(o.comb)}</div><div class="l">${o.nA} abastecimento${o.nA===1?'':'s'}</div></div><div class="kpi"><div class="n">${o.litros100?brNum(o.litros100/100)+' L':'—'}</div><div class="l">${o.litros100?'preço médio '+R(Core.roundDiv(BigInt(o.combComLitros)*100n,o.litros100))+'/L nos registros com litros':'sem litros informados'}</div></div><div class="kpi"><div class="n">${o.fretes?Math.round(o.comb/o.fretes*100)+'%':'—'}</div><div class="l">do valor dos fretes para o tanque</div></div><div class="kpi"><div class="n">${o.km?brNum(o.km)+' km':'—'}</div><div class="l">anotados nos fretes</div></div></div></details>`;
 if(m==='dia'){
  const lst=S.lanc.filter(l=>l.data===a).slice().reverse();
  h+=`<div class="card"><h2>Lançamentos</h2>${lst.length?lst.map(entryRow).join(''):'<div class="empty">Nada neste dia.</div>'}
  <button class="btn main wide" data-act="novo" data-tipo="viagem" data-d="${a}">Anotar frete neste dia</button>${atalhosGastos(a)}</div>`;
 }
 if(m==='mes'){
  const mes=a.slice(0,7),previstas=contasNoMes(mes).filter(c=>c.label==='Prevista');
  if(previstas.length)h+=`<div class="card"><h2>Contas previstas</h2><p>${R(previstas.reduce((total,c)=>total+c.valor,0))}</p><button class="btn line wide" data-act="accounts" data-m="${mes}">Ver contas do mês</button></div>`;
  h+=fuelMonths(a);
 }
 return h+'<button class="btn line wide" data-act="share">Compartilhar resumo</button>';
}
function fuelMonths(ref){const end=ref.slice(0,7)+'-01',months=Array.from({length:6},(_,i)=>addM(end,i-5)),totals=months.map(m=>Core.summary(S,...monthRange(m)).comb),max=Math.max(...totals,1);return `<details class="card"><summary>Combustível nos últimos 6 meses</summary><svg viewBox="0 0 340 170" role="img" aria-label="Gasto mensal com combustível; valores na lista abaixo" style="width:100%;height:auto">${months.map((m,i)=>{const h=totals[i]/max*112;return `<rect x="${i*56+12}" y="${130-h}" width="30" height="${h}" rx="3" fill="#A96A2D"/><text x="${i*56+27}" y="150" text-anchor="middle" font-size="12" fill="#565D59">${MESC[pd(m).getMonth()]}</text>`}).join('')}</svg>${months.map((m,i)=>`<div class="row"><span>${MESC[pd(m).getMonth()]} ${m.slice(0,4)}${m<=TODAY&&m.slice(0,7)===TODAY.slice(0,7)?' · parcial':''}</span><b>${R(totals[i])}</b></div>`).join('')}</details>`}
function shareText(){
 const[a,b]=range(),o=resumo(a,b),pedagio=o.cats['Pedágio']||0;
 return `Meus Fretes — ${rotulo(a,b)}\nValor ganho: ${R(o.fretes)}\nValor gasto: ${R(o.comb+o.outras)}\nCombustível: ${R(o.comb)}\nPedágio: ${R(pedagio)}\nOutros gastos: ${R(o.outras-pedagio)}\nTotal do trabalho: ${R(o.lucro)}${rep.modo==='dia'?'\nMeta do dia: '+R(dayGoal(a)):''}`;
}
function vAj(){const s=S.settings,g=dayGoal(TODAY);return `<h1 style="margin-bottom:12px">Ajustes</h1><form class="card" id="ajForm"><div class="field"><label for="aMeta">Meta de lucro por dia</label><div class="money"><span>R$</span><input id="aMeta" inputmode="decimal" value="${inp(g/100)}"></div><span class="sub">A nova meta diária vale a partir de hoje.</span></div><div class="two"><div class="field"><label for="aKml">Consumo (km por litro)</label><input id="aKml" inputmode="decimal" value="${inp(s.kml)}"></div><div class="field"><label for="aPreco">Preço do litro</label><input id="aPreco" inputmode="decimal" value="${inp(latestPrice()/100)}"></div></div><div class="err" id="ajErr" role="alert"></div><button class="btn main wide" type="submit">Salvar ajustes</button></form><div class="card"><h2>Tamanho da letra</h2><div class="fontBtns">${[[18,'A'],[20,'A+'],[23,'A++']].map(([v,t])=>`<button class="btn ${s.fs===v?'main':'line'}" style="grid-column:auto" data-act="fs" data-v="${v}" aria-pressed="${s.fs===v}">${t}</button>`).join('')}</div></div><div class="card"><h2>Contas do mês</h2><button class="btn line wide" data-act="accounts">Ver despesas mensais</button></div><div class="card"><h2>Cópia de segurança</h2><p class="sub">${s.lastBackup?'Última exportação iniciada: '+new Date(s.lastBackup).toLocaleString('pt-BR'):'Nenhuma cópia feita.'}${!s.lastBackup||Date.now()-Date.parse(s.lastBackup)>=7*86400000?' É hora de fazer uma cópia.':''}</p><button class="btn line wide" data-act="backup">Fazer cópia</button><button class="btn line wide" data-act="restore">Restaurar cópia</button>${temPreventiva()?'<button class="btn line wide" data-act="voltarCopia">Voltar aos dados anteriores</button>':''}<input id="restoreFile" type="file" accept=".fretes,application/json" hidden></div><details class="card"><summary>Dados de teste</summary><p class="sub">Use os exemplos somente para experimentar o protótipo.</p><button class="btn line wide" data-act="reset">Recarregar exemplos</button><button class="btn line wide danger" data-act="limpar">Apagar tudo</button></details>`}
function contasNoMes(mes){
 const lancamentos=new Map(S.lanc.map(l=>[l.id,l]));
 return S.rec.flatMap(r=>{
  const v=Core.recVersion(r,mes);
  if(!v||mes<r.inicio.slice(0,7))return [];
  const o=S.occ[r.id+'|'+mes],l=lancamentos.get(o?.lancId),d=l?.data||o?.override?.data||Core.dueDate(mes,v.dia);
  if(!l&&d<r.inicio&&o?.status!=='pulada')return [];
  const label=o?.status==='pulada'?'Pulada neste mês':l?'Lançada':v.fim&&d>v.fim?'Repetição encerrada':!v.ativa&&(!o?.override||o.status==='previstaPausada')?'Repetição pausada':'Prevista';
  return [{r,v,o,l,d,label,valor:l?.valor??o?.override?.valor??v.valor}];
 });
}
function vAccounts(){
 const contas=contasNoMes(recMonth);
 let h=`<h1>Contas do mês</h1><div class="period"><button class="icon-btn" data-act="recPrev" aria-label="Mês anterior">${I.left}</button><b>${MES[pd(recMonth+'-01').getMonth()]} ${recMonth.slice(0,4)}</b><button class="icon-btn" data-act="recNext" aria-label="Mês seguinte">${I.right}</button></div><button class="btn main wide" data-act="recNew">Cadastrar despesa mensal</button>`;
 for(const {r,v,o,l,d,label,valor} of contas)h+=`<div class="card"><div class="row"><div><h2>${esc(l?.desc||o?.override?.desc||v.nome)}</h2><span class="sub">${dm(d)} · ${label}</span></div><b>${R(valor)}</b></div><div class="two"><button class="btn line" data-act="recEdit" data-id="${esc(r.id)}">Editar conta</button><button class="btn line" data-act="recSkip" data-id="${esc(r.id)}">${o?.status==='pulada'?'Retomar este mês':'Pular este mês'}</button><button class="btn line" data-act="recToggle" data-id="${esc(r.id)}">${v.ativa?'Pausar repetição':'Retomar repetição'}</button>${l?`<button class="btn line" data-act="edit" data-id="${esc(l.id)}">Ver lançamento</button>`:''}</div></div>`;
 return h+(contas.length?'':'<div class="card empty">Nenhuma conta para este mês.</div>');
}
let edit=null,cat='Pedágio',saving=false,sheetKind='',drafts={},returnFocus=null;
const CATS=['Pedágio','Manutenção','Estacionamento','Lavagem','Multa','Conta mensal','Outros'];
function formHead(title){return `<h3 id="sheetTitle">${title}<button type="button" class="icon-btn" data-act="close" aria-label="Fechar">${I.x}</button></h3>`}
function openSheet(html,kind=''){returnFocus=document.activeElement;sheetKind=kind;document.getElementById('sheet').innerHTML=html;document.getElementById('sheet').setAttribute('aria-labelledby','sheetTitle');document.getElementById('sheetBg').classList.add('open');(document.getElementById('fVal')||document.querySelector('#sheet input, #sheet button'))?.focus()}
function draftKey(){return edit?.key||''}
function captureDraft(){if(sheetKind!=='entry'||!edit)return;const inputs={};document.querySelectorAll('#sheet input').forEach(e=>inputs[e.id]=e.value);drafts[draftKey()]={inputs,cat,abMode}}
function closeSheet(preserve=true){if(preserve)captureDraft();document.getElementById('sheetBg').classList.remove('open');document.getElementById('sheet').innerHTML='';edit=null;sheetKind='';returnFocus?.focus?.()}
function sheetForm(tipo,l=null,data,sugestao){
 const dataInicial=l?.data||data||TODAY;
 const key=tipo+':'+(l?.id||sugestao||'novo')+':'+dataInicial;
 const draft=drafts[key];
 edit={tipo,id:l?.id||null,key,original:l,confirmacao:null};
 cat=draft?.cat||l?.cat||sugestao||'Outros';
 const date=draft?.inputs?.fData||dataInicial;
 const titulo=l?'Corrigir lançamento':tipo==='viagem'?'Anotar frete':tipo==='abast'?'Anotar combustível':sugestao==='Pedágio'?'Anotar pedágio':'Outro gasto';
 let h=formHead(titulo)+`<form id="fForm" novalidate><div class="sub" id="fDia">${date===TODAY?'Hoje, ':''}${dm(date)}</div>
 <div class="field"><label for="fVal">${tipo==='viagem'?'Valor ganho':'Valor gasto'}</label>
 <div class="money ${tipo==='viagem'?'ganho':'gasto'}"><span>R$</span><input id="fVal" inputmode="decimal" placeholder="0,00" value="${l?inp(l.valor/100):''}" autocomplete="off"></div></div>
 <details ${l||tipo==='despesa'&&!sugestao?'open':''}><summary>Mais detalhes</summary>`;
 if(tipo==='abast'){
  abMode=draft?.abMode||l?.modoEntrada||'preco';
  abInit={preco:l?.preco?inp(l.preco/100):'',litros:l?.litros100?inp(l.litros100/100):''};
  h+=`<div class="seg" role="group" aria-label="Detalhes do combustível"><button type="button" data-act="abMode" data-m="preco" aria-pressed="${abMode==='preco'}">Preço por litro</button><button type="button" data-act="abMode" data-m="litros" aria-pressed="${abMode==='litros'}">Quantos litros</button></div><div class="field" id="fAuxBox"></div><div id="fPL" aria-live="polite"></div>`;
 }
 if(tipo==='despesa')h+=`<div class="field"><label>Categoria</label><div class="chips" id="fCats">${(CATS.includes(cat)?CATS:[...CATS,cat]).map(c=>`<button type="button" data-act="cat" data-c="${esc(c)}" aria-pressed="${c===cat}">${esc(c)}</button>`).join('')}</div></div>`;
 h+=`<div class="field"><label for="fDesc">${tipo==='viagem'?'Cliente ou descrição':'Observação'} <span class="sub">(opcional)</span></label><input id="fDesc" maxlength="160" value="${esc(l?.desc||'')}"></div>`;
 if(tipo==='viagem')h+=`<div class="two"><div class="field"><label for="fKm">Km rodados</label><input id="fKm" inputmode="decimal" value="${l?.km?inp(l.km):''}"></div><div class="field"><label for="fMin">Duração (minutos)</label><input id="fMin" inputmode="numeric" value="${l?.min||''}"></div></div>`;
 h+=`<div class="field"><label for="fData">Data</label><input id="fData" type="date" value="${date}" max="${TODAY}"></div></details><div class="sub" id="fPedagios"></div><div class="err" id="fErr" role="alert"></div><button class="btn main wide" type="submit">Salvar</button>${tipo==='viagem'&&!l?'<button type="button" class="btn line wide" data-act="saveMore">Salvar e anotar outro</button>':''}</form>`;
 openSheet(h,'entry');
 if(tipo==='abast')abAux();
 if(draft)for(const[id,value]of Object.entries(draft.inputs)){const el=document.getElementById(id);if(el)el.value=value}
 if(tipo==='abast')abUpd();
 atualizarPedagios();
}
function atualizarPedagios(){
 const box=document.getElementById('fPedagios');
 if(!box)return;
 const itens=cat==='Pedágio'&&edit?.tipo==='despesa'?S.lanc.filter(l=>l.tipo==='despesa'&&l.cat==='Pedágio'&&l.data===val('fData')):[];
 box.textContent=itens.length?'Pedágio no dia: '+R(itens.reduce((total,l)=>total+l.valor,0))+' · Últimos: '+itens.slice(-3).reverse().map(l=>R(l.valor)).join(' · '):'';
}
let abMode='preco',abInit={};
function abAux(){document.getElementById('fAuxBox').innerHTML=abMode==='preco'?`<label for="fAux">Preço do litro (opcional)</label><div class="money"><span>R$</span><input id="fAux" inputmode="decimal" value="${abInit.preco||''}"></div>`:`<label for="fAux">Litros (opcional)</label><div class="money"><span>L</span><input id="fAux" inputmode="decimal" value="${abInit.litros||''}"></div>`;abUpd()}
function abCalc(){return Core.fuel(parseMoney(val('fVal')),parseMoney(val('fAux')),abMode)}
function abUpd(){const c=abCalc(),el=document.getElementById('fPL');if(!el)return;el.innerHTML=c?(abMode==='preco'?`<span class="sub">Entraram no tanque</span><b>${brNum(c.litros100/100)} litros</b>`:`<span class="sub">Você pagou por litro</span><b>${R(c.preco)}</b>`):''}
async function submitEntry(more){
 if(saving||!edit)return;
 const err=document.getElementById('fErr'),v=parseMoney(val('fVal')),data=val('fData');
 if(!(v>0)){err.textContent='Confira o valor. Ex.: 1.234,56 ou 85,50.';return}
 if(!Core.validDate(data)||data>TODAY){err.textContent='Informe uma data válida, até hoje.';return}
 const editor=edit,old=editor.original;
 const o={...old,id:editor.id||uid(),tipo:editor.tipo,data,valor:v,desc:val('fDesc').trim(),editadoEm:new Date().toISOString(),criadoEm:old?.criadoEm||new Date().toISOString()};
 if(editor.tipo==='viagem'){
  const km=val('fKm').trim()?parseNum(val('fKm')):0,min=val('fMin').trim()?Core.integer(val('fMin')):0;
  if(!Number.isFinite(km)||km<0||!Number.isSafeInteger(min)||min<0){err.textContent='Confira os km e a duração em minutos.';return}
  delete o.km;delete o.min;
  if(km)o.km=km;
  if(min)o.min=min;
 }
 if(editor.tipo==='abast'){
  const c=abCalc();
  if(val('fAux').trim()&&!c){err.textContent='Confira o preço por litro ou os litros.';return}
  delete o.litros;delete o.litros100;delete o.preco;delete o.modoEntrada;
  if(c)Object.assign(o,c,{modoEntrada:abMode});
 }
 if(editor.tipo==='despesa')o.cat=cat;
 const confirmacao=data+'|'+v;
 let repetido=false;
 saving=true;
 try{
  const ok=await commit(s=>{
   if(old&&JSON.stringify(s.lanc.find(l=>l.id===old.id))!==JSON.stringify(old))throw Error('O lançamento mudou em outra aba.');
   if(o.tipo==='despesa'&&o.cat==='Pedágio'&&editor.confirmacao!==confirmacao&&s.lanc.some(l=>l.id!==o.id&&l.tipo==='despesa'&&l.cat===o.cat&&l.data===data&&l.valor===v)){
    repetido=true;
    return false;
   }
   Core.saveEntry(s,o);
  });
  if(!ok){err.textContent=storageProblem;return}
  if(repetido){
   editor.confirmacao=confirmacao;
   err.textContent='Já existe um pedágio deste valor neste dia.';
   document.querySelector('#fForm button[type=submit]').textContent='Salvar mesmo assim';
   return;
  }
  delete drafts[editor.key];
  if(edit!==editor)return;
  closeSheet(false);render(true);
  if(more)sheetForm('viagem',null,data);
 }finally{saving=false}
}

function calcSheet(){openSheet(formHead('Calcular frete')+`<p class="sub">Esta conta não altera os lançamentos do dia.</p><div class="field"><label for="cVal">Quanto fica para você</label><div class="money"><span>R$</span><input id="cVal" inputmode="decimal" placeholder="0,00"></div></div><div class="two"><div class="field"><label for="cKm">Km totais</label><input id="cKm" inputmode="decimal"><span class="sub">Inclua coleta e retorno, se houver.</span></div><div class="field"><label for="cMin">Duração (minutos)</label><input id="cMin" inputmode="numeric" placeholder="Opcional"></div></div><div class="field"><label for="cOut">Pedágio e outros custos</label><div class="money"><span>R$</span><input id="cOut" inputmode="decimal" placeholder="0,00"></div></div><div class="two"><div class="field"><label for="cKml">Km por litro</label><input id="cKml" inputmode="decimal" value="${inp(S.settings.kml)}"></div><div class="field"><label for="cPr">Preço do litro</label><input id="cPr" inputmode="decimal" value="${inp(latestPrice()/100)}"></div></div><div class="calcOut" id="cOutBox" aria-live="polite"><div class="empty">Preencha os valores para ver a conta.</div></div><button class="btn line wide" data-act="calcClear">Limpar cálculo</button><button class="btn main wide" data-act="close">Fechar</button>`,'calc')}
function calcData(){return Core.calc({fare:parseMoney(val('cVal')),km100:parseMoney(val('cKm')),kml100:parseMoney(val('cKml')),price:parseMoney(val('cPr')),minutes:val('cMin').trim()?Core.integer(val('cMin')):0,cost:val('cOut').trim()?parseMoney(val('cOut')):0})}
function calcUpd(){const c=calcData(),el=document.getElementById('cOutBox');if(!el)return;if(!c){el.innerHTML='<div class="empty">Confira valor, km, consumo e custos.</div>';return}el.innerHTML=`<div class="row"><span>Valor do frete</span><b>${R(c.fare)}</b></div><div class="row"><span>Combustível (${brNum(c.liters100/100)} L)</span><b>− ${R(c.fuel)}</b></div>${c.cost?`<div class="row"><span>Outros custos</span><b>− ${R(c.cost)}</b></div>`:''}<div class="row total"><b>Sobra</b><b class="${c.res<0?'neg':''}">${R(c.res)}</b></div><div class="kpis"><div class="kpi"><div class="n">${R(c.perKm)}</div><div class="l">por km</div></div>${c.perHour!==null?`<div class="kpi"><div class="n">${R(c.perHour)}</div><div class="l">por hora</div></div>`:''}</div>`}
function customRange(){const[a,b]=rep.modo==='periodo'?range():monthRange(TODAY);openSheet(formHead('Escolher período')+`<form id="rangeForm" novalidate><div class="field"><label for="rangeFrom">Data inicial</label><input id="rangeFrom" type="date" value="${a}" max="${TODAY}"></div><div class="field"><label for="rangeTo">Data final</label><input id="rangeTo" type="date" value="${b>TODAY?TODAY:b}" max="${TODAY}"></div><p class="sub">Até 6 meses por consulta. O histórico mais antigo continua disponível.</p><div class="err" id="rangeErr" role="alert"></div><button class="btn main wide" type="submit">Ver período</button></form>`,'range')}
function twoDays(){const ds=rep.modo==='dois'?rep.dates:[addD(TODAY,-1),TODAY];openSheet(formHead('Comparar dois dias')+`<form id="twoForm" novalidate><div class="field"><label for="twoA">Primeiro dia</label><input id="twoA" type="date" value="${ds[0]}" max="${TODAY}"></div><div class="field"><label for="twoB">Segundo dia</label><input id="twoB" type="date" value="${ds[1]}" max="${TODAY}"></div><div class="err" id="twoErr" role="alert"></div><button class="btn main wide" type="submit">Somar e comparar</button></form>`,'two')}
let recEditId=null;
function recForm(r=null){recEditId=r?.id||null;const v=r?Core.recVersion(r,recMonth):null;
 const alt=r?S.occ[r.id+'|'+recMonth]?.override:null;
 openSheet(formHead(r?'Editar conta mensal':'Despesa mensal')+`<form id="recForm" novalidate><div class="field"><label for="rNome">Nome da despesa</label><input id="rNome" maxlength="160" value="${esc(alt?.desc||v?.nome||'')}"></div><div class="field"><label for="rValor">Valor mensal</label><div class="money"><span>R$</span><input id="rValor" inputmode="decimal" value="${v?inp((alt?.valor??v.valor)/100):''}"></div></div><div class="two"><div class="field"><label for="rDia">Dia do mês</label><input id="rDia" inputmode="numeric" value="${(alt?pd(alt.data).getDate():v?.dia)||pd(TODAY).getDate()}"></div><div class="field"><label for="rInicio">Data inicial</label><input id="rInicio" type="date" value="${r?.inicio||TODAY}" ${r?'readonly':''}></div></div><div class="field"><label for="rFim">Última data (opcional)</label><input id="rFim" type="date" value="${v?.fim||''}"></div>${r?`<div class="field"><label for="rScope">Aplicar alteração</label><select id="rScope"><option value="month">Só ${recMonth}</option><option value="forward">De ${recMonth} em diante</option></select></div>`:''}<p class="sub">O lançamento é automático na data. Dia 31 ajusta para 30 quando necessário; fevereiro usa dia 28.</p><div class="err" id="recErr" role="alert"></div><button class="btn main wide" type="submit">Salvar conta mensal</button></form>`,'rec')}
function reconcileRec(s,r,from){for(const[key,o]of Object.entries(s.occ)){if(o.recId!==r.id||o.mes<from||o.status==='pulada'||o.override)continue;const l=s.lanc.find(l=>l.id===o.lancId);if(!l)continue;const v=Core.recVersion(r,o.mes),d=Core.dueDate(o.mes,v.dia);if(!v.ativa)continue;if(d<r.inicio||d>TODAY||(v.fim&&d>v.fim)){s.lanc=s.lanc.filter(x=>x.id!==l.id);Core.touch(s,l.data);s.occ[key]={recId:r.id,mes:o.mes,status:'prevista'};continue}Core.saveEntry(s,{...l,data:d,valor:v.valor,desc:v.nome,cat:v.cat});delete s.occ[key].override}}
async function submitRec(){const nome=val('rNome').trim(),valor=parseMoney(val('rValor')),dia=Core.integer(val('rDia')),inicio=val('rInicio'),fim=val('rFim'),err=document.getElementById('recErr');if(!nome||!(valor>0)||!(dia>=1&&dia<=31)||!Core.validDate(inicio)||(fim&&(!Core.validDate(fim)||fim<inicio))){err.textContent='Confira nome, valor, dia e datas.';return}const id=recEditId||uid(),scope=val('rScope'),m=recMonth;const ok=await commit(s=>{let r=s.rec.find(x=>x.id===id);const v={desde:m,nome,valor,dia,cat:'Conta mensal',fim,ativa:r?Core.recVersion(r,m)?.ativa===true:true};if(!r){r={id,inicio,versions:[{...v,desde:inicio.slice(0,7)}]};s.rec.push(r)}else if(scope==='month'){const key=id+'|'+m,o=s.occ[key],l=o?.lancId?s.lanc.find(x=>x.id===o.lancId):null,override={data:Core.dueDate(m,dia),valor,desc:nome,cat:'Conta mensal'};if(l){if(override.data>=r.inicio&&override.data<=TODAY){Core.saveEntry(s,{...l,...override})}else{s.lanc=s.lanc.filter(x=>x.id!==l.id);Core.touch(s,l.data);s.occ[key]={recId:id,mes:m,status:'prevista',override}}}else s.occ[key]={recId:id,mes:m,status:o?.status==='pulada'?'pulada':!v.ativa&&(!o?.override||o.status==='previstaPausada')?'previstaPausada':'prevista',override}}else{r.versions=r.versions.filter(v=>v.desde<m);r.versions.push(v);reconcileRec(s,r,m)}Core.generate(s,TODAY,uid)});if(ok){closeSheet(false);tab='contas';render();toast('Conta mensal salva.')}else err.textContent=storageProblem}
async function recSkip(id){const key=id+'|'+recMonth;const ok=await commit(s=>{const o=s.occ[key];if(o?.status==='pulada'){const r=s.rec.find(r=>r.id===id),v=Core.recVersion(r,recMonth);s.occ[key]={recId:id,mes:recMonth,status:'prevista',override:o.override||{data:Core.dueDate(recMonth,v.dia),valor:v.valor,desc:v.nome,cat:v.cat}};Core.generate(s,TODAY,uid)}else{if(o?.lancId){const l=s.lanc.find(x=>x.id===o.lancId);if(l){s.lanc=s.lanc.filter(x=>x.id!==l.id);Core.touch(s,l.data)}}s.occ[key]={recId:id,mes:recMonth,status:'pulada',...(o?.override?{override:o.override}:{})}}});if(ok){render(true);toast('Conta atualizada.')}}
async function recToggle(id){if(await commit(s=>{const r=s.rec.find(x=>x.id===id),v=Core.recVersion(r,recMonth);r.versions=r.versions.filter(x=>x.desde<recMonth);r.versions.push({...v,desde:recMonth,ativa:!v.ativa});Core.generate(s,TODAY,uid)})){render(true);toast('Repetição atualizada.')}}
let tTimer,undoFn=null;
function toast(text,button,fn){const el=document.getElementById('toast'),b=document.getElementById('toastBtn');document.getElementById('toastTxt').textContent=text;b.textContent=button||'';b.style.display=button?'block':'none';undoFn=fn||null;el.classList.add('show');clearTimeout(tTimer);tTimer=setTimeout(()=>{el.classList.remove('show');undoFn=null},button?6000:3000)}
document.getElementById('toastBtn').onclick=()=>{const fn=undoFn;undoFn=null;if(fn)fn();document.getElementById('toast').classList.remove('show')};
function confirmar(text,button,fn){openSheet(formHead(text)+`<div class="two"><button class="btn line" data-act="close">Cancelar</button><button class="btn main danger" style="grid-column:auto" id="confirmBtn">${button}</button></div>`,'confirm');document.getElementById('confirmBtn').onclick=fn}
const bytes64=a=>{let s='';for(let i=0;i<a.length;i+=32768)s+=String.fromCharCode(...a.subarray(i,i+32768));return btoa(s)};
const from64=s=>Uint8Array.from(atob(s),c=>c.charCodeAt(0));
async function passwordKey(password,salt,usage){const material=await crypto.subtle.importKey('raw',new TextEncoder().encode(password),'PBKDF2',false,['deriveKey']);return crypto.subtle.deriveKey({name:'PBKDF2',salt,iterations:210000,hash:'SHA-256'},material,{name:'AES-GCM',length:256},false,usage)}
async function encodeBackup(state,password){const payload=JSON.stringify(Core.normalize(state));if(!password)return JSON.stringify({format:'FretesHumberto',formatVersion:1,encrypted:false,payload:JSON.parse(payload)});const salt=crypto.getRandomValues(new Uint8Array(16)),iv=crypto.getRandomValues(new Uint8Array(12)),ct=crypto.subtle?await crypto.subtle.encrypt({name:'AES-GCM',iv,additionalData:new TextEncoder().encode('FretesHumberto:1')},await passwordKey(password,salt,['encrypt']),new TextEncoder().encode(payload)):await FretesCrypto.encrypt(password,salt,iv,new TextEncoder().encode(payload));return JSON.stringify({format:'FretesHumberto',formatVersion:1,encrypted:true,algorithm:'AES-GCM',kdf:'PBKDF2-SHA256',iterations:210000,salt:bytes64(salt),iv:bytes64(iv),ciphertext:bytes64(new Uint8Array(ct))})}
async function decodeBackup(text,password){if(text.length>10000000)throw Error('Arquivo muito grande.');let env;try{env=JSON.parse(text)}catch{throw Error('O arquivo não é uma cópia válida.')}if(env.format!=='FretesHumberto'||env.formatVersion!==1)throw Error('Formato de cópia incompatível.');if(env.encrypted===false)return Core.normalize(env.payload);if(env.encrypted!==true||env.algorithm!=='AES-GCM'||env.kdf!=='PBKDF2-SHA256'||env.iterations!==210000)throw Error('Proteção incompatível.');try{const salt=from64(env.salt),iv=from64(env.iv);if(salt.length!==16||iv.length!==12)throw Error();const data=crypto.subtle?await crypto.subtle.decrypt({name:'AES-GCM',iv,additionalData:new TextEncoder().encode('FretesHumberto:1')},await passwordKey(password,salt,['decrypt']),from64(env.ciphertext)):await FretesCrypto.decrypt(password,salt,iv,from64(env.ciphertext));return Core.normalize(JSON.parse(new TextDecoder().decode(data)))}catch{throw Error('Senha incorreta ou cópia danificada.')}}
let currentDownloadURL=null;
function downloadURL(text){if(currentDownloadURL)URL.revokeObjectURL(currentDownloadURL);currentDownloadURL=URL.createObjectURL(new Blob([text],{type:'application/json'}));return currentDownloadURL}
function backupReady(encoded,stamp){const url=downloadURL(encoded);openSheet(formHead('Cópia pronta')+`<p class="sub">Salve o arquivo e guarde uma cópia fora deste aparelho.</p><a id="backupDownload" class="btn main wide" href="${url}" download="MeusFretes-${TODAY}.fretes">Salvar arquivo de cópia</a><button class="btn line wide" data-act="close">Fechar</button>`,'backupReady');document.getElementById('backupDownload').onclick=async()=>{if(await commit(s=>s.settings.lastBackup=stamp))toast('Exportação iniciada.')};}
function backupSheet(){openSheet(formHead('Fazer cópia')+`<form id="backupForm"><p class="sub">Guarde uma cópia fora deste aparelho para poder restaurar depois.</p><div class="field"><label for="backupPassword">Senha (opcional)</label><input id="backupPassword" type="password" autocomplete="new-password"></div><div class="err" id="backupErr" role="alert"></div><button class="btn main wide" type="submit">Baixar cópia</button></form>`,'backup')}
let restoreText='',restoreCandidate=null;
function restoreSheet(text){restoreText=text;restoreCandidate=null;openSheet(formHead('Restaurar cópia')+`<form id="restoreForm"><p class="sub">Confira a cópia antes de restaurar.</p><div class="field"><label for="restorePassword">Senha da cópia, se houver</label><input id="restorePassword" type="password" autocomplete="current-password"></div><div class="err" id="restoreErr" role="alert"></div><button class="btn main wide" type="submit">Validar cópia</button></form>`,'restore')}
function temPreventiva(){
 try{return localStorage.getItem(PREVENTIVA)!==null}catch{return false}
}
async function finishRestore(candidate, guardar=true){
 try{
  await comGravacao(()=>{
   const atual=localStorage.getItem(KEY);
   const next=Core.normalize(candidate);
   if(guardar){
    Core.generate(next,TODAY,uid);
    if(atual!==null){
     localStorage.setItem(PREVENTIVA,atual);
     if(localStorage.getItem(PREVENTIVA)!==atual)throw Error('Cópia preventiva indisponível.');
    }
   }
   persistir(next,atual);
  });
  drafts={};
  closeSheet(false);
  tab='hoje';
  applyFs();
  render();
  toast(guardar?'Cópia restaurada.':'Dados anteriores restaurados.');
 }catch{toast('Não foi possível restaurar. Os dados anteriores foram mantidos.')}
}
function voltarCopia(){
 confirmar('Voltar aos dados anteriores?','Voltar',()=>{
  try{finishRestore(JSON.parse(localStorage.getItem(PREVENTIVA)),false)}
  catch{toast('Não foi possível ler a cópia anterior.')}
 });
}
function showCopy(text){openSheet(formHead('Resumo')+`<p class="sub">Copie este texto para compartilhar.</p><textarea class="copy" readonly>${esc(text)}</textarea>`,'copy')}
async function share(){const text=shareText();if(navigator.share){try{await navigator.share({title:'Meus Fretes',text});return}catch(e){if(e.name==='AbortError')return}}try{await navigator.clipboard.writeText(text);toast('Resumo copiado.')}catch{showCopy(text)}}
async function resetData(withExamples){const next=Core.defaults();next.settings.fs=S.settings.fs;next.demo=withExamples;next.lanc=withExamples?seed():[];if(await commit(s=>{for(const k of Object.keys(s))delete s[k];Object.assign(s,next)})){drafts={};closeSheet(false);tab='hoje';render();toast(withExamples?'Exemplos carregados.':'Pronto para começar.')}}
document.addEventListener('click',async e=>{const t=e.target.closest('[data-tab],[data-act]');if(!t)return;if(t.dataset.tab){tab=t.dataset.tab;render();return}const a=t.dataset.act;if(a==='novo')sheetForm(t.dataset.tipo,null,t.dataset.d,t.dataset.cat);else if(a==='edit'){const l=S.lanc.find(x=>x.id===t.dataset.id);if(l)sheetForm(l.tipo,l)}else if(a==='del'){let removed;if(await commit(s=>{removed=Core.removeEntry(s,t.dataset.id)})){render(true);toast('Lançamento apagado.','Desfazer',async()=>{if(await commit(s=>Core.restoreEntry(s,removed)))render(true)})}}else if(a==='close')closeSheet();else if(a==='saveMore')submitEntry(true);else if(a==='cat'){cat=t.dataset.c;edit.confirmacao=null;document.querySelector('#fForm button[type=submit]').textContent='Salvar';atualizarPedagios();document.querySelectorAll('#fCats button').forEach(b=>b.setAttribute('aria-pressed',b.dataset.c===cat))}else if(a==='abMode'){if(t.dataset.m===abMode)return;const c=abCalc();if(abMode==='preco'){abInit.preco=val('fAux');if(c)abInit.litros=inp(c.litros100/100)}else{abInit.litros=val('fAux');if(c)abInit.preco=inp(c.preco/100)}abMode=t.dataset.m;document.querySelectorAll('[data-act=abMode]').forEach(b=>b.setAttribute('aria-pressed',b.dataset.m===abMode));abAux()}else if(a==='calc'||a==='calcClear')calcSheet();else if(a==='vermes'){tab='rel';rep={modo:'mes',ref:TODAY};render()}else if(a==='dia'){tab='rel';rep={modo:'dia',ref:t.dataset.d};render()}else if(a==='modo'){if(t.dataset.m==='periodo'){customRange();return}rep={modo:t.dataset.m,ref:rep.ref||TODAY};chartSel=null;render()}else if(a==='customRange')customRange();else if(a==='twoDays')twoDays();else if(a==='prev'){shift(-1);render()}else if(a==='next'){if(range()[1]<TODAY){shift(1);render()}}else if(a==='bar'){chartSel=t.dataset.d;render(true)}else if(a==='share')share();else if(a==='fs'){if(await commit(s=>s.settings.fs=Number(t.dataset.v))){applyFs();render(true)}}else if(a==='accounts'){recMonth=t.dataset.m||TODAY.slice(0,7);tab='contas';render()}else if(a==='recPrev'||a==='recNext'){recMonth=addM(recMonth+'-01',a==='recPrev'?-1:1).slice(0,7);render()}else if(a==='recNew')recForm();else if(a==='recEdit'){const r=S.rec.find(r=>r.id===t.dataset.id);if(r)recForm(r)}else if(a==='recSkip')recSkip(t.dataset.id);else if(a==='recToggle')recToggle(t.dataset.id);else if(a==='backup')backupSheet();else if(a==='voltarCopia')voltarCopia();else if(a==='restore')document.getElementById('restoreFile').click();else if(a==='limpar')confirmar('Apagar todos os lançamentos?','Apagar tudo',()=>resetData(false));else if(a==='reset')confirmar('Substituir os dados pelos exemplos?','Substituir',()=>resetData(true))});
document.addEventListener('input',e=>{const el=e.target;if(el.inputMode==='numeric')el.value=el.value.replace(/\D/g,'');if(sheetKind==='entry'){if(['fVal','fData'].includes(el.id)){edit.confirmacao=null;document.querySelector('#fForm button[type=submit]').textContent='Salvar';document.getElementById('fErr').textContent=''}captureDraft();if(edit?.tipo==='abast'&&['fVal','fAux'].includes(el.id))abUpd()}if(sheetKind==='calc')calcUpd()});
document.addEventListener('change',async e=>{if(e.target.id==='fData'){const d=val('fData');document.getElementById('fDia').textContent=(d===TODAY?'Hoje, ':'')+(d?dm(d):'');atualizarPedagios()}if(e.target.id==='restoreFile'){const f=e.target.files?.[0];if(!f)return;if(f.size>10000000){toast('A cópia é muito grande.');return}try{restoreSheet(await f.text())}catch{toast('Não foi possível ler o arquivo.')}finally{e.target.value=''}}});
document.addEventListener('focusout',e=>{if(e.target.matches('input[inputmode=decimal]')){const n=parseMoney(e.target.value);if(Number.isFinite(n))e.target.value=inp(n/100)}});
document.addEventListener('submit',async e=>{e.preventDefault();const id=e.target.id;if(id==='fForm')await submitEntry(false);else if(id==='recForm')await submitRec();else if(id==='rangeForm'){const a=val('rangeFrom'),b=val('rangeTo'),err=Core.validRange(a,b,TODAY);if(err){document.getElementById('rangeErr').textContent=err;return}rep={modo:'periodo',de:a,ate:b,ref:b};chartSel=null;closeSheet(false);tab='rel';render()}else if(id==='twoForm'){const a=val('twoA'),b=val('twoB');if(!Core.validDate(a)||!Core.validDate(b)||a>TODAY||b>TODAY||a===b){document.getElementById('twoErr').textContent='Escolha dois dias diferentes, até hoje.';return}rep={modo:'dois',dates:[a,b].sort(),ref:b};chartSel=null;closeSheet(false);tab='rel';render()}else if(id==='ajForm'){const g=parseMoney(val('aMeta')),k=parseNum(val('aKml')),p=parseMoney(val('aPreco'));if(!(g>0)||!(k>0&&k<=1000)||!(p>0)){document.getElementById('ajErr').textContent='Confira meta, consumo e preço.';return}if(await commit(s=>{if(g!==Core.dayGoal(s,TODAY))Core.setDayGoal(s,TODAY,g);s.settings.kml=k;if(p!==Core.price(s)){s.settings.preco=p;s.settings.manualPrice={data:TODAY,preco:p,stamp:new Date().toISOString()}}})){toast('Ajustes salvos.');render(true)}}else if(id==='backupForm'){const btn=e.target.querySelector('[type=submit]');btn.disabled=true;try{const next=Core.clone(S);next.settings.lastBackup=new Date().toISOString();const encoded=await encodeBackup(next,val('backupPassword'));backupReady(encoded,next.settings.lastBackup);}catch(err){document.getElementById('backupErr').textContent=err.message}finally{btn.disabled=false}}else if(id==='restoreForm'){const btn=e.target.querySelector('[type=submit]');btn.disabled=true;try{restoreCandidate=await decodeBackup(restoreText,val('restorePassword'));const n=restoreCandidate.lanc.length;
 openSheet(formHead('Cópia validada')+`<p>${n} lançamentos encontrados.</p><p class="sub">A base anterior poderá ser recuperada em Ajustes.</p><button class="btn main wide" id="applyRestore">Restaurar esta cópia</button><button class="btn line wide" data-act="close">Cancelar</button>`,'confirmRestore');
 document.getElementById('applyRestore').onclick=async e=>{
  e.target.disabled=true;
  try{await finishRestore(restoreCandidate)}finally{e.target.disabled=false}
 };
}catch(err){document.getElementById('restoreErr').textContent=err.message}finally{btn.disabled=false}}});
document.getElementById('sheetBg').addEventListener('click',e=>{if(e.target.id==='sheetBg')closeSheet()});
document.addEventListener('keydown',e=>{if(!document.getElementById('sheetBg').classList.contains('open'))return;if(e.key==='Escape'){closeSheet();return}if(e.key==='Tab'){const els=[...document.querySelectorAll('#sheet a[href],#sheet button:not([disabled]),#sheet input:not([hidden]),#sheet select,#sheet textarea')].filter(el=>el.offsetParent!==null);const first=els[0],last=els.at(-1);if(e.shiftKey&&document.activeElement===first){e.preventDefault();last?.focus()}else if(!e.shiftKey&&document.activeElement===last){e.preventDefault();first?.focus()}}});
function applyFs(){document.documentElement.style.setProperty('--fs',(S.settings.fs||18)+'px')}
async function aoRetomar(){await rollDate();if(await processRecurring()&&!sheetKind)render(true)}
window.addEventListener('focus',aoRetomar);
document.addEventListener('visibilitychange',()=>{if(!document.hidden)aoRetomar()});
setInterval(rollDate,30000);
load();applyFs();render();processRecurring().then(()=>{if(!sheetKind)render(true)});
