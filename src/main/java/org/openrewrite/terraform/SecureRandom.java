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
import org.openrewrite.hcl.HclIsoVisitor;
import org.openrewrite.hcl.HclVisitor;
import org.openrewrite.hcl.tree.Hcl;
import org.openrewrite.internal.lang.Nullable;

import java.time.Duration;

@Value
@EqualsAndHashCode(callSuper = true)
public class SecureRandom extends Recipe {
    private static final int DEFAULT_MINIMUM = 20;

    @Option(displayName = "Byte length",
            description = "The minimum byte length to use.",
            required = false)
    @Nullable
    Integer byteLength;

    @Override
    public String getDisplayName() {
        return "Use a long enough byte length for `random` resources";
    }

    @Override
    public String getDescription() {
        return "Use a long enough byte length for `random` resources.";
    }

    @Override
    public Duration getEstimatedEffortPerOccurrence() {
        return Duration.ofMinutes(5);
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        return new HclIsoVisitor<ExecutionContext>() {
            @Override
            public Hcl.Attribute visitAttribute(Hcl.Attribute attribute, ExecutionContext ctx) {
                Hcl parent = getCursor().getParentOrThrow().getValue();
                if (parent instanceof Hcl.Block && TerraformResource.isResource((Hcl.Block) parent, "random_id")) {
                    if ("byte_length".equals(attribute.getSimpleName()) && attribute.getValue() instanceof Hcl.Literal) {
                        Hcl.Literal value = (Hcl.Literal) attribute.getValue();
                        int minLength = byteLength == null ? DEFAULT_MINIMUM : byteLength;
                        if (Integer.parseInt(value.getValueSource()) < minLength) {
                            return attribute.withValue(value.withValue(minLength)
                                    .withValueSource(Integer.toString(minLength)));
                        }
                    }
                }
                return attribute;
            }
        };
    }
}
