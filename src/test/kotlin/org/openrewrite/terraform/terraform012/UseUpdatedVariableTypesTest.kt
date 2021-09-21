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
@Issue("https://github.com/openrewrite/rewrite-terraform/issues/5")
class UseUpdatedVariableTypesTest : HclRecipeTest {
    override val recipe: Recipe
        get() = UseUpdatedVariableTypes()

    @Test
    fun string() = assertChanged(
        before = """
            variable "str" {
              type = "string"
            }
        """,
        after = """
            variable "str" {
              type = string
            }
        """
    )

    @Test
    fun listOfString() = assertChanged(
        before = """
            variable "vpc_security_group_ids" {
              type = "list"
            }
        """,
        after = """
            variable "vpc_security_group_ids" {
              type = list(string)
            }
        """
    )

    @Test
    fun mapOfString() = assertChanged(
        before = """
            variable "tags" {
              type    = "map"

              default = {
                Name = "dev"
              }
            }
        """,
        after = """
            variable "tags" {
              type    = map(string)

              default = {
                Name = "dev"
              }
            }
        """
    )

    @Test
    @Disabled("check whether this is in-scope for this particular issue")
    fun boolean() = assertChanged(
        before = """
            variable "enabled" {
              default = "false"
              type = "bool"
            }
        """,
        after = """
            variable "enabled" {
              default = false
              type = bool
            }
        """
    )

}
