package io.github.tandemdude.hklbsupport;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.fileTypes.FileTypeManager;
import com.intellij.openapi.module.ModuleManager;
import com.intellij.openapi.project.ProjectManager;
import com.intellij.openapi.projectRoots.Sdk;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.search.FileTypeIndex;
import com.intellij.psi.search.GlobalSearchScope;
import com.jetbrains.python.packaging.PyPackageManager;
import com.jetbrains.python.sdk.PythonSdkUtil;
import io.github.tandemdude.hklbsupport.utils.Notifier;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class LightbulbPackageManagerListener implements PyPackageManager.Listener {
    private static final ObjectMapper MAPPER = new ObjectMapper();

    public record ParamData(Map<String, String> required, Map<String, String> optional) {}
    public record LightbulbData(String version, Map<String, ParamData> paramData) {}

    private static final ConcurrentHashMap<Sdk, LightbulbData> sdkLightbulbData = new ConcurrentHashMap<>();

    public static ConcurrentHashMap<Sdk, LightbulbData> getSdkLightbulbData() {
        return sdkLightbulbData;
    }

    LightbulbData readMetaparamsFile(String version, VirtualFile vf) {
        try {
            var parsedParamData =
                    MAPPER.readValue(vf.getInputStream(), new TypeReference<Map<String, ParamData>>() {});
            return new LightbulbData(version, parsedParamData);
        } catch (IOException e) {
            return null;
        }
    }

    @Override
    public void packagesRefreshed(@NotNull Sdk sdk) {
        ApplicationManager.getApplication().runReadAction(() -> {
            var packages = PyPackageManager.getInstance(sdk).getPackages();
            if (packages == null) {
                return;
            }

            var lightbulb = packages.stream()
                    .filter(p -> p.getName().equals("hikari-lightbulb"))
                    .findFirst();

            if (lightbulb.isEmpty()) {
                return;
            }

            var existingData = sdkLightbulbData.get(sdk);
            if (existingData != null && existingData.version().equals(lightbulb.get().getVersion())) {
                // The cache is still up-to-date - do not refresh
                return;
            }

            var lightbulbLocation = lightbulb.get().getLocation();
            if (lightbulbLocation == null) {
                return;
            }

            var searchScopes = new ArrayList<GlobalSearchScope>();
            for (var project : ProjectManager.getInstance().getOpenProjects()) {
                Arrays.stream(ModuleManager.getInstance(project).getModules()).forEach(module -> {
                    var maybeSdk = PythonSdkUtil.findPythonSdk(module);
                    if (maybeSdk == sdk) {
                        searchScopes.add(GlobalSearchScope.moduleWithDependenciesAndLibrariesScope(module));
                    }
                });
            }

            FileTypeIndex.processFiles(FileTypeManager.getInstance().getFileTypeByExtension("json"), file -> {
                if (
                    file.getPath().endsWith("metaparams.json")
                    && file.getPath().contains("lightbulb")
                    && file.getPath().contains(lightbulbLocation)
                ) {
                    var data = readMetaparamsFile(lightbulb.get().getVersion(), file);
                    if (data == null) {
                        return false;
                    }

                    sdkLightbulbData.put(sdk, data);
                    Notifier.notifyInformation(null, "Lightbulb configuration loaded successfully");
                    return false;
                }
                return true;
            }, GlobalSearchScope.union(searchScopes));
        });
    }
}
