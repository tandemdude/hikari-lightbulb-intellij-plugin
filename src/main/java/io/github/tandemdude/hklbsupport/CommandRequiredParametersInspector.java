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
