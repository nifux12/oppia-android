package org.oppia.android.util.logging

import android.app.Application
import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import dagger.BindsInstance
import dagger.Component
import dagger.Module
import dagger.Provides
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.oppia.android.testing.TestLogReportingModule
import org.oppia.android.testing.junit.OppiaParameterizedTestRunner
import org.oppia.android.testing.junit.OppiaParameterizedTestRunner.SelectRunnerPlatform
import org.oppia.android.testing.junit.ParameterizedRobolectricTestRunner
import org.oppia.android.testing.robolectric.RobolectricModule
import org.oppia.android.testing.threading.BackgroundTestDispatcher
import org.oppia.android.testing.threading.TestCoroutineDispatcher
import org.oppia.android.testing.threading.TestCoroutineDispatchers
import org.oppia.android.testing.threading.TestDispatcherModule
import org.oppia.android.testing.time.FakeOppiaClockModule
import org.oppia.android.util.data.DataProvidersInjector
import org.oppia.android.util.data.DataProvidersInjectorProvider
import org.oppia.android.util.locale.testing.LocaleTestModule
import org.robolectric.annotation.Config
import org.robolectric.annotation.LooperMode
import java.io.File
import java.io.PrintWriter
import javax.inject.Inject
import javax.inject.Singleton

@Suppress("FunctionName")
@RunWith(OppiaParameterizedTestRunner::class)
@SelectRunnerPlatform(ParameterizedRobolectricTestRunner::class)
@LooperMode(LooperMode.Mode.PAUSED)
@Config(
  application = ConsoleLoggerTest.TestApplication::class
)
class ConsoleLoggerTest {
  private companion object {
    private const val testTag = "tag"
    private val testLogLevel: LogLevel = LogLevel.ERROR
    private const val testMessage = "test error message"
  }

  @Inject lateinit var context: Context
  @Inject lateinit var consoleLogger: ConsoleLogger
  @Inject lateinit var testCoroutineDispatchers: TestCoroutineDispatchers
  @field:[Inject BackgroundTestDispatcher]
  lateinit var backgroundTestDispatcher: TestCoroutineDispatcher

  private lateinit var logFile: File

  @Before
  fun setUp() {
    setUpTestApplicationComponentWithFileLogging()
    // Initialize logFile and ensure parent directories exist
    logFile = File(context.filesDir, "oppia_app.log")
    logFile.parentFile?.mkdirs()
    // Clean up any existing log file
    logFile.delete()
  }

  @After
  fun tearDown() {
    // Clean up log file after test
    logFile.delete()
  }

  private fun setUpTestApplicationComponentWithFileLogging() {
    ApplicationProvider.getApplicationContext<TestApplication>().inject(this)
    // Override file logging setting
    val enableFileLogField = ConsoleLogger::class.java.getDeclaredField("enableFileLog")
    enableFileLogField.isAccessible = true
    enableFileLogField.set(consoleLogger, true)
  }

  @Test
  fun testConsoleLogger_multipleLogCalls_appendsToFile() {
    consoleLogger.e(testTag, "Error 1")
    testCoroutineDispatchers.advanceUntilIdle()
    consoleLogger.e(testTag, "Error 2")
    testCoroutineDispatchers.advanceUntilIdle()

    val logContent = logFile.readText()
    assertThat(logContent).contains("Error 1")
    assertThat(logContent).contains("Error 2")
    assertThat(logContent.indexOf("Error 1")).isLessThan(logContent.indexOf("Error 2"))
  }

  @Test
  fun testConsoleLogger_closeAndReopen_continuesToAppend() = runTest {
    // Write initial log message
    consoleLogger.e(testTag, "first message")
    testCoroutineDispatchers.advanceUntilIdle()

    // Force close the PrintWriter to simulate app restart
    val printWriterField = ConsoleLogger::class.java.getDeclaredField("printWriter")
    printWriterField.isAccessible = true
    (printWriterField.get(consoleLogger) as? PrintWriter)?.close()
    printWriterField.set(consoleLogger, null)

    // Write second message after "reopening"
    consoleLogger.e(testTag, "second message")
    testCoroutineDispatchers.advanceUntilIdle()

    // Verify log contents
    val logContent = logFile.readText()
    assertThat(logContent).contains("first message")
    assertThat(logContent).contains("second message")
  }

  private fun setUpTestApplicationComponent() {
    ApplicationProvider.getApplicationContext<TestApplication>().inject(this)
  }

  @Module
  class TestModule {
    @Provides
    @Singleton
    fun provideContext(application: Application): Context {
      return application
    }

    @Provides
    @Singleton
    @EnableConsoleLog
    fun provideEnableConsoleLog(): Boolean = true

    @Provides
    @Singleton
    @EnableFileLog
    fun provideEnableFileLog(): Boolean = true

    @Provides
    @Singleton
    @GlobalLogLevel
    fun provideGlobalLogLevel(): LogLevel = LogLevel.ERROR
  }

  // TODO(#89): Move this to a common test application component.
  @Singleton
  @Component(
    modules = [
      RobolectricModule::class, TestModule::class, LocaleTestModule::class,
      TestLogReportingModule::class, TestDispatcherModule::class,
      FakeOppiaClockModule::class,
    ]
  )
  interface TestApplicationComponent : DataProvidersInjector {
    @Component.Builder
    interface Builder {
      @BindsInstance
      fun setApplication(application: Application): Builder

      fun build(): TestApplicationComponent
    }

    fun inject(test: ConsoleLoggerTest)
  }

  class TestApplication : Application(), DataProvidersInjectorProvider {
    private val component: TestApplicationComponent by lazy {
      DaggerConsoleLoggerTest_TestApplicationComponent.builder()
        .setApplication(this)
        .build()
    }

    fun inject(test: ConsoleLoggerTest) {
      component.inject(test)
    }

    public override fun attachBaseContext(base: Context?) {
      super.attachBaseContext(base)
    }

    override fun getDataProvidersInjector(): DataProvidersInjector = component
  }
}
