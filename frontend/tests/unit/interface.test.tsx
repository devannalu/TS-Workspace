// @vitest-environment jsdom
import { beforeEach, afterEach, describe, it, expect, vi } from "vitest";
import {
  render,
  screen,
  cleanup,
  fireEvent,
  waitFor,
} from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { FormularioLogin } from "../../src/components/autenticacao/formulario-login";
import { ProtecaoSessao } from "../../src/components/autenticacao/protecao-sessao";
import { SairButton } from "../../src/components/autenticacao/sair-button";
import { ShellAplicacao } from "../../src/components/layout/shell-aplicacao";
import { FormularioConvite } from "../../src/components/convites/formulario-convite";
import { FormularioAceiteConvite } from "../../src/components/convites/formulario-aceite-convite";
import { Pagination } from "../../src/components/ui/pagination";
import { ConvitePublico } from "../../src/components/convites/convite-publico";
import { GestaoEquipes } from "../../src/components/equipes/gestao-equipes";
import { GestaoUsuarios } from "../../src/components/usuarios/gestao-usuarios";
import { ErroApi } from "../../src/lib/api/http";
const mock = vi.hoisted(() => ({
  replace: vi.fn(),
  refresh: vi.fn(),
  push: vi.fn(),
  login: vi.fn(),
  logout: vi.fn(),
  me: vi.fn(),
  createInvite: vi.fn(),
  acceptInvite: vi.fn(),
  validateInvite: vi.fn(),
  teamDetail: vi.fn(),
  path: "/workspace",
}));
vi.mock("next/navigation", () => ({
  useRouter: () => mock,
  usePathname: () => mock.path,
  useSearchParams: () => new URLSearchParams(),
}));
vi.mock("../../src/lib/api/autenticacao", () => ({
  entrarNoWorkspace: mock.login,
  sairDoWorkspace: mock.logout,
  buscarUsuarioAtual: mock.me,
}));
vi.mock("../../src/lib/api/acoes", () => ({
  criarConviteComFeedback: mock.createInvite,
  aceitarConviteComFeedback: mock.acceptInvite,
  criarEquipeComFeedback: vi.fn(),
  arquivarEquipeComFeedback: vi.fn(),
  editarEquipeComFeedback: vi.fn(),
  adicionarIntegranteComFeedback: vi.fn(),
  removerIntegranteComFeedback: vi.fn(),
}));
vi.mock("../../src/lib/api/convites", () => ({
  consultarConvitePublico: mock.validateInvite,
}));
vi.mock("../../src/lib/api/equipes", () => ({
  buscarEquipe: mock.teamDetail,
}));
const admin = {
  id: "fixture",
  name: "Usuária Exemplo",
  email: "fixture@example.test",
  jobTitle: null,
  status: "ACTIVE" as const,
  role: { id: "role", key: "SUPER_ADMIN" as const, name: "Super Admin" },
  permissions: ["users.view", "teams.view", "users.create"],
};
beforeEach(() => {
  vi.clearAllMocks();
  mock.path = "/workspace";
  mock.me.mockResolvedValue(admin);
  mock.login.mockResolvedValue(admin);
  mock.logout.mockResolvedValue(undefined);
  HTMLDialogElement.prototype.showModal = function () {
    this.setAttribute("open", "");
  };
  HTMLDialogElement.prototype.close = function () {
    this.removeAttribute("open");
  };
});
afterEach(() => {
  cleanup();
  vi.unstubAllGlobals();
});
describe("autenticação e shell", () => {
  it("envia login e entra no Workspace", async () => {
    render(<FormularioLogin />);
    await userEvent.type(
      screen.getByLabelText("E-mail"),
      "fixture@example.test",
    );
    await userEvent.type(screen.getByLabelText("Senha"), "password-example");
    await userEvent.click(screen.getByRole("button", { name: "Entrar" }));
    await waitFor(() =>
      expect(mock.replace).toHaveBeenCalledWith("/workspace"),
    );
    expect(mock.login).toHaveBeenCalledWith(
      "fixture@example.test",
      "password-example",
    );
  });
  it("trata credenciais inválidas sem revelar detalhes", async () => {
    mock.login.mockRejectedValue(new ErroApi(401, "ignored"));
    render(<FormularioLogin />);
    fireEvent.submit(
      screen.getByRole("button", { name: "Entrar" }).closest("form")!,
    );
    expect(await screen.findByRole("alert")).toHaveProperty(
      "textContent",
      expect.stringContaining("Confira suas credenciais"),
    );
  });
  it("trata backend offline e permite nova tentativa", async () => {
    mock.login.mockRejectedValue(new TypeError("network-internal"));
    render(<FormularioLogin />);
    fireEvent.submit(
      screen.getByRole("button", { name: "Entrar" }).closest("form")!,
    );
    expect(await screen.findByRole("alert")).toHaveProperty(
      "textContent",
      "Não foi possível conectar. Tente novamente em instantes.",
    );
    expect(
      screen.getByRole("button", { name: "Entrar" }).hasAttribute("disabled"),
    ).toBe(false);
  });
  it("alternar senha preserva o valor", async () => {
    render(<FormularioLogin />);
    const input = screen.getByLabelText("Senha") as HTMLInputElement;
    await userEvent.type(input, "example-password");
    await userEvent.click(
      screen.getByRole("button", { name: "Mostrar senha" }),
    );
    expect(input.type).toBe("text");
    expect(input.value).toBe("example-password");
    await userEvent.click(
      screen.getByRole("button", { name: "Ocultar senha" }),
    );
    expect(input.type).toBe("password");
  });
  it("não mostra conteúdo protegido durante bootstrap", async () => {
    mock.me.mockReturnValue(new Promise(() => {}));
    render(
      <ProtecaoSessao>
        <p>Conteúdo protegido</p>
      </ProtecaoSessao>,
    );
    expect(screen.queryByText("Conteúdo protegido")).toBeNull();
    expect(screen.getByRole("status")).not.toBeNull();
  });
  it("redireciona sem sessão", async () => {
    mock.me.mockResolvedValue(null);
    render(
      <ProtecaoSessao>
        <p>Conteúdo protegido</p>
      </ProtecaoSessao>,
    );
    await waitFor(() => expect(mock.replace).toHaveBeenCalledWith("/login"));
    expect(screen.queryByText("Conteúdo protegido")).toBeNull();
  });
  it("mostra 403 controlado", async () => {
    mock.me.mockRejectedValue(new ErroApi(403, "SQL not shown"));
    render(
      <ProtecaoSessao>
        <p>Conteúdo protegido</p>
      </ProtecaoSessao>,
    );
    expect(await screen.findByRole("alert")).toHaveProperty(
      "textContent",
      expect.stringContaining("Você não tem permissão"),
    );
  });
  it("logout volta ao login", async () => {
    render(<SairButton />);
    await userEvent.click(screen.getByRole("button", { name: "Sair" }));
    await waitFor(() => expect(mock.replace).toHaveBeenCalledWith("/login"));
  });
  it("sidebar respeita SUPPORT e não cria links futuros", () => {
    render(
      <ShellAplicacao
        usuario={{
          ...admin,
          role: { ...admin.role, key: "SUPPORT", name: "Suporte" },
          permissions: ["teams.view"],
        }}
      >
        <p>Área</p>
      </ShellAplicacao>,
    );
    expect(screen.queryByRole("link", { name: "Usuárias" })).toBeNull();
    expect(
      screen.getAllByRole("link", { name: "Equipes" }).length,
    ).toBeGreaterThan(0);
    expect(screen.queryByRole("link", { name: "Tasks" })).toBeNull();
  });
  it("destaca rota ativa e abre navegação mobile", async () => {
    render(
      <ShellAplicacao usuario={admin}>
        <p>Área</p>
      </ShellAplicacao>,
    );
    expect(
      screen
        .getAllByRole("link", { name: "Início" })[0]
        .getAttribute("aria-current"),
    ).toBe("page");
    await userEvent.click(screen.getByRole("button", { name: "Abrir menu" }));
    expect(screen.getByRole("dialog")).not.toBeNull();
  });
});
describe("gestão real sem perder segurança", () => {
  it("carrega integrantes só ao abrir o diálogo e devolve o foco", async () => {
    mock.teamDetail.mockResolvedValue({
      members: [
        {
          id: "member",
          name: "Integrante exemplo",
          email: "member@example.test",
        },
      ],
    });
    render(
      <GestaoEquipes
        equipes={[
          {
            id: "root",
            name: "Equipe exemplo",
            description: null,
            parentId: null,
            memberCount: 1,
          },
        ]}
        usuarios={[]}
        podeCriar={false}
        podeEditar={false}
        podeArquivar={false}
        podeGerenciarIntegrantes
      />,
    );
    expect(mock.teamDetail).not.toHaveBeenCalled();
    const trigger = screen.getByRole("button", {
      name: "Gerenciar integrantes de Equipe exemplo",
    });
    await userEvent.click(trigger);
    expect(await screen.findByText("member@example.test")).not.toBeNull();
    expect(mock.teamDetail).toHaveBeenCalledExactlyOnceWith("root");
    await userEvent.click(screen.getByRole("button", { name: "Fechar" }));
    await waitFor(() => expect(document.activeElement).toBe(trigger));
  });
  it("Teams oculta mutações para acesso limitado", () => {
    render(
      <GestaoEquipes
        equipes={[
          {
            id: "root",
            name: "Equipe exemplo",
            description: null,
            parentId: null,
            memberCount: 0,
          },
        ]}
        usuarios={[]}
        podeCriar={false}
        podeEditar={false}
        podeArquivar={false}
        podeGerenciarIntegrantes={false}
      />,
    );
    expect(screen.queryByRole("button", { name: "Nova equipe" })).toBeNull();
    expect(screen.queryByRole("button", { name: /Integrantes/ })).toBeNull();
  });
  it("arquivamento pede confirmação com explicação", async () => {
    render(
      <GestaoEquipes
        equipes={[
          {
            id: "root",
            name: "Raiz",
            description: null,
            parentId: null,
            memberCount: 0,
          },
          {
            id: "child",
            name: "Filha",
            description: null,
            parentId: "root",
            memberCount: 0,
          },
        ]}
        usuarios={[]}
        podeCriar={false}
        podeEditar={false}
        podeArquivar
        podeGerenciarIntegrantes={false}
      />,
    );
    await userEvent.click(
      screen.getByRole("button", { name: "Arquivar Filha" }),
    );
    expect(
      screen.getByRole("dialog", { name: "Arquivar equipe" }).textContent,
    ).toContain("equipes filhas");
    expect(
      screen.getByRole("button", { name: "Confirmar arquivamento" }),
    ).not.toBeNull();
  });
  it("usuárias sem resultado têm filtros e empty state", async () => {
    render(
      <GestaoUsuarios
        paginaUsuarios={{ items: [], total: 0, page: 0, size: 20 }}
        perfisAcesso={[]}
        equipes={[]}
        podeEditar={false}
        podeInativar={false}
      />,
    );
    expect(screen.getByText("Nenhuma usuária encontrada")).not.toBeNull();
    await userEvent.type(screen.getByLabelText("Busca"), "Exemplo");
    await userEvent.click(
      screen.getByRole("button", { name: "Aplicar filtros" }),
    );
    expect(mock.push.mock.calls[0][0]).toContain("search=Exemplo");
  });
  it("link do convite é mostrado uma vez e descartado ao fechar", async () => {
    mock.createInvite.mockResolvedValue({
      ok: true,
      url: "http://localhost:3010/convite/exemplo",
    });
    render(
      <FormularioConvite
        perfisAcesso={[{ id: "support", name: "Suporte" }]}
        equipes={[{ id: "team", name: "Equipe exemplo" }]}
      />,
    );
    await userEvent.click(
      screen.getByRole("button", { name: "Convidar usuária" }),
    );
    await userEvent.type(
      screen.getByLabelText("E-mail"),
      "fixture@example.test",
    );
    await userEvent.selectOptions(
      screen.getByLabelText("Perfil de acesso"),
      "support",
    );
    await userEvent.click(
      screen.getByRole("checkbox", { name: "Equipe exemplo" }),
    );
    await userEvent.click(
      screen.getByRole("button", { name: "Criar convite" }),
    );
    expect(await screen.findByLabelText("Link de convite")).not.toBeNull();
    await userEvent.click(screen.getByRole("button", { name: "Concluir" }));
    await userEvent.click(
      screen.getByRole("button", { name: "Convidar usuária" }),
    );
    expect(screen.queryByLabelText("Link de convite")).toBeNull();
    expect(localStorage.length).toBe(0);
    expect(sessionStorage.length).toBe(0);
  });
  it("convite inválido, usado, expirado ou cancelado usa mensagem segura", async () => {
    mock.validateInvite.mockRejectedValue(new ErroApi(400, "database-detail"));
    render(<ConvitePublico token="fixture" />);
    expect(await screen.findByRole("alert")).toHaveProperty(
      "textContent",
      expect.stringContaining("expirado"),
    );
    expect(screen.queryByText("database-detail")).toBeNull();
  });
});

describe("limites e estados adicionais", () => {
  it("paginação navega sem carregar todas as usuárias", async () => {
    const page = vi.fn();
    render(<Pagination page={0} size={20} total={41} onPage={page} />);
    expect(
      screen.getByRole("button", { name: "Anterior" }).hasAttribute("disabled"),
    ).toBe(true);
    await userEvent.click(screen.getByRole("button", { name: "Próxima" }));
    expect(page).toHaveBeenCalledWith(1);
  });
  it("aceite associa labels sem incluir o botão de visibilidade", () => {
    render(<FormularioAceiteConvite token="fixture" />);
    expect(
      screen.getByLabelText("Crie sua senha", { exact: true }),
    ).not.toBeNull();
    expect(
      screen.getByLabelText("Confirme sua senha", { exact: true }),
    ).not.toBeNull();
  });
  it("confirmação divergente não envia o aceite", async () => {
    render(<FormularioAceiteConvite token="fixture" />);
    await userEvent.type(screen.getByLabelText("Seu nome"), "Nome de teste");
    await userEvent.type(
      screen.getByLabelText("Crie sua senha"),
      "password-example",
    );
    await userEvent.type(
      screen.getByLabelText("Confirme sua senha"),
      "different-example",
    );
    await userEvent.click(
      screen.getByRole("button", { name: "Criar meu acesso" }),
    );
    expect(screen.getByRole("alert").textContent).toContain(
      "senhas precisam ser iguais",
    );
    expect(mock.acceptInvite).not.toHaveBeenCalled();
  });
  it("aceite mostra sucesso e pede login explícito", async () => {
    mock.acceptInvite.mockResolvedValue({ ok: true });
    render(<FormularioAceiteConvite token="fixture" />);
    await userEvent.type(screen.getByLabelText("Seu nome"), "Nome de teste");
    await userEvent.type(
      screen.getByLabelText("Crie sua senha"),
      "password-example",
    );
    await userEvent.type(
      screen.getByLabelText("Confirme sua senha"),
      "password-example",
    );
    await userEvent.click(
      screen.getByRole("button", { name: "Criar meu acesso" }),
    );
    expect(
      await screen.findByRole("link", { name: "Ir para o login" }),
    ).not.toBeNull();
    expect(mock.login).not.toHaveBeenCalled();
  });
});
