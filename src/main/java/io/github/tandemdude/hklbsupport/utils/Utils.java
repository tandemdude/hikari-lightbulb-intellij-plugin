package io.github.tandemdude.hklbsupport.utils;

import com.intellij.openapi.module.ModuleUtilCore;
import com.intellij.openapi.util.Pair;
import com.jetbrains.python.psi.PyClass;
import com.jetbrains.python.psi.PyExpression;
import com.jetbrains.python.psi.PyKeywordArgument;
import com.jetbrains.python.psi.types.TypeEvalContext;
import com.jetbrains.python.sdk.PythonSdkUtil;
import io.github.tandemdude.hklbsupport.ProjectDataService;
import io.github.tandemdude.hklbsupport.models.LightbulbData;
import java.util.Arrays;
import java.util.Map;
import java.util.stream.Collectors;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Utility container class for methods that do not fit nicely within other classes.
 */
public class Utils {
    /**
     * Extract the Lightbulb superclass that a {@link PyClass} inherits from.<br>
     * <br>
     * Only the first Lightbulb superclass found will be returned.
     *
     * @param context the current {@link TypeEvalContext}.
     * @param pyClass the class to extract the Lightbulb superclass from.
     * @param moduleData the module data for the version of Lightbulb installed in the module.
     * @return the Lightbulb superclass as a {@link PyClass}, or {@code null} if none was found.
     */
    public static @Nullable PyClass getLightbulbSuperclass(
            @NotNull TypeEvalContext context, @NotNull PyClass pyClass, @NotNull LightbulbData moduleData) {
        PyClass commandSuperClass = null;
        for (var superClass : pyClass.getSuperClasses(context)) {
            if (moduleData.paramData().containsKey(superClass.getQualifiedName())) {
                commandSuperClass = superClass;
                break;
            }
        }
        return commandSuperClass;
    }

    public static @Nullable LightbulbData getLightbulbDataForNode(@NotNull PyClass node) {
        var module = ModuleUtilCore.findModuleForFile(node.getContainingFile());
        if (module == null) {
            return null;
        }

        var sdk = PythonSdkUtil.findPythonSdk(module);
        if (sdk == null) {
            return null;
        }

        return module.getProject().getService(ProjectDataService.class).getLightbulbData(sdk);
    }

    public static Map<String, PyExpression> getKeywordSuperclassExpressions(PyClass node) {
        return Arrays.stream(node.getSuperClassExpressions())
                .filter(expr -> expr instanceof PyKeywordArgument)
                .map(expr -> (PyKeywordArgument) expr)
                // I had a null pointer exception from this previously so probably good to just make sure
                .filter(expr -> expr.getKeyword() != null && expr.getValueExpression() != null)
                .map(expr -> Pair.create(expr.getKeyword(), expr.getValueExpression()))
                .collect(Collectors.toMap(p -> p.getFirst(), p -> p.getSecond()));
    }
}
