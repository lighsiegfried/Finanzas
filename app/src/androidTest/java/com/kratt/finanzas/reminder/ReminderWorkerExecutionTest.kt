package com.kratt.finanzas.reminder

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.work.ListenableWorker
import androidx.work.testing.TestListenableWorkerBuilder
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

// valida que el worker local se ejecute sin red y termine bien
@RunWith(AndroidJUnit4::class)
class ReminderWorkerExecutionTest {

    @Test
    fun workerRunsAndSucceeds() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val worker = TestListenableWorkerBuilder<ReminderWorker>(context).build()
        val result = worker.doWork()
        assertTrue(result is ListenableWorker.Result.Success)
    }
}
