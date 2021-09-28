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
class UseUpdatedListSyntaxTest : HclRecipeTest {
    override val recipe: Recipe
        get() = UpgradeExpressions()

    @Test
    @Disabled
    @Issue("https://github.com/hashicorp/terraform/tree/v0.12.31/configs/configupgrade/testdata/valid/redundant-list")
    fun redundantList() = assertChanged(
        before = """
            variable "listy" {
              type = "list"
            }
            
            resource "test_instance" "other" {
              count = 2
            }
            
            resource "test_instance" "bad1" {
              security_groups = ["${'$'}{test_instance.other.*.id}"]
            }
            
            resource "test_instance" "bad2" {
              security_groups = ["${'$'}{var.listy}"]
            }
            
            resource "test_instance" "bad3" {
              security_groups = ["${'$'}{module.foo.outputs_always_dynamic}"]
            }
            
            resource "test_instance" "bad4" {
              security_groups = ["${'$'}{list("a", "b", "c")}"]
            }
            
            resource "test_instance" "bad5" {
              security_groups = ["${'$'}{test_instance.bad1.subnet_ids}"] # this one references a set
            }
            
            resource "test_instance" "bad6" {
              subnet_ids = ["${'$'}{test_instance.bad1.security_groups}"] # this one defines a set
            }
            
            resource "test_instance" "bad7" {
              subnet_ids = ["${'$'}{test_instance.bad1.*.id}"] # this one defines a set
            }
            
            # The rest of these should keep the same amount of list-ness
            
            resource "test_instance" "ok1" {
              security_groups = []
            }
            
            resource "test_instance" "ok2" {
              security_groups = ["notalist"]
            }
            
            resource "test_instance" "ok3" {
              security_groups = ["${'$'}{path.module}"]
            }
            
            resource "test_instance" "ok4" {
              security_groups = [["foo"], ["bar"]]
            }
            
            resource "test_instance" "ok5" {
              security_groups = "${'$'}{test_instance.other.*.id}"
            }
            
            resource "test_instance" "ok6" {
              security_groups = [
                "${'$'}{test_instance.other1.*.id}",
                "${'$'}{test_instance.other2.*.id}",
              ]
            }
        """,
        after = """
            variable "listy" {
              type = list(string)
            }
            
            resource "test_instance" "other" {
              count = 2
            }
            
            resource "test_instance" "bad1" {
              security_groups = test_instance.other.*.id
            }
            
            resource "test_instance" "bad2" {
              security_groups = var.listy
            }
            
            resource "test_instance" "bad3" {
              # TF-UPGRADE-TODO: In Terraform v0.10 and earlier, it was sometimes necessary to
              # force an interpolation expression to be interpreted as a list by wrapping it
              # in an extra set of list brackets. That form was supported for compatibility in
              # v0.11, but is no longer supported in Terraform v0.12.
              #
              # If the expression in the following list itself returns a list, remove the
              # brackets to avoid interpretation as a list of lists. If the expression
              # returns a single list item then leave it as-is and remove this TODO comment.
              security_groups = [module.foo.outputs_always_dynamic]
            }
            
            resource "test_instance" "bad4" {
              security_groups = ["a", "b", "c"]
            }
            
            resource "test_instance" "bad5" {
              security_groups = test_instance.bad1.subnet_ids # this one references a set
            }
            
            resource "test_instance" "bad6" {
              subnet_ids = test_instance.bad1.security_groups # this one defines a set
            }
            
            resource "test_instance" "bad7" {
              subnet_ids = test_instance.bad1.*.id # this one defines a set
            }
            
            # The rest of these should keep the same amount of list-ness
            
            resource "test_instance" "ok1" {
              security_groups = []
            }
            
            resource "test_instance" "ok2" {
              security_groups = ["notalist"]
            }
            
            resource "test_instance" "ok3" {
              security_groups = [path.module]
            }
            
            resource "test_instance" "ok4" {
              security_groups = [["foo"], ["bar"]]
            }
            
            resource "test_instance" "ok5" {
              security_groups = test_instance.other.*.id
            }
            
            resource "test_instance" "ok6" {
              security_groups = [
                test_instance.other1.*.id,
                test_instance.other2.*.id,
              ]
            }
        """
    )

}
