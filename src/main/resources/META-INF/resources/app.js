"use strict";

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
            message_error: "O Vagora não está respondendo. Confira se ele está rodando com ./run.sh (ou run.cmd no Windows).",
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

const api = {
    getProfile: () => request("GET", "/profile"),
    createProfile: (form) => request("POST", "/profile", form),
    updateProfile: (form) => request("PATCH", "/profile", form),
    deleteProfile: () => request("DELETE", "/profile"),
    linkAccount: (account) => request("POST", "/profile/accounts", account),
    unlinkAccount: (provider) => request("DELETE", `/profile/accounts/${provider}`),
    openLinkedinLogin: () => request("POST", "/profile/accounts/LINKEDIN/session"),
    claudeStatus: () => request("GET", "/profile/accounts/CLAUDE/session"),
    connectClaude: (method) => request("POST", "/profile/accounts/CLAUDE/session", { method }),
    sendClaudeCode: (code) => request("POST", "/profile/accounts/CLAUDE/code", { code }),
    listFolders: () => request("GET", "/profile/email/folders"),
    updateFolder: (folder, source) => request("PUT", "/profile/email/folder", { folder, source }),
    listMatches: () => request("GET", "/profile/job-matches"),
    generateCurriculum: (id) => request("POST", `/profile/job-matches/${id}/curriculum`),
    listPending: () => request("GET", "/profile/pending-jobs"),
    prioritize: (id) => request("POST", `/profile/pending-jobs/${id}/priority`),
    status: () => request("GET", "/profile/status"),
    scraper: () => request("GET", "/profile/scraper"),
    listGupySearches: () => request("GET", "/gupy/searches"),
    createGupySearch: (search) => request("POST", "/gupy/searches", search),
    updateGupySearch: (id, search) => request("PUT", `/gupy/searches/${id}`, search),
    deleteGupySearch: (id) => request("DELETE", `/gupy/searches/${id}`),
};

const MATCH_CUT = 70;

const state = {
    profile: null,
    matches: [],
    filter: "all",
    sort: "score",
    query: "",
    selected: null,
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

function rich(parts) {
    return parts.map((part) => (typeof part === "string" ? part : el("strong", "", part.strong)));
}

let uid = 0;
function cloneTemplate(id) {
    const node = $(`#${id}`).content.firstElementChild.cloneNode(true);

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

const SOURCE_LABELS = { LINKEDIN: "LinkedIn", INDEED: "Indeed", GLASSDOOR: "Glassdoor", GUPY: "Gupy" };

function folderOf(email, source) {
    return { INDEED: email.indeedFolder, GLASSDOOR: email.glassdoorFolder }[source] ?? (source === "LINKEDIN" ? email.folder : null);
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

function wireTip(tip) {
    const trigger = $(".tip-trigger", tip);
    const bubble = $(".tip-bubble", tip);
    if (!bubble.id) bubble.id = `tip-${++uid}`;
    trigger.setAttribute("aria-describedby", bubble.id);
    trigger.setAttribute("aria-expanded", "false");

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

function showError(error, target) {
    if (!(error instanceof ApiError)) {
        console.error(error);
        error = new ApiError({ type_error: "INTERNAL_ERROR", message_error: "Algo deu errado nesta página." });
    }

    if (error.type === "NOT_FOUND" && state.profile) {
        toast("Esse perfil não existe mais.");
        boot();
        return;
    }

    const box = target ?? $("#setup-warning");
    box.replaceChildren(messageFor(error));

    if (error.type === "BUSINESS_RULE" && !box.closest("[data-panel='config']")) {
        const link = el("a", "", "Abrir configurações");
        link.href = "#config";
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

const KEY_MASK = "••••••••••••••••";

const SUBPAGES = {
    vagas: [["vagas", "Analisadas"], ["pendentes", "Na fila"], ["gupy", "Gupy"]],
    atividade: [["linkedin", "LinkedIn"], ["indeed", "Indeed"], ["glassdoor", "Glassdoor"]],
    config: [["config", "Currículo"], ["config-ia", "IA"], ["config-fluxo", "Fluxo da IA"], ["config-contas", "Contas"], ["config-automacao", "Automação"], ["config-perfil", "Perfil"]],
};
const PAGE_SECTION = {
    vagas: "vagas", pendentes: "vagas", gupy: "vagas", linkedin: "atividade", indeed: "atividade", glassdoor: "atividade",
    config: "config", "config-ia": "config", "config-fluxo": "config", "config-contas": "config", "config-automacao": "config", "config-perfil": "config",
};

let pageTimer;
function route() {
    if ($("#app").hidden) return;
    const tab = PAGE_SECTION[location.hash.slice(1)] ? location.hash.slice(1) : "vagas";
    const section = PAGE_SECTION[tab];

    $$("[data-tab]").forEach((link) => {
        if (link.dataset.tab === section) link.setAttribute("aria-current", "page");
        else link.removeAttribute("aria-current");
    });
    $$("[data-panel]").forEach((panel) => { panel.hidden = !panel.dataset.panel.split(" ").includes(tab); });
    renderSubnav($("#subnav"), section === "config" ? null : SUBPAGES[section], tab);
    renderSubnav($("#config-nav"), section === "config" ? SUBPAGES.config : null, tab);

    clearInterval(pageTimer);
    if (tab === "vagas") {
        loadMatches(true);

        pageTimer = setInterval(() => loadMatches(false), 60_000);
    }
    if (tab === "pendentes") {
        loadPending();
        pageTimer = setInterval(loadPending, 30_000);
    }
    if (tab === "linkedin" || tab === "indeed" || tab === "glassdoor") {
        state.readerSource = tab.toUpperCase();
        $("#reader-title").textContent = `Leitor do ${SOURCE_LABELS[state.readerSource]}`;
        $("#reader-source").textContent = SOURCE_LABELS[state.readerSource];
        loadScraper();

        pageTimer = setInterval(loadScraper, 5_000);
    }
    if (section === "config") {
        $$(".save-status[data-state='dirty']").forEach(clearStatus);
        $$(".setting").forEach((setting) => { setting.hidden = setting.dataset.page !== tab; });
        renderProfile();
        renderEditableSettings();
        renderAccounts();
    }
    if (tab === "gupy") {
        renderGupy();
        loadGupyActivity();
        pageTimer = setInterval(loadGupyActivity, 10_000);
    }
    if (tab === "vagas") renderGupyInvite();
    window.scrollTo({ top: 0 });
}

function renderSubnav(nav, pages, current) {
    nav.hidden = !pages;
    nav.replaceChildren(...(pages ?? []).map(([page, label]) => {
        const link = el("a", "", label);
        link.href = `#${page}`;
        if (page === current) link.setAttribute("aria-current", "page");
        return link;
    }));
}

window.addEventListener("hashchange", route);

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

function hasAiKey() {
    return state.profile.analysisChain.some(llmAvailable);
}

function missingSetup() {
    const missing = [];
    if (!account("EMAIL")) missing.push("o email");
    if (!account("LINKEDIN")) missing.push("o LinkedIn");
    if (!hasAiKey()) missing.push("uma IA disponível para a nota das vagas");
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

const ERROR_ACTIONS = {
    LINKEDIN_CHECKPOINT: ["Entrar no LinkedIn", "#config-contas"],
    LINKEDIN_LOGIN_FAILED: ["Abrir configurações", "#config-contas"],
    LINKEDIN_NOT_CONNECTED: ["Abrir configurações", "#config-contas"],
    LINKEDIN_PAGE_UNREADABLE: ["Ver o leitor", "#leitor"],
    SCRAPER_FAILED: ["Ver o leitor", "#leitor"],
    LLM_KEY_MISSING: ["Abrir configurações", "#config-ia"],
    LLM_KEY_REJECTED: ["Abrir configurações", "#config-ia"],
    CLAUDE_NOT_INSTALLED: ["Abrir configurações", "#config-ia"],
    CLAUDE_NOT_CONNECTED: ["Entrar no Claude", "#config-ia"],
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
        + (email.indeedFolder ? `, os do Indeed, da pasta ${email.indeedFolder}` : "")
        + (email.glassdoorFolder ? `, os do Glassdoor, da pasta ${email.glassdoorFolder}` : "") + ".");
    unlockStep("linkedin", linkedinForm(onSetupLinkedinLinked));
}

async function onSetupLinkedinLinked() {
    const opened = await openLinkedinLogin();
    completeStep("linkedin", opened
        ? "LinkedIn conectado. Termine o login na janela do navegador que abriu: captcha, código ou aprovação no app do celular."
        : "LinkedIn conectado. Abra a janela de login depois em Configurações, no botão \"Entrar no LinkedIn\".");
    $("#setup-finish").hidden = false;
    $("#setup-finish").focus();
}

$("#setup-finish").addEventListener("click", () => {
    location.hash = "#vagas";
    boot();
});

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
    const link = el("a", "", "Abrir configurações");
    link.href = "#config";
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

    if (!visible.some((m) => m.id === state.selected)) state.selected = visible[0]?.id ?? null;
    $("#matches").replaceChildren(...visible.map(matchRow));
    renderDetail(animate);
    renderSpread(all);
}

const wideDesk = matchMedia("(min-width: 60rem)");
wideDesk.addEventListener("change", () => renderDetail(false));

function matchRow(match) {
    const row = el("li", "row");
    row.id = `row-${match.id}`;
    row.dataset.fit = match.match ? "yes" : "no";
    const button = el("button", "row-button");
    button.type = "button";
    button.setAttribute("aria-pressed", String(match.id === state.selected));
    const text = el("span", "row-text");
    text.append(el("span", "row-title", match.jobTitle ?? "Vaga sem título"), el("span", "row-company", match.companyName ?? ""));
    button.append(el("span", "row-score", String(match.score)), text);
    button.addEventListener("click", () => selectMatch(match.id));
    row.append(button);
    return row;
}

function selectMatch(id) {
    state.selected = id;
    $$("#matches .row").forEach((row) => {
        $(".row-button", row).setAttribute("aria-pressed", String(row.id === `row-${id}`));
    });
    $$("#spread-marks .spread-mark").forEach((mark) => mark.setAttribute("aria-pressed", String(mark.dataset.id === id)));
    renderDetail(true);
}

function renderDetail(animate) {
    const pane = $("#match-detail");
    $$("#matches .match").forEach((item) => item.remove());
    const match = state.matches.find((m) => m.id === state.selected);
    pane.replaceChildren();
    pane.hidden = !match || !wideDesk.matches;
    if (!match) return;
    const item = matchItem(match);
    item.classList.toggle("is-entering", animate);
    (wideDesk.matches ? pane : $(`#row-${match.id}`))?.append(item);
}

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
        mark.dataset.id = match.id;
        mark.setAttribute("aria-pressed", String(match.id === state.selected));
        mark.setAttribute("aria-label", `${match.score}: ${match.jobTitle ?? "vaga"}${match.companyName ? `, ${match.companyName}` : ""}`);
        mark.title = `${match.score} · ${match.jobTitle ?? "vaga"}`;
        mark.addEventListener("click", () => focusMatch(match.id));
        return mark;
    });
    $("#spread-marks").replaceChildren(...marks);
}

function focusMatch(id) {
    if (!document.getElementById(`row-${id}`)) {
        state.query = "";
        $("#search").value = "";
        setFilter("all");
    }
    const row = document.getElementById(`row-${id}`);
    if (!row) return;
    selectMatch(id);
    row.scrollIntoView({ behavior: matchMedia("(prefers-reduced-motion: reduce)").matches ? "auto" : "smooth", block: "nearest" });
    const item = $(".match", wideDesk.matches ? $("#match-detail") : row);
    item.classList.add("is-highlighted");
    $(".row-button", row).focus({ preventScroll: true });
}

function matchItem(match) {
    const item = cloneTemplate("match-template");
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
        const pdf = $(".pdf-ats", item);
        pdf.hidden = false;
        pdf.href = `/profile/job-matches/${match.id}/curriculum`;
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
    } else if (match.canGenerateCurriculum) {
        const generate = $(".generate-ats", item);
        generate.hidden = false;
        if (generating.has(match.id)) showGenerating(generate);
        generate.addEventListener("click", () => generateCurriculum(match.id, generate));
    }

    return item;
}

async function loadPending() {
    let queue;
    try {
        queue = await api.listPending();
    } catch (error) {
        const box = $("#pending-status");
        box.className = "notice warn";
        box.textContent = messageFor(error);
        box.hidden = false;
        return;
    }
    renderPending(queue);
}

function renderPending(queue) {
    const jobs = queue.jobs;
    $("#pending-summary").textContent = jobs.length === 0
        ? "As vagas lidas dos alertas esperam aqui pela análise da IA."
        : `${jobs.length} ${jobs.length === 1 ? "vaga aguardando" : "vagas aguardando"} análise. As priorizadas vão primeiro.`;
    $("#pending-empty").hidden = jobs.length > 0;
    $("#pending").replaceChildren(...jobs.map((job) => pendingItem(job, queue.maxAttempts)));
    renderAnalysisStatus(queue.analysis, jobs.length);
}

function renderAnalysisStatus(analysis, total) {
    const box = $("#pending-status");
    let text = null;
    let warn = false;
    if (total === 0) {
        text = null;
    } else if (!state.profile.monitoringEnabled) {
        text = "Monitoramento pausado: a análise volta quando você retomar.";
        warn = true;
    } else if (!hasAiKey()) {
        text = "Configure uma IA em Configurações para analisar as vagas.";
        warn = true;
    } else if (analysis?.currentAlert) {
        text = `Analisando ${analysis.currentAlert} agora.`;
    } else if (analysis?.lastError) {
        const retry = analysis.nextAlertAt && new Date(analysis.nextAlertAt) > new Date()
            ? ` Nova tentativa às ${timeFormat.format(new Date(analysis.nextAlertAt))}.`
            : " Nova tentativa em até 2 minutos.";
        text = `A última análise falhou: ${analysis.lastError}${retry}`;
        warn = true;
    } else {
        text = "A próxima análise acontece em até 2 minutos.";
    }
    box.className = warn ? "notice warn" : "notice info";
    box.textContent = text ?? "";
    box.hidden = text === null;
}

function pendingItem(job, maxAttempts) {
    const item = cloneTemplate("pending-template");
    const stuck = job.attempts >= maxAttempts;
    item.dataset.priority = job.prioritized ? "yes" : "no";

    $(".job-title", item).textContent = job.jobTitle || "Vaga sem título";
    $(".job-company", item).textContent = job.companyName ?? "";
    $(".job-source", item).textContent = SOURCE_LABELS[job.source] ?? "";
    if (job.createdAt) $(".analysed-at", item).textContent = `Lida em ${dateFormat.format(new Date(job.createdAt))}`;
    $(".verdict", item).textContent = job.prioritized
        ? "Prioridade"
        : stuck ? `Falhou ${job.attempts} vezes` : job.attempts > 0 ? "Falhou, nova tentativa" : "Na fila";
    $(".job-link", item).href = job.jobLink;

    const button = $(".prioritize", item);
    button.hidden = job.prioritized;
    if (stuck) button.textContent = "Tentar de novo";
    button.addEventListener("click", () => prioritize(job.id, button));
    return item;
}

async function prioritize(id, button) {
    button.disabled = true;
    try {
        await api.prioritize(id);
        toast("Vaga priorizada: entra na próxima análise.");
    } catch (error) {
        toast(messageFor(error));
    } finally {
        await loadPending();
    }
}

const generating = new Set();

function showGenerating(button) {
    button.disabled = true;
    button.replaceChildren(el("span", "spinner"), "Gerando currículo (pode levar alguns minutos)…");
}

async function generateCurriculum(id, button) {
    generating.add(id);
    showGenerating(button);
    try {
        const updated = await api.generateCurriculum(id);
        state.matches = state.matches.map((m) => (m.id === id ? updated : m));
        toast("Currículo adaptado gerado.");
    } catch (error) {
        toast(error.message);
    } finally {
        generating.delete(id);
        renderMatches(false);
    }
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

const SCRAPER_HELP = {
    LINKEDIN_PAGE_UNREADABLE: "Olhe a captura abaixo. Se aparece uma tela de login ou de verificação, ligue \"Mostrar o navegador\" em Configurações para resolver na próxima análise. Se a página parece normal, foi lentidão e a próxima tentativa costuma passar.",
    LINKEDIN_CHECKPOINT: "Em Configurações, clique em \"Entrar no LinkedIn\". O navegador abre na tela com login e senha preenchidos e espera até 5 minutos você resolver a verificação.",
    LINKEDIN_LOGIN_FAILED: "Confira o login e a senha da conta do LinkedIn em Configurações. Se mudou a senha, desconecte e conecte de novo.",
    SCRAPER_FAILED: "Os detalhes técnicos estão no terminal onde o Vagora está rodando. Se o erro se repetir, pare o Vagora e rode ./run.sh --clean (ou run.cmd --clean no Windows).",
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
            detail: (activity.jobIndex ? `Vaga ${activity.jobIndex} de ${activity.jobTotal}${status?.currentAlert ? ` do alerta "${status.currentAlert}"` : ""}.` : "Abrindo o navegador.")
                + (activity.analyzing ? ` A IA já está dando nota em ${activity.analysisTotal} ${activity.analysisTotal === 1 ? "vaga lida" : "vagas lidas"}.` : "") };
    } else if (activity.analyzing) {
        view = { state: "busy", title: "Analisando com a IA",
            detail: `Comparando ${activity.analysisTotal} ${activity.analysisTotal === 1 ? "vaga" : "vagas"} com o seu currículo.` };
    } else if (activity.failure) {
        view = { state: "error", title: "Parou com um problema", detail: next ? `Vai tentar de novo às ${next}.` : "Vai tentar de novo no próximo ciclo." };
    } else if (!state.profile.monitoringEnabled) {
        view = { state: "paused", title: "Monitoramento pausado", detail: "Nenhuma vaga é aberta até você retomar em Configurações." };
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

    const failure = $("#scraper-failure");
    failure.hidden = !activity.failure && !activity.screenshot;
    if (!failure.hidden) {
        $("#failure-title").textContent = activity.failure ? "O que deu errado" : "Vaga pulada";
        $("#scraper-failure-text").textContent = activity.failure
            ?? "Uma vaga não carregou a tempo e foi pulada; as outras foram lidas normalmente.";
        $("#scraper-failure-help").textContent = activity.failure
            ? (SCRAPER_HELP[status?.lastErrorCode] ?? "O alerta continua no email e será tentado de novo.")
            : "Veja abaixo o que o navegador mostrava. Se for uma tela de login ou de verificação, ligue \"Mostrar o navegador\" em Configurações.";
        $("#scraper-shot").hidden = !activity.screenshot;
        if (activity.screenshot) {
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

function renderAccounts() {
    renderEmailAccount();
    renderLinkedinAccount();
    renderIndeedAccount();
    renderGlassdoorAccount();
    renderClaudeAccount();
}

function accountHead(iconName, title, connected, labels = ["Conectado", "Não conectado"]) {
    const head = el("div", "account-head");
    const badge = el("span", "account-icon");
    badge.append(icon(iconName));
    head.append(badge, el("h2", "", title), el("span", `chip ${connected ? "on" : "off"}`, connected ? labels[0] : labels[1]));
    return head;
}

const ACCOUNT_NAMES = { EMAIL: "email", LINKEDIN: "LinkedIn", CLAUDE: "Claude" };

function unlinkButton(provider) {
    const button = el("button", "button quiet", `Desconectar ${ACCOUNT_NAMES[provider]}`);
    button.type = "button";
    let armed = false;
    button.addEventListener("click", async () => {
        if (!armed) {
            armed = true;
            button.textContent = "Clique de novo para desconectar";
            setTimeout(() => { armed = false; button.textContent = `Desconectar ${ACCOUNT_NAMES[provider]}`; }, 4000);
            return;
        }
        button.disabled = true;
        try {
            state.profile = await api.unlinkAccount(provider);
            toast(provider === "EMAIL" ? "Email desconectado." : `${ACCOUNT_NAMES[provider]} desconectado.`);
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
        const status = el("p", "account-status", "Conecte o email que recebe os alertas de vaga do LinkedIn, do Indeed e do Glassdoor.");
        card.replaceChildren(accountHead("mail", "Email", false), status, emailForm(onLinked));
        return;
    }

    const status = el("p", "account-status");
    status.append(...rich(["Lendo ", { strong: email.username }, ". A pasta de cada rede é escolhida no card dela."]));
    card.replaceChildren(accountHead("mail", "Email", true), status, unlinkButton("EMAIL"));
}

function renderIndeedAccount() {
    const email = account("EMAIL");
    const reading = Boolean(email?.indeedFolder);
    $("#account-indeed").replaceChildren(
        accountHead("search", "Indeed", reading, ["Lendo alertas", "Desligado"]),
        email
            ? folderPicker(email, "INDEED")
            : el("p", "account-status", "Conecte o email para escolher a pasta dos alertas do Indeed."),
    );
}

function renderGlassdoorAccount() {
    const email = account("EMAIL");
    const reading = Boolean(email?.glassdoorFolder);
    $("#account-glassdoor").replaceChildren(
        accountHead("search", "Glassdoor", reading, ["Lendo alertas", "Desligado"]),
        email
            ? folderPicker(email, "GLASSDOOR")
            : el("p", "account-status", "Conecte o email para escolher a pasta dos alertas do Glassdoor."),
    );
}

function folderPicker(email, source) {
    const wrapper = el("div", "folder-picker");
    const current = folderOf(email, source);
    const text = el("p", "account-status");
    text.append(...rich(current
        ? ["Lê os alertas da pasta ", { strong: current }, " do email."]
        : [`Os alertas do ${SOURCE_LABELS[source]} não estão sendo lidos. Escolha a pasta do email onde eles chegam.`]));
    const label = current ? "Trocar pasta" : "Escolher pasta";
    const load = el("button", current ? "button quiet" : "button primary");
    load.type = "button";
    load.append(icon("folder"), label);
    const error = el("p", "form-error");
    error.hidden = true;

    load.addEventListener("click", async () => {
        load.disabled = true;
        load.replaceChildren(el("span", "spinner"), "Contando os alertas em cada pasta (pode levar até 1 minuto)…");
        error.hidden = true;
        try {
            wrapper.replaceChildren(folderForm(email, source, await api.listFolders()));
        } catch (e) {
            showError(e, error);
            load.disabled = false;
            load.replaceChildren(icon("folder"), label);
        }
    });

    wrapper.append(text, load, error);
    return wrapper;
}

function folderForm(email, source, folders) {
    const form = el("form", "form folder-form");
    const field = el("div", "field");
    const label = el("label", "", `Pasta dos alertas do ${SOURCE_LABELS[source]}`);
    const select = el("select");
    select.id = `f-${++uid}`;
    label.htmlFor = select.id;

    const alerts = (f) => f.jobAlerts[source] ?? 0;
    const current = folderOf(email, source);
    const sorted = [...folders].sort((a, b) => alerts(b) - alerts(a));
    if (source !== "LINKEDIN") select.append(new Option(`Não ler alertas do ${SOURCE_LABELS[source]}`, "", false, !current));
    select.append(...sorted.map((f) => new Option(
        `${f.name} · ${alerts(f) === 0 ? "nenhum alerta" : `${alerts(f)} ${alerts(f) === 1 ? "alerta" : "alertas"}`}${f.name === current ? " (atual)" : ""}`,
        f.name, false, f.name === current)));

    const senders = { LINKEDIN: "jobalerts-noreply@linkedin.com", INDEED: "indeedapply@indeed.com", GLASSDOOR: "noreply@glassdoor.com" };
    const hint = el("p", "hint", `Só emails de ${senders[source]} são lidos.${source === "LINKEDIN" ? "" : " Pode ser a mesma pasta do LinkedIn."}`);
    field.append(label, select, hint);

    const error = el("p", "form-error");
    error.hidden = true;
    const save = el("button", "button primary", "Salvar pasta");
    const cancel = el("button", "button quiet", "Cancelar");
    cancel.type = "button";
    cancel.addEventListener("click", renderAccounts);
    const actions = el("div", "folder-actions");
    actions.append(save, cancel);
    form.append(field, error, actions);

    form.addEventListener("submit", async (event) => {
        event.preventDefault();
        if (select.value === (current ?? "")) return renderAccounts();
        setBusy(form, true);
        error.hidden = true;
        try {
            state.profile = await api.updateFolder(select.value, source);
            toast("Pasta salva.");
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

    const email = account("EMAIL");
    const folder = email ? [folderPicker(email, "LINKEDIN")] : [];

    if (!linkedin) {
        const status = el("p", "account-status", "O Vagora abre cada vaga com a sua conta para ler a descrição completa.");
        card.replaceChildren(accountHead("globe", "LinkedIn", false), status, ...folder, linkedinForm(onLinked));
        return;
    }

    const status = el("p", "account-status");
    status.append(...rich(["Conectado como ", { strong: linkedin.username }, "."]));
    card.replaceChildren(
        accountHead("globe", "LinkedIn", true),
        status,
        ...folder,
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

const CLAUDE_LOGIN_METHODS = [
    ["CLAUDEAI", "Assinatura do Claude (Pro, Max, Team ou Enterprise)"],
    ["CONSOLE", "Anthropic Console, cobrado pelo uso da API"],
    ["SSO", "SSO da empresa"],
];

let claudePoll = null;

async function renderClaudeAccount(refreshProfile = false) {
    clearTimeout(claudePoll);
    const card = $("#account-claude");
    let status;
    try {
        if (refreshProfile) state.profile = await api.getProfile();
        status = await api.claudeStatus();
    } catch (error) {
        card.replaceChildren(accountHead("spark", "Claude", false), el("p", "form-error", messageFor(error)));
        return;
    }

    const claude = account("CLAUDE");
    const head = accountHead("spark", "Claude", Boolean(claude && status.loggedIn));
    const intro = el("p", "account-status", "O Claude analisa as vagas com a sua conta, pelo Claude Code instalado neste computador. Não precisa de chave.");

    if (!status.installed) {
        const command = el("code", "", "curl -fsSL https://claude.ai/install.sh | bash");
        const hint = el("p", "hint");
        hint.append("Instale rodando no terminal ", command, " e depois clique em Verificar de novo.");
        card.replaceChildren(head, intro, hint, claudeButton("Verificar de novo", "quiet", () => renderClaudeAccount()));
        if (claude) card.append(unlinkButton("CLAUDE"));
        return;
    }

    if (status.loggingIn) {
        card.replaceChildren(head, ...claudeLoginInProgress(status));
        if (location.hash === "#config-ia") claudePoll = setTimeout(() => renderClaudeAccount(true), 2000);
        return;
    }

    const error = status.loginError ? [el("p", "form-error", status.loginError)] : [];

    if (claude && status.loggedIn) {
        const text = el("p", "account-status");
        text.append(...rich(["Conectado como ", { strong: claude.username }, status.plan ? ` (plano ${status.plan}).` : "."]));
        const hint = el("p", "hint");
        hint.append("O Claude Code continua logado neste computador. Para sair dele também, rode ", el("code", "", "claude auth logout"), ".");
        card.replaceChildren(head, text, hint, unlinkButton("CLAUDE"));
        return;
    }

    if (claude) {
        const text = el("p", "form-error", "O Claude Code saiu da conta neste computador. Entre de novo para o Claude voltar a analisar as vagas.");
        card.replaceChildren(head, text, ...error, claudeLoginForm(), unlinkButton("CLAUDE"));
        return;
    }

    if (status.loggedIn) {
        const text = el("p", "account-status");
        text.append(...rich(["O Claude Code deste computador já está logado como ", { strong: status.email ?? "uma conta" }, "."]));
        card.replaceChildren(head, intro, text, claudeButton("Usar esta conta", "primary", () => connectClaude(null)));
        return;
    }

    card.replaceChildren(head, intro, ...error, claudeLoginForm());
}

function claudeButton(text, kind, onClick) {
    const button = el("button", `button ${kind}`, text);
    button.type = "button";
    button.addEventListener("click", async () => {
        button.disabled = true;
        await onClick();
        button.disabled = false;
    });
    return button;
}

function claudeLoginForm() {
    const form = el("form", "form");
    const field = el("div", "field");
    const label = el("label", "", "Como entrar");
    const select = el("select");
    select.id = `f-${++uid}`;
    label.htmlFor = select.id;
    select.append(...CLAUDE_LOGIN_METHODS.map(([value, text]) => new Option(text, value)));
    field.append(label, select);
    form.append(field, el("button", "button primary", "Entrar no Claude"));
    form.addEventListener("submit", async (event) => {
        event.preventDefault();
        setBusy(form, true);
        await connectClaude(select.value);
        setBusy(form, false);
    });
    return form;
}

async function connectClaude(method) {
    try {
        state.profile = await api.connectClaude(method);
        if (account("CLAUDE")) {
            toast("Claude conectado.");
            renderAccounts();
            loadStatus();
            return;
        }
        toast("O navegador abriu: entre na sua conta do Claude.");
        renderClaudeAccount();
    } catch (error) {
        toast(messageFor(error));
    }
}

function claudeLoginInProgress(status) {
    const text = el("p", "account-status", "O navegador abriu para você entrar na conta do Claude. Assim que terminar, esta tela atualiza sozinha.");
    const parts = [text];
    if (!status.loginUrl) return parts;

    const hint = el("p", "hint", "Não abriu? Abra o link, entre e cole aqui o código que aparecer no fim.");
    const link = el("a", "button quiet", "Abrir o login do Claude");
    link.href = status.loginUrl;
    link.target = "_blank";
    link.rel = "noopener";

    const form = el("form", "form");
    const field = el("div", "field");
    const label = el("label", "", "Código do Claude");
    const input = el("input");
    input.id = `f-${++uid}`;
    input.autocomplete = "off";
    label.htmlFor = input.id;
    field.append(label, input);
    const error = el("p", "form-error");
    error.hidden = true;
    form.append(field, error, el("button", "button primary", "Enviar código"));
    input.addEventListener("focus", () => clearTimeout(claudePoll));
    form.addEventListener("submit", async (event) => {
        event.preventDefault();
        setBusy(form, true);
        error.hidden = true;
        try {
            await api.sendClaudeCode(input.value);
            renderClaudeAccount(true);
        } catch (e) {
            showError(e, error);
            setBusy(form, false);
        }
    });
    parts.push(hint, link, form);
    return parts;
}

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
            const page = field.closest("[data-page]")?.dataset.page;
            const show = () => {
                field.scrollIntoView({ block: "center" });
                field.focus();
            };
            if (page && location.hash !== `#${page}`) {
                window.addEventListener("hashchange", show, { once: true });
                location.hash = page;
            } else {
                show();
            }
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
    const missing = [
        !state.profile.curriculumName && { name: "Currículo", detail: ["Sem ele não há com o que comparar as vagas."],
            actions: [actionLink("Enviar", "#", { focus: "#profile-curriculum", primary: true })] },
        !hasAiKey() && { name: "IA", detail: ["Nenhuma IA marcada em \"Nota da vaga\" tem chave ou conta. Configure uma chave na aba IA ou marque outra IA em Fluxo da IA."],
            actions: [actionLink("Configurar", "#", { focus: "#profile-gemini", primary: true })] },
        !account("EMAIL") && { name: "Email", detail: ["É de onde vêm os alertas de vaga."],
            actions: [actionLink("Conectar", "#", { focus: "#account-email", primary: true })] },
        !account("LINKEDIN") && { name: "LinkedIn", detail: ["Sem ele o Vagora não lê a descrição das vagas."],
            actions: [actionLink("Conectar", "#", { focus: "#account-linkedin", primary: true })] },
    ].filter(Boolean);

    $(".readiness").hidden = missing.length === 0;
    $("#readiness-title").textContent = `Falta ${missing.length} ${missing.length === 1 ? "item" : "itens"} para analisar vagas`;
    $("#checklist").replaceChildren(...missing.map((item) => checkItem({ ...item, ok: false })));
}

const LLM_NAMES = { GEMINI: "Gemini", GROQ: "Groq", CLAUDE: "Claude", LOCAL: "IA local" };

function llmAvailable(provider) {
    return { GEMINI: state.profile.hasGeminiApiKey, GROQ: state.profile.hasGroqApiKey, CLAUDE: Boolean(account("CLAUDE")), LOCAL: state.profile.hasLocalLlm }[provider];
}

function aiKeysText(profile) {
    const chain = (order) => order.filter(llmAvailable).map((provider) => LLM_NAMES[provider]).join(" → ") || "nenhuma IA disponível";
    return `Nota: ${chain(profile.analysisChain)}. Currículo: ${chain(profile.curriculumChain)}.`;
}

function renderFlow() {
    const form = $("#flow-form");
    clearError(form);
    setBusy(form, false);
    $$(".chain", form).forEach((list) => {
        const chosen = state.profile[list.dataset.chain];
        const others = Object.keys(LLM_NAMES).filter((provider) => !chosen.includes(provider));
        list.replaceChildren(...chosen.map((provider) => chainItem(provider, true)), ...others.map((provider) => chainItem(provider, false)));
        renumberChain(list);
    });
}

function chainItem(provider, used) {
    const name = LLM_NAMES[provider];
    const item = el("li", "chain-item");
    item.dataset.provider = provider;

    const check = el("input");
    check.type = "checkbox";
    check.checked = used;
    check.setAttribute("aria-label", `Usar ${name} nesta etapa`);
    check.addEventListener("change", () => renumberChain(item.parentElement));

    const label = el("span", "chain-label");
    label.append(el("span", "chain-name", name), el("span", "chain-role"));

    item.append(check, label, moveButton(item, -1, `Subir ${name}`, "↑"), moveButton(item, 1, `Descer ${name}`, "↓"));
    if (!llmAvailable(provider)) {
        const missing = el("span", "chain-status");
        const link = el("a", "", "configure na aba IA");
        link.href = "#config-ia";
        missing.append(`${provider === "CLAUDE" ? "Claude não conectado" : "Sem chave"}: fica de fora até você `, link, ".");
        item.append(missing);
    }
    return item;
}

function moveButton(item, direction, label, text) {
    const button = el("button", "chain-move", text);
    button.type = "button";
    button.setAttribute("aria-label", label);
    button.addEventListener("click", () => {
        const list = item.parentElement;
        if (direction < 0) list.insertBefore(item, item.previousElementSibling);
        else list.insertBefore(item.nextElementSibling, item);
        renumberChain(list);
        markDirty(item.closest("form"));
        button.disabled ? $("input", item).focus() : button.focus();
    });
    return button;
}

function renumberChain(list) {
    const items = $$(".chain-item", list);
    let position = 0;
    items.forEach((item, index) => {
        const used = $("input", item).checked;
        item.dataset.role = !used ? "off" : position === 0 ? "primary" : "backup";
        $(".chain-role", item).textContent = !used ? "Fora desta etapa" : position === 0 ? "Principal" : `Reserva ${position}`;
        if (used) position++;
        $$(".chain-move", item)[0].disabled = index === 0;
        $$(".chain-move", item)[1].disabled = index === items.length - 1;
    });
}

$("#flow-form").addEventListener("submit", async (event) => {
    event.preventDefault();
    const form = event.currentTarget;
    clearError(form);
    const data = new FormData();
    $$(".chain", form).forEach((list) => {
        const used = $$(".chain-item", list).filter((item) => $("input", item).checked).map((item) => item.dataset.provider);
        data.append(list.dataset.chain, used.join(","));
    });
    setBusy(form, true);
    try {
        state.profile = await api.updateProfile(data);
        renderFlow();
        markSaved(form);
        $("#ai-summary").textContent = aiKeysText(state.profile);
        toast("Fluxo da IA salvo.");
    } catch (error) {
        showError(error, $(".form-error", form));
        setBusy(form, false);
    }
});

function showCurriculum(name, size, pending) {
    $("#curriculum-file").hidden = !name;
    $("#curriculum-empty").hidden = Boolean(name);
    $("#curriculum-name").textContent = name ?? "";
    $("#curriculum-meta").textContent = [formatSize(size), pending ? "ainda não salvo" : ""].filter(Boolean).join(", ");
    $("#open-curriculum").hidden = pending;
    $("#save-curriculum").hidden = !pending;
}

$("#profile-curriculum").addEventListener("change", (event) => {
    const file = event.target.files[0];
    if (file) showCurriculum(file.name, file.size, true);
    else showCurriculum(state.profile.curriculumName, state.profile.curriculumSizeBytes, false);
});

function initials(name) {
    const parts = (name ?? "").trim().split(/\s+/).filter(Boolean);
    return ((parts[0]?.[0] ?? "?") + (parts.length > 1 ? parts.at(-1)[0] : "")).toUpperCase();
}

function renderProfile(forms = $$(".profile-form")) {
    const profile = state.profile;
    $("#avatar").textContent = initials(profile.name);
    $("#profile-title").textContent = profile.name || "Seu perfil";
    const email = account("EMAIL");
    $("#profile-handle").textContent = email ? `Alertas de ${email.username}` : "Nenhum email conectado ainda";
    $("#ai-summary").textContent = aiKeysText(profile);
    $("#curriculum-pick-label").textContent = profile.curriculumName ? "Trocar currículo" : "Escolher PDF";
    renderReadiness();

    if (forms.some((form) => form.contains($("#profile-curriculum")))) {
        showCurriculum(profile.curriculumName, profile.curriculumSizeBytes, false);
    }
    forms.forEach((form) => {
        form.reset();
        clearError(form);
        setBusy(form, false);
        for (const [name, configured] of [["geminiApiKey", profile.hasGeminiApiKey], ["groqApiKey", profile.hasGroqApiKey], ["localLlmApiKey", profile.hasLocalLlmApiKey]]) {
            const input = form.elements.namedItem(name);
            if (input) input.value = configured ? KEY_MASK : "";
        }
    });
    if (forms.includes($("#profile-form"))) $("#profile-name").value = profile.name ?? "";
    if (forms.some((form) => form.contains($("#profile-local-url")))) {
        $("#profile-local-url").value = profile.localLlmUrl ?? "";
        $("#profile-local-model").value = profile.localLlmModel ?? "";
    }
    $("#local-head").replaceChildren(...accountHead("spark", "IA local", profile.hasLocalLlm, ["Configurada", "Sem endereço"]).children);
    $("#gemini-head").replaceChildren(...accountHead("spark", "Gemini", profile.hasGeminiApiKey, ["Configurada", "Sem chave"]).children);
    $("#groq-head").replaceChildren(...accountHead("spark", "Groq", profile.hasGroqApiKey, ["Configurada", "Sem chave"]).children);
    $("#profile-gemini-hint").replaceChildren(...keyHint(profile.hasGeminiApiKey));
    $("#profile-groq-hint").replaceChildren(...keyHint(profile.hasGroqApiKey));
    if (forms.includes($("#poll-form"))) {
        $("#poll-min").value = profile.pollMinMinutes;
        $("#poll-max").value = profile.pollMaxMinutes;
        renderPollWarning();
    }
}

function renderEditableSettings() {
    TOGGLES.forEach(([key, id]) => { $(id).checked = Boolean(state.profile[key]); });
    renderFlow();
}

const TOGGLES = [["monitoringEnabled", "#monitoring"], ["autoCurriculum", "#auto-curriculum"], ["showBrowser", "#show-browser"]];

$("#toggles-form").addEventListener("submit", async (event) => {
    event.preventDefault();
    const form = event.currentTarget;
    clearError(form);
    const changed = TOGGLES.filter(([key, id]) => $(id).checked !== Boolean(state.profile[key]));
    if (changed.length === 0) {
        clearStatus(saveStatus(form));
        toast("Nada para salvar.");
        return;
    }
    const data = new FormData();
    changed.forEach(([key, id]) => data.append(key, String($(id).checked)));
    setBusy(form, true);
    try {
        state.profile = await api.updateProfile(data);
        markSaved(form);
        toast("Automação salva.");
        renderMonitor();
        renderProcessingAlert();
    } catch (error) {
        showError(error, $(".form-error", form));
    } finally {
        setBusy(form, false);
    }
});

function saveStatus(form) {
    let status = $(".save-status", form);
    if (!status) {
        status = el("p", "save-status");
        status.setAttribute("aria-live", "polite");
        $("button[type=submit]", form).after(status);
    }
    return status;
}

function clearStatus(status) {
    status.textContent = "";
    status.removeAttribute("data-state");
}

function markDirty(form) {
    const status = saveStatus(form);
    status.dataset.state = "dirty";
    status.textContent = "Alterações não salvas";
}

function markSaved(form) {
    const status = saveStatus(form);
    status.dataset.state = "saved";
    status.textContent = `Salvo às ${timeFormat.format(new Date())}`;
}

$$("[data-panel~='config'] form").forEach((form) => {
    form.addEventListener("input", () => markDirty(form));
    form.addEventListener("change", () => markDirty(form));
});

function keyHint(configured) {
    return [configured ? "Para trocar, clique no campo e cole a nova chave." : "Cole aqui a chave criada no botão acima."];
}

function renderPollWarning() {
    const min = Number($("#poll-min").value);
    const box = $("#poll-warning");
    box.hidden = !min || min >= 15;
    if (box.hidden) return;
    box.replaceChildren(
        el("strong", "", min < 10 ? "Risco alto para a sua conta do LinkedIn." : "Risco maior para a sua conta do LinkedIn."),
        ` Com mínimo de ${min} minutos entre os acessos, o LinkedIn é aberto com mais frequência que o padrão e fica mais `
        + "fácil perceber a automação. Se aparecerem verificações de segurança, volte para 15 a 25 minutos.");
}

$("#poll-min").addEventListener("input", renderPollWarning);

for (const [id, configured] of [["#profile-gemini", "hasGeminiApiKey"], ["#profile-groq", "hasGroqApiKey"]]) {
    const input = $(id);
    input.addEventListener("focus", () => { if (input.value === KEY_MASK) input.value = ""; });
    input.addEventListener("blur", () => { if (!input.value && state.profile[configured]) input.value = KEY_MASK; });
}

$$(".profile-form").forEach((form) => form.addEventListener("submit", async (event) => {
    event.preventDefault();
    const form = event.currentTarget;
    clearError(form);

    const data = new FormData();
    const field = form.elements;
    const name = field.namedItem("name")?.value.trim();
    const gemini = field.namedItem("geminiApiKey")?.value.trim();
    const groq = field.namedItem("groqApiKey")?.value.trim();
    const curriculum = field.namedItem("curriculum")?.files[0];
    if (name && name !== state.profile.name) data.append("name", name);
    if (gemini && gemini !== KEY_MASK) data.append("geminiApiKey", gemini);
    if (groq && groq !== KEY_MASK) data.append("groqApiKey", groq);
    if (curriculum) data.append("curriculum", curriculum);
    for (const key of ["localLlmUrl", "localLlmModel"]) {
        const value = field.namedItem(key)?.value.trim();
        if (value !== undefined && value !== (state.profile[key] ?? "")) data.append(key, value);
    }
    const localKey = field.namedItem("localLlmApiKey")?.value.trim();
    if (localKey && localKey !== KEY_MASK) data.append("localLlmApiKey", localKey);
    for (const key of ["pollMinMinutes", "pollMaxMinutes"]) {
        const value = field.namedItem(key)?.value;
        const minutes = Math.round(Number(value));
        if (value && minutes !== state.profile[key]) data.append(key, String(minutes));
    }

    if ([...data.keys()].length === 0) {
        clearStatus(saveStatus(form));
        toast("Nada para salvar.");
        return;
    }

    setBusy(form, true);
    try {
        state.profile = await api.updateProfile(data);
        renderProfile([form]);
        markSaved(form);
        toast("Alterações salvas.");
    } catch (error) {
        showError(error, $(".form-error", form));
        setBusy(form, false);
    }
}));

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

const GUPY_WORKPLACES = [["REMOTE", "Remoto"], ["HYBRID", "Híbrido"], ["ON_SITE", "Presencial"]];
const GUPY_CONTRACTS = [["CLT", "CLT"], ["PJ", "PJ"], ["TEMPORARY", "Temporária"], ["INTERNSHIP", "Estágio"],
    ["TALENT_POOL", "Banco de talentos"], ["OTHER", "Outros"]];
const BRAZIL_STATES = ["Acre", "Alagoas", "Amapá", "Amazonas", "Bahia", "Ceará", "Distrito Federal", "Espírito Santo",
    "Goiás", "Maranhão", "Mato Grosso", "Mato Grosso do Sul", "Minas Gerais", "Pará", "Paraíba", "Paraná", "Pernambuco",
    "Piauí", "Rio de Janeiro", "Rio Grande do Norte", "Rio Grande do Sul", "Rondônia", "Roraima", "Santa Catarina",
    "São Paulo", "Sergipe", "Tocantins"];
const NEW_GUPY_SEARCH = {
    jobName: "", state: null, city: null, everyHours: 4, enabled: true, lastRunAt: null,
    workplaces: ["REMOTE", "HYBRID", "ON_SITE"], contracts: ["CLT", "PJ", "TEMPORARY", "OTHER"],
};
let gupyFieldCount = 0;

async function renderGupyInvite() {
    try {
        $("#gupy-invite").hidden = (await api.listGupySearches()).length > 0;
    } catch {
        $("#gupy-invite").hidden = true;
    }
}

const isGupyLink = (link) => /^https:\/\/[^/]+\.gupy\.io\//.test(link ?? "");

async function loadGupyActivity() {
    try {
        const [activities, queue, matches] = await Promise.all([api.scraper(), api.listPending(), api.listMatches()]);
        renderGupyActivity(activities.GUPY, queue.jobs.filter((job) => job.source === "GUPY"),
            matches.filter((match) => isGupyLink(match.jobLink)));
    } catch (error) {
        showError(error, $("#gupy-error"));
    }
}

function gupyJobItem(title, company, link, badge, badgeState) {
    const item = el("li", "gupy-job");
    const anchor = el("a", "gupy-job-title", title ?? "Vaga sem título");
    anchor.href = link;
    anchor.target = "_blank";
    anchor.rel = "noopener";
    const text = el("span", "gupy-job-text");
    text.append(anchor, el("span", "gupy-job-company", company ?? ""));
    const tag = el("span", "gupy-job-badge", badge);
    tag.dataset.state = badgeState;
    item.append(text, tag);
    return item;
}

function renderGupyActivity(activity, queue, analyzed) {
    const analyzing = activity.analyzing ? activity.analysisTotal : 0;
    let view;
    if (analyzing) {
        view = { state: "busy", title: "Analisando com a IA",
            detail: `Comparando ${analyzing} ${analyzing === 1 ? "vaga" : "vagas"} da Gupy com o seu currículo.` };
    } else if (activity.failure) {
        view = { state: "error", title: "Parou com um problema", detail: `${activity.failure} Vai tentar de novo no próximo ciclo.` };
    } else if (!state.profile.monitoringEnabled) {
        view = { state: "paused", title: "Monitoramento pausado", detail: "Nenhuma busca roda até você retomar." };
    } else if (queue.length) {
        view = { state: "ok", title: "Vagas na fila",
            detail: `${queue.length} ${queue.length === 1 ? "vaga da Gupy espera" : "vagas da Gupy esperam"} a vez da IA.` };
    } else if (activity.events.length) {
        view = { state: "ok", title: "Em dia", detail: `Último passo: ${activity.events[0].message}.` };
    } else {
        view = { state: "idle", title: "Nenhuma busca rodou ainda", detail: "Crie uma busca abaixo. A primeira roda em até 10 minutos." };
    }
    $("#gupy-now").dataset.state = view.state;
    $("#gupy-now-title").textContent = view.title;
    $("#gupy-now-detail").textContent = view.detail;

    $("#gupy-queue-title").textContent = queue.length ? `Na fila (${queue.length})` : "Na fila";
    $("#gupy-queue").replaceChildren(...queue.slice(0, 8).map((job, index) => index < analyzing
        ? gupyJobItem(job.jobTitle, job.companyName, job.jobLink, "Analisando", "busy")
        : gupyJobItem(job.jobTitle, job.companyName, job.jobLink, "Na fila", "idle")));
    $("#gupy-queue-empty").hidden = queue.length > 0;
    $("#gupy-queue-more").hidden = queue.length <= 8;
    $("#gupy-queue-more").textContent = `Mais ${queue.length - 8} na fila completa`;

    const recent = [...analyzed].sort((a, b) => b.createdAt.localeCompare(a.createdAt)).slice(0, 8);
    const fits = analyzed.filter((match) => match.match).length;
    $("#gupy-analyzed-title").textContent = analyzed.length
        ? `Analisadas (${analyzed.length}, ${fits} ${fits === 1 ? "compatível" : "compatíveis"})`
        : "Analisadas";
    $("#gupy-analyzed").replaceChildren(...recent.map((match) =>
        gupyJobItem(match.jobTitle, match.companyName, match.jobLink, String(match.score), match.match ? "fit" : "miss")));
    $("#gupy-analyzed-empty").hidden = analyzed.length > 0;
    $("#gupy-all").hidden = analyzed.length <= recent.length;

    $("#gupy-history").hidden = activity.events.length === 0;
    $("#gupy-events").replaceChildren(...activity.events.slice(0, 15).map((event) => {
        const li = el("li", event.problem ? "is-problem" : "");
        li.append(el("time", "", dateFormat.format(new Date(event.at))), el("span", "", event.message));
        return li;
    }));
}

async function renderGupy() {
    try {
        const searches = await api.listGupySearches();
        $("#gupy-searches").replaceChildren(...searches.map(gupyForm));
        $("#gupy-error").hidden = true;
    } catch (error) {
        showError(error, $("#gupy-error"));
    }
}

function gupyField(labelText, input) {
    const field = el("div", "field");
    const label = el("label", "", labelText);
    input.id = `gupy-field-${++gupyFieldCount}`;
    label.htmlFor = input.id;
    field.append(label, input);
    return field;
}

function gupyChoices(legend, name, options, selected) {
    const set = el("fieldset", "choices");
    set.append(el("legend", "", legend));
    for (const [value, label] of options) {
        const item = el("label", "check");
        const box = el("input");
        box.type = "checkbox";
        box.name = name;
        box.value = value;
        box.checked = selected.includes(value);
        item.append(box, el("span", "", label));
        set.append(item);
    }
    return set;
}

function gupySwitch(search) {
    const row = el("div", "toggle-row");
    const text = el("div");
    text.append(el("h3", "toggle-title", search.jobName || "Nova busca"),
        el("p", "hint", search.lastRunAt ? `Última busca ${timeAgo(search.lastRunAt)}.` : "Ainda não buscou."));
    const toggle = el("label", "switch");
    const box = el("input");
    box.type = "checkbox";
    box.name = "enabled";
    box.checked = search.enabled;
    const track = el("span", "switch-track");
    track.setAttribute("aria-hidden", "true");
    track.append(el("span", "switch-thumb"));
    toggle.append(box, track, el("span", "visually-hidden", "Busca ativa"));
    row.append(text, toggle);
    return row;
}

function gupyForm(search) {
    const form = el("form", "panel form gupy-search");
    form.noValidate = true;
    if (search.id) form.dataset.id = search.id;

    const jobName = el("input");
    jobName.value = search.jobName;
    jobName.placeholder = "Ex.: Desenvolvedor Java";
    jobName.autocomplete = "off";
    const state = el("select");
    state.append(new Option("Qualquer estado", ""), ...BRAZIL_STATES.map((name) => new Option(name, name, false, name === search.state)));
    const city = el("input");
    city.value = search.city ?? "";
    city.placeholder = "Qualquer cidade";
    const every = el("input");
    every.type = "number";
    every.min = "1";
    every.step = "1";
    every.inputMode = "numeric";
    every.value = String(search.everyHours);

    const fields = el("div", "gupy-fields");
    fields.append(gupyField("Cargo", jobName), gupyField("Estado", state), gupyField("Cidade", city), gupyField("Buscar a cada (horas)", every));
    const error = el("p", "form-error");
    error.setAttribute("role", "alert");
    error.hidden = true;
    const save = el("button", "button primary", "Salvar busca");
    save.type = "submit";
    const remove = el("button", "button quiet", "Excluir busca");
    remove.type = "button";
    const actions = el("div", "gupy-actions");
    actions.append(save, remove);
    form.append(gupySwitch(search), fields,
        gupyChoices("Modalidade", "workplaces", GUPY_WORKPLACES, search.workplaces),
        gupyChoices("Tipo de vaga", "contracts", GUPY_CONTRACTS, search.contracts), error, actions);

    form.addEventListener("input", () => markDirty(form));
    form.addEventListener("change", () => markDirty(form));
    form.addEventListener("submit", async (event) => {
        event.preventDefault();
        clearError(form);
        save.disabled = true;
        const body = {
            jobName: jobName.value,
            state: state.value || null,
            city: city.value.trim() || null,
            everyHours: Number(every.value),
            enabled: $("input[name=enabled]", form).checked,
            workplaces: $$("input[name=workplaces]:checked", form).map((box) => box.value),
            contracts: $$("input[name=contracts]:checked", form).map((box) => box.value),
        };
        try {
            const saved = form.dataset.id
                ? await api.updateGupySearch(form.dataset.id, body)
                : await api.createGupySearch(body);
            form.dataset.id = saved.id;
            $(".toggle-title", form).textContent = saved.jobName;
            markSaved(form);
        } catch (failure) {
            showError(failure, error);
        } finally {
            save.disabled = false;
        }
    });
    remove.addEventListener("click", async () => {
        if (!form.dataset.id) {
            form.remove();
            return;
        }
        remove.disabled = true;
        try {
            await api.deleteGupySearch(form.dataset.id);
            form.remove();
            toast("Busca excluída.");
        } catch (failure) {
            showError(failure, error);
            remove.disabled = false;
        }
    });
    return form;
}

$("#gupy-add").addEventListener("click", () => {
    const form = gupyForm(NEW_GUPY_SEARCH);
    $("#gupy-searches").append(form);
    markDirty(form);
    $("input", $(".gupy-fields", form)).focus();
});

$$(".tip").forEach(wireTip);
boot();
