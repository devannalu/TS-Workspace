# Roadmap do produto

O roadmap organiza capacidades do produto e suas dependências. Não estabelece
datas nem substitui a [lista de módulos implementados](modules.md).

## 1. Acesso e organização

Fundação Java, Auth, RBAC, Teams e Usuárias/Convites concluídos.
O frontend oficial utiliza Spring Boot com MySQL.

## 2. Produtividade

Tarefas já oferecem Kanban, responsáveis, prioridades, prazo, filtros e arquivamento,
com autorização no Java e resumo real no Dashboard. Projetos organizam objetivos
por equipe, com período, responsáveis e progresso derivado das tarefas, sem exigir
vínculo para tarefas independentes. Comentários, anexos e atividade contextual já atendem os dois recursos. Talks/Reuniões organizam encontros com horários, participantes e resultados, integrados ao calendário. Checklist já oferece itens contextuais ordenados e progresso derivado em Tarefas. Notificações pessoais já acompanham atribuições, comentários, mudanças e prazos de tarefas e projetos.

## 3. Operação da comunidade

Eventos da comunidade já oferecem operação, responsáveis, horários/fusos e calendário.
Ampliar o Workspace para Comunicação/Conteúdo e Parcerias,
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


## Anexos e armazenamento

Anexos contextuais utilizam storage S3 compatível, Garage local e R2 configurável em produção. Sem preview, avatar, integração externa ou anexos em comentários.

## Calendário

Calendário é uma projeção de Tarefas, Projetos, Reuniões/Talks e Eventos e não possui persistência própria.
A visão temporal disponível agrega prazos e períodos, respeita permissões e escopo das fontes
e inclui encontros com horário/fuso. Sincronização externa e ICS permanecem fora desta entrega.
