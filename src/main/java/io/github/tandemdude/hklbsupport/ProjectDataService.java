/*
 * Copyright (c) 2024-present tandemdude
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

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

    boolean populateCacheForSdk(Sdk sdk, VirtualFile paramsFile) {
        if (!paramsFile.getName().equals("metaparams.json")
                || !paramsFile.getParent().getName().equals("lightbulb")) {
            return true;
        }

        var initFile = paramsFile.getParent().findChild("__init__.py");
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

            if (sdkCache.get(sdk) != null && version.equals(sdkCache.get(sdk).version())) {
                return false;
            }

            var data = readMetaparamsFile(version, paramsFile);
            sdkCache.put(sdk, data);
            Notifier.notifyInformation(project, "Lightbulb configuration loaded successfully (%s)", sdk.getName());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return false;
    }

    public void notifyChange(Sdk sdk) {
        FileTypeIndex.processFiles(
                FileTypeManager.getInstance().getFileTypeByExtension("json"),
                file -> populateCacheForSdk(sdk, file),
                GlobalSearchScope.allScope(project));
    }
}
