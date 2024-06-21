package io.github.tandemdude.hklbsupport.actions;

import com.intellij.openapi.actionSystem.ActionUpdateThread;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.module.ModuleManager;
import com.jetbrains.python.sdk.PythonSdkUtil;
import io.github.tandemdude.hklbsupport.ProjectDataService;
import org.jetbrains.annotations.NotNull;

public class CacheRefreshAction extends AnAction {
    @Override
    public void update(@NotNull AnActionEvent e) {
        e.getPresentation().setEnabledAndVisible(e.getProject() != null);
    }

    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
        if (e.getProject() == null) {
            return;
        }

        var dataService = e.getProject().getService(ProjectDataService.class);
        dataService.flush();

        var modules = ModuleManager.getInstance(e.getProject()).getModules();
        for (var module : modules) {
            var sdk = PythonSdkUtil.findPythonSdk(module);
            if (sdk != null) {
                dataService.notifyChange(sdk);
            }
        }
    }

    @Override
    public @NotNull ActionUpdateThread getActionUpdateThread() {
        return ActionUpdateThread.BGT;
    }
}
