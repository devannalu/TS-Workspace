# Roadmap do produto

O roadmap organiza capacidades do produto e suas dependências. Não estabelece
datas nem substitui a [lista de módulos implementados](modules.md).

## 1. Acesso e organização

Consolidar identidade, sessão, perfis de acesso, convites, usuárias, equipes e
uma página inicial baseada em dados reais. A base organizacional já existe;
a consolidação em Java deve preservar suas regras e validar cada substituição.

## 2. Produtividade

Introduzir Tasks como unidade central do trabalho, conectadas a Projetos,
Talks/Reuniões, comentários, checklist, arquivos, calendário e notificações.
A organização de responsabilidades e acompanhamento orienta essa evolução.

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
