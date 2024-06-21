package io.github.tandemdude.hklbsupport;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.intellij.openapi.components.Service;
import com.intellij.openapi.fileTypes.FileTypeManager;
import com.intellij.openapi.module.ModuleManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.projectRoots.Sdk;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.search.FileTypeIndex;
import com.intellij.psi.search.GlobalSearchScope;
import com.jetbrains.python.sdk.PythonSdkUtil;
import io.github.tandemdude.hklbsupport.models.LightbulbData;
import io.github.tandemdude.hklbsupport.models.ParamData;
import io.github.tandemdude.hklbsupport.utils.Notifier;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;

@Service(Service.Level.PROJECT)
public final class ProjectDataService {
    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final Pattern VERSION_PATTERN = Pattern.compile("__version__\\s*=\\s*\"([^\"]+)\"");

    private final Project project;

    private final ConcurrentHashMap<Sdk, LightbulbData> sdkCache = new ConcurrentHashMap<>();

    public ProjectDataService(Project project) {
        this.project = project;
    }

    public void loadModules() {
        Arrays.stream(ModuleManager.getInstance(project).getModules()).forEach(module -> {
            var maybeSdk = PythonSdkUtil.findPythonSdk(module);
            if (maybeSdk != null) {
                sdkCache.put(maybeSdk, new LightbulbData("-1", Collections.emptyMap()));
            }
        });
    }

    public void flush() {
        this.sdkCache.clear();
    }

    public LightbulbData getLightbulbData(Sdk sdk) {
        return sdkCache.get(sdk);
    }

    LightbulbData readMetaparamsFile(String version, VirtualFile vf) throws IOException {
        var parsedParamData = MAPPER.readValue(vf.getInputStream(), new TypeReference<Map<String, ParamData>>() {});
        return new LightbulbData(version, parsedParamData);
    }

    public void notifyChange(Sdk sdk) {
        if (!sdkCache.containsKey(sdk)) {
            return;
        }

        FileTypeIndex.processFiles(
                FileTypeManager.getInstance().getFileTypeByExtension("json"),
                file -> {
                    if (!file.getName().equals("metaparams.json")
                            || !file.getParent().getName().equals("lightbulb")) {
                        return true;
                    }

                    var initFile = file.getParent().findChild("__init__.py");
                    if (initFile == null) {
                        return true;
                    }

                    try {
                        var matcher = VERSION_PATTERN.matcher(new String(initFile.contentsToByteArray()));
                        String version = null;
                        while (matcher.find()) {
                            version = matcher.group(1);
                        }

                        if (version == null) {
                            return true;
                        }

                        if (version.equals(sdkCache.get(sdk).version())) {
                            return false;
                        }

                        var data = readMetaparamsFile(version, file);
                        sdkCache.put(sdk, data);
                        Notifier.notifyInformation(
                                project, "Lightbulb configuration loaded successfully (%s)", sdk.getName());
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                    return false;
                },
                GlobalSearchScope.allScope(project));
    }
}
