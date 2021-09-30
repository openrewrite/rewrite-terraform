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
import org.openrewrite.Tree;
import org.openrewrite.hcl.HclVisitor;
import org.openrewrite.hcl.tree.Expression;
import org.openrewrite.hcl.tree.Hcl;
import org.openrewrite.hcl.tree.HclRightPadded;
import org.openrewrite.hcl.tree.Space;
import org.openrewrite.marker.Markers;

/**
 * @see <a href="https://www.hashicorp.com/blog/terraform-0-12-preview-first-class-expressions">https://www.hashicorp.com/blog/terraform-0-12-preview-first-class-expressions</a>
 * @see <a href="https://www.terraform.io/upgrade-guides/0-12.html#first-class-expressions">https://www.terraform.io/upgrade-guides/0-12.html#first-class-expressions</a>
 */
@Incubating(since = "0.7.0")
public class UpgradeExpressions extends Recipe {
    @Override
    public String getDisplayName() {
        return "Upgrade expressions";
    }

    @Override
    public String getDescription() {
        return "Update expressions using string interpolation template syntax (`\"${var.variable}\"`) " +
                "to using first-class expression syntax (`var.variable`).";
    }

    @Override
    protected HclVisitor<ExecutionContext> getVisitor() {
        return new UpgradeExpression();
    }

    /**
     * https://github.com/hashicorp/terraform/blob/v0.12.31/configs/configupgrade/upgrade_expr.go
     */
    private static class UpgradeExpression extends HclVisitor<ExecutionContext> {
        @Override
        public Hcl visitConfigFile(Hcl.ConfigFile configFile, ExecutionContext ctx) {
            doAfterVisit(new UpgradeVariableTypes());
            return super.visitConfigFile(configFile, ctx);
        }

        @Override
        public Hcl visitQuotedTemplate(Hcl.QuotedTemplate template, ExecutionContext ctx) {
            if (template.getExpressions().size() == 1) {
                Expression e = template.getExpressions().get(0);
                if (e instanceof Hcl.TemplateInterpolation) {
                    Hcl.TemplateInterpolation t = (Hcl.TemplateInterpolation) e;
                    return t.getExpression().withPrefix(template.getPrefix());
                }
            }
            return super.visitQuotedTemplate(template, ctx);
        }

        @Override
        public Hcl visitFunctionCall(Hcl.FunctionCall functionCall, ExecutionContext ctx) {
            Hcl.FunctionCall f = (Hcl.FunctionCall) super.visitFunctionCall(functionCall, ctx);
            switch (f.getName().getName()) {
                case "list":
                    return new Hcl.Tuple(Tree.randomId(), f.getPrefix(), Markers.EMPTY, f.getPadding().getArguments());
                case "map":
                    // todo
                    break;
                case "lookup":
                    // A lookup call with only two arguments is equivalent to native index syntax.
                    // A third argument would specify a default value, so calls like that must be left alone.
                    if (f.getVariables().size() == 2) {
                        return new Hcl.Index(
                                Tree.randomId(),
                                f.getPrefix(),
                                Markers.EMPTY,
                                f.getVariables().get(0),
                                new Hcl.Index.Position(
                                        Tree.randomId(),
                                        Space.EMPTY,
                                        Markers.EMPTY,
                                        HclRightPadded.build(f.getVariables().get(1).withPrefix(Space.EMPTY))
                                )
                        );
                    }
                    break;
                case "element":
                    // todo
                    break;
                case "__builtin_BoolToString":
                    // todo
                    break;
                case "__builtin_FloatToString":
                    // todo
                    break;
                case "__builtin_IntToString":
                    // todo
                    break;
                case "__builtin_StringToInt":
                    // todo
                    break;
                case "__builtin_StringToFloat":
                    // todo
                    break;
                case "__builtin_StringToBool":
                    // todo
                    break;
                case "__builtin_FloatToInt":
                case "__builtin_IntToFloat":
                    // todo
                    break;
            }
            return f;
        }

    }

    /**
     * Use updated variable type syntax
     * <p>
     * Removes unnecessary quotation marks around variable type declarations.
     * Additionally, adjusts variable types of `list` and `map` to be explicitly type constrained by `string`
     * to be in compliance with Terraform 0.12.+ syntax. In Terraform 0.11 and earlier,
     * `list` and `map` really meant '`list` and `map` of type `string`.
     *
     * @see <a href="https://www.terraform.io/upgrade-guides/0-12.html#type-constraints-on-variables">https://www.terraform.io/upgrade-guides/0-12.html#type-constraints-on-variables</a>
     */
    private static class UpgradeVariableTypes extends HclVisitor<ExecutionContext> {
        @Override
        public Hcl visitBlock(Hcl.Block block, ExecutionContext ctx) {
            if (block.getType() != null && block.getType().getName().equals("variable")) {
                return super.visitBlock(block, ctx);
            }
            return block;
        }

        @Override
        public Hcl visitAttribute(Hcl.Attribute attribute, ExecutionContext ctx) {
            // todo
            // There is almost certainly a cleaner way to do this by extracting out some sort of "upgradeExpression"
            // logic or visitor, or using visitLiteral, etc. For the moment, going to leave this as-is, but
            // feel free to change this.
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
