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
import org.openrewrite.ExecutionContext;
import org.openrewrite.Option;
import org.openrewrite.Recipe;
import org.openrewrite.TreeVisitor;
import org.openrewrite.hcl.HclTemplate;
import org.openrewrite.hcl.HclVisitor;
import org.openrewrite.hcl.tree.BodyContent;
import org.openrewrite.hcl.tree.Hcl;

import java.time.Duration;
import java.util.List;

@Value
@EqualsAndHashCode(callSuper = false)
public class AddConfiguration extends Recipe {
    @Option(displayName = "Resource name",
            description = "A Terraform resource name, without the quotes.",
            example = "aws_ebs_volume")
    String resourceName;

    @Option(displayName = "Body content",
            description = "Terraform to insert if an attribute with the same name or block with the same 'type' is not found.",
            example = "encrypted = true")
    String content;

    @Override
    public String getDisplayName() {
        return "Add Terraform configuration";
    }

    @Override
    public String getDescription() {
        return "If the configuration has a different value, leave it alone. If it is missing, add it.";
    }

    @Override
    public Duration getEstimatedEffortPerOccurrence() {
        return Duration.ofMinutes(5);
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        return new HclVisitor<ExecutionContext>() {
            @Override
            public Hcl visitBlock(Hcl.Block block, ExecutionContext ctx) {
                Hcl.Block b = block;

                if (TerraformResource.isResource(b, resourceName)) {
                    b = HclTemplate.builder(content).build().apply(
                            getCursor(),
                            b.getCoordinates().last());

                    List<BodyContent> body = b.getBody();
                    BodyContent parsedContent = body.get(body.size() - 1);

                    String contentName;
                    if (parsedContent instanceof Hcl.Attribute) {
                        contentName = ((Hcl.Attribute) parsedContent).getSimpleName();
                    } else {
                        Hcl.Identifier type = ((Hcl.Block) parsedContent).getType();
                        assert type != null;
                        contentName = type.getName();
                    }

                    for (int i = 0; i < body.size() - 1; i++) {
                        BodyContent content1 = body.get(i);
                        if (content1 instanceof Hcl.Attribute && ((Hcl.Attribute) content1).getSimpleName().equals(contentName)) {
                            // discard the in-progress change and return
                            return block;
                        } else if (content1 instanceof Hcl.Block) {
                            Hcl.Block siblingBlock = (Hcl.Block) content1;
                            if (siblingBlock.getType() != null && siblingBlock.getType().getName().equals(contentName)) {
                                return block;
                            }
                        }
                    }
                }

                // resources will only ever be found at the top level of a config file
                return b;
            }
        };
    }
}
