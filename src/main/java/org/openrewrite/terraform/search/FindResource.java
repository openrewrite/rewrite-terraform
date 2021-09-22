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
package org.openrewrite.terraform.search;

import lombok.EqualsAndHashCode;
import lombok.Value;
import org.openrewrite.ExecutionContext;
import org.openrewrite.Option;
import org.openrewrite.Recipe;
import org.openrewrite.hcl.HclVisitor;
import org.openrewrite.hcl.tree.Hcl;
import org.openrewrite.terraform.TerraformResource;

@Value
@EqualsAndHashCode(callSuper = true)
public class FindResource extends Recipe {
    @Option(displayName = "Resource name",
            description = "A Terraform resource name, without the quotes.",
            example = "aws_ebs_volume")
    String resourceName;

    @Override
    public String getDisplayName() {
        return "Find Terraform resource";
    }

    @Override
    public String getDescription() {
        return "Find a Terraform resource by resource type.";
    }

    @Override
    protected HclVisitor<ExecutionContext> getVisitor() {
        return new HclVisitor<ExecutionContext>() {
            @Override
            public Hcl visitBlock(Hcl.Block block, ExecutionContext executionContext) {
                Hcl.Block b = block;

                if (TerraformResource.isResource(b, resourceName)) {
                    b = b.withMarkers(block.getMarkers().searchResult());
                }

                // resources will only ever be found at the top level of a config file
                return b;
            }
        };
    }
}
