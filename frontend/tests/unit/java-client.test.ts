import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";
import { ApiError, clearCsrf, javaRequest } from "../../src/lib/api/http";
import { loginJava, logoutJava, getCurrentJavaUser } from "../../src/lib/api/auth";

const response=(status:number,body:unknown={})=>new Response(status===204?null:JSON.stringify(body),{status,headers:{"Content-Type":"application/json"}});
const fetchMock=vi.fn();
beforeEach(()=>{clearCsrf();fetchMock.mockReset();vi.stubGlobal("fetch",fetchMock);});
afterEach(()=>vi.unstubAllGlobals());

describe("cliente oficial Java",()=>{
  it("compartilha CSRF entre escritas e envia cookies sem guardar autenticação",async()=>{
    fetchMock.mockResolvedValueOnce(response(200,{token:"csrf-test"})).mockResolvedValueOnce(response(201,{id:"first"})).mockResolvedValueOnce(response(200,{id:"second"}));
    await javaRequest("/teams","POST",{name:"Team"});await javaRequest("/users/test","PATCH",{jobTitle:"Developer"});
    expect(fetchMock).toHaveBeenCalledTimes(3);
    expect(fetchMock.mock.calls[1][1]).toMatchObject({credentials:"include",cache:"no-store",headers:{"X-XSRF-TOKEN":"csrf-test","Content-Type":"application/json"}});
  });
  it("deduplica preparação CSRF concorrente",async()=>{
    fetchMock.mockResolvedValueOnce(response(200,{token:"csrf-test"})).mockImplementation(()=>Promise.resolve(response(204)));
    await Promise.all([javaRequest("/one","POST"),javaRequest("/two","POST")]);
    expect(fetchMock.mock.calls.filter(c=>String(c[0]).endsWith("/auth/csrf"))).toHaveLength(1);
  });
  it("login invalida o CSRF anterior para respeitar rotação do Spring",async()=>{
    fetchMock.mockResolvedValueOnce(response(200,{token:"before-login"})).mockResolvedValueOnce(response(200,{id:"user"})).mockResolvedValueOnce(response(200,{token:"after-login"})).mockResolvedValueOnce(response(204));
    await loginJava("user@example.test","test-password");await logoutJava();
    expect(fetchMock.mock.calls[3][1].headers["X-XSRF-TOKEN"]).toBe("after-login");
  });
  it("logout aceita resposta sem corpo e invalida CSRF",async()=>{
    fetchMock.mockResolvedValueOnce(response(200,{token:"csrf-test"})).mockResolvedValueOnce(response(204)).mockResolvedValueOnce(response(200,{token:"fresh"})).mockResolvedValueOnce(response(204));
    await logoutJava();await javaRequest("/operation","POST");expect(fetchMock).toHaveBeenCalledTimes(4);
  });
  it("401 em me vira sessão ausente",async()=>{
    fetchMock.mockResolvedValueOnce(response(401));expect(await getCurrentJavaUser()).toBeNull();
  });
  it.each([400,403,409,500])("mantém status %s sem expor SQL/stack do Problem Detail",async status=>{
    fetchMock.mockResolvedValueOnce(response(status,{detail:"Hibernate SQL secret",stack:"private"}));
    const error=await javaRequest("/users").catch(e=>e);expect(error).toBeInstanceOf(ApiError);if(!(error instanceof ApiError))throw error;expect(error.status).toBe(status);expect(error.message).not.toMatch(/Hibernate|SQL|secret|private/);
  });
  it("403 não reenvia mutação, e próxima ação explícita renova CSRF",async()=>{
    fetchMock.mockResolvedValueOnce(response(200,{token:"old"})).mockResolvedValueOnce(response(403)).mockResolvedValueOnce(response(200,{token:"new"})).mockResolvedValueOnce(response(204));
    await expect(javaRequest("/operation","POST")).rejects.toMatchObject({status:403});expect(fetchMock).toHaveBeenCalledTimes(2);
    await javaRequest("/operation","POST");expect(fetchMock.mock.calls[3][1].headers["X-XSRF-TOKEN"]).toBe("new");
  });
});
