const heroCard = document.getElementById("heroCard");
const statusLabel = document.getElementById("statusLabel");
const mainTitle = document.getElementById("mainTitle");
const mainDescription = document.getElementById("mainDescription");
const mainInstruction = document.getElementById("mainInstruction");
const liveClock = document.getElementById("liveClock");
const confettiLayer = document.getElementById("confettiLayer");
const holidayPanel = document.getElementById("holidayPanel");
const holidayLabel = document.getElementById("holidayLabel");
const holidayDetails = document.getElementById("holidayDetails");
const orefConnection = document.getElementById("orefConnection");
const connectionLabel = document.getElementById("connectionLabel");
const trafficLight = document.getElementById("trafficLight");
const trafficLightLabel = document.getElementById("trafficLightLabel");
const alertTimer = document.getElementById("alertTimer");
const alertTimerValue = document.getElementById("alertTimerValue");
const lastPoll = document.getElementById("lastPoll");
const activeUntil = document.getElementById("activeUntil");
const lastAlertElapsed = document.getElementById("lastAlertElapsed");
const lastAlertElapsedKiosk = document.getElementById("lastAlertElapsedKiosk");
const historySummary = document.getElementById("historySummary");
const historyUpdatedAt = document.getElementById("historyUpdatedAt");
const hourChart = document.getElementById("hourChart");
const historyFeed = document.getElementById("historyFeed");
const sourceMessage = document.getElementById("sourceMessage");
const demoButton = document.getElementById("demoButton");
const clearButton = document.getElementById("clearButton");
const isKiosk = document.querySelector(".app-shell")?.dataset.kiosk === "true";

let lastAlertKey = null;
let audioUnlocked = false;
let lastMessageTimestamp = null;
let latestSnapshot = null;
let localServerFailureCount = 0;
let lastTrafficLightState = null;
let lastSpokenAlertKey = null;
const RED_ALERT_COUNTDOWN_MS = 12 * 60 * 1000;

function updateLastMessageElapsed(value) {
    const formatted = formatElapsed(value);
    if (lastAlertElapsed) {
        lastAlertElapsed.textContent = formatted;
    }
    if (lastAlertElapsedKiosk) {
        lastAlertElapsedKiosk.textContent = formatted;
    }
}

function formatCountdown(value) {
    const totalSeconds = Math.max(0, Math.floor(value / 1000));
    const minutes = Math.floor(totalSeconds / 60);
    const seconds = totalSeconds % 60;
    return `${String(minutes).padStart(2, "0")}:${String(seconds).padStart(2, "0")}`;
}

function updateAlertTimer(snapshot = latestSnapshot) {
    if (!alertTimer || !alertTimerValue) {
        return;
    }

    const isRed = String(snapshot?.trafficLightState || "").toUpperCase() === "RED";
    const timerStartedAt = snapshot?.lastAlertActivatedAt || snapshot?.alertReceivedAt;
    if (!isRed || !timerStartedAt) {
        alertTimer.classList.remove("active", "warning", "critical");
        return;
    }

    const endsAt = new Date(timerStartedAt).getTime() + RED_ALERT_COUNTDOWN_MS;
    const remainingMs = endsAt - Date.now();
    alertTimer.classList.add("active");
    alertTimer.classList.toggle("warning", remainingMs > 60000 && remainingMs <= 5 * 60 * 1000);
    alertTimer.classList.toggle("critical", remainingMs <= 60000);
    alertTimerValue.textContent = formatCountdown(remainingMs);
}

function updateEmergencyMode(snapshot) {
    const isFullEmergency = isKiosk && String(snapshot?.trafficLightState || "").toUpperCase() === "RED";
    document.body.classList.toggle("full-alert-mode", isFullEmergency);
}

function speakAlert(snapshot, trafficState, alertKey) {
    if (!("speechSynthesis" in window) || !alertKey || alertKey === lastSpokenAlertKey) {
        return;
    }

    const utterance = new SpeechSynthesisUtterance();
    utterance.lang = "he-IL";
    utterance.rate = 0.95;
    utterance.pitch = 1;

    if (trafficState === "RED") {
        utterance.text = `${snapshot.title}. ${snapshot.city}. ${snapshot.instruction || snapshot.description || "יש להיכנס למרחב מוגן."}`;
    } else if (trafficState === "YELLOW") {
        utterance.text = `${snapshot.title || "התרעה מוקדמת"}. ${snapshot.city}. ${snapshot.instruction || snapshot.description || "יש לשהות בקרבת מרחב מוגן."}`;
    } else {
        return;
    }

    window.speechSynthesis.cancel();
    window.speechSynthesis.speak(utterance);
    lastSpokenAlertKey = alertKey;
}

const HOLIDAY_WINDOWS = [
    {
        name: "שבת ויקרא",
        start: "2026-03-20T17:31:00+02:00",
        end: "2026-03-21T18:47:00+02:00",
        candles: "17:31",
        endsAt: "18:47",
        hebrewDate: "א' בניסן",
        portion: "ויקרא (החודש)"
    },
    {
        name: "שבת צו",
        start: "2026-03-27T18:35:00+02:00",
        end: "2026-03-28T19:51:00+02:00",
        candles: "18:35",
        endsAt: "19:51",
        hebrewDate: "ח' בניסן",
        portion: "צו (הגדול)"
    },
    {
        name: "ערב פסח",
        start: "2026-04-02T18:39:00+03:00",
        end: "2026-04-03T18:40:00+03:00",
        candles: "18:39",
        endsAt: "-",
        hebrewDate: "י\"ד בניסן",
        portion: "-"
    },
    {
        name: "שבת פסח",
        start: "2026-04-03T18:40:00+03:00",
        end: "2026-04-04T19:57:00+03:00",
        candles: "18:40",
        endsAt: "19:57",
        hebrewDate: "ט\"ו-ט\"ז בניסן",
        portion: "חול המועד פסח"
    }
];

function getCurrentHolidayWindow() {
    const now = Date.now();
    return HOLIDAY_WINDOWS.find(windowDef => {
        const start = new Date(windowDef.start).getTime();
        const end = new Date(windowDef.end).getTime();
        return now >= start && now <= end;
    }) || null;
}

function renderHolidayMode() {
    const activeWindow = getCurrentHolidayWindow();
    document.body.classList.toggle("holiday-mode", Boolean(activeWindow) && isKiosk && !Boolean(latestSnapshot?.active));

    if (!holidayPanel || !holidayLabel) {
        return;
    }

    holidayPanel.classList.toggle("active", Boolean(activeWindow));
    holidayLabel.textContent = activeWindow
        ? `${activeWindow.name} · ${activeWindow.hebrewDate}`
        : "";
    if (holidayDetails) {
        holidayDetails.textContent = activeWindow
            ? `פרשה: ${activeWindow.portion} | הדלקת נרות: ${activeWindow.candles} | צאת שבת/חג: ${activeWindow.endsAt}`
            : "";
    }
}

function isNightHours() {
    const currentHour = new Date().getHours();
    return currentHour >= 20 || currentHour < 8;
}

function syncNightMode() {
    if (!isKiosk) {
        document.body.classList.remove("night-mode");
        return;
    }

    const shouldUseNightMode = isNightHours() && !Boolean(latestSnapshot?.active);
    document.body.classList.toggle("night-mode", shouldUseNightMode);
}

function renderClock() {
    if (!liveClock) {
        return;
    }

    liveClock.textContent = new Date().toLocaleTimeString("he-IL", {
        hour: "2-digit",
        minute: "2-digit",
        second: "2-digit"
    });
    updateLastMessageElapsed(lastMessageTimestamp || latestSnapshot?.lastAlertActivatedAt);
    updateAlertTimer();
    renderHolidayMode();
    syncNightMode();
}

function formatDate(value) {
    if (!value || value === "1970-01-01T00:00:00Z") {
        return "-";
    }

    return new Date(value).toLocaleString("he-IL", {
        dateStyle: "short",
        timeStyle: "medium"
    });
}

function renderConnectionStatus(snapshot) {
    if (!orefConnection || !connectionLabel) {
        return;
    }

    orefConnection.classList.remove("state-live", "state-offline", "state-starting", "state-demo");
    const state = String(snapshot.sourceStatus || "starting").toLowerCase();
    orefConnection.classList.add(`state-${state}`);

    if (state === "live") {
        connectionLabel.textContent = "מקור הנתונים זמין";
    } else if (state === "offline") {
        connectionLabel.textContent = "מקור הנתונים לא זמין";
    } else if (state === "demo") {
        connectionLabel.textContent = "מצב הדמיה מקומי";
    } else {
        connectionLabel.textContent = "בודק מקור נתונים";
    }
}

function renderLocalServerError() {
    if (orefConnection && connectionLabel) {
        orefConnection.classList.remove("state-live", "state-starting", "state-demo");
        orefConnection.classList.add("state-offline");
        connectionLabel.textContent = "אין חיבור לשרת המקומי";
    }
}

function formatElapsed(value) {
    if (!value) {
        return "-";
    }

    const seconds = Math.max(0, Math.floor((Date.now() - new Date(value).getTime()) / 1000));
    const days = Math.floor(seconds / 86400);
    const hours = Math.floor((seconds % 86400) / 3600);
    const minutes = Math.floor((seconds % 3600) / 60);
    const remainingSeconds = seconds % 60;

    if (days > 0) {
        return `${days} ימים ו-${hours} שעות`;
    }
    if (hours > 0) {
        return `${hours} שעות ו-${minutes} דקות`;
    }
    if (minutes > 0) {
        return `${minutes} דקות ו-${remainingSeconds} שניות`;
    }
    return `${remainingSeconds} שניות`;
}

function playToneSequence(steps, duration = 0.18) {
    if (!audioUnlocked) {
        return;
    }

    const context = new (window.AudioContext || window.webkitAudioContext)();
    const gain = context.createGain();
    gain.connect(context.destination);
    gain.gain.setValueAtTime(0.001, context.currentTime);

    steps.forEach((frequency, index) => {
        const startAt = context.currentTime + index * duration;
        const oscillator = context.createOscillator();
        oscillator.type = "sawtooth";
        oscillator.frequency.setValueAtTime(frequency, startAt);
        oscillator.connect(gain);
        oscillator.start(startAt);
        oscillator.stop(startAt + duration);
    });

    gain.gain.exponentialRampToValueAtTime(0.14, context.currentTime + 0.04);
    gain.gain.exponentialRampToValueAtTime(0.001, context.currentTime + steps.length * duration + 0.08);
    setTimeout(() => context.close().catch(() => {}), (steps.length * duration + 0.2) * 1000);
}

function playStateSound(state) {
    if (state === "RED") {
        playToneSequence([660, 880, 520], 0.32);
    } else if (state === "YELLOW") {
        playToneSequence([720, 640], 0.22);
    } else if (state === "GREEN") {
        playToneSequence([520, 660, 880], 0.16);
    }
}

function launchConfetti() {
    if (!confettiLayer) {
        return;
    }

    const colors = ["#f0b429", "#d92d20", "#1f9d55", "#ffffff", "#ff8a65"];
    confettiLayer.replaceChildren();

    for (let i = 0; i < 36; i += 1) {
        const piece = document.createElement("span");
        piece.className = "confetti-piece";
        piece.style.left = `${Math.random() * 100}%`;
        piece.style.background = colors[i % colors.length];
        piece.style.setProperty("--drift", `${(Math.random() - 0.5) * 240}px`);
        piece.style.animationDelay = `${Math.random() * 0.3}s`;
        piece.style.transform = `translateY(0) rotate(${Math.random() * 180}deg)`;
        confettiLayer.appendChild(piece);
    }

    setTimeout(() => confettiLayer.replaceChildren(), 3200);
}

async function unlockAudio() {
    if (audioUnlocked) {
        return;
    }

    try {
        const context = new (window.AudioContext || window.webkitAudioContext)();
        await context.resume();
        audioUnlocked = true;
        await context.close();
    } catch (error) {
        console.error("Audio unlock failed", error);
    }
}

function render(snapshot) {
    latestSnapshot = snapshot;
    const isActive = Boolean(snapshot.active);
    heroCard.classList.toggle("alert-active", isActive);
    heroCard.classList.toggle("no-alert", !isActive);
    renderConnectionStatus(snapshot);
    syncNightMode();
    const trafficState = String(snapshot.trafficLightState || "GREEN").toUpperCase();
    updateEmergencyMode(snapshot);
    trafficLight.classList.remove("state-red", "state-yellow", "state-green");
    trafficLight.classList.add(`state-${trafficState.toLowerCase()}`);
    trafficLightLabel.textContent = snapshot.trafficLightLabel || "";
    statusLabel.textContent = isActive
        ? (snapshot.demoMode ? "הדמיה פעילה" : "התרעה פעילה")
        : "מעקב שוטף";
    mainTitle.textContent = isActive
        ? `${snapshot.title} ב${snapshot.city}`
        : `אין כרגע התרעה ב${snapshot.city}`;
    mainDescription.textContent = snapshot.description;
    mainInstruction.textContent = snapshot.instruction;
    if (lastPoll) {
        lastPoll.textContent = formatDate(snapshot.lastSuccessfulPollAt);
    }
    if (activeUntil) {
        activeUntil.textContent = formatDate(snapshot.activeUntil);
    }
    updateLastMessageElapsed(lastMessageTimestamp || snapshot.lastAlertActivatedAt);
    updateAlertTimer(snapshot);
    if (sourceMessage) {
        sourceMessage.textContent = snapshot.sourceMessage || "";
    }

    const currentAlertKey = trafficState !== "GREEN"
        ? `${trafficState}:${snapshot.title}:${snapshot.alertReceivedAt}:${snapshot.demoMode}`
        : null;
    if (currentAlertKey && currentAlertKey !== lastAlertKey) {
        playStateSound(trafficState);
    } else if (trafficState !== lastTrafficLightState && trafficState !== "GREEN") {
        playStateSound(trafficState);
    } else if (trafficState === "GREEN" && lastTrafficLightState && lastTrafficLightState !== "GREEN") {
        playStateSound("GREEN");
        launchConfetti();
    }
    speakAlert(snapshot, trafficState, currentAlertKey);
    if (trafficState === "GREEN") {
        window.speechSynthesis?.cancel();
        lastSpokenAlertKey = null;
    }
    lastAlertKey = currentAlertKey;
    lastTrafficLightState = trafficState;
}

async function refresh() {
    try {
        const response = await fetch("/api/alerts/current", { cache: "no-store" });
        const snapshot = await response.json();
        localServerFailureCount = 0;
        render(snapshot);
    } catch (error) {
        localServerFailureCount += 1;
        statusLabel.textContent = "נתק מהשרת המקומי";
        if (sourceMessage) {
            sourceMessage.textContent = "לא ניתן היה למשוך מצב עדכני מהשרת המקומי.";
        }
        renderLocalServerError();
        console.error(error);
    }
}

function tryRecoverLocalServer() {
    if (!isKiosk) {
        return;
    }

    if (localServerFailureCount >= 3) {
        window.location.reload();
    }
}

function renderHistory(snapshot) {
    historySummary.textContent = `סה״כ ${snapshot.totalAlerts || 0} התרעות ו-${snapshot.totalListItems || 0} רשומות היסטוריות ב${snapshot.city}`;
    historyUpdatedAt.textContent = `עודכן: ${formatDate(snapshot.lastUpdatedAt)}`;
    lastMessageTimestamp = snapshot.recentAlerts?.length ? snapshot.recentAlerts[0].alertDate : lastMessageTimestamp;
    updateLastMessageElapsed(lastMessageTimestamp);

    if (!historySummary || !historyUpdatedAt || !hourChart || !historyFeed) {
        return;
    }

    const maxCount = Math.max(...(snapshot.hourlyDistribution || []).map(item => item.count), 1);
    hourChart.innerHTML = (snapshot.hourlyDistribution || []).map(item => {
        const height = item.count === 0 ? 8 : Math.max(18, Math.round((item.count / maxCount) * 150));
        return `
            <div class="chart-bar" title="${item.label} - ${item.count}">
                <span class="chart-value">${item.count}</span>
                <div class="chart-column" style="height:${height}px"></div>
                <span class="chart-label">${item.label.slice(0, 2)}</span>
            </div>
        `;
    }).join("");

    if (!snapshot.recentAlerts?.length) {
        historyFeed.innerHTML = '<div class="history-empty">לא נמצאו התרעות היסטוריות לרעננה במקור הנתונים הנוכחי.</div>';
        return;
    }

    historyFeed.innerHTML = snapshot.recentAlerts.map(item => `
        <article class="history-item">
            <strong>${item.title}</strong>
            <time>${formatDate(item.alertDate)}</time>
            <span>${item.location}</span>
        </article>
    `).join("");
}

async function refreshHistory() {
    try {
        const response = await fetch("/api/alerts/history", { cache: "no-store" });
        const snapshot = await response.json();
        renderHistory(snapshot);
    } catch (error) {
        historySummary.textContent = "לא ניתן לטעון כרגע את היסטוריית ההתרעות.";
        historyUpdatedAt.textContent = "-";
        historyFeed.innerHTML = '<div class="history-empty">כשל בטעינת ההיסטוריה.</div>';
        console.error(error);
    }
}

async function triggerDemo() {
    await unlockAudio();
    await fetch("/api/alerts/demo", {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({
            title: "ירי רקטות וטילים",
            description: "היכנסו למרחב המוגן ושהו בו 10 דקות",
            secondsToLive: 180
        })
    });
    await refresh();
}

async function clearDemo() {
    await fetch("/api/alerts/demo/clear", { method: "POST" });
    await refresh();
}

document.addEventListener("click", unlockAudio, { once: true });
if (demoButton) {
    demoButton.addEventListener("click", triggerDemo);
}
if (clearButton) {
    clearButton.addEventListener("click", clearDemo);
}

renderClock();
refresh();
refreshHistory();
setInterval(renderClock, 1000);
setInterval(refresh, 4000);
setInterval(refreshHistory, 60000);
setInterval(tryRecoverLocalServer, 15000);
