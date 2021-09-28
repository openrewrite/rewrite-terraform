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

/**
 * @see <a href="https://github.com/hashicorp/terraform/tree/v0.12.31/configs/configupgrade/testdata/valid/number-literals">https://github.com/hashicorp/terraform/tree/v0.12.31/configs/configupgrade/testdata/valid/number-literals</a>
 */
@Suppress("RemoveCurlyBracesFromTemplate")
@Issue("https://github.com/openrewrite/rewrite-terraform/issues/13")
class UseUpdatedNumberLiteralsTest : HclRecipeTest {
    override val recipe: Recipe
        get() = UseFirstClassExpressions()

    @Test
    @Disabled
    fun useUpdatedNumberLiterals() = assertChanged(
        before = """
            locals {
              decimal_int          = 1
              decimal_float        = 1.5
              decimal_float_tricky = 0.1
              hex_int              = 0xff
              octal_int            = 0777
            }
        """,
        after = """
            locals {
              decimal_int          = 1
              decimal_float        = 1.5
              decimal_float_tricky = 0.1
              hex_int              = 255
              octal_int            = 511
            }
        """
    )

}
