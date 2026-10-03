# Design System do TS Workspace

## Identidade

Rosa, creme e pastéis expressam a Tech Sisters com superfícies leves e texto
escuro. Lavanda, amarelo manteiga e pêssego são apoios. Bordô permanece apenas
no favicon original, preservado como detalhe de marca. Não há dark mode.
O monograma ts. existente continua presente; não foi criado um novo símbolo.

Os tokens ficam em frontend/src/app/globals.css e são mapeados pelo @theme
do Tailwind. Não colocar hex codes em componentes. A única exceção técnica é
themeColor em metadata, que acompanha o token de background por valor literal.

| Grupo | Valores principais |
| --- | --- |
| Background / Surface | #faf7f2 / #fffdf9 |
| Texto / Muted | #302a2d / #71636b |
| Primary / Hover / Active | #ad4269 / #963554 / #822c49 |
| Primary soft | #f9e3eb |
| Lavanda / Manteiga / Pêssego | #eee7f7 / #f7efcc / #f9e7db |
| Border / Strong | #e2d8d3 / #96857d |
| Success / Danger / Warning | #32654b / #aa3545 / #775815 |

Famílias incluem surface/elevated/muted/pink/cream; secondary e secondary-soft;
accent-lilac/butter/blush/peach; border/soft/strong; text/muted/soft;
success/warning/danger e suas superfícies soft; radius-sm/md/lg e shadow-sm/md.
Aliases card/muted/accent mantêm a compatibilidade das primitivas existentes.

## Tipografia e densidade

System sans, sem dependência de fonte externa. Display de login 48 px no desktop;
page-title 24/30 px; títulos de seção 16 px; body 14/16 px; labels 14 px e
captions 12 px. Conteúdo usa espaçamento de 16–32 px e superfícies compactas.
Botões e campos têm alvos mínimos de 44 px. Bordas de campos usam border-strong.

## Componentes

AppShell, Brand, PublicShell, Avatar, Button (primary/secondary/ghost/danger),
Input, PasswordInput, Badge, Card, Dialog/Drawer, Toast, Skeleton, EmptyState,
ErrorState e Pagination. Menus de contexto usam disclosure nativo e botões.
Use labels associados; não deixe o botão de senha integrar o label do campo.

Dialogs prendem foco, têm título/descrição, fecham com Esc quando disponíveis
e devolvem o foco ao acionador. Operações pendentes impedem fechamento e novas
mutações. Toasts duram 4,5 segundos e permitem dispensa; não são notificações
persistentes. O link do convite não é mantido após concluir ou fechar o dialog.

## Layout e acessibilidade

Sidebar a partir de 1024 px; abaixo disso, topbar e drawer. Dashboard reorganiza
cards conforme permissions. Usuárias usam tabela a partir de 1280 px e cards
abaixo desse tamanho; filtros quebram em linhas, mantendo labels visíveis.
Login e convite retiram a composição decorativa abaixo de 1024 px.

Manter contraste de texto normal >= 4,5:1 e controles essenciais >= 3:1,
focus visible, navegação por teclado, aria-current e reduced motion. Pastéis
servem de superfície, não de texto sobre creme. Não usar emojis estruturais.
Não guardar identidade, permissions ou sessão em storage do browser.

Métricas, listas e estados vêm do Java; placeholders de campos não são dados.
Módulos futuros não aparecem na navegação nem têm contagens inventadas.
