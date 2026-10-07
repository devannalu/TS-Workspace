import { requisitarJava } from "./http";
import type { ReferenciaTarefa } from "./tarefas";
export const statusConteudo={IDEIA:"Ideia",PLANEJADO:"Planejado",EM_PRODUCAO:"Em produção",EM_REVISAO:"Em revisão",APROVADO:"Aprovado",PUBLICADO:"Publicado"} as const;
export const canaisConteudo={INSTAGRAM:"Instagram",LINKEDIN:"LinkedIn",EMAIL:"E-mail",SITE:"Site",INTERNO:"Interno"} as const;
export const formatosConteudo={POST:"Post",CARROSSEL:"Carrossel",VIDEO:"Vídeo",ARTIGO:"Artigo",EMAIL:"E-mail",TEXTO:"Texto"} as const;
export type Conteudo={id:string;titulo:string;briefing:string|null;canal:keyof typeof canaisConteudo;formato:keyof typeof formatosConteudo;status:keyof typeof statusConteudo;equipe:ReferenciaTarefa;responsavel:ReferenciaTarefa|null;publicacaoPlanejada:string|null;eventoId:string|null;projetoId:string|null;criadaPor:ReferenciaTarefa;versao:number;criadaEm:string;atualizadaEm:string;arquivada:boolean;capacidades:{editar:boolean;arquivar:boolean;verEvento:boolean;verProjeto:boolean}};
export type DadosConteudo=Pick<Conteudo,"titulo"|"canal"|"formato"|"status"|"publicacaoPlanejada"|"eventoId"|"projetoId"> & {briefing:string;equipeId:string;responsavelId:string|null};
export type OpcoesConteudos={equipes:ReferenciaTarefa[];responsaveis:ReferenciaTarefa[];eventos:ReferenciaTarefa[];projetos:ReferenciaTarefa[]};
export type PaginaConteudos={items:Conteudo[];total:number;page:number;size:number};
export function listarConteudos(busca="",equipe="",status="",arquivadas=false,pagina=0){const p=new URLSearchParams({search:busca,archived:String(arquivadas),page:String(pagina),size:"25"});if(equipe)p.set("teamId",equipe);if(status)p.set("status",status);return requisitarJava<PaginaConteudos>(`/content?${p}`);}
export const buscarConteudo=(id:string)=>requisitarJava<Conteudo>(`/content/${id}`);
export const opcoesConteudos=(equipe="")=>requisitarJava<OpcoesConteudos>(`/content/options${equipe?`?teamId=${equipe}`:""}`);
export const criarConteudo=(dados:DadosConteudo)=>requisitarJava<Conteudo>("/content","POST",dados);
export const editarConteudo=(conteudo:Conteudo,dados:DadosConteudo)=>requisitarJava<Conteudo>(`/content/${conteudo.id}`,"PATCH",{...dados,versao:conteudo.versao});
export const arquivarConteudo=(conteudo:Conteudo)=>requisitarJava<Conteudo>(`/content/${conteudo.id}/archive`,"POST",{versao:conteudo.versao});
