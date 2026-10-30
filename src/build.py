from pathlib import Path
import re
root=Path(__file__).resolve().parents[1]
old=(root/'src/template-original.html').read_text()
script=re.search(r'<script>([\s\S]*?)</script>',old).group(1)
icons=script[:script.index('/* ---------- utilidades')]
road=script[script.index('/* ---------- cena da estrada'):script.index('/* ---------- estado de tela')]
seed=script[script.index('function seed()'):script.index('function load()')]
head=old[:old.index('<script>')]
head=re.sub(r'<link[^>]+(?:fonts.googleapis|fonts.gstatic)[^>]*>\s*','',head)
font=(root/'src/fonts.css').read_text() if (root/'src/fonts.css').exists() else ''
css='''
.entry .info{background:none;border:0;text-align:left;cursor:pointer;padding:0;min-height:48px}
.entry .v{font-weight:700;white-space:nowrap}
.wide{width:100%;margin-top:10px}
.danger{color:#A9221B!important;border-color:#A9221B!important}
.confirm .danger{background:#A9221B;color:#fff}
.field input,.field select,.two>*{min-width:0}
.big{overflow-wrap:anywhere}
.day{gap:12px}.day .v{font-weight:700;flex-shrink:0}
.mhead{gap:12px;flex-wrap:wrap;text-transform:capitalize}
.chartScroll{overflow-x:auto;max-width:100%;padding-bottom:8px;overscroll-behavior-x:contain}
.chartScroll svg{height:190px;display:block}
summary{gap:8px;line-height:1.35;padding:8px 0}
details.card>summary{color:#191D1B;font-size:1.05rem;margin-bottom:6px}
.kpi .n{overflow-wrap:anywhere}.kpi .l{font-size:.85rem}
.period .p{overflow-wrap:anywhere}
.sheetBg{z-index:30}.sheet{overscroll-behavior:contain;scrollbar-gutter:stable;max-height:92dvh}
.toast{z-index:40;max-width:calc(100vw - 24px);width:max-content;box-shadow:0 2px 10px #0002}
input:read-only{background:#F6F4EE}
button:disabled{opacity:.45;cursor:default}
button:focus-visible,summary:focus-visible{outline:3px solid #17603F;outline-offset:3px}
[data-large=true] .two,[data-large=true] .kpis{grid-template-columns:1fr}
[data-large=true] .day{align-items:flex-start;flex-direction:column}
[data-large=true] .entry{flex-wrap:wrap}
[data-large=true] .entry .info{flex-basis:100%}
[data-large=true] .seg{flex-wrap:wrap;gap:4px}
[data-large=true] .seg button{flex-basis:40%}
@media(max-width:380px){.big{font-size:2.1rem}.actions{gap:8px}.wrap{padding-left:12px;padding-right:12px}.card{padding:14px}.btn{padding:10px}.kpi .n{font-size:1.15rem}}
@media(prefers-reduced-motion:reduce){*{scroll-behavior:auto!important}.btn:active{transform:none}}
'''
head=head.replace('</style>',font+'\n'+css+'\n</style>')
app=(root/'src/app.js').read_text().replace("document.documentElement.style.setProperty('--fs',(S.settings.fs||18)+'px')","document.documentElement.style.setProperty('--fs',(S.settings.fs||18)+'px');document.documentElement.dataset.large=String(S.settings.fs===23)")
js='\n'.join([(root/'src/backup-crypto.bundle.js').read_text(),icons,(root/'src/core.js').read_text(),road,seed,app])
(root/'docs/prototipo.html').write_text(head+'<script>\n'+js+'\n</script>\n</body>\n</html>\n')
(root/'src/prototipo-check.js').write_text(js)
print('HTML atualizado, mantendo estilos, ícones e estrada originais.')
