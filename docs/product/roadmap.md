# Roadmap do produto

O roadmap organiza capacidades do produto e suas dependências. Não estabelece
datas nem substitui a [lista de módulos implementados](modules.md).

## 1. Acesso e organização

Fundação Java, Auth, RBAC, Teams e Usuárias/Convites concluídos.
O frontend oficial utiliza Spring Boot com MySQL.

## 2. Produtividade

Tarefas já oferecem Kanban, responsáveis, prioridades, prazo, filtros e arquivamento,
com autorização no Java e resumo real no Dashboard. A próxima evolução poderá
conectar Projetos, Talks/Reuniões, comentários, checklist, arquivos, calendário e
notificações. Esses recursos ainda não estão implementados.

## 3. Operação da comunidade

Ampliar o Workspace para Comunicação/Conteúdo, Eventos e Parcerias,
com notas e chat como recursos de colaboração. A organização deve respeitar
as equipes e frentes reais da Tech Sisters.

## 4. Qualidade e operação em produção

Consolidar busca global, auditoria completa, envio de emails, integrações,
performance, testes e polimento da interface. Preparar configuração segura,
operação e recuperação de dados para o ambiente de produção.

## Critérios de evolução

Um módulo deve combinar interface, persistência, validação, autorização,
estados de uso e testes proporcionais ao risco. Código em andamento não deve
ser apresentado como funcionalidade validada.

Ao substituir um domínio existente, preservar seu funcionamento até a
validação da nova implementação e planejar a retirada da anterior.
Os resultados de cada execução pertencem ao [histórico](../history/README.md);
mudanças de comportamento atualizam a documentação viva.
