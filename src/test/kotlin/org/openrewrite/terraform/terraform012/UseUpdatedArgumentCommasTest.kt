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
package org.openrewrite.terraform.terraform012

import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import org.openrewrite.Issue
import org.openrewrite.Recipe
import org.openrewrite.hcl.HclRecipeTest

@Suppress("RemoveCurlyBracesFromTemplate")
@Issue("https://github.com/hashicorp/terraform/tree/v0.12.31/configs/configupgrade/testdata/valid/argument-commas")
class UseUpdatedArgumentCommasTest : HclRecipeTest {
    override val recipe: Recipe
        get() = UseFirstClassExpressions()

    @Test
    @Disabled
    fun useUpdatedArgumentCommas() = assertChanged(
        before = """
            locals {
              foo = "bar", baz = "boop"
            }

            resource "test_instance" "foo" {
              image = "b", type = "d"
            }
        """,
        after = """
            locals {
              foo = "bar"
              baz = "boop"
            }

            resource "test_instance" "foo" {
              image = "b"
              type  = "d"
            }
        """
    )

}
