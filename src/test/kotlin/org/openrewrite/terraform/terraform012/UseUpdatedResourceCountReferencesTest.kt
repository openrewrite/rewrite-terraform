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
import org.openrewrite.Recipe
import org.openrewrite.hcl.HclRecipeTest

/**
 * @see <a href="https://github.com/hashicorp/terraform/tree/v0.12.31/configs/configupgrade/testdata/valid/resource-count-ref">https://github.com/hashicorp/terraform/tree/v0.12.31/configs/configupgrade/testdata/valid/resource-count-ref</a>
 */
@Suppress("RemoveCurlyBracesFromTemplate")
class UseUpdatedResourceCountReferencesTest : HclRecipeTest {
    override val recipe: Recipe
        get() = UpgradeExpressions()

    @Test
    @Disabled
    fun useUpdatedResourceCountReferences() = assertChanged(
        before = """
            resource "test_instance" "one" {
            }

            resource "test_instance" "many" {
              count = 2
            }

            data "terraform_remote_state" "one" {
            }

            data "terraform_remote_state" "many" {
              count = 2
            }

            output "managed_one" {
              value = "${'$'}{test_instance.one.count}"
            }

            output "managed_many" {
              value = "${'$'}{test_instance.many.count}"
            }

            output "data_one" {
              value = "${'$'}{data.terraform_remote_state.one.count}"
            }

            output "data_many" {
              value = "${'$'}{data.terraform_remote_state.many.count}"
            }
        """,
        after = """
            resource "test_instance" "one" {
            }

            resource "test_instance" "many" {
              count = 2
            }

            data "terraform_remote_state" "one" {
            }

            data "terraform_remote_state" "many" {
              count = 2
            }

            output "managed_one" {
              value = 1
            }

            output "managed_many" {
              value = length(test_instance.many)
            }

            output "data_one" {
              value = 1
            }

            output "data_many" {
              value = length(data.terraform_remote_state.many)
            }
        """
    )

}
