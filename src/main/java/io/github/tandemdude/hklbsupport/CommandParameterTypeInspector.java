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
import com.jetbrains.python.documentation.PythonDocumentationProvider;
import com.jetbrains.python.inspections.PyInspection;
import com.jetbrains.python.inspections.PyInspectionVisitor;
import com.jetbrains.python.psi.PyClass;
import com.jetbrains.python.psi.PyExpression;
import com.jetbrains.python.psi.types.PyType;
import com.jetbrains.python.psi.types.PyTypeChecker;
import com.jetbrains.python.psi.types.PyTypeParser;
import com.jetbrains.python.psi.types.TypeEvalContext;
import io.github.tandemdude.hklbsupport.utils.Utils;
import java.util.Map;
import org.jetbrains.annotations.NotNull;

public class CommandParameterTypeInspector extends PyInspection {
    @Override
    public @NotNull PsiElementVisitor buildVisitor(
            @NotNull ProblemsHolder holder, boolean isOnTheFly, @NotNull LocalInspectionToolSession session) {
        return new Visitor(holder, PyInspectionVisitor.getContext(session));
    }

    static final class Visitor extends PyInspectionVisitor {
        Visitor(@NotNull ProblemsHolder holder, @NotNull TypeEvalContext context) {
            super(holder, context);
        }

        /**
         * Check if the {@code actual} type is compatible with the {@code expected} type. If not,
         * register an incorrect type passed problem for the given {@link PyExpression}.
         *
         * @param at the {@link PyExpression} to register the problem for.
         * @param expected the command parameter {@link PyType} that the {@code actual} type must be compatible with.
         * @param actual the {@link PyType} passed to the command parameter by the user.
         */
        void registerProblemIfIncorrectType(PyExpression at, PyType expected, PyType actual) {
            if (PyTypeChecker.match(expected, actual, myTypeEvalContext)) {
                return;
            }

            var expectedName = PythonDocumentationProvider.getTypeName(expected, myTypeEvalContext);
            var actualName = PythonDocumentationProvider.getTypeName(actual, myTypeEvalContext);

            registerProblem(
                    at,
                    "Expected type '" + expectedName + "', got '" + actualName + "' instead",
                    ProblemHighlightType.WARNING);
        }

        void checkParamTypes(
                @NotNull PyClass node, Map<String, PyExpression> existingParams, Map<String, String> actualParams) {
            actualParams.forEach((name, type) -> {
                if (!existingParams.containsKey(name)) {
                    return;
                }

                var expectedType =
                        PyTypeParser.parse(node, type, myTypeEvalContext).getType();
                var actualType = myTypeEvalContext.getType(existingParams.get(name));
                registerProblemIfIncorrectType(existingParams.get(name), expectedType, actualType);
            });
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
            var optionalParams =
                    lbData.paramData().get(lbSuperclass.getQualifiedName()).optional();

            checkParamTypes(node, existingParameters, requiredParams);
            checkParamTypes(node, existingParameters, optionalParams);
        }
    }
}
