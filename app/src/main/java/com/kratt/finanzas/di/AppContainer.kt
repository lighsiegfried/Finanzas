package com.kratt.finanzas.di

import android.content.Context
import android.os.SystemClock
import com.kratt.finanzas.data.assistant.AssistantEngine
import com.kratt.finanzas.data.assistant.AssistantToolExecutor
import com.kratt.finanzas.data.assistant.DefaultAssistantToolExecutor
import com.kratt.finanzas.data.assistant.DeviceCapabilityDetector
import com.kratt.finanzas.data.assistant.NoOpGenerativeAssistant
import com.kratt.finanzas.BuildConfig
import com.kratt.finanzas.data.update.UpdateContinuityPreferences
import com.kratt.finanzas.data.update.UpdateHealthChecker
import com.kratt.finanzas.data.update.UpdateHealthResult
import com.kratt.finanzas.domain.update.UpdateContinuity
import com.kratt.finanzas.domain.update.UpdateSituation
import com.kratt.finanzas.domain.update.UpdateStatus
import com.kratt.finanzas.data.backup.BackupAttachment
import com.kratt.finanzas.data.backup.BackupHeader
import com.kratt.finanzas.data.backup.BackupManager
import com.kratt.finanzas.data.backup.BackupPreferencesRepository
import com.kratt.finanzas.data.backup.DatabaseSnapshot
import com.kratt.finanzas.data.backup.RestoreCandidate
import com.kratt.finanzas.data.backup.RestoreOutcome
import com.kratt.finanzas.data.csv.CsvImporter
import com.kratt.finanzas.data.local.AppDatabase
import com.kratt.finanzas.data.onboarding.OnboardingPreferences
import com.kratt.finanzas.data.preferences.DisplayPreferences
import com.kratt.finanzas.data.preferences.SecurityPreferencesRepositoryImpl
import com.kratt.finanzas.data.reminder.CommitmentService
import com.kratt.finanzas.data.reminder.ReminderPreferencesRepository
import com.kratt.finanzas.data.report.BudgetWarningPreferences
import com.kratt.finanzas.data.report.CsvExporter
import com.kratt.finanzas.data.repository.AccountRepositoryImpl
import com.kratt.finanzas.data.repository.BudgetRepository
import com.kratt.finanzas.data.repository.CategoryRepositoryImpl
import com.kratt.finanzas.data.repository.InstallmentRepository
import com.kratt.finanzas.data.repository.RecurringRepository
import com.kratt.finanzas.data.repository.ReportRepository
import com.kratt.finanzas.data.repository.TransactionRepositoryImpl
import com.kratt.finanzas.data.repository.TransactionTemplateRepository
import com.kratt.finanzas.data.security.DatabaseFiles
import com.kratt.finanzas.data.security.DatabaseKeyManager
import com.kratt.finanzas.data.security.DatabasePassphraseProvider
import com.kratt.finanzas.data.security.EncryptedDatabaseBootstrap
import com.kratt.finanzas.reminder.ReminderScheduler
import com.kratt.finanzas.domain.repository.AccountRepository
import com.kratt.finanzas.domain.repository.CategoryRepository
import com.kratt.finanzas.domain.repository.SecurityPreferencesRepository
import com.kratt.finanzas.domain.repository.TransactionRepository
import com.kratt.finanzas.domain.usecase.AddTransactionUseCase
import com.kratt.finanzas.domain.usecase.DeleteTransactionUseCase
import com.kratt.finanzas.domain.usecase.EditTransactionUseCase
import com.kratt.finanzas.domain.usecase.ObserveMonthlySummaryUseCase
import com.kratt.finanzas.domain.usecase.SaveTransferUseCase
import com.kratt.finanzas.domain.usecase.ObserveRecentTransactionsUseCase
import com.kratt.finanzas.domain.usecase.ObserveTransactionsUseCase
import com.kratt.finanzas.domain.usecase.ValidateTransactionUseCase
import com.kratt.finanzas.data.attachment.AttachmentFileStore
import com.kratt.finanzas.data.ocr.NoOpOcrEngine
import com.kratt.finanzas.data.repository.AttachmentRepositoryImpl
import com.kratt.finanzas.domain.ocr.OcrEngine
import com.kratt.finanzas.domain.repository.AttachmentRepository
import com.kratt.finanzas.domain.usecase.AddAttachmentUseCase
import com.kratt.finanzas.domain.usecase.AttachmentStorageSummaryUseCase
import com.kratt.finanzas.domain.usecase.DeleteAttachmentUseCase
import com.kratt.finanzas.domain.usecase.ObserveAttachmentsUseCase
import com.kratt.finanzas.domain.usecase.ReadAttachmentUseCase
import com.kratt.finanzas.domain.usecase.RunOcrUseCase
import com.kratt.finanzas.security.AppLockManager
import java.io.InputStream
import java.io.OutputStream
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

// contenedor manual de dependencias, arma la base cifrada, los repos, los casos de uso y el respaldo
class AppContainer(context: Context) {

    private val appContext = context.applicationContext
    private val databaseFiles = DatabaseFiles(appContext, AppDatabase.NAME)
    private val databaseKeyManager = DatabaseKeyManager()
    private val bootstrap = EncryptedDatabaseBootstrap(appContext, databaseFiles, databaseKeyManager)

    private val _databaseState = MutableStateFlow(DatabaseBootstrapState.PREPARING)
    val databaseState: StateFlow<DatabaseBootstrapState> = _databaseState.asStateFlow()

    // ruta pendiente pedida por un acceso directo o un widget; la consume el navhost ya desbloqueado
    private val _pendingNavRoute = MutableStateFlow<String?>(null)
    val pendingNavRoute: StateFlow<String?> = _pendingNavRoute.asStateFlow()

    fun requestNavigation(route: String) { _pendingNavRoute.value = route }
    fun consumeNavigation() { _pendingNavRoute.value = null }

    lateinit var database: AppDatabase
        private set

    // repos y casos de uso se crean solo cuando la base ya esta lista
    val accountRepository: AccountRepository by lazy { AccountRepositoryImpl(database.accountDao()) }
    val categoryRepository: CategoryRepository by lazy { CategoryRepositoryImpl(database.categoryDao()) }
    val transactionRepository: TransactionRepository by lazy { TransactionRepositoryImpl(database.transactionDao()) }

    val validateTransaction: ValidateTransactionUseCase by lazy { ValidateTransactionUseCase() }
    val addTransaction: AddTransactionUseCase by lazy { AddTransactionUseCase(transactionRepository, validateTransaction) }
    val editTransaction: EditTransactionUseCase by lazy { EditTransactionUseCase(transactionRepository, validateTransaction) }
    val deleteTransaction: DeleteTransactionUseCase by lazy { DeleteTransactionUseCase(transactionRepository) }
    val saveTransfer: SaveTransferUseCase by lazy { SaveTransferUseCase(transactionRepository) }
    val observeMonthlySummary: ObserveMonthlySummaryUseCase by lazy { ObserveMonthlySummaryUseCase(transactionRepository) }
    val observeRecentTransactions: ObserveRecentTransactionsUseCase by lazy { ObserveRecentTransactionsUseCase(transactionRepository) }
    val observeTransactions: ObserveTransactionsUseCase by lazy { ObserveTransactionsUseCase(transactionRepository) }

    // adjuntos cifrados de los movimientos; el archivo no necesita la base pero el repo si
    val attachmentFileStore = AttachmentFileStore(appContext)
    val attachmentRepository: AttachmentRepository by lazy {
        AttachmentRepositoryImpl(database.attachmentDao(), attachmentFileStore, System::currentTimeMillis)
    }
    val addAttachment: AddAttachmentUseCase by lazy { AddAttachmentUseCase(attachmentRepository) }
    val deleteAttachment: DeleteAttachmentUseCase by lazy { DeleteAttachmentUseCase(attachmentRepository) }
    val observeAttachments: ObserveAttachmentsUseCase by lazy { ObserveAttachmentsUseCase(attachmentRepository) }
    val readAttachment: ReadAttachmentUseCase by lazy { ReadAttachmentUseCase(attachmentRepository) }
    val attachmentStorageSummary: AttachmentStorageSummaryUseCase by lazy { AttachmentStorageSummaryUseCase(attachmentRepository) }

    // motor de ocr local pluggable; por defecto no disponible hasta conectar el motor real (tesseract)
    val ocrEngine: OcrEngine = NoOpOcrEngine()
    val runOcr: RunOcrUseCase = RunOcrUseCase(ocrEngine)

    // cuotas, recurrentes y compromisos, tambien dependen de la base lista
    val installmentRepository: InstallmentRepository by lazy { InstallmentRepository(database) }
    val recurringRepository: RecurringRepository by lazy { RecurringRepository(database) }
    val commitmentService: CommitmentService by lazy { CommitmentService(installmentRepository, recurringRepository) }

    // presupuestos y reportes, tambien dependen de la base lista
    val budgetRepository: BudgetRepository by lazy { BudgetRepository(database) }
    val reportRepository: ReportRepository by lazy { ReportRepository(database, accountRepository) }
    // plantillas de movimiento
    val transactionTemplateRepository: TransactionTemplateRepository by lazy { TransactionTemplateRepository(database) }
    // metas de ahorro, aportes y compras planificadas
    val savingsGoalRepository: com.kratt.finanzas.data.repository.SavingsGoalRepository by lazy { com.kratt.finanzas.data.repository.SavingsGoalRepository(database) }
    val savingsContributionRepository: com.kratt.finanzas.data.repository.SavingsContributionRepository by lazy { com.kratt.finanzas.data.repository.SavingsContributionRepository(database) }
    val plannedPurchaseRepository: com.kratt.finanzas.data.repository.PlannedPurchaseRepository by lazy { com.kratt.finanzas.data.repository.PlannedPurchaseRepository(database) }
    val csvImporter: CsvImporter by lazy { CsvImporter(database, accountRepository, categoryRepository, transactionRepository) }
    val csvExporter: CsvExporter = CsvExporter()
    // importacion y exportacion de metas y compras planificadas
    val planningCsvImporter: com.kratt.finanzas.data.csv.PlanningCsvImporter by lazy {
        com.kratt.finanzas.data.csv.PlanningCsvImporter(database, savingsGoalRepository)
    }
    val budgetWarningPreferences = BudgetWarningPreferences(appContext)

    // asistente financiero local, solo lectura; reutiliza los repos y casos de uso existentes
    // detecta la disponibilidad del modo avanzado, que no esta presente en los dispositivos objetivo
    val deviceCapabilityDetector = DeviceCapabilityDetector(appContext)
    private val generativeAssistant = NoOpGenerativeAssistant()
    val assistantToolExecutor: AssistantToolExecutor by lazy {
        DefaultAssistantToolExecutor(
            transactionRepository = transactionRepository,
            accountRepository = accountRepository,
            categoryRepository = categoryRepository,
            reportRepository = reportRepository,
            budgetRepository = budgetRepository,
            installmentRepository = installmentRepository,
            recurringRepository = recurringRepository,
            commitmentService = commitmentService,
            savingsGoalRepository = savingsGoalRepository,
            savingsContributionRepository = savingsContributionRepository,
            plannedPurchaseRepository = plannedPurchaseRepository,
            backupPreferencesRepository = backupPreferencesRepository,
            observeMonthlySummary = observeMonthlySummary,
        )
    }
    val assistantEngine: AssistantEngine by lazy {
        AssistantEngine(
            executor = assistantToolExecutor,
            generative = generativeAssistant,
            deviceSupported = { deviceCapabilityDetector.isGenerativeSupported() },
        )
    }

    // continuidad de datos entre actualizaciones (fase 6b), solo preferencias no sensibles
    val updateContinuityPreferences = UpdateContinuityPreferences(appContext)
    val updateHealthChecker: UpdateHealthChecker by lazy {
        UpdateHealthChecker(accountRepository, transactionRepository, observeMonthlySummary)
    }
    private val _updateStatus = MutableStateFlow(UpdateStatus.NONE)
    val updateStatus: StateFlow<UpdateStatus> = _updateStatus.asStateFlow()

    // el estado de seguridad no necesita la base, se crea de una vez
    private val securityScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    val securityPreferencesRepository: SecurityPreferencesRepository =
        SecurityPreferencesRepositoryImpl(appContext)
    val appLockManager: AppLockManager = AppLockManager(
        securityPreferencesRepository = securityPreferencesRepository,
        scope = securityScope,
        elapsedRealtime = SystemClock::elapsedRealtime,
    )

    // ajustes de recordatorios, no necesita la base
    val reminderPreferencesRepository = ReminderPreferencesRepository(appContext)
    // recordatorios opcionales por meta y por compra
    val planningReminderPreferences = com.kratt.finanzas.data.preferences.PlanningReminderPreferences(appContext)

    // estado de la configuracion inicial, no necesita la base
    val onboardingPreferences = OnboardingPreferences(appContext)

    // preferencias visuales no sensibles, no necesita la base
    val displayPreferences = DisplayPreferences(appContext)

    // preferencia no sensible de los widgets (mostrar montos)
    val widgetPreferences = com.kratt.finanzas.data.preferences.WidgetPreferences(appContext)

    // actualiza todos los widgets tras una operacion exitosa; punto unico de refresco
    fun refreshWidgets() {
        securityScope.launch { runCatching { com.kratt.finanzas.widget.WidgetUpdater.updateAll(appContext) } }
    }

    // respaldo portable cifrado
    val backupPreferencesRepository = BackupPreferencesRepository(appContext)
    private val backupManager = BackupManager(
        context = appContext,
        files = databaseFiles,
        keyManager = databaseKeyManager,
        passphraseProvider = DatabasePassphraseProvider(databaseFiles.envelope, databaseKeyManager),
        snapshot = DatabaseSnapshot(appContext, databaseFiles),
        nowMillis = System::currentTimeMillis,
    )

    private val bootstrapMutex = Mutex()

    fun startDatabaseBootstrap() {
        securityScope.launch { runBootstrap() }
        startWidgetObservers()
    }

    private var widgetObserversStarted = false

    // observa el cambio de privacidad de saldos para reflejarlo en los widgets sin trabajo pesado
    private fun startWidgetObservers() {
        if (widgetObserversStarted) return
        widgetObserversStarted = true
        securityScope.launch {
            displayPreferences.settings.map { it.balancesHidden }.distinctUntilChanged().drop(1).collect { refreshWidgets() }
        }
    }

    // al abrir la app se reagenda el recordatorio por si cambio la zona horaria del dispositivo
    // se agenda si hay recordatorios de pagos o si hay alguna meta o compra con recordatorio activo
    fun rescheduleRemindersIfEnabled(context: Context) {
        securityScope.launch {
            val settings = reminderPreferencesRepository.settings.first()
            if (settings.enabled || planningReminderPreferences.hasAny()) {
                runCatching { ReminderScheduler.schedule(context, settings.hour, settings.minute) }
            }
        }
    }

    fun retryDatabaseBootstrap() {
        securityScope.launch { runBootstrap() }
    }

    // reagenda el trabajo de recordatorios usando el contexto de la app
    fun rescheduleReminders() = rescheduleRemindersIfEnabled(appContext)

    private suspend fun runBootstrap() = bootstrapMutex.withLock {
        if (::database.isInitialized) {
            _databaseState.value = DatabaseBootstrapState.READY
            return@withLock
        }
        _databaseState.value = DatabaseBootstrapState.PREPARING
        val result = withContext(Dispatchers.IO) {
            bootstrap.bootstrap(onMigrating = { _databaseState.value = DatabaseBootstrapState.MIGRATING })
        }
        applyBootstrapResult(result)
    }

    private fun applyBootstrapResult(result: EncryptedDatabaseBootstrap.Result) {
        when (result) {
            is EncryptedDatabaseBootstrap.Result.Ready -> {
                database = result.database
                _databaseState.value = DatabaseBootstrapState.READY
                // al abrir la app se recalculan compromisos y se registran las ocurrencias en automatico
                securityScope.launch { runCatching { commitmentService.sync() } }
                // limpia archivos de adjuntos sin metadato y metadatos sin archivo (por ejemplo tras restaurar)
                securityScope.launch {
                    runCatching {
                        attachmentRepository.sweepOrphans()
                        attachmentRepository.pruneMissingFiles()
                    }
                }
                // detecta primer arranque tras una actualizacion y corre la verificacion de salud
                securityScope.launch { runCatching { runUpdateContinuity() } }
            }
            EncryptedDatabaseBootstrap.Result.RecoveryRequired ->
                _databaseState.value = DatabaseBootstrapState.RECOVERY_REQUIRED
        }
    }

    // detecta primer arranque tras actualizar y corre una verificacion de salud no destructiva
    // nunca registra valores financieros; los datos siguen en el dispositivo pase lo que pase
    private suspend fun runUpdateContinuity() {
        val current = BuildConfig.VERSION_CODE
        val last = updateContinuityPreferences.lastSuccessfulVersionCode.first()
        when (UpdateContinuity.situation(last, current)) {
            UpdateSituation.FIRST_INSTALL, UpdateSituation.SAME_VERSION -> {
                // se registra la version en silencio, sin aviso de actualizacion
                if (updateHealthChecker.check() is UpdateHealthResult.Healthy) {
                    updateContinuityPreferences.recordSuccessfulVersion(current)
                }
            }
            UpdateSituation.UPDATED -> {
                if (updateHealthChecker.check() is UpdateHealthResult.Healthy) {
                    updateContinuityPreferences.recordSuccessfulVersion(current)
                    _updateStatus.value = UpdateStatus.SUCCESS
                } else {
                    // no se registra la version; se muestra la pantalla de fallo no destructiva
                    _updateStatus.value = UpdateStatus.FAILED
                }
            }
        }
    }

    fun acknowledgeUpdateSuccess() { _updateStatus.value = UpdateStatus.NONE }

    // permite entrar a la app para crear una copia de diagnostico tras un fallo de actualizacion
    fun proceedPastUpdateFailure() { _updateStatus.value = UpdateStatus.NONE }

    fun retryUpdateHealthCheck() { securityScope.launch { runUpdateContinuity() } }

    // registra que se mostro el aviso de desinstalar; solo una marca no sensible
    fun markUninstallWarningShown() {
        securityScope.launch { runCatching { updateContinuityPreferences.setUninstallWarningShown() } }
    }

    // asegura que la base este lista antes de que el worker toque los datos
    suspend fun ensureDatabaseReady(): Boolean {
        if (::database.isInitialized) return true
        runBootstrap()
        return ::database.isInitialized
    }

    // crea un respaldo cifrado en el destino elegido por el usuario
    // solo reune los adjuntos descifrados si el usuario pidio incluirlos en el respaldo
    suspend fun createBackup(output: OutputStream, password: ByteArray, includeAttachments: Boolean): Boolean =
        withContext(Dispatchers.IO) {
            try {
                val attachments = if (includeAttachments) {
                    attachmentRepository.readAllForBackup().map { (name, bytes) -> BackupAttachment(name, bytes) }
                } else {
                    emptyList()
                }
                val createdAt = backupManager.createBackup(output, password, database, attachments)
                backupPreferencesRepository.recordSuccessfulBackup(createdAt, BackupHeader.FORMAT_VERSION)
                true
            } catch (e: Exception) {
                false
            }
        }

    suspend fun prepareRestore(input: InputStream, password: ByteArray): RestoreOutcome =
        withContext(Dispatchers.IO) { backupManager.prepareRestore(input, password) }

    fun discardRestore(candidate: RestoreCandidate) = backupManager.discardRestore(candidate)

    // reemplaza la base por la del respaldo y reabre; si algo falla vuelve a la original
    suspend fun commitRestore(candidate: RestoreCandidate): Boolean = bootstrapMutex.withLock {
        withContext(Dispatchers.IO) {
            val ok = try {
                backupManager.commitRestore(
                    candidate,
                    closeCurrentDatabase = { if (::database.isInitialized) database.close() },
                    // vuelve a cifrar cada adjunto restaurado con la clave local del dispositivo destino
                    writeAttachment = { name, plaintext -> attachmentFileStore.writeForName(name, plaintext) },
                )
                true
            } catch (e: Exception) {
                false
            }
            applyBootstrapResult(bootstrap.bootstrap())
            // los datos cambiaron tras restaurar, refresca los widgets
            refreshWidgets()
            ok
        }
    }
}
