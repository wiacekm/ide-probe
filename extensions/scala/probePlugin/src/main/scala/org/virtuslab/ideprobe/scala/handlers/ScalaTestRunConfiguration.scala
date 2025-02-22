package org.virtuslab.ideprobe.scala.handlers

import java.util.Collections

import scala.concurrent.ExecutionContext

import com.intellij.compiler.options.CompileStepBeforeRun.MakeBeforeRunTask
import com.intellij.execution.impl.RunManagerImpl
import com.intellij.execution.impl.RunnerAndConfigurationSettingsImpl
import org.jetbrains.plugins.scala.testingSupport.test.AbstractTestRunConfiguration
import org.jetbrains.plugins.scala.testingSupport.test.TestKind
import org.jetbrains.plugins.scala.testingSupport.test.scalatest.ScalaTestConfigurationType
import org.jetbrains.plugins.scala.testingSupport.test.scalatest.ScalaTestRunConfigurationFactory
import org.jetbrains.plugins.scala.testingSupport.test.testdata.AllInPackageTestData
import org.jetbrains.plugins.scala.testingSupport.test.testdata.ClassTestData
import org.jetbrains.plugins.scala.testingSupport.test.testdata.SingleTestData

import org.virtuslab.ideprobe.handlers.Modules
import org.virtuslab.ideprobe.handlers.RunConfigurations
import org.virtuslab.ideprobe.handlers.Tests
import org.virtuslab.ideprobe.protocol.TestsRunResult
import org.virtuslab.ideprobe.scala.protocol.{ScalaTestRunConfiguration => RunConfiguration}

object ScalaTestRunConfiguration {
  def execute(runConfiguration: RunConfiguration)(implicit ec: ExecutionContext): TestsRunResult = {
    val module = Modules.resolve(runConfiguration.module)
    val project = module.getProject

    val runManager = RunManagerImpl.getInstanceImpl(project)
    val configurationType = ScalaTestConfigurationType.instance
    val factory = new ScalaTestRunConfigurationFactory(configurationType)
    val configuration = factory.createTemplateConfiguration(project).asInstanceOf[AbstractTestRunConfiguration]
    configuration.setModule(module)

    runConfiguration match {
      case RunConfiguration.Module(_) => {
        configuration.setTestKind(TestKind.ALL_IN_PACKAGE)
        configuration.testConfigurationData = AllInPackageTestData(configuration, "")
      }
      case RunConfiguration.Package(_, packageName) => {
        configuration.setTestKind(TestKind.ALL_IN_PACKAGE)
        configuration.testConfigurationData = AllInPackageTestData(configuration, packageName)
      }
      case RunConfiguration.Class(_, className) => {
        configuration.setTestKind(TestKind.CLAZZ)
        configuration.testConfigurationData = ClassTestData(configuration, className)
      }
      case RunConfiguration.Method(_, className, methodName) => {
        configuration.setTestKind(TestKind.TEST_NAME)
        configuration.testConfigurationData = SingleTestData(configuration, className, methodName)
      }
    }
    configuration.setBeforeRunTasks(Collections.singletonList(new MakeBeforeRunTask))

    configuration.testConfigurationData.setWorkingDirectory(project.getBasePath)
    configuration.testConfigurationData.setUseSbt(true)
    configuration.testConfigurationData.setUseUiWithSbt(true)

    val settings = new RunnerAndConfigurationSettingsImpl(runManager, configuration)

    runManager.addConfiguration(settings)
    runManager.setTemporaryConfiguration(settings)
    runManager.setSelectedConfiguration(settings)

    Tests.awaitTestResults(project, () => RunConfigurations.launch(project, settings))
  }
}
