package io.github.tandemdude.hklbsupport;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.ProjectManager;
import com.intellij.openapi.projectRoots.Sdk;
import com.jetbrains.python.packaging.common.PythonPackageManagementListener;
import org.jetbrains.annotations.NotNull;

public class LightbulbPackageManagerListener implements PythonPackageManagementListener {
    @Override
    public void packagesChanged(@NotNull Sdk sdk) {
        ApplicationManager.getApplication().runReadAction(() -> {
            for (var project : ProjectManager.getInstance().getOpenProjects()) {
                project.getService(ProjectDataService.class).notifyChange(sdk);
            }
        });
    }
}
