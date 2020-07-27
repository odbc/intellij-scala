package org.jetbrains.sbt.project

import com.intellij.ide.CommandLineInspectionProjectConfigurator
import com.intellij.ide.CommandLineInspectionProjectConfigurator.ConfiguratorContext
import com.intellij.openapi.externalSystem.importing.ImportSpecBuilder
import com.intellij.openapi.externalSystem.service.execution.ProgressExecutionMode
import com.intellij.openapi.externalSystem.util.ExternalSystemUtil
import com.intellij.openapi.project.{Project, ProjectUtil}
import com.intellij.openapi.projectRoots.{JavaSdk, ProjectJdkTable}
import com.intellij.openapi.util.registry.Registry
import org.jetbrains.plugins.scala.extensions.invokeAndWait
import org.jetbrains.sbt.{SbtBundle, inWriteAction}

class SbtCommandLineProjectConfigurator
  extends CommandLineInspectionProjectConfigurator {

  override def getName: String = "sbt"

  override def getDescription: String =
    SbtBundle.message("sbt.command.line.project.configurator.description")

  override def configureEnvironment(context: ConfiguratorContext): Unit =
    Registry.get("external.system.auto.import.disabled").setValue(true)

  override def configureProject(project: Project, context: ConfiguratorContext): Unit =
    Option(ProjectUtil.guessProjectDir(project))
      .filter(SbtProjectImportProvider.canImport)
      .foreach { projectDir =>
        val jdk = JavaSdk.getInstance.createJdk("11", "/usr/lib/jvm/java-11-amazon-corretto")
        invokeAndWait(inWriteAction(ProjectJdkTable.getInstance.addJdk(jdk)))
        val importSpecBuilder = new ImportSpecBuilder(project, SbtProjectSystem.Id).use(ProgressExecutionMode.MODAL_SYNC)
        ExternalSystemUtil.refreshProject(projectDir.getCanonicalPath, importSpecBuilder)
      }
}
