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
        return "Add terraform configuration";
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
