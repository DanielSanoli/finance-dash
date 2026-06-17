const { test, expect } = require("@playwright/test");

async function clearSession(page) {
    await page.goto("/app.html");
    await page.evaluate(() => {
        window.localStorage.removeItem("financeDashToken");
        window.localStorage.removeItem("financeDashRefreshToken");
    });
    await page.reload();
}

async function waitForLoggedInApp(page) {
    await expect(page.locator("#app-content")).not.toHaveClass(/hidden/, { timeout: 15_000 });
}

async function loginAsDemo(page) {
    await clearSession(page);
    await expect(page.getByRole("heading", { name: "Entre ou crie sua conta" })).toBeVisible();
    await page.locator("#login-form input[name='email']").fill("demo@financedash.com");
    await page.locator("#login-form input[name='password']").fill("demo12345");
    await page.getByRole("button", { name: "Entrar" }).click();
    await waitForLoggedInApp(page);
}

test.describe("FinanceDash frontend", () => {
    test("landing exibe cadastro e link para o app", async ({ page }) => {
        await clearSession(page);
        await page.goto("/");

        await expect(page.getByRole("heading", { name: /Saiba .* se você vai fechar/ })).toBeVisible();
        await expect(page.getByRole("heading", { name: "Crie sua conta grátis" })).toBeVisible();
        await expect(page.getByRole("link", { name: "Entrar" }).first()).toHaveAttribute("href", "/app.html");
    });

    test("app exige login antes de exibir o dashboard", async ({ page }) => {
        await clearSession(page);

        await expect(page.getByRole("heading", { name: "Entre ou crie sua conta" })).toBeVisible();
        await expect(page.locator("#app-content")).toHaveClass(/hidden/);
    });

    test("cadastro pela landing redireciona para o app logado", async ({ page }) => {
        await clearSession(page);
        const email = `e2e-${Date.now()}@example.com`;

        await page.goto("/");
        await page.locator("#su-name").fill("Usuário E2E");
        await page.locator("#su-email").fill(email);
        await page.locator("#su-pass").fill("senha12345");
        await page.getByRole("button", { name: "Criar conta grátis →" }).click();

        await expect(page).toHaveURL(/\/app\.html$/, { timeout: 15_000 });
        await waitForLoggedInApp(page);
    });

    test("landing redireciona para o app quando já existe sessão", async ({ page }) => {
        await loginAsDemo(page);
        await page.goto("/");
        await expect(page).toHaveURL(/\/app\.html$/, { timeout: 10_000 });
        await waitForLoggedInApp(page);
    });

    test("carrega dashboard após login demo", async ({ page }) => {
        await loginAsDemo(page);

        await expect(page.getByRole("heading", { name: "Novo lançamento" })).toBeVisible();
        await expect(page.getByRole("heading", { name: "Categorias" })).toBeVisible();
        await expect(page.getByRole("heading", { name: "Nova meta" })).toBeVisible();
        await expect(page.getByLabel("Filtros de lançamentos")).toBeVisible();
    });

    test("permite alternar filtros de transações sem recarregar a página", async ({ page }) => {
        await loginAsDemo(page);

        await page.locator("#transaction-filter-type").selectOption("EXPENSE");
        await page.getByRole("button", { name: "Aplicar filtros" }).click();

        await expect(page.getByText(/registros/)).toBeVisible();
    });
});
