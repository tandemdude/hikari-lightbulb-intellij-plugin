package io.github.tandemdude.hklbsupport.macros;

import com.intellij.codeInsight.template.Expression;
import com.intellij.codeInsight.template.ExpressionContext;
import com.intellij.codeInsight.template.Result;
import com.intellij.codeInsight.template.TextResult;
import com.intellij.codeInsight.template.macro.MacroBase;
import com.intellij.psi.util.PsiTreeUtil;
import com.jetbrains.python.psi.PyAssignmentStatement;
import com.jetbrains.python.psi.PyClass;
import com.jetbrains.python.psi.PyFile;
import com.jetbrains.python.psi.PyTargetExpression;
import com.jetbrains.python.psi.types.PyClassType;
import com.jetbrains.python.psi.types.TypeEvalContext;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class LightbulbRegisterCommandDecoratorMacro extends MacroBase {
    public LightbulbRegisterCommandDecoratorMacro() {
        super("lbRegisterCommandDecorator", "lbRegisterCommandDecorator()");
    }

    private LightbulbRegisterCommandDecoratorMacro(String name, String description) {
        super(name, description);
    }

    private boolean isSubclassOfType(PyClass pyClass, TypeEvalContext ctx, String targetQualName) {
        if (pyClass == null) return false;

        String qualifiedName = pyClass.getQualifiedName();
        if (targetQualName.equals(qualifiedName)) {
            return true;
        }

        for (PyClass superClass : pyClass.getSuperClasses(ctx)) {
            if (isSubclassOfType(superClass, ctx, targetQualName)) {
                return true;
            }
        }
        return false;
    }

    private @Nullable String findVariableNameOfType(PyFile file, TypeEvalContext typeContext, String targetQualName) {
        for (var assignment : PsiTreeUtil.findChildrenOfType(file, PyAssignmentStatement.class)) {
            var left = assignment.getLeftHandSideExpression();
            var right = assignment.getAssignedValue();
            if (!(left instanceof PyTargetExpression) || right == null) continue;

            var type = typeContext.getType(right);
            if (type instanceof PyClassType clsType) {
                var pyClass = clsType.getPyClass();

                if (isSubclassOfType(pyClass, typeContext, targetQualName) && left.getName() != null) {
                    return left.getName();
                }
            }
        }

        return null;
    }

    @Override
    protected @Nullable Result calculateResult(
            Expression @NotNull [] expressions, ExpressionContext context, boolean b) {
        var element = context.getPsiElementAtStartOffset();
        if (element == null) {
            return null;
        }

        var pyFile = PsiTreeUtil.getParentOfType(element, PyFile.class);
        if (pyFile == null) {
            return null;
        }

        var project = context.getProject();
        var typeContext = TypeEvalContext.codeAnalysis(project, pyFile);

        var loaderVariableName = findVariableNameOfType(pyFile, typeContext, "lightbulb.loaders.Loader");
        if (loaderVariableName != null) {
            return new TextResult("@" + loaderVariableName + ".command");
        }

        var clientVariableName = findVariableNameOfType(pyFile, typeContext, "lightbulb.client.Client");
        if (clientVariableName != null) {
            return new TextResult("@" + clientVariableName + ".register");
        }

        return null;
    }
}
