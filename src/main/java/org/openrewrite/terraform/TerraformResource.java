package org.openrewrite.terraform;

import org.openrewrite.hcl.tree.Hcl;
import org.openrewrite.hcl.tree.Label;

public class TerraformResource {
    private TerraformResource() {
    }

    public static boolean isResource(Hcl.Block block, String resourceName) {
        for (Label label : block.getLabels()) {
            if (label instanceof Hcl.Identifier) {
                return ((Hcl.Identifier) label).getName().equals(resourceName);
            } else if (label instanceof Hcl.Literal) {
                return ((Hcl.Literal) label).getValue().toString().equals(resourceName);
            }
        }
        return false;
    }
}
