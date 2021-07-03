package org.openrewrite.terraform.search;

import lombok.EqualsAndHashCode;
import lombok.Value;
import org.openrewrite.ExecutionContext;
import org.openrewrite.Option;
import org.openrewrite.Recipe;
import org.openrewrite.hcl.HclVisitor;
import org.openrewrite.hcl.marker.HclSearchResult;
import org.openrewrite.hcl.tree.Hcl;
import org.openrewrite.hcl.tree.Label;
import org.openrewrite.terraform.TerraformResource;

import static org.openrewrite.Tree.randomId;

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
        return "Find a Terraform resource by resource type";
    }

    @Override
    protected HclVisitor<ExecutionContext> getVisitor() {
        return new HclVisitor<ExecutionContext>() {
            @Override
            public Hcl visitBlock(Hcl.Block block, ExecutionContext executionContext) {
                Hcl.Block b = block;

                if (TerraformResource.isResource(b, resourceName)) {
                    b = b.withMarkers(block.getMarkers().addIfAbsent(new HclSearchResult(randomId(),
                            FindResource.this)));
                }

                // resources will only ever be found at the top level of a config file
                return b;
            }
        };
    }
}
