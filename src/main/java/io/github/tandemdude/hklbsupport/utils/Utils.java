package io.github.tandemdude.hklbsupport.utils;

import com.jetbrains.python.psi.PyClass;
import com.jetbrains.python.psi.types.TypeEvalContext;
import io.github.tandemdude.hklbsupport.CommandParameterCompletionLoader;
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
            @NotNull TypeEvalContext context,
            @NotNull PyClass pyClass,
            @NotNull CommandParameterCompletionLoader.LightbulbData moduleData) {
        PyClass commandSuperClass = null;
        for (var superClass : pyClass.getSuperClasses(context)) {
            if (moduleData.paramData().containsKey(superClass.getQualifiedName())) {
                commandSuperClass = superClass;
                break;
            }
        }
        return commandSuperClass;
    }
}
