"""Generate one Markdown handoff from the maintained documentation sources."""
from pathlib import Path
import os
import re

ROOT = Path(__file__).resolve().parents[1]
OUT = ROOT / 'docs/CONTEXTO_COMPLETO.md'
SOURCES = [
    'README.md', 'CLAUDE.md', 'AGENTS.md', 'docs/REGRAS_DE_NEGOCIO.md',
    'docs/DECISOES.md', 'docs/ESTADO_ATUAL.md', 'docs/ARQUITETURA.md',
    'docs/PLANO_ANDROID.md', 'docs/TESTES.md', 'docs/CHAVE_ASSINATURA.md',
    'docs/MELHORIAS_PESQUISADAS.md', 'CHANGELOG.md', 'design-qa.md',
]

def rebase_link(target, source):
    if not target or target.startswith(('#', '/', 'http:', 'https:', 'mailto:', 'data:')):
        return target
    path, sep, fragment = target.partition('#')
    resolved = (source.parent / path).resolve()
    return Path(os.path.relpath(resolved, OUT.parent)).as_posix() + (sep + fragment if sep else '')

def convert(text, source):
    # Rebase local Markdown links and HTML image paths to the generated doc folder.
    text = re.sub(r'(!?\[[^\]\n]*\]\()([^\s)]+)(\))',
                  lambda m: m[1] + rebase_link(m[2], source) + m[3], text)
    text = re.sub(r'(src=")([^"]+)(")',
                  lambda m: m[1] + rebase_link(m[2], source) + m[3], text)
    inside = False
    lines = []
    for line in text.splitlines():
        if line.startswith('```'):
            inside = not inside
        if not inside and re.match(r'^#{1,5} ', line):
            line = '#' + line
        lines.append(line)
    return '\n'.join(lines)

parts = [
    '# Meus Fretes — contexto completo para programação\n',
    '> Consolidação de 30/10/2026. Protótipo web pronto; Android em construção (`:core` com regras e testes, `:app` em esqueleto).\n'
    '> Arquivo gerado por `scripts/build_context.py`. Edite os documentos-fonte e regenere; '
    'não mantenha uma segunda especificação manual aqui.\n',
    'Esta cópia reúne objetivo, instruções, regras, decisões, estado, arquitetura, plano, '
    'testes e histórico. Instruções atuais do usuário prevalecem. Os links apontam para '
    'as fontes mantidas no repositório.\n',
]
for name in SOURCES:
    source = ROOT / name
    target = Path(os.path.relpath(source, OUT.parent)).as_posix()
    parts.append(f'\n---\n\nDocumento-fonte: [{name}]({target})\n\n')
    parts.append(convert(source.read_text(encoding='utf-8'), source))
OUT.write_text('\n'.join(parts).rstrip() + '\n', encoding='utf-8')
print(f'Contexto gerado: {OUT.relative_to(ROOT)} ({OUT.stat().st_size} bytes)')
