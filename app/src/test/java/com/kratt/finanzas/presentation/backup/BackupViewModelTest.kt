package com.kratt.finanzas.presentation.backup

import com.kratt.finanzas.R
import com.kratt.finanzas.data.backup.BackupMetadata
import com.kratt.finanzas.data.backup.RestoreOutcome
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class BackupViewModelTest {

    @Before
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun viewModel(metadata: BackupMetadata = BackupMetadata.EMPTY) = BackupViewModel(
        metadata = MutableStateFlow(metadata),
        createBackup = { _, _, _ -> true },
        prepareRestore = { _, _, _ -> RestoreOutcome.WrongPasswordOrCorrupt },
        commitRestore = { _ -> true },
        discardRestore = {},
    )

    @Test
    fun metadataMapsIntoState() = runTest {
        val vm = viewModel(BackupMetadata(lastBackupMillis = 123L, formatVersion = 1, hasBackup = true))
        val state = vm.uiState.value
        assertEquals(123L, state.lastBackupMillis)
        assertTrue(state.hasBackup)
    }

    @Test
    fun emptyMetadataMeansNoBackup() = runTest {
        val vm = viewModel()
        assertFalse(vm.uiState.value.hasBackup)
        assertNull(vm.uiState.value.lastBackupMillis)
    }

    @Test
    fun passwordChangeRejectsBeyondMaxCodePoints() = runTest {
        val vm = viewModel()
        vm.onPasswordChange("a".repeat(129))
        assertEquals("", vm.uiState.value.password)
        vm.onPasswordChange("a".repeat(128))
        assertEquals(128, vm.uiState.value.password.length)
    }

    @Test
    fun toggleShowPassword() = runTest {
        val vm = viewModel()
        assertFalse(vm.uiState.value.showPassword)
        vm.onToggleShowPassword()
        assertTrue(vm.uiState.value.showPassword)
    }

    @Test
    fun toggleIncludeAttachmentsDefaultsOff() = runTest {
        val vm = viewModel()
        assertFalse(vm.uiState.value.includeAttachments)
        vm.onToggleIncludeAttachments()
        assertTrue(vm.uiState.value.includeAttachments)
    }

    @Test
    fun continueWithEmptyPasswordShowsError() = runTest {
        val vm = viewModel()
        vm.onCreateBackupClick()
        assertFalse(vm.onCreatePasswordContinue())
        assertEquals(R.string.error_empty_password, vm.uiState.value.passwordErrorRes)
        assertEquals(BackupStage.CREATE_PASSWORD, vm.uiState.value.stage)
    }

    @Test
    fun continueWithShortPasswordShowsError() = runTest {
        val vm = viewModel()
        vm.onCreateBackupClick()
        vm.onPasswordChange("corta123")
        vm.onConfirmChange("corta123")
        assertFalse(vm.onCreatePasswordContinue())
        assertEquals(R.string.password_too_short, vm.uiState.value.passwordErrorRes)
    }

    @Test
    fun continueWithMismatchShowsError() = runTest {
        val vm = viewModel()
        vm.onCreateBackupClick()
        vm.onPasswordChange("contrasena-larga")
        vm.onConfirmChange("contrasena-distinta")
        assertFalse(vm.onCreatePasswordContinue())
        assertEquals(R.string.password_mismatch, vm.uiState.value.passwordErrorRes)
    }

    @Test
    fun continueWithValidPasswordAdvancesToConfirm() = runTest {
        val vm = viewModel()
        vm.onCreateBackupClick()
        vm.onPasswordChange("contrasena-valida")
        vm.onConfirmChange("contrasena-valida")
        assertTrue(vm.onCreatePasswordContinue())
        assertNull(vm.uiState.value.passwordErrorRes)
        assertEquals(BackupStage.EXPORT_CONFIRM, vm.uiState.value.stage)
    }
}
