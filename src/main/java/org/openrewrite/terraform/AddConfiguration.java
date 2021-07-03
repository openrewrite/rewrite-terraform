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
package org.openrewrite.terraform;

import lombok.EqualsAndHashCode;
import lombok.Value;
import org.openrewrite.*;
import org.openrewrite.hcl.HclParser;
import org.openrewrite.hcl.HclTemplate;
import org.openrewrite.hcl.HclVisitor;
import org.openrewrite.hcl.tree.BodyContent;
import org.openrewrite.hcl.tree.Hcl;

import java.util.List;

@Value
@EqualsAndHashCode(callSuper = true)
public class AddConfiguration extends Recipe {

    @Option(displayName = "Resource name",
            description = "A Terraform resource name, without the quotes.",
            example = "aws_ebs_volume")
    String resourceName;

    @Option(displayName = "Attribute",
            description = "A Terraform attribute to insert if an attribute with the same name is not found.",
            example = "encrypted = true")
    String attribute;

    @Override
    public String getDisplayName() {
        return "Add Terraform configuration";
    }

    @Override
    public String getDescription() {
        return "If the configuration has a different value, leave it alone. If it is missing, add it.";
    }

    @Override
    public Validated validate() {
        return super.validate().and(Validated.test("attribute", "The configuration must be an attribute.",
                attribute, a -> HclParser.builder().build().parse(attribute).get(0).getBody().get(0) instanceof Hcl.Attribute));
    }

    @Override
    protected HclVisitor<ExecutionContext> getVisitor() {
        return new HclVisitor<ExecutionContext>() {
            @Override
            public Hcl visitBlock(Hcl.Block block, ExecutionContext executionContext) {
                Hcl.Block b = block;

                if (TerraformResource.isResource(b, resourceName)) {
                    b = b.withTemplate(HclTemplate.builder(getCursor()::getParent, attribute).build(),
                            b.getCoordinates().last());

                    List<BodyContent> body = b.getBody();
                    Hcl.Attribute added = (Hcl.Attribute) body.get(body.size() - 1);

                    for (int i = 0; i < body.size() - 1; i++) {
                        BodyContent content = body.get(i);
                        if (content instanceof Hcl.Attribute && ((Hcl.Attribute) content).getSimpleName().equals(added.getSimpleName())) {
                            // discard the in-progress change and return
                            return block;
                        }
                    }
                }

                // resources will only ever be found at the top level of a config file
                return b;
            }
        };
    }
}
