package io.github.tandemdude.hklbsupport.inspections;

import com.intellij.codeInspection.LocalInspectionToolSession;
import com.intellij.codeInspection.LocalQuickFix;
import com.intellij.codeInspection.ProblemDescriptor;
import com.intellij.codeInspection.ProblemHighlightType;
import com.intellij.codeInspection.ProblemsHolder;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiElementVisitor;
import com.jetbrains.python.inspections.PyInspection;
import com.jetbrains.python.inspections.PyInspectionVisitor;
import com.jetbrains.python.psi.PyCallExpression;
import com.jetbrains.python.psi.PyClass;
import com.jetbrains.python.psi.PyFunction;
import com.jetbrains.python.psi.PyReferenceExpression;
import com.jetbrains.python.psi.types.TypeEvalContext;
import io.github.tandemdude.hklbsupport.utils.Utils;
import org.jetbrains.annotations.NotNull;

public class CommandInvalidOptionUsageInspector extends PyInspection {
    @Override
    public @NotNull PsiElementVisitor buildVisitor(
            @NotNull ProblemsHolder holder, boolean isOnTheFly, @NotNull LocalInspectionToolSession session) {
        return new Visitor(holder, PyInspectionVisitor.getContext(session));
    }

    static final class Visitor extends PyInspectionVisitor {
        Visitor(@NotNull ProblemsHolder holder, @NotNull TypeEvalContext context) {
            super(holder, context);
        }

        @Override
        public void visitPyClass(@NotNull PyClass node) {
            var lbData = Utils.getLightbulbDataForNode(node);
            if (lbData == null || lbData.optionFuncs().isEmpty()) {
                return;
            }

            var lbSuperclass = Utils.getLightbulbSuperclass(myTypeEvalContext, node, lbData);
            // Options are only disallowed for non-slash command types
            if (lbSuperclass == null
                    || (lbSuperclass.getQualifiedName() != null
                            && lbSuperclass.getQualifiedName().contains("SlashCommand"))) {
                return;
            }

            for (var attribute : node.getClassAttributes()) {
                if (
                // if there isn't an assigment statement, or the assigment doesn't include a function call
                !(attribute.findAssignedValue() instanceof PyCallExpression callExpr)
                        // if the function call isn't a reference (this is more just for casting purposes)
                        || !(callExpr.getCallee() instanceof PyReferenceExpression refExpr)
                        // if the reference isn't to a function
                        || !(refExpr.getReference().resolve() instanceof PyFunction func)) {
                    continue;
                }

                // if the referenced function is a built-in lightbulb option function
                if (func.getQualifiedName() != null && lbData.optionFuncs().contains(func.getQualifiedName())) {
                    registerProblem(
                            attribute.getParent(),
                            "Options are not permitted for this command type",
                            ProblemHighlightType.GENERIC_ERROR,
                            null,
                            new RemoveInvalidOptionQuickFix(attribute.getName()));
                }
            }
        }
    }

    record RemoveInvalidOptionQuickFix(String attributeName) implements LocalQuickFix {
        @Override
        public @NotNull String getFamilyName() {
            return "Remove invalid option";
        }

        @Override
        public @NotNull String getName() {
            return "Remove option '" + attributeName + "'";
        }

        @Override
        public void applyFix(@NotNull Project project, @NotNull ProblemDescriptor descriptor) {
            descriptor.getPsiElement().delete();
        }
    }
}
