/*
 * Copyright 2021 the original author or authors.
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * https://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.openrewrite.terraform.terraform012;

import org.openrewrite.ExecutionContext;
import org.openrewrite.Incubating;
import org.openrewrite.Recipe;
import org.openrewrite.hcl.HclVisitor;
import org.openrewrite.hcl.tree.Expression;
import org.openrewrite.hcl.tree.Hcl;

import java.util.List;

/**
 * @see <a href="https://www.hashicorp.com/blog/terraform-0-12-preview-first-class-expressions">https://www.hashicorp.com/blog/terraform-0-12-preview-first-class-expressions</a>
 * @see <a href="https://www.terraform.io/upgrade-guides/0-12.html#first-class-expressions">https://www.terraform.io/upgrade-guides/0-12.html#first-class-expressions</a>
 */
@Incubating(since = "0.7.0")
public class UseFirstClassExpressions extends Recipe {
    @Override
    public String getDisplayName() {
        return "Use first class expressions";
    }

    @Override
    public String getDescription() {
        return "Replace expressions using string interpolation template syntax (`\"${var.variable}\"`) " +
                "to using first-class expression syntax (`var.variable`).";
    }

    @Override
    protected HclVisitor<ExecutionContext> getVisitor() {
        return new UseFirstClassExpressionsVisitor();
    }

    private static class UseFirstClassExpressionsVisitor extends HclVisitor<ExecutionContext> {
        @Override
        public Hcl visitQuotedTemplate(Hcl.QuotedTemplate template, ExecutionContext ctx) {
            List<Expression> expressions = template.getExpressions();
            if (!expressions.isEmpty()) {
                Expression e = template.getExpressions().get(0);
                if (e instanceof Hcl.TemplateInterpolation) {
                    Hcl.TemplateInterpolation t = (Hcl.TemplateInterpolation) e;
                    return t.getExpression().withPrefix(template.getPrefix());
                }
            }
            return super.visitQuotedTemplate(template, ctx);
        }
    }

}
