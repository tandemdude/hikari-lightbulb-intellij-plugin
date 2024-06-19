package io.github.tandemdude.hklbsupport;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.intellij.openapi.components.Service;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleManager;
import com.intellij.openapi.project.Project;
import com.jetbrains.python.sdk.PythonSdkUtil;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;

import io.github.tandemdude.hklbsupport.utils.Notifier;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Service allowing loading and supplying lightbulb metaclass parameter configurations.
 */
@Service(Service.Level.PROJECT)
public final class CommandParameterCompletionLoader {
    enum LoaderError {
        NONE,
        NO_PYTHON_SDK,
        NO_SITE_PACKAGES,
        LB_NOT_INSTALLED,
        LB_MISSING_DATA_FILES,
        LB_VERSION_PARSE_FAILED,
        LB_FILE_READ_FAILED,
        LB_JSON_PARSE_FAILED
    }

    public record ParamData(Map<String, String> required, Map<String, String> optional) {}

    public record LightbulbData(String version, Map<String, ParamData> paramData) {}

    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final Logger LOGGER = Logger.getInstance(CommandParameterCompletionLoader.class);
    private static final Pattern VERSION_PATTERN = Pattern.compile("__version__\\s*=\\s*\"([^\"]+)\"");

    private final Project project;
    private final HashMap<Module, LightbulbData> completionData = new HashMap<>();

    public CommandParameterCompletionLoader(Project project) {
        this.project = project;
    }

    public @Nullable LightbulbData getLightbulbData(@NotNull Module module) {
        return completionData.get(module);
    }

    @NotNull LoaderError loadModuleConfiguration(@NotNull Module module) {
        var sdk = PythonSdkUtil.findPythonSdk(module);
        if (sdk == null) {
            return LoaderError.NO_PYTHON_SDK;
        }

        var sitePackages = PythonSdkUtil.getSitePackagesDirectory(sdk);
        if (sitePackages == null) {
            return LoaderError.NO_SITE_PACKAGES;
        }
        LOGGER.info("found site packages directory " + sitePackages.getPath());

        var lightbulb = sitePackages.findChild("lightbulb");
        if (lightbulb == null) {
            LOGGER.info("could not find lightbulb package directory");
            return LoaderError.LB_NOT_INSTALLED;
        }

        var metaparams = lightbulb.findChild("metaparams.json");
        var init = lightbulb.findChild("__init__.py");
        if (init == null || metaparams == null) {
            LOGGER.info("could not find both 'metaparams.json' and '__init__.py'");
            return LoaderError.LB_MISSING_DATA_FILES;
        }

        try {
            var initContents = new String(init.contentsToByteArray(), StandardCharsets.UTF_8);
            var matcher = VERSION_PATTERN.matcher(initContents);
            String version = null;
            while (matcher.find()) {
                version = matcher.group(1);
            }

            if (version == null) {
                return LoaderError.LB_VERSION_PARSE_FAILED;
            }

            var parsedParamData =
                    MAPPER.readValue(metaparams.getInputStream(), new TypeReference<Map<String, ParamData>>() {});
            var lightbulbData = new LightbulbData(version, parsedParamData);
            completionData.put(module, lightbulbData);

            LOGGER.info("loaded lightbulb metaparams description file successfully (version=" + version + ")");
        } catch (IOException e) {
            if (e instanceof JsonProcessingException) {
                return LoaderError.LB_JSON_PARSE_FAILED;
            }

            return LoaderError.LB_FILE_READ_FAILED;
        }

        return LoaderError.NONE;
    }

    public void loadLightbulbConfiguration() {
        Arrays.stream(ModuleManager.getInstance(project).getModules()).forEach(module -> {
            var status = loadModuleConfiguration(module);
            if (LoaderError.LB_FILE_READ_FAILED.equals(status) || LoaderError.LB_JSON_PARSE_FAILED.equals(status)) {
                Notifier.notifyWarning(
                        project,
                        "Could not load lightbulb configuration for module '%s' - code: %s",
                        module.getName(),
                        status.name());
            }
        });
    }
}
