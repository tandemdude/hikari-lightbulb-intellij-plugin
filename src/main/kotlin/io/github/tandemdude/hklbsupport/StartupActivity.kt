package io.github.tandemdude.hklbsupport

import com.intellij.openapi.project.Project
import com.intellij.openapi.startup.ProjectActivity

/**
 * Startup activity that causes module Lightbulb configurations to attempt to be
 * loaded when the user opens a new project.
 */
class StartupActivity : ProjectActivity {
    override suspend fun execute(project: Project) {
        project.getService(CommandParameterCompletionLoader::class.java).loadLightbulbConfiguration()
    }
}
