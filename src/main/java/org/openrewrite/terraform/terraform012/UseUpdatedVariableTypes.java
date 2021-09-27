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

/**
 * @see <a href="https://www.terraform.io/upgrade-guides/0-12.html#type-constraints-on-variables">https://www.terraform.io/upgrade-guides/0-12.html#type-constraints-on-variables</a>
 */
@Incubating(since = "0.7.0")
public class UseUpdatedVariableTypes extends Recipe {
    @Override
    public String getDisplayName() {
        return "Use updated variable type syntax";
    }

    @Override
    public String getDescription() {
        return "Removes unnecessary quotation marks around variable type declarations. " +
                "Additionally, adjusts variable types of `list` and `map` to be explicitly type constrained by `string` " +
                "to be in compliance with Terraform 0.12.+ syntax. In Terraform 0.11 and earlier, " +
                "`list` and `map` really meant '`list` and `map` of type `string`'.";
    }

    @Override
    protected HclVisitor<ExecutionContext> getVisitor() {
        return new UseUpdatedVariableTypesVisitor();
    }

    private static class UseUpdatedVariableTypesVisitor extends HclVisitor<ExecutionContext> {
        @Override
        public Hcl visitBlock(Hcl.Block block, ExecutionContext ctx) {
            // only continue processing on "variable" blocks, otherwise we can bail out early.
            if (block.getType() != null && block.getType().getName().equals("variable")) {
                return super.visitBlock(block, ctx);
            }
            return block;
        }

        @Override
        public Hcl visitAttribute(Hcl.Attribute attribute, ExecutionContext ctx) {
            // There is almost certainly a cleaner way to do this by extracting out some sort of "upgradeExpression"
            // logic or visitor, or using visitLiteral, etc. For the moment, going to leave this as-is, but
            // feel free to change this // todo
            Hcl.Attribute attr = (Hcl.Attribute) super.visitAttribute(attribute, ctx);
            if (attr.getSimpleName().equals("type")) {
                Expression e = attr.getValue();
                if (e instanceof Hcl.QuotedTemplate && !((Hcl.QuotedTemplate) e).getExpressions().isEmpty()) {
                    Expression first = ((Hcl.QuotedTemplate) e).getExpressions().get(0);
                    if (first instanceof Hcl.Literal) {
                        Hcl.Literal l = (Hcl.Literal) first;
                        if (l.getValueSource().equals("list")) {
                            l = l.withValueSource("list(string)");
                        } else if (l.getValueSource().equals("map")) {
                            l = l.withValueSource("map(string)");
                        }
                        return attr.withValue(l.withPrefix(e.getPrefix()));
                    }
                }
            }
            return attr;
        }
    }

}
