package io.github.tandemdude.hklbsupport;

import com.intellij.codeInspection.LocalInspectionToolSession;
import com.intellij.codeInspection.ProblemHighlightType;
import com.intellij.codeInspection.ProblemsHolder;
import com.intellij.psi.PsiElementVisitor;
import com.jetbrains.python.inspections.PyInspection;
import com.jetbrains.python.inspections.PyInspectionVisitor;
import com.jetbrains.python.psi.PyClass;
import com.jetbrains.python.psi.types.TypeEvalContext;
import io.github.tandemdude.hklbsupport.utils.Utils;
import org.jetbrains.annotations.NotNull;

/**
 * Local inspection provider allowing problem reporting within Lightbulb command metaclass parameters.
 */
public class CommandRequiredParametersInspector extends PyInspection {
    @Override
    public @NotNull PsiElementVisitor buildVisitor(
            @NotNull ProblemsHolder holder, boolean isOnTheFly, @NotNull LocalInspectionToolSession session) {
        return new Visitor(holder, PyInspectionVisitor.getContext(session));
    }

    /**
     * Visitor that checks for the presence of problems within Lightbulb command definitions.
     */
    static final class Visitor extends PyInspectionVisitor {
        Visitor(@NotNull ProblemsHolder holder, @NotNull TypeEvalContext context) {
            super(holder, context);
        }

        @Override
        public void visitPyClass(@NotNull PyClass node) {
            var lbData = Utils.getLightbulbDataForNode(node);
            if (lbData == null) {
                return;
            }

            var lbSuperclass = Utils.getLightbulbSuperclass(myTypeEvalContext, node, lbData);
            if (lbSuperclass == null) {
                return;
            }

            var existingParameters = Utils.getKeywordSuperclassExpressions(node);
            var requiredParams =
                    lbData.paramData().get(lbSuperclass.getQualifiedName()).required();

            requiredParams.forEach((name, type) -> {
                if (!existingParameters.containsKey(name)) {
                    registerProblem(
                            node.getSuperClassExpressionList(),
                            "Command missing required parameter '" + name + "'",
                            ProblemHighlightType.GENERIC_ERROR);
                }
            });
        }
    }
}
