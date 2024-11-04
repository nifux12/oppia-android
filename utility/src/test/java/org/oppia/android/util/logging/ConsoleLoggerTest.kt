package org.oppia.android.util.logging

import android.app.Application
import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import dagger.BindsInstance
import dagger.Component
import dagger.Module
import dagger.Provides
import kotlinx.coroutines.ExperimentalCoroutinesApi
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
    setUpTestApplicationComponent()
    // Initialize logFile and ensure parent directories exist
    logFile = File(context.filesDir, "oppia_app.log")
    logFile.parentFile?.mkdirs()
    logFile.delete()
  }

  @After
  fun tearDown() {
    logFile.delete()
  }

  private fun logTestMessage(message: String) {
    when (testLogLevel) {
      LogLevel.VERBOSE -> consoleLogger.v(testTag, message)
      LogLevel.DEBUG -> consoleLogger.d(testTag, message)
      LogLevel.INFO -> consoleLogger.i(testTag, message)
      LogLevel.WARNING -> consoleLogger.w(testTag, message)
      LogLevel.ERROR -> consoleLogger.e(testTag, message)
    }
    testCoroutineDispatchers.advanceUntilIdle()
  }

  @Test
  fun testConsoleLogger_multipleLogCalls_appendsToFile() {
    logTestMessage(testMessage)
    logTestMessage("$testMessage 2")

    val logContent = logFile.readText()
    assertThat(logContent).contains(testMessage)
    assertThat(logContent).contains("$testMessage 2")
    assertThat(logContent.indexOf(testMessage)).isLessThan(logContent.indexOf("$testMessage 2"))
  }

  @Test
  @OptIn(ExperimentalCoroutinesApi::class)
  fun testConsoleLogger_closeAndReopen_continuesToAppend() = runTest {
    logTestMessage("first $testMessage")

    // Force close the PrintWriter to simulate app restart
    val printWriterField = ConsoleLogger::class.java.getDeclaredField("printWriter")
    printWriterField.isAccessible = true
    (printWriterField.get(consoleLogger) as? PrintWriter)?.close()
    printWriterField.set(consoleLogger, null)

    logTestMessage("second $testMessage")

    val logContent = logFile.readText()
    assertThat(logContent).contains("first $testMessage")
    assertThat(logContent).contains("second $testMessage")
  }

  private fun setUpTestApplicationComponent() {
    ApplicationProvider.getApplicationContext<TestApplication>().inject(this)
  }

  @Module
  class TestModule {
    @Provides
    @Singleton
    fun provideContext(application: Application): Context = application

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
