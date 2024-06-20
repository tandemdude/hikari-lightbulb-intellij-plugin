package io.github.tandemdude.hklbsupport;

import com.intellij.codeInsight.completion.CompletionContributor;
import com.intellij.codeInsight.completion.CompletionParameters;
import com.intellij.codeInsight.completion.CompletionProvider;
import com.intellij.codeInsight.completion.CompletionResultSet;
import com.intellij.codeInsight.completion.CompletionType;
import com.intellij.codeInsight.lookup.LookupElement;
import com.intellij.codeInsight.lookup.LookupElementBuilder;
import com.intellij.openapi.module.ModuleUtilCore;
import com.intellij.patterns.PlatformPatterns;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.util.ProcessingContext;
import com.jetbrains.python.PythonLanguage;
import com.jetbrains.python.codeInsight.completion.PyMetaClassCompletionContributor;
import com.jetbrains.python.psi.PyArgumentList;
import com.jetbrains.python.psi.PyClass;
import com.jetbrains.python.psi.PyReferenceExpression;
import com.jetbrains.python.psi.types.TypeEvalContext;
import com.jetbrains.python.sdk.PythonSdkUtil;
import io.github.tandemdude.hklbsupport.utils.Utils;
import java.util.ArrayList;
import org.jetbrains.annotations.NotNull;

/**
 * Completion contributor providing metaclass parameter suggestions for Lightbulb command classes.
 */
public class CommandParameterCompletionContributor extends CompletionContributor {
    static class CommandParameterCompletionProvider extends CompletionProvider<CompletionParameters> {
        @Override
        protected void addCompletions(
                @NotNull CompletionParameters parameters,
                @NotNull ProcessingContext context,
                @NotNull CompletionResultSet result) {
            var module = ModuleUtilCore.findModuleForFile(parameters.getOriginalFile());
            if (module == null) {
                // We cannot suggest parameters if we don't know the module (and therefore the SDK)
                // that the file belongs to
                return;
            }

            var sdk = PythonSdkUtil.findPythonSdk(module);
            if (sdk == null) {
                return;
            }

            var moduleLightbulbData = LightbulbPackageManagerListener.getSdkLightbulbData().get(sdk);
            if (moduleLightbulbData == null) {
                // We don't have the data for the specified module - maybe it isn't part of this project?
                // Alternatively, lightbulb may not be installed - a filesystem listener will load the
                // data when it becomes available.
                return;
            }

            var cls = PsiTreeUtil.getParentOfType(parameters.getPosition(), PyClass.class);
            if (cls == null) {
                // This shouldn't happen given we specified in our expression only to suggest parameters
                // within a class definition - but we don't want to error the user's IDE just in case.
                return;
            }

            var evalContext = TypeEvalContext.codeCompletion(cls.getProject(), parameters.getOriginalFile());
            var commandSuperClass = Utils.getLightbulbSuperclass(evalContext, cls, moduleLightbulbData);
            if (commandSuperClass == null) {
                // The current class does not inherit from one of lightbulb's command classes, so we have
                // nothing to autocomplete.
                return;
            }

            var completionElements = new ArrayList<LookupElement>();
            var paramData = moduleLightbulbData.paramData().get(commandSuperClass.getQualifiedName());
            paramData
                    .required()
                    .forEach((key, value) -> completionElements.add(
                            LookupElementBuilder.create(key + "=").withTypeText(value)));
            paramData
                    .optional()
                    .forEach((key, value) -> completionElements.add(
                            LookupElementBuilder.create(key + "=").withTypeText(value)));

            result.addAllElements(completionElements);
        }
    }

    public CommandParameterCompletionContributor() {
        extend(
                CompletionType.BASIC,
                PlatformPatterns.psiElement()
                        .withLanguage(PythonLanguage.getInstance())
                        .withParents(PyReferenceExpression.class, PyArgumentList.class, PyClass.class)
                        .and(PyMetaClassCompletionContributor.hasLanguageLevel(level -> !level.isPython2())),
                new CommandParameterCompletionProvider());
    }
}
