"use strict";

/* ============================================================
 * API
 * ============================================================ */

class ApiError extends Error {
    constructor(body) {
        super(body.message_error);
        this.type = body.type_error;
        this.status = body.status;
        this.errorId = body.error_id;
    }
}

async function request(method, path, body) {
    const options = { method, headers: {} };
    if (body instanceof FormData) {
        options.body = body;
    } else if (body !== undefined) {
        options.headers["Content-Type"] = "application/json";
        options.body = JSON.stringify(body);
    }

    let response;
    try {
        response = await fetch(path, options);
    } catch {
        throw new ApiError({
            type_error: "NETWORK",
            message_error: "O Vagora não está respondendo. Confira se ele está rodando com ./run.sh.",
            status: 0,
        });
    }

    if (response.status === 204) return null;

    const data = await response.json().catch(() => null);
    if (!response.ok) {
        throw new ApiError(data ?? {
            type_error: "HTTP_ERROR",
            message_error: `O servidor respondeu ${response.status}.`,
            status: response.status,
        });
    }
    return data;
}

// um perfil por instalação: nenhuma rota leva id
const api = {
    getProfile: () => request("GET", "/profile"),
    createProfile: (form) => request("POST", "/profile", form),
    updateProfile: (form) => request("PATCH", "/profile", form),
    deleteProfile: () => request("DELETE", "/profile"),
    linkAccount: (account) => request("POST", "/profile/accounts", account),
    unlinkAccount: (provider) => request("DELETE", `/profile/accounts/${provider}`),
    openLinkedinLogin: () => request("POST", "/profile/accounts/LINKEDIN/session"),
    listFolders: () => request("GET", "/profile/email/folders"),
    updateFolder: (folder, source) => request("PUT", "/profile/email/folder", { folder, source }),
    listMatches: () => request("GET", "/profile/job-matches"),
    status: () => request("GET", "/profile/status"),
    scraper: () => request("GET", "/profile/scraper"),
};

/* ============================================================
 * Estado e utilitários
 * ============================================================ */

const MATCH_CUT = 70;

const state = {
    profile: null,
    matches: [],
    filter: "all",
    sort: "score",
    query: "",
    status: null,
    timers: [],
};

const $ = (selector, root = document) => root.querySelector(selector);
const $$ = (selector, root = document) => [...root.querySelectorAll(selector)];

function el(tag, className, text) {
    const node = document.createElement(tag);
    if (className) node.className = className;
    if (text !== undefined) node.textContent = text;
    return node;
}

function icon(name) {
    const svg = document.createElementNS("http://www.w3.org/2000/svg", "svg");
    svg.setAttribute("class", "icon");
    svg.setAttribute("aria-hidden", "true");
    const use = document.createElementNS("http://www.w3.org/2000/svg", "use");
    use.setAttribute("href", `#i-${name}`);
    svg.append(use);
    return svg;
}

// texto com partes em negrito: ["Conectado a ", { strong: "x" }, "."]
function rich(parts) {
    return parts.map((part) => (typeof part === "string" ? part : el("strong", "", part.strong)));
}

let uid = 0;
function cloneTemplate(id) {
    const node = $(`#${id}`).content.firstElementChild.cloneNode(true);
    // um template pode ser clonado várias vezes: os ids precisam ser únicos
    $$(".field", node).forEach((field) => {
        const input = $("input, select", field);
        const label = $("label", field);
        if (input && label && !input.id) {
            input.id = `f-${++uid}`;
            label.htmlFor = input.id;
        }
    });
    $$(".tip", node).forEach(wireTip);
    $$("[aria-labelledby]", node).forEach((region) => {
        const title = $("h2, h3", region);
        if (title) {
            title.id = `t-${++uid}`;
            region.setAttribute("aria-labelledby", title.id);
        }
    });
    return node;
}

function account(provider) {
    return state.profile?.linkedAccounts.find((a) => a.externalProvider === provider) ?? null;
}

const SOURCE_LABELS = { LINKEDIN: "LinkedIn", INDEED: "Indeed" };

function folderOf(email, source) {
    return source === "INDEED" ? email.indeedFolder : email.folder;
}

function foldersText(email) {
    return [
        "Alertas do LinkedIn na pasta ", { strong: email.folder },
        ...(email.indeedFolder ? [", do Indeed na pasta ", { strong: email.indeedFolder }] : [". O Indeed não está sendo lido (nenhuma pasta escolhida)"]),
    ];
}

let toastTimer;
function toast(message) {
    const box = $("#toast");
    box.textContent = message;
    box.hidden = false;
    clearTimeout(toastTimer);
    toastTimer = setTimeout(() => { box.hidden = true; }, 3200);
}

function setBusy(form, busy) {
    $$("button, input, select", form).forEach((control) => { control.disabled = busy; });
}

const dateFormat = new Intl.DateTimeFormat("pt-BR", { dateStyle: "short", timeStyle: "short" });
const timeFormat = new Intl.DateTimeFormat("pt-BR", { timeStyle: "short" });

function timeAgo(isoDate) {
    const seconds = Math.max(0, Math.round((Date.now() - new Date(isoDate).getTime()) / 1000));
    if (seconds < 60) return "agora há pouco";
    const minutes = Math.round(seconds / 60);
    if (minutes < 60) return `há ${minutes} min`;
    return dateFormat.format(new Date(isoDate));
}

function formatSize(bytes) {
    if (!bytes) return "";
    return bytes < 1024 * 1024
        ? `${Math.max(1, Math.round(bytes / 1024))} KB`
        : `${(bytes / 1024 / 1024).toFixed(1).replace(".", ",")} MB`;
}

function normalize(text) {
    return (text ?? "").normalize("NFD").replace(/[̀-ͯ]/g, "").toLowerCase();
}

/* ============================================================
 * Tooltip: hover e foco no CSS; aqui ficam o toque, o Esc e os ids
 * ============================================================ */

function wireTip(tip) {
    const trigger = $(".tip-trigger", tip);
    const bubble = $(".tip-bubble", tip);
    if (!bubble.id) bubble.id = `tip-${++uid}`;
    trigger.setAttribute("aria-describedby", bubble.id);
    trigger.setAttribute("aria-expanded", "false");

    // perto das bordas da tela, a bolha deixa de ser centralizada para não vazar
    const alignBubble = () => {
        const { left, right } = trigger.getBoundingClientRect();
        if (left < 180) tip.dataset.align = "start";
        else if (window.innerWidth - right < 180) tip.dataset.align = "end";
        else delete tip.dataset.align;
    };
    tip.addEventListener("mouseenter", alignBubble);
    trigger.addEventListener("focus", alignBubble);

    trigger.addEventListener("click", (event) => {
        event.stopPropagation();
        const open = !tip.hasAttribute("data-open");
        $$(".tip[data-open]").forEach(closeTip);
        if (open) {
            alignBubble();
            tip.setAttribute("data-open", "");
            trigger.setAttribute("aria-expanded", "true");
        }
    });
}

function closeTip(tip) {
    tip.removeAttribute("data-open");
    $(".tip-trigger", tip)?.setAttribute("aria-expanded", "false");
}

document.addEventListener("click", () => $$(".tip[data-open]").forEach(closeTip));
document.addEventListener("keydown", (event) => {
    if (event.key !== "Escape") return;
    $$(".tip[data-open]").forEach(closeTip);
    if (document.activeElement?.classList.contains("tip-trigger")) document.activeElement.blur();
});

/* ============================================================
 * Erros: a decisão é pelo type_error, a mensagem é só para exibir
 * ============================================================ */

function showError(error, target) {
    if (!(error instanceof ApiError)) {
        console.error(error);
        error = new ApiError({ type_error: "INTERNAL_ERROR", message_error: "Algo deu errado nesta página." });
    }

    // o perfil sumiu (ex.: banco apagado): volta para o início
    if (error.type === "NOT_FOUND" && state.profile) {
        toast("Esse perfil não existe mais.");
        boot();
        return;
    }

    const box = target ?? $("#setup-warning");
    box.replaceChildren(messageFor(error));

    if (error.type === "BUSINESS_RULE" && !box.closest("[data-panel='contas']")) {
        const link = el("a", "", "Abrir contas");
        link.href = "#contas";
        box.append(" ", link);
    }
    if (error.errorId) {
        box.append(el("span", "error-id", `Código do erro: ${error.errorId}`));
    }
    box.hidden = false;
}

function messageFor(error) {
    switch (error.type) {
        case "EXTERNAL_SERVICE_ERROR":
            return `${error.message} Tente de novo em instantes.`;
        case "INTERNAL_ERROR":
            return error.errorId ? "Erro inesperado no servidor." : error.message;
        default:
            return error.message;
    }
}

function clearError(form) {
    const box = $(".form-error", form);
    if (box) box.hidden = true;
}

/* ============================================================
 * Inicialização e navegação
 * ============================================================ */

function stopTimers() {
    state.timers.forEach(clearInterval);
    state.timers = [];
}

async function boot() {
    stopTimers();
    try {
        state.profile = await api.getProfile();
    } catch (error) {
        if (error.type === "NOT_FOUND") {
            state.profile = null;
            showSetup();
            return;
        }
        $("#app").hidden = true;
        $("#setup").hidden = false;
        showError(error, $("#setup-profile-form .form-error"));
        return;
    }

    showApp();
}

function showApp() {
    $("#setup").hidden = true;
    $("#app").hidden = false;
    loadStatus();
    state.timers.push(setInterval(loadStatus, 15_000));
    route();
}

let pageTimer;
function route() {
    if ($("#app").hidden) return;
    const tab = ["vagas", "contas", "linkedin", "indeed", "perfil"].includes(location.hash.slice(1)) ? location.hash.slice(1) : "vagas";

    $$("[data-tab]").forEach((link) => {
        if (link.dataset.tab === tab) link.setAttribute("aria-current", "page");
        else link.removeAttribute("aria-current");
    });
    $$("[data-panel]").forEach((panel) => { panel.hidden = !panel.dataset.panel.split(" ").includes(tab); });

    clearInterval(pageTimer);
    if (tab === "vagas") {
        loadMatches(true);
        // o servidor verifica o email a cada minuto; a lista acompanha
        pageTimer = setInterval(() => loadMatches(false), 60_000);
    }
    if (tab === "contas") renderAccounts();
    if (tab === "linkedin" || tab === "indeed") {
        state.readerSource = tab.toUpperCase();
        $("#reader-title").textContent = `Leitor do ${SOURCE_LABELS[state.readerSource]}`;
        $("#reader-source").textContent = SOURCE_LABELS[state.readerSource];
        loadScraper();
        // enquanto a página está aberta, acompanha o leitor quase em tempo real
        pageTimer = setInterval(loadScraper, 5_000);
    }
    if (tab === "perfil") renderProfile();
    window.scrollTo({ top: 0 });
}

window.addEventListener("hashchange", route);

/* ============================================================
 * Monitor: o que o Vagora está fazendo agora
 * ============================================================ */

async function loadStatus() {
    if (!state.profile) return;
    try {
        state.statuses = await api.status();
        state.status = mergeStatus(state.statuses);
    } catch {
        state.statuses = null;
        state.status = null;
    }
    renderMonitor();
    renderProcessingAlert();
}

function mergeStatus(statuses) {
    const entries = Object.entries(statuses).map(([source, s]) => ({ ...s, source, label: SOURCE_LABELS[source] }));
    const busy = entries.find((s) => s.currentAlert);
    const failed = entries.find((s) => s.lastError);
    const now = new Date();
    const waits = entries.map((s) => s.nextAlertAt).filter((at) => at && new Date(at) > now).sort();
    const checks = entries.map((s) => s.lastCheckAt).filter(Boolean).sort();
    return {
        currentAlert: busy ? `${busy.label}: ${busy.currentAlert}` : null,
        lastError: failed ? `${failed.label}: ${failed.lastError}` : null,
        lastErrorCode: failed?.lastErrorCode ?? null,
        lastErrorSource: failed?.source ?? null,
        nextAlertAt: waits[0] ?? null,
        queuedAlerts: entries.reduce((total, s) => total + s.queuedAlerts, 0),
        lastCheckAt: checks.at(-1) ?? null,
    };
}

function missingSetup() {
    const missing = [];
    if (!account("EMAIL")) missing.push("o email");
    if (!account("LINKEDIN")) missing.push("o LinkedIn");
    if (!state.profile.hasGeminiApiKey) missing.push("a chave do Gemini");
    if (!state.profile.curriculumName) missing.push("o currículo");
    return missing;
}

function renderMonitor() {
    const monitor = $("#monitor");
    const status = state.status;
    let view;

    if (!state.profile.monitoringEnabled) {
        view = { state: "paused", title: "Monitoramento pausado", detail: "Nenhum email é lido até você retomar." };
    } else if (missingSetup().length > 0) {
        view = { state: "idle", title: "Configuração incompleta", detail: `Falta ${joinList(missingSetup())}.` };
    } else if (status?.currentAlert) {
        view = { state: "busy", title: "Analisando um alerta", detail: status.currentAlert };
    } else if (status?.lastError) {
        view = { state: "error", title: "Última análise falhou", detail: status.lastError };
    } else if (status?.nextAlertAt && new Date(status.nextAlertAt) > new Date()) {
        // ritmo de segurança: 15 a 25 minutos entre um acesso ao LinkedIn e outro
        const at = timeFormat.format(new Date(status.nextAlertAt));
        const queue = status.queuedAlerts > 0
            ? ` · ${status.queuedAlerts} ${status.queuedAlerts === 1 ? "alerta" : "alertas"} na fila`
            : "";
        view = { state: "ok", title: "Intervalo de segurança", detail: `Próximo alerta às ${at}${queue}.` };
    } else if (status?.lastCheckAt) {
        view = { state: "ok", title: "Monitorando o email", detail: `Última verificação ${timeAgo(status.lastCheckAt)}.` };
    } else {
        view = { state: "idle", title: "Iniciando", detail: "A primeira verificação acontece em instantes." };
    }

    monitor.dataset.state = view.state;
    $("#monitor-title").textContent = view.title;
    $("#monitor-detail").textContent = view.detail;

    const toggle = $("#monitor-toggle");
    toggle.hidden = false;
    toggle.textContent = state.profile.monitoringEnabled ? "Pausar" : "Retomar";
    toggle.setAttribute("aria-label", state.profile.monitoringEnabled ? "Pausar o monitoramento do email" : "Retomar o monitoramento do email");
}

async function setMonitoring(enabled) {
    const data = new FormData();
    data.append("monitoringEnabled", String(enabled));
    $("#monitor-toggle").disabled = true;
    $("#monitoring").disabled = true;
    try {
        state.profile = await api.updateProfile(data);
        toast(enabled
            ? "Monitoramento retomado. O próximo alerta respeita o intervalo de segurança."
            : "Monitoramento pausado. Os alertas ficam guardados no email.");
    } catch (error) {
        toast(messageFor(error));
    } finally {
        $("#monitor-toggle").disabled = false;
        $("#monitoring").disabled = false;
        $("#monitoring").checked = Boolean(state.profile.monitoringEnabled);
        renderMonitor();
    }
}

$("#monitor-toggle").addEventListener("click", () => setMonitoring(!state.profile.monitoringEnabled));
$("#monitoring").addEventListener("change", (event) => setMonitoring(event.target.checked));

// o atalho de cada falha vem do código do erro (ErrorCode no backend), nunca do texto
const ERROR_ACTIONS = {
    LINKEDIN_CHECKPOINT: ["Entrar no LinkedIn", "#contas"],
    LINKEDIN_LOGIN_FAILED: ["Abrir contas", "#contas"],
    LINKEDIN_NOT_CONNECTED: ["Abrir contas", "#contas"],
    LINKEDIN_PAGE_UNREADABLE: ["Ver o leitor", "#leitor"],
    SCRAPER_FAILED: ["Ver o leitor", "#leitor"],
    GEMINI_KEY_MISSING: ["Abrir perfil", "#perfil"],
    GEMINI_KEY_REJECTED: ["Abrir perfil", "#perfil"],
};

function renderProcessingAlert() {
    const box = $("#processing-alert");
    const error = state.status?.lastError;
    if (!error) {
        box.hidden = true;
        return;
    }
    box.replaceChildren(el("strong", "", "A última análise falhou. "), error, " ");
    const action = ERROR_ACTIONS[state.status.lastErrorCode];
    if (action) {
        const link = el("a", "", action[0]);
        link.href = action[1] === "#leitor" ? `#${state.status.lastErrorSource.toLowerCase()}` : action[1];
        box.append(link);
    } else {
        box.append("O alerta continua no email e será tentado de novo.");
    }
    box.hidden = false;
}

/* ============================================================
 * Configuração inicial
 * ============================================================ */

function showSetup() {
    $("#app").hidden = true;
    $("#setup").hidden = false;
    setStep("profile", "current");
}

function setStep(name, stepState) {
    $(`.step[data-step="${name}"]`).dataset.state = stepState;
}

function unlockStep(name, form) {
    const step = $(`.step[data-step="${name}"]`);
    $(".step-locked", step).hidden = true;
    const body = $(".step-body", step);
    body.classList.add("panel");
    body.replaceChildren(form);
    body.hidden = false;
    setStep(name, "current");
    $("input", form)?.focus();
}

function completeStep(name, text) {
    const step = $(`.step[data-step="${name}"]`);
    setStep(name, "done");
    const body = $(".step-body", step) ?? $("form", step);
    body.replaceWith(el("p", "step-done", text));
}

$("#setup-profile-form").addEventListener("submit", async (event) => {
    event.preventDefault();
    const form = event.currentTarget;
    if (!form.reportValidity()) return;
    clearError(form);
    // lê os campos antes de desabilitar: campos desabilitados ficam fora do FormData
    const data = new FormData(form);
    setBusy(form, true);
    try {
        state.profile = await api.createProfile(data);
        completeStep("profile", `Perfil criado para ${state.profile.name}.`);
        unlockStep("email", emailForm(onSetupEmailLinked));
    } catch (error) {
        showError(error, $(".form-error", form));
        setBusy(form, false);
    }
});

function onSetupEmailLinked(profile) {
    const email = profile.linkedAccounts.find((a) => a.externalProvider === "EMAIL");
    completeStep("email", `Conectado a ${email.username}. Os alertas do LinkedIn serão lidos da pasta ${email.folder}`
        + (email.indeedFolder ? ` e os do Indeed, da pasta ${email.indeedFolder}.` : "."));
    unlockStep("linkedin", linkedinForm(onSetupLinkedinLinked));
}

async function onSetupLinkedinLinked() {
    const opened = await openLinkedinLogin();
    completeStep("linkedin", opened
        ? "LinkedIn conectado. Termine o login na janela do navegador que abriu: captcha, código ou aprovação no app do celular."
        : "LinkedIn conectado. Abra a janela de login depois em Contas, no botão \"Entrar no LinkedIn\".");
    $("#setup-finish").hidden = false;
    $("#setup-finish").focus();
}

$("#setup-finish").addEventListener("click", () => {
    location.hash = "#vagas";
    boot();
});

/* ============================================================
 * Formulários de conta (configuração inicial e aba Contas)
 * ============================================================ */

function accountForm(templateId, provider, onLinked) {
    const form = cloneTemplate(templateId);
    form.addEventListener("submit", async (event) => {
        event.preventDefault();
        if (!form.reportValidity()) return;
        clearError(form);
        const data = new FormData(form);
        const progress = $(".progress", form);
        setBusy(form, true);
        if (progress) progress.hidden = false;
        try {
            state.profile = await api.linkAccount({
                username: data.get("username"),
                password: data.get("password"),
                externalProvider: provider,
                folder: null,
            });
            onLinked(state.profile);
        } catch (error) {
            showError(error, $(".form-error", form));
            setBusy(form, false);
        } finally {
            if (progress) progress.hidden = true;
        }
    });
    return form;
}

const emailForm = (onLinked) => accountForm("email-form-template", "EMAIL", onLinked);
const linkedinForm = (onLinked) => accountForm("linkedin-form-template", "LINKEDIN", onLinked);

/* ============================================================
 * Vagas
 * ============================================================ */

async function loadMatches(animate) {
    $("#setup-warning").hidden = true;
    renderMissingSetup();

    try {
        state.matches = await api.listMatches();
    } catch (error) {
        showError(error, $("#setup-warning"));
        return;
    }
    renderMatches(animate);
}

function renderMissingSetup() {
    const missing = missingSetup();
    if (missing.length === 0) return;

    const box = $("#setup-warning");
    const onlyProfile = missing.every((item) => item === "a chave do Gemini" || item === "o currículo");
    const link = el("a", "", onlyProfile ? "Abrir perfil" : "Abrir contas");
    link.href = onlyProfile ? "#perfil" : "#contas";
    box.replaceChildren(`Para analisar novas vagas, falta configurar ${joinList(missing)}. `, link);
    box.hidden = false;
}

function joinList(items) {
    return items.length < 2 ? items.join("") : `${items.slice(0, -1).join(", ")} e ${items.at(-1)}`;
}

function visibleMatches() {
    const query = normalize(state.query.trim());
    const sorted = [...state.matches].sort(state.sort === "recent"
        ? (a, b) => new Date(b.createdAt) - new Date(a.createdAt) || b.score - a.score
        : (a, b) => b.score - a.score);
    return sorted.filter((m) =>
        (state.filter === "all" || m.match) &&
        (!query || normalize(`${m.jobTitle} ${m.companyName}`).includes(query)));
}

function renderMatches(animate) {
    const all = state.matches;
    const fits = all.filter((m) => m.match).length;
    const visible = visibleMatches();

    $("#matches-summary").textContent = all.length === 0
        ? "As vagas dos seus alertas aparecem aqui com a nota de compatibilidade."
        : `${all.length} ${all.length === 1 ? "vaga analisada" : "vagas analisadas"}, ${fits} ${fits === 1 ? "compatível" : "compatíveis"}.`;

    $("#toolbar").hidden = all.length === 0;
    $("#matches-empty").hidden = all.length > 0;

    const filteredEmpty = all.length > 0 && visible.length === 0;
    $("#matches-filtered-empty").hidden = !filteredEmpty;
    if (filteredEmpty) {
        const searching = state.query.trim() !== "";
        $("#filtered-empty-title").textContent = searching ? "Nenhuma vaga encontrada" : "Nenhuma vaga passou do corte ainda";
        $("#filtered-empty-text").textContent = searching
            ? `Nada corresponde a "${state.query.trim()}". Tente o nome da empresa ou outra palavra do título.`
            : "Uma vaga é compatível quando a nota chega a 70 e os requisitos obrigatórios são atendidos.";
    }

    const list = $("#matches");
    list.replaceChildren(...visible.map(matchItem));
    list.classList.toggle("is-entering", animate);
    renderSpread(all);
}

// Todas as notas numa régua só. Notas muito próximas viram colunas mais altas para não se esconderem.
function renderSpread(matches) {
    const spread = $("#spread");
    spread.hidden = matches.length === 0;
    if (spread.hidden) return;

    const above = matches.filter((m) => m.match).length;
    const below = matches.length - above;
    $("#legend-below").textContent = `${below} abaixo do corte`;
    $("#legend-above").textContent = `${above} ${above === 1 ? "compatível" : "compatíveis"}`;

    const placed = [];
    const marks = [...matches].sort((a, b) => a.score - b.score).map((match) => {
        const lane = placed.filter((score) => Math.abs(score - match.score) < 2).length;
        placed.push(match.score);

        const mark = el("button", "spread-mark");
        mark.type = "button";
        mark.dataset.fit = match.match ? "yes" : "no";
        mark.style.setProperty("--score", Math.max(0, Math.min(100, match.score)));
        mark.style.setProperty("--lane", lane);
        mark.setAttribute("aria-label", `${match.score}: ${match.jobTitle ?? "vaga"}${match.companyName ? `, ${match.companyName}` : ""}`);
        mark.title = `${match.score} · ${match.jobTitle ?? "vaga"}`;
        mark.addEventListener("click", () => focusMatch(match.id));
        return mark;
    });
    $("#spread-marks").replaceChildren(...marks);
}

function focusMatch(id) {
    let item = document.getElementById(`match-${id}`);
    if (!item) {
        // a vaga pode estar escondida pela busca ou pelo filtro
        state.query = "";
        $("#search").value = "";
        setFilter("all");
        item = document.getElementById(`match-${id}`);
    }
    if (!item) return;
    item.scrollIntoView({ behavior: matchMedia("(prefers-reduced-motion: reduce)").matches ? "auto" : "smooth", block: "start" });
    item.classList.remove("is-highlighted");
    void item.offsetWidth;
    item.classList.add("is-highlighted");
    $(".job-link", item).focus({ preventScroll: true });
}

function matchItem(match) {
    const item = cloneTemplate("match-template");
    item.id = `match-${match.id}`;
    item.dataset.fit = match.match ? "yes" : "no";
    item.style.setProperty("--score", Math.max(0, Math.min(100, match.score)));

    $(".score-value", item).textContent = match.score;
    $(".job-title", item).textContent = match.jobTitle ?? "Vaga sem título";
    $(".job-company", item).textContent = match.companyName ?? "";
    if (match.createdAt) $(".analysed-at", item).textContent = `Analisada em ${dateFormat.format(new Date(match.createdAt))}`;
    $(".verdict", item).textContent = match.match
        ? "Compatível"
        : match.score >= MATCH_CUT ? "Falta requisito obrigatório" : `${MATCH_CUT - match.score} pontos abaixo do corte`;

    $(".reasons", item).replaceChildren(...(match.motivos ?? []).map((reason) => el("li", "", reason)));
    $(".job-link", item).href = match.jobLink;

    if (match.curriculoAts) {
        const copy = $(".copy-ats", item);
        copy.hidden = false;
        copy.addEventListener("click", async () => {
            try {
                await navigator.clipboard.writeText(match.curriculoAts);
                toast("Currículo adaptado copiado.");
            } catch {
                $(".ats", item).open = true;
                toast("Não foi possível copiar. Selecione o texto abaixo.");
            }
        });
        $(".ats", item).hidden = false;
        $(".ats-text", item).textContent = match.curriculoAts;
    }

    return item;
}

function setFilter(filter) {
    state.filter = filter;
    $$(".segmented button").forEach((b) => b.setAttribute("aria-pressed", String(b.dataset.filter === filter)));
    renderMatches(false);
}

$$(".segmented button").forEach((button) => button.addEventListener("click", () => setFilter(button.dataset.filter)));

$("#search").addEventListener("input", (event) => {
    state.query = event.target.value;
    renderMatches(false);
});

$("#sort").addEventListener("change", (event) => {
    state.sort = event.target.value;
    renderMatches(false);
});

$("#refresh-matches").addEventListener("click", () => {
    loadMatches(false);
    loadStatus();
});

/* ============================================================
 * Leitor do LinkedIn
 * ============================================================ */

// o que fazer em cada falha do leitor, pelo código do erro
const SCRAPER_HELP = {
    LINKEDIN_PAGE_UNREADABLE: "Olhe a captura abaixo. Se aparece uma tela de login ou de verificação, ligue \"Mostrar o navegador\" no Perfil para resolver na próxima análise. Se a página parece normal, foi lentidão e a próxima tentativa costuma passar.",
    LINKEDIN_CHECKPOINT: "Em Contas, clique em \"Entrar no LinkedIn\". O navegador abre na tela com login e senha preenchidos e espera até 5 minutos você resolver a verificação.",
    LINKEDIN_LOGIN_FAILED: "Confira o login e a senha da conta do LinkedIn em Contas. Se mudou a senha, desconecte e conecte de novo.",
    SCRAPER_FAILED: "Os detalhes técnicos estão no terminal onde o Vagora está rodando. Se o erro se repetir, pare o Vagora e rode ./run.sh --clean.",
};

const clockFormat = new Intl.DateTimeFormat("pt-BR", { timeStyle: "medium" });

async function loadScraper() {
    try {
        renderScraper((await api.scraper())[state.readerSource]);
    } catch (error) {
        showError(error, $("#scraper-failure-text"));
        $("#scraper-failure").hidden = false;
    }
}

function renderScraper(activity) {
    const box = $("#scraper-now");
    const status = state.statuses?.[state.readerSource];
    const next = status?.nextAlertAt && new Date(status.nextAlertAt) > new Date() ? timeFormat.format(new Date(status.nextAlertAt)) : null;
    let view;
    if (activity.running && !activity.jobTotal) {
        view = { state: "busy", title: "Entrando no LinkedIn", detail: "Termine o login na janela do navegador." };
    } else if (activity.running) {
        view = { state: "busy", title: "Lendo vagas",
            detail: activity.jobIndex ? `Vaga ${activity.jobIndex} de ${activity.jobTotal}${status?.currentAlert ? ` do alerta "${status.currentAlert}"` : ""}.` : "Abrindo o navegador." };
    } else if (activity.analyzing) {
        view = { state: "busy", title: "Analisando com o Gemini",
            detail: `Comparando ${activity.jobTotal} ${activity.jobTotal === 1 ? "vaga" : "vagas"} com o seu currículo.` };
    } else if (activity.failure) {
        view = { state: "error", title: "Parou com um problema", detail: next ? `Vai tentar de novo às ${next}.` : "Vai tentar de novo no próximo ciclo." };
    } else if (!state.profile.monitoringEnabled) {
        view = { state: "paused", title: "Monitoramento pausado", detail: "Nenhuma vaga é aberta até você retomar no Perfil." };
    } else {
        view = { state: activity.events.length ? "ok" : "idle", title: "Parado",
            detail: next ? `Próximo alerta às ${next}${status.queuedAlerts ? ` · ${status.queuedAlerts} na fila` : ""}.` : "Esperando um alerta de vaga chegar no email." };
    }
    box.dataset.state = view.state;
    $("#scraper-title").textContent = view.title;
    $("#scraper-detail").textContent = view.detail;

    const progress = $("#scraper-progress");
    progress.hidden = !(activity.running && activity.jobTotal);
    $("#scraper-progress-fill").style.width = activity.jobTotal ? `${Math.round(100 * activity.jobIndex / activity.jobTotal)}%` : "0";

    // painel de problema: falha do alerta ou vaga pulada (com a captura do que o navegador mostrou)
    const failure = $("#scraper-failure");
    failure.hidden = !activity.failure && !activity.screenshot;
    if (!failure.hidden) {
        $("#failure-title").textContent = activity.failure ? "O que deu errado" : "Vaga pulada";
        $("#scraper-failure-text").textContent = activity.failure
            ?? "Uma vaga não carregou a tempo e foi pulada; as outras foram lidas normalmente.";
        $("#scraper-failure-help").textContent = activity.failure
            ? (SCRAPER_HELP[status?.lastErrorCode] ?? "O alerta continua no email e será tentado de novo.")
            : "Veja abaixo o que o navegador mostrava. Se for uma tela de login ou de verificação, ligue \"Mostrar o navegador\" no Perfil.";
        $("#scraper-shot").hidden = !activity.screenshot;
        if (activity.screenshot) {
            // a URL muda a cada evento para o navegador não reaproveitar a imagem anterior
            const src = `/profile/scraper/screenshot?at=${encodeURIComponent(activity.events[0]?.at ?? "")}`;
            if ($("#scraper-shot-img").getAttribute("src") !== src) $("#scraper-shot-img").src = src;
            $("#scraper-shot-link").href = src;
        }
    }


    $("#scraper-empty").hidden = activity.events.length > 0;
    $("#scraper-events").replaceChildren(...activity.events.map((event) => {
        const li = el("li", event.problem ? "is-problem" : "");
        li.append(el("time", "", clockFormat.format(new Date(event.at))), el("span", "", event.message));
        return li;
    }));
}

/* ============================================================
 * Contas
 * ============================================================ */

function renderAccounts() {
    renderEmailAccount();
    renderLinkedinAccount();
}

function accountHead(iconName, title, connected) {
    const head = el("div", "account-head");
    const badge = el("span", "account-icon");
    badge.append(icon(iconName));
    head.append(badge, el("h2", "", title), el("span", `chip ${connected ? "on" : "off"}`, connected ? "Conectado" : "Não conectado"));
    return head;
}

// uma conta por site: para trocar, desconecta e conecta de novo. Pede um segundo clique para confirmar.
function unlinkButton(provider) {
    const button = el("button", "button quiet", `Desconectar ${provider === "EMAIL" ? "email" : "LinkedIn"}`);
    button.type = "button";
    let armed = false;
    button.addEventListener("click", async () => {
        if (!armed) {
            armed = true;
            button.textContent = "Clique de novo para desconectar";
            setTimeout(() => { armed = false; button.textContent = `Desconectar ${provider === "EMAIL" ? "email" : "LinkedIn"}`; }, 4000);
            return;
        }
        button.disabled = true;
        try {
            state.profile = await api.unlinkAccount(provider);
            toast(provider === "EMAIL" ? "Email desconectado." : "LinkedIn desconectado.");
            renderAccounts();
            loadStatus();
        } catch (error) {
            button.disabled = false;
            toast(messageFor(error));
        }
    });
    return button;
}

function renderEmailAccount() {
    const card = $("#account-email");
    const email = account("EMAIL");
    const onLinked = () => { toast("Email conectado."); renderAccounts(); loadStatus(); };

    if (!email) {
        const status = el("p", "account-status", "Conecte o email que recebe os alertas de vaga do LinkedIn e do Indeed.");
        card.replaceChildren(accountHead("mail", "Email", false), status, emailForm(onLinked));
        return;
    }

    const status = el("p", "account-status");
    status.append(...rich(["Lendo ", { strong: email.username }, ". ", ...foldersText(email), "."]));
    card.replaceChildren(
        accountHead("mail", "Email", true),
        status,
        folderPicker(email),
        unlinkButton("EMAIL"),
    );
}

function folderPicker(email) {
    const wrapper = el("div", "folder-picker");
    const label = email.indeedFolder ? "Trocar as pastas do LinkedIn e do Indeed" : "Escolher a pasta do Indeed";
    const load = el("button", email.indeedFolder ? "button quiet" : "button primary");
    load.type = "button";
    load.append(icon("folder"), label);
    const error = el("p", "form-error");
    error.hidden = true;

    load.addEventListener("click", async () => {
        load.disabled = true;
        load.replaceChildren(el("span", "spinner"), "Contando os alertas em cada pasta (pode levar até 1 minuto)…");
        error.hidden = true;
        try {
            wrapper.replaceChildren(folderForm(email, await api.listFolders()));
        } catch (e) {
            showError(e, error);
            load.disabled = false;
            load.replaceChildren(icon("folder"), label);
        }
    });

    wrapper.append(load, error);
    return wrapper;
}

function folderForm(email, folders) {
    const form = el("form", "form folder-form");
    const selects = {};
    for (const source of ["LINKEDIN", "INDEED"]) {
        const field = el("div", "field");
        const label = el("label", "", `Alertas do ${SOURCE_LABELS[source]}`);
        const select = el("select");
        select.id = `f-${++uid}`;
        label.htmlFor = select.id;

        const alerts = (f) => f.jobAlerts[source] ?? 0;
        const current = folderOf(email, source);
        // pastas com alertas primeiro; empate fica na ordem do servidor de email (INBOX antes de [Gmail]/...)
        const sorted = [...folders].sort((a, b) => alerts(b) - alerts(a));
        if (source === "INDEED") select.append(new Option("Não ler alertas do Indeed", "", false, !current));
        select.append(...sorted.map((f) => new Option(
            `${f.name} · ${alerts(f) === 0 ? "nenhum alerta" : `${alerts(f)} ${alerts(f) === 1 ? "alerta" : "alertas"}`}${f.name === current ? " (atual)" : ""}`,
            f.name, false, f.name === current)));

        const hint = el("p", "hint", source === "LINKEDIN"
            ? "Só emails de jobalerts-noreply@linkedin.com são lidos."
            : "Só emails de donotreply@match.indeed.com são lidos. Pode ser a mesma pasta do LinkedIn.");
        field.append(label, select, hint);
        form.append(field);
        selects[source] = select;
    }

    const error = el("p", "form-error");
    error.hidden = true;
    const save = el("button", "button primary", "Salvar pastas");
    const cancel = el("button", "button quiet", "Cancelar");
    cancel.type = "button";
    cancel.addEventListener("click", renderAccounts);
    const actions = el("div", "folder-actions");
    actions.append(save, cancel);
    form.append(error, actions);

    form.addEventListener("submit", async (event) => {
        event.preventDefault();
        const changed = Object.entries(selects).filter(([source, select]) => select.value !== (folderOf(email, source) ?? ""));
        if (!changed.length) return renderAccounts();
        setBusy(form, true);
        error.hidden = true;
        try {
            for (const [source, select] of changed) state.profile = await api.updateFolder(select.value, source);
            toast("Pastas salvas.");
            renderAccounts();
        } catch (e) {
            showError(e, error);
            setBusy(form, false);
        }
    });
    return form;
}

function renderLinkedinAccount() {
    const card = $("#account-linkedin");
    const linkedin = account("LINKEDIN");
    const onLinked = async () => { renderAccounts(); loadStatus(); await openLinkedinLogin(); };

    if (!linkedin) {
        const status = el("p", "account-status", "O Vagora abre cada vaga com a sua conta para ler a descrição completa.");
        card.replaceChildren(accountHead("globe", "LinkedIn", false), status, linkedinForm(onLinked));
        return;
    }

    const status = el("p", "account-status");
    status.append(...rich(["Conectado como ", { strong: linkedin.username }, "."]));
    card.replaceChildren(
        accountHead("globe", "LinkedIn", true),
        status,
        el("p", "hint", "Se o LinkedIn pedir verificação, entre por aqui: o navegador do Vagora abre na tela e guarda a sessão."),
        linkedinLoginButton(),
        unlinkButton("LINKEDIN"),
    );
}

function linkedinLoginButton() {
    const button = el("button", "button primary", "Entrar no LinkedIn");
    button.type = "button";
    button.addEventListener("click", async () => {
        button.disabled = true;
        if (await openLinkedinLogin()) location.hash = "#linkedin";
        button.disabled = false;
    });
    return button;
}

async function openLinkedinLogin() {
    try {
        await api.openLinkedinLogin();
        toast("Navegador aberto: termine o login na janela (captcha, código ou aprovação no celular).");
        return true;
    } catch (error) {
        toast(messageFor(error));
        return false;
    }
}

/* ============================================================
 * Perfil
 * ============================================================ */

function actionLink(text, href, options = {}) {
    const link = el("a", `button ${options.primary ? "primary" : "quiet"}`, text);
    link.href = href;
    if (options.newTab) {
        link.target = "_blank";
        link.rel = "noopener";
    }
    if (options.focus) {
        link.addEventListener("click", (event) => {
            event.preventDefault();
            const field = $(options.focus);
            field.scrollIntoView({ block: "center" });
            field.focus();
        });
    }
    return link;
}

function checkItem({ ok, name, detail, actions }) {
    const li = el("li", "check-item");
    li.dataset.ok = ok ? "yes" : "no";

    const mark = el("span", "check-mark");
    mark.append(icon(ok ? "check" : "alert"));
    mark.setAttribute("role", "img");
    mark.setAttribute("aria-label", ok ? "Pronto" : "Pendente");

    const text = el("span", "check-detail");
    text.append(...rich(detail));

    const buttons = el("span", "check-actions");
    buttons.append(...actions);

    li.append(mark, el("span", "check-name", name), text, buttons);
    return li;
}

function renderReadiness() {
    const profile = state.profile;
    const email = account("EMAIL");
    const linkedin = account("LINKEDIN");

    const items = [
        profile.curriculumName
            ? { ok: true, name: "Currículo", detail: [{ strong: profile.curriculumName }, profile.curriculumSizeBytes ? `, ${formatSize(profile.curriculumSizeBytes)}` : ""],
                actions: [actionLink("Abrir PDF", "/profile/curriculum", { newTab: true }), actionLink("Trocar", "#", { focus: "#profile-curriculum" })] }
            : { ok: false, name: "Currículo", detail: ["Nenhum currículo enviado. Sem ele não há com o que comparar as vagas."],
                actions: [actionLink("Enviar", "#", { focus: "#profile-curriculum", primary: true })] },
        profile.hasGeminiApiKey
            ? { ok: true, name: "Chave do Gemini", detail: ["Configurada. Ela não é exibida por segurança."],
                actions: [actionLink("Trocar", "#", { focus: "#profile-gemini" })] }
            : { ok: false, name: "Chave do Gemini", detail: ["Falta a chave que permite ao Gemini dar a nota."],
                actions: [actionLink("Adicionar", "#", { focus: "#profile-gemini", primary: true })] },
        email
            ? { ok: true, name: "Email", detail: [{ strong: email.username }, ". ", ...foldersText(email)],
                actions: [actionLink("Gerenciar", "#contas")] }
            : { ok: false, name: "Email", detail: ["Nenhum email conectado. É de onde vêm os alertas de vaga."],
                actions: [actionLink("Conectar", "#contas", { primary: true })] },
        linkedin
            ? { ok: true, name: "LinkedIn", detail: ["Conectado como ", { strong: linkedin.username }],
                actions: [actionLink("Gerenciar", "#contas")] }
            : { ok: false, name: "LinkedIn", detail: ["Não conectado. Sem ele o Vagora não lê a descrição das vagas."],
                actions: [actionLink("Conectar", "#contas", { primary: true })] },
    ];

    const missing = items.filter((item) => !item.ok).length;
    $(".readiness").dataset.ready = missing === 0 ? "yes" : "no";
    $("#readiness-title").textContent = missing === 0
        ? "Tudo pronto para analisar vagas"
        : `Falta ${missing} ${missing === 1 ? "item" : "itens"} para analisar vagas`;
    $("#readiness-lede").textContent = missing === 0
        ? "O Vagora acompanha seu email, analisa os alertas da última semana e espera de 15 a 25 minutos entre um acesso ao LinkedIn e outro."
        : "Enquanto algum item estiver pendente, os alertas ficam no email esperando, sem serem marcados como lidos.";
    $("#checklist").replaceChildren(...items.map(checkItem));
}

function initials(name) {
    const parts = (name ?? "").trim().split(/\s+/).filter(Boolean);
    return ((parts[0]?.[0] ?? "?") + (parts.length > 1 ? parts.at(-1)[0] : "")).toUpperCase();
}

function renderProfile() {
    const profile = state.profile;
    $("#avatar").textContent = initials(profile.name);
    $("#profile-title").textContent = profile.name || "Seu perfil";
    const email = account("EMAIL");
    $("#profile-handle").textContent = email ? `Alertas de ${email.username}` : "Nenhum email conectado ainda";
    $("#show-browser").checked = Boolean(profile.showBrowser);
    $("#monitoring").checked = Boolean(profile.monitoringEnabled);
    renderReadiness();

    const form = $("#profile-form");
    form.reset();
    clearError(form);
    setBusy(form, false);
    $("#profile-name").value = profile.name ?? "";
    $("#profile-gemini-hint").textContent = profile.hasGeminiApiKey
        ? "Chave configurada. Deixe em branco para manter a atual."
        : "Nenhuma chave configurada: sem ela as vagas não são analisadas.";
    $("#profile-curriculum-hint").textContent = profile.curriculumName
        ? `Atual: ${profile.curriculumName}. PDF de até 5 MB.`
        : "PDF de até 5 MB.";
}

$("#show-browser").addEventListener("change", async (event) => {
    const toggle = event.target;
    const data = new FormData();
    data.append("showBrowser", String(toggle.checked));
    toggle.disabled = true;
    try {
        state.profile = await api.updateProfile(data);
        toast(toggle.checked
            ? "O navegador vai aparecer na próxima análise."
            : "O navegador vai rodar escondido.");
        renderProcessingAlert();
    } catch (error) {
        toggle.checked = !toggle.checked;
        toast(messageFor(error));
    } finally {
        toggle.disabled = false;
    }
});

$("#profile-form").addEventListener("submit", async (event) => {
    event.preventDefault();
    const form = event.currentTarget;
    clearError(form);

    // envia só o que mudou: campos vazios mantêm o valor atual
    const data = new FormData();
    const name = $("#profile-name").value.trim();
    const gemini = $("#profile-gemini").value.trim();
    const curriculum = $("#profile-curriculum").files[0];
    if (name && name !== state.profile.name) data.append("name", name);
    if (gemini) data.append("geminiApiKey", gemini);
    if (curriculum) data.append("curriculum", curriculum);

    if ([...data.keys()].length === 0) {
        toast("Nada para salvar.");
        return;
    }

    setBusy(form, true);
    try {
        state.profile = await api.updateProfile(data);
        renderProfile();
        toast("Perfil salvo.");
    } catch (error) {
        showError(error, $(".form-error", form));
        setBusy(form, false);
    }
});

$("#delete-profile").addEventListener("click", () => $("#confirm-delete").showModal());

$("#confirm-delete").addEventListener("close", async () => {
    if ($("#confirm-delete").returnValue !== "confirm") return;
    try {
        await api.deleteProfile();
        toast("Perfil excluído.");
        location.hash = "";
        boot();
    } catch (error) {
        showError(error, $("#profile-form .form-error"));
    }
});

$$(".tip").forEach(wireTip);
boot();
