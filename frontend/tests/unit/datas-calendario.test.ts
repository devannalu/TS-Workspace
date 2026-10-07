import { it, expect, afterEach } from "vitest";
import { dataValida, somarDias, inicioSemana, intervaloVisivel, navegarPeriodo, formatarData } from "../../src/components/calendario/datas";
const timezoneOriginal = process.env.TZ;
afterEach(() => { process.env.TZ = timezoneOriginal; });
for (const zona of ["America/Bahia", "Europe/Lisbon", "Pacific/Kiritimati", "America/Los_Angeles"]) {
  it(`preserva date-only em ${zona}`, () => {
    process.env.TZ = zona;
    expect(somarDias("2026-10-07", 0)).toBe("2026-10-07");
    expect(formatarData("2026-10-07")).toBe("7 de outubro de 2026");
    expect(inicioSemana("2026-10-07")).toBe("2026-10-05");
  });
}
it("valida datas reais sem normalizar entradas inválidas", () => {
  expect(dataValida("2026-02-30")).toBe(false); expect(dataValida("2024-02-29")).toBe(true);
  expect(dataValida("07/10/2026")).toBe(false); expect(dataValida("0000-01-01")).toBe(false);
});
it("mês contém semanas completas de segunda a domingo", () => {
  expect(intervaloVisivel("2026-10-07", "month")).toEqual({de:"2026-09-28",ate:"2026-11-01"});
  expect(intervaloVisivel("2026-10-07", "week")).toEqual({de:"2026-10-05",ate:"2026-10-11"});
  expect(intervaloVisivel("2026-10-07", "agenda")).toEqual({de:"2026-10-01",ate:"2026-10-31"});
});
it("navega ano, mês curto e semana sem deslocamento", () => {
  expect(navegarPeriodo("2026-12-31","month",1)).toBe("2027-01-01");
  expect(navegarPeriodo("2026-03-31","agenda",-1)).toBe("2026-02-01");
  expect(navegarPeriodo("2026-10-07","week",1)).toBe("2026-10-14");
});
