package dev.imhamid.period.ui

import android.app.Application
import android.net.ConnectivityManager
import android.net.Network
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import dev.imhamid.period.data.Api
import dev.imhamid.period.data.ApiException
import dev.imhamid.period.data.Repo
import dev.imhamid.period.data.SessionStore
import dev.imhamid.period.data.DayLog
import dev.imhamid.period.data.PairRow
import dev.imhamid.period.data.Offline
import dev.imhamid.period.data.PendingOp
import dev.imhamid.period.data.patched
import dev.imhamid.period.data.Snapshot
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import dev.imhamid.period.notify.SyncWorker
import dev.imhamid.period.update.UpdateState
import dev.imhamid.period.update.Updater
import kotlinx.coroutines.launch
import kotlinx.serialization.json.JsonObjectBuilder
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import java.time.LocalDate

enum class Route { Boot, Intro, Auth, Name, Role, Setup, Home }

class AppVM(private val app: Application) : AndroidViewModel(app) {
    val store = SessionStore(app)
    private val repo = Repo(store)

    var booting by mutableStateOf(true)
    var signedIn by mutableStateOf(store.signedIn)
    var introSeen by mutableStateOf(store.introSeen)
    var snap by mutableStateOf<Snapshot?>(null)
    var busy by mutableStateOf(false)
    var error by mutableStateOf<String?>(null)

    /** Set for a few seconds after she removes a logged day, so the snackbar can offer "undo". */
    var undoDay by mutableStateOf<LocalDate?>(null)
    private var undoJob: Job? = null
    var refreshing by mutableStateOf(false)
    var update by mutableStateOf<UpdateState>(UpdateState.None)
    var lockEnabled by mutableStateOf(store.lockEnabled)
    var themeMode by mutableStateOf(store.themeMode)
    var savedMessages by mutableStateOf(store.savedMessages)
    var accent by mutableIntStateOf(store.accent)
    var pairLabels by mutableStateOf(store.pairLabels)

    /** True while the server cannot be reached; everything keeps working from the saved copy. */
    var offline by mutableStateOf(false)

    /** Changes waiting to be sent to the server. */
    var pendingChanges by mutableIntStateOf(repoPending())
    private var netCallback: ConnectivityManager.NetworkCallback? = null

    private val dayJobs = mutableMapOf<LocalDate, Job>()

    val route: Route
        get() {
            if (booting) return Route.Boot
            if (!signedIn) return if (!introSeen) Route.Intro else Route.Auth
            val s = snap ?: return Route.Boot
            val me = s.me
            return when {
                me.name.isNullOrBlank() -> Route.Name
                me.role == null -> Route.Role
                me.role == "her" && !me.onboarded -> Route.Setup
                else -> Route.Home
            }
        }

    private fun repoPending(): Int = Repo(store).pendingCount()

    init {
        // open instantly from the saved copy, then refresh in the background
        if (signedIn) Offline.load(store)?.let { snap = it; booting = false }
        watchNetwork()
        viewModelScope.launch {
            if (signedIn) {
                load()
                WorkManager.getInstance(app).enqueue(OneTimeWorkRequestBuilder<SyncWorker>().build())
            }
            booting = false
            checkUpdate()
        }
    }

    private fun fail(e: Exception) {
        if (e is ApiException) {
            if (e.code == 401 && !store.signedIn) logoutLocal() else error = e.message
        } else error = e.message ?: "خطای ناشناخته"
    }

    private fun logoutLocal() {
        snap = null
        signedIn = false
        offline = false
        pendingChanges = 0
    }

    /** Sends queued changes, then refreshes everything. Offline: keeps the saved copy. */
    suspend fun load() {
        try {
            val r = repo.flush()
            pendingChanges = repo.pendingCount()
            r.dropped?.let { fail(it) }
            if (r.blocked != null) {
                offline = r.blocked.code == -1
                if (snap == null) snap = Offline.load(store)
                return
            }
            val s = repo.snapshot()
            snap = s
            offline = false
            Offline.save(store, s)
        } catch (e: Exception) {
            if (e is ApiException && e.code == -1) offline = true else fail(e)
            if (snap == null) snap = Offline.load(store)
        }
    }

    /** Queue a change, update the saved copy, and try to send it. */
    private fun queue(op: PendingOp) {
        repo.enqueue(op)
        pendingChanges = repo.pendingCount()
        snap?.let { Offline.save(store, it) }
    }

    private suspend fun flushNow() {
        try {
            val r = repo.flush()
            pendingChanges = repo.pendingCount()
            if (r.blocked == null) offline = false else if (r.blocked.code == -1) offline = true
            if (r.dropped != null) { fail(r.dropped); load() }
        } catch (e: Exception) {
            fail(e)
        }
    }

    private fun watchNetwork() {
        val cm = app.getSystemService(ConnectivityManager::class.java) ?: return
        val cb = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) {
                if (store.signedIn) viewModelScope.launch { load() }
            }
        }
        runCatching { cm.registerDefaultNetworkCallback(cb) }.onSuccess { netCallback = cb }
    }

    override fun onCleared() {
        netCallback?.let { cb -> runCatching { app.getSystemService(ConnectivityManager::class.java)?.unregisterNetworkCallback(cb) } }
        super.onCleared()
    }

    fun refresh() = viewModelScope.launch { load() }

    /** Pull-to-refresh: same as [refresh] but exposes the in-progress state for the indicator. */
    fun pullRefresh() {
        viewModelScope.launch {
            refreshing = true
            try { load() } finally { refreshing = false }
        }
    }

    // ------------------------------------------------------------------ intro / auth
    fun finishIntro() {
        store.introSeen = true
        introSeen = true
    }

    fun submitAuth(email: String, password: String) {
        if (email.isBlank() || password.isBlank()) { error = "ایمیل و رمز رو وارد کن"; return }
        viewModelScope.launch {
            busy = true
            try {
                try {
                    repo.signIn(email.trim(), password)
                } catch (e: ApiException) {
                    if (e.message != dev.imhamid.period.data.ERR_INVALID) throw e
                    // unknown email -> create the account; known email -> it was a wrong password
                    try {
                        repo.signUp(email.trim(), password)
                    } catch (e2: ApiException) {
                        if (e2.message == dev.imhamid.period.data.ERR_EXISTS) throw ApiException(400, "رمز عبور اشتباهه") else throw e2
                    }
                }
                signedIn = true
                load()
                WorkManager.getInstance(app).enqueue(OneTimeWorkRequestBuilder<SyncWorker>().build())
            } catch (e: Exception) {
                fail(e)
            } finally {
                busy = false
            }
        }
    }

    fun logout() {
        repo.signOut()
        logoutLocal()
    }

    // ------------------------------------------------------------------ profile
    fun patch(block: JsonObjectBuilder.() -> Unit) {
        val s = snap ?: return
        val o = buildJsonObject(block)
        snap = s.withMe(s.me.patched(o))
        queue(PendingOp("profile", payload = o.toString()))
        viewModelScope.launch {
            flushNow()
            if (pendingChanges == 0 && !offline) load()
        }
    }

    fun saveName(name: String) {
        if (name.isBlank()) { error = "اسمت رو بنویس"; return }
        patch { put("name", name.trim()) }
    }

    fun saveRole(role: String) = patch { put("role", role) }

    fun saveSetup(
        lastStart: LocalDate, earlier: List<LocalDate>, cycle: Int, period: Int,
        pms: Int, regularity: Int, hormonal: Boolean,
    ) {
        val s = snap ?: return
        val today = LocalDate.now()
        val days = (earlier + lastStart)
            .flatMap { st -> (0 until period).map { st.plusDays(it.toLong()) } }
            .filter { !it.isAfter(today) }
            .distinct()
        val o = buildJsonObject {
            put("cycle_length", cycle)
            put("period_length", period)
            put("pms_days", pms)
            put("regularity", regularity)
            put("hormonal", hormonal)
            put("onboarded", true)
        }
        snap = s.with(logs = s.logs + days).withMe(s.me.patched(o))
        queue(PendingOp("days", days = days.map { it.toString() }))
        queue(PendingOp("profile", payload = o.toString()))
        viewModelScope.launch {
            busy = true
            try {
                flushNow()
                if (pendingChanges == 0 && !offline) load()
                WorkManager.getInstance(app).enqueue(OneTimeWorkRequestBuilder<SyncWorker>().build())
            } finally {
                busy = false
            }
        }
    }

    // ------------------------------------------------------------------ logging
    fun toggleDay(d: LocalDate) {
        val s = snap ?: return
        if (s.me.role != "her" || d.isAfter(LocalDate.now())) return
        val on = d !in s.logs
        val newLogs = if (on) s.logs + d else s.logs - d
        snap = s.with(logs = newLogs)
        queue(PendingOp("day", day = d.toString(), on = on))
        viewModelScope.launch { flushNow() }
        undoJob?.cancel()
        if (on) {
            if (undoDay == d) undoDay = null
        } else {
            undoDay = d
            undoJob = viewModelScope.launch { delay(5000); undoDay = null }
        }
    }

    fun undoRemove() {
        val d = undoDay ?: return
        undoDay = null
        undoJob?.cancel()
        toggleDay(d)
    }

    // ------------------------------------------------------------------ mood / symptoms
    fun saveDay(day: LocalDate, mood: Int?, energy: Int?, symptoms: List<String>) {
        val s = snap ?: return
        if (s.me.role != "her") return
        val dl = DayLog(s.me.id, day.toString(), mood, energy, symptoms)
        snap = s.with(days = s.days + (day to dl))
        queue(PendingOp("daylog", day = day.toString(), payload = dev.imhamid.period.data.Api.json.encodeToString(DayLog.serializer(), dl)))
        dayJobs[day]?.cancel()
        dayJobs[day] = viewModelScope.launch {
            delay(450)
            flushNow()
        }
    }

    // ------------------------------------------------------------------ quick messages
    /** Her: [pairId] = one person, null = everyone connected. Partner: always to her. */
    fun sendMessage(body: String, pairId: String? = null) {
        val s = snap ?: return
        if (body.isBlank()) return
        val isHer = s.me.role == "her"
        val targets = if (isHer) s.pairs.filter { it.status == "active" && (pairId == null || it.id == pairId) } else listOfNotNull(s.activePair)
        if (targets.isEmpty()) { error = "هنوز به هم وصل نیستید"; return }
        targets.forEach { pr -> queue(PendingOp("msg", pairId = pr.id, toId = if (isHer) pr.partnerId else pr.herId, payload = body)) }
        viewModelScope.launch {
            flushNow()
            if (pendingChanges > 0) error = "پیام ذخیره شد؛ وقتی اینترنت وصل شد می‌ره" else load()
        }
    }

    // ------------------------------------------------------------------ roles / appearance
    fun labelOf(p: PairRow): String? = p.label?.takeIf { it.isNotBlank() } ?: pairLabels[p.id]?.takeIf { it.isNotBlank() }

    /** "Name (role)" for a connection, from the point of view of the current user. */
    fun personTitle(p: PairRow): String {
        val s = snap ?: return ""
        val otherId = if (s.me.role == "her") p.partnerId else p.herId
        val n = s.others[otherId]?.name?.takeIf { it.isNotBlank() } ?: "پارتنر"
        val l = labelOf(p)
        return if (l != null && s.me.role == "her") "$n ($l)" else n
    }

    fun setPairLabel(pairId: String, label: String) {
        val t = label.trim().take(20)
        val next = if (t.isEmpty()) store.pairLabels - pairId else store.pairLabels + (pairId to t)
        store.pairLabels = next
        pairLabels = next
        viewModelScope.launch {
            runCatching { repo.setPairLabel(pairId, t) } // fine if the optional migration was not run
            load()
        }
    }

    fun chooseAccent(i: Int) {
        store.accent = i
        accent = i
        C.accent = i
    }

    fun setFlow(day: LocalDate, flow: String?) {
        val dl = snap?.days?.get(day)
        val rest = dl?.symptoms.orEmpty().filterNot { it.startsWith("flow_") }
        saveDay(day, dl?.mood, dl?.energy, if (flow == null) rest else rest + flow)
    }

    fun markRead() {
        val s = snap ?: return
        val ids = s.inbox.filter { it.toId == s.me.id && it.readAt == null }.map { it.id }
        if (ids.isEmpty()) return
        viewModelScope.launch { runCatching { repo.markRead(ids) }; load() }
    }

    // ------------------------------------------------------------------ app lock
    fun setLock(v: Boolean) {
        store.lockEnabled = v
        lockEnabled = v
    }

    // ------------------------------------------------------------------ appearance / saved messages
    fun setTheme(mode: Int) {
        store.themeMode = mode
        themeMode = mode
    }

    fun saveMessage(text: String) {
        val t = text.trim()
        if (t.isEmpty() || t in savedMessages) return
        val next = (listOf(t) + savedMessages).take(12)
        store.savedMessages = next
        savedMessages = next
    }

    fun deleteSavedMessage(text: String) {
        val next = savedMessages - text
        store.savedMessages = next
        savedMessages = next
    }

    // ------------------------------------------------------------------ pairing
    fun connect(code: String) {
        if (code.length < 4) { error = "کد رو کامل وارد کن"; return }
        viewModelScope.launch {
            busy = true
            try {
                repo.requestPair(code)
                load()
            } catch (e: Exception) {
                fail(e)
            } finally {
                busy = false
            }
        }
    }

    fun approve(pairId: String) = viewModelScope.launch {
        try { repo.approve(pairId); load() } catch (e: Exception) { fail(e) }
    }

    fun removePair(pairId: String) = viewModelScope.launch {
        try { repo.removePair(pairId); load() } catch (e: Exception) { fail(e) }
    }

    // ------------------------------------------------------------------ updates
    fun checkUpdate() {
        if (update is UpdateState.Downloading) return
        viewModelScope.launch {
            val r = Updater.check()
            if (r is UpdateState.Available && update !is UpdateState.Ready) update = r
        }
    }

    fun startDownload() {
        val u = update
        val (version, url, shaUrl) = when (u) {
            is UpdateState.Available -> Triple(u.version, u.url, u.shaUrl)
            is UpdateState.Failed -> Triple(u.version, u.url, u.shaUrl)
            else -> return
        }
        update = UpdateState.Downloading(version, 0f)
        viewModelScope.launch {
            try {
                val f = Updater.download(app, version, url, shaUrl) { p -> update = UpdateState.Downloading(version, p) }
                update = UpdateState.Ready(version, f)
            } catch (e: Exception) {
                update = UpdateState.Failed(version, url, e.message ?: "دانلود ناموفق بود", shaUrl)
            }
        }
    }

    fun install() {
        val u = update as? UpdateState.Ready ?: return
        if (!Updater.install(app, u.file)) error = "اجازه‌ی نصب برنامه‌ها رو فعال کن و دوباره بزن"
    }

    val configured get() = Api.configured
}
