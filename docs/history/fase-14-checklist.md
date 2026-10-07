# Fase 14 — Checklist

Checkpoint inicial: a0c9c84042362dc36c62e4898d725370d5bac00d.

## Entrega

V13 adiciona task_checklist. Arquivos de persistência, serviço e controller ficam no domínio tarefas; nenhuma página global, dependência ou permission nova. Itens têm texto simples, ordem, conclusão, autoria, versão e timestamps. Remoção lógica. Progresso calculado como concluídos/total. Limite operacional de 200 itens e texto de 500 caracteres, sem subtarefas, responsáveis ou prazos próprios.

GET/POST /api/v1/tasks/{id}/checklist; PATCH /{item}; PATCH /order; POST /{item}/remove. Escritas exigem CSRF e capacidade editar atual da tarefa; leitura herda view/escopo. Arquivo bloqueia edição. Item externo retorna 404. Versão obsoleta ou ordenação sem o conjunto atual retorna 409. Bloqueio de linha da tarefa serializa as escritas do checklist; não altera a versão dos campos da tarefa.

## Validação

Suítes test e package aprovadas: 254 testes em cada execução; JAR executável gerado, zero failures/errors/skips. Sete testes Java novos cobrem CRUD, autoria, progresso, remoção lógica, ordenação versionada, escopo, ownership, arquivo, limites, IDOR, validação HTTP e duas edições concorrentes com uma única vencedora.

Frontend: typecheck, lint, 135 testes e build aprovados. Seis testes novos verificam progresso, conclusão, submit, edição, confirmação, ordem, leitura, texto seguro, vazio e retry.

HTTP real aprovou criação, edição, conclusão, ordenação, conflito 409, remoção lógica, sessão 401 e validação 400. Backend iniciou com V13 e Hibernate validate, health 200. Navegador aprovou CRUD e ordem no detalhe existente; console sem erros; layout sem overflow horizontal em 1440/1280/1024/768/390px.

Acesso somente para leitura confirmado no navegador com SUPPORT sem autoria/responsabilidade: nenhum controle de mutação, caixas desabilitadas. Fixtures removidas com hashes e quantidades oficiais idênticos ao baseline.
