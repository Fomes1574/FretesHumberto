import http from 'node:http';
import {readFile} from 'node:fs/promises';
import {resolve,extname,sep} from 'node:path';
const args=process.argv.slice(2), option=(k,d)=>args.includes(k)?args[args.indexOf(k)+1]:d;
const root=resolve('docs'), port=Number(option('--port',4173)), host=option('--host','0.0.0.0');
http.createServer(async(req,res)=>{try{const url=new URL(req.url,'http://local');const name=decodeURIComponent(url.pathname==='/'?'/prototipo.html':url.pathname);const file=resolve(root,'.'+name);if(!file.startsWith(root+sep)){res.writeHead(403);res.end();return}const content=await readFile(file);res.setHeader('Content-Type',({'.html':'text/html; charset=utf-8','.png':'image/png','.woff2':'font/woff2','.json':'application/json','.md':'text/plain; charset=utf-8'})[extname(file)]||'application/octet-stream');res.setHeader('Cache-Control','no-store');res.end(content)}catch{res.writeHead(404);res.end('Não encontrado')}}).listen(port,host,()=>process.stdout.write(`Preview on ${host}:${port}\n`));
