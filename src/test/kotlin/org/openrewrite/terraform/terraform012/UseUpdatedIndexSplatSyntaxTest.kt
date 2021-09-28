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
 * @see <a href="https://github.com/hashicorp/terraform/tree/v0.12.31/configs/configupgrade/testdata/valid/indexed-splat">https://github.com/hashicorp/terraform/tree/v0.12.31/configs/configupgrade/testdata/valid/indexed-splat</a>
 */
@Suppress("RemoveCurlyBracesFromTemplate")
class UseUpdatedIndexSplatSyntaxTest : HclRecipeTest {
    override val recipe: Recipe
        get() = UpgradeExpressions()

    @Test
    @Disabled
    fun useUpdatedIndexSplatSyntax() = assertChanged(
        before = """
            resource "test_instance" "first_many" {
              count = 2
            }
            
            resource "test_instance" "one" {
              image = "${'$'}{test_instance.first_many.*.id[0]}"
            }
            
            resource "test_instance" "splat_of_one" {
              image = "${'$'}{test_instance.one.*.id[0]}"
            }
            
            resource "test_instance" "second_many" {
              count = "${'$'}{length(test_instance.first_many)}"
              security_groups = "${'$'}{test_instance.first_many.*.id[count.index]}"
            }
        """,
        after = """
            resource "test_instance" "first_many" {
              count = 2
            }
            
            resource "test_instance" "one" {
              image = test_instance.first_many[0].id
            }
            
            resource "test_instance" "splat_of_one" {
              image = test_instance.one.*.id[0]
            }
            
            resource "test_instance" "second_many" {
              count           = length(test_instance.first_many)
              security_groups = test_instance.first_many[count.index].id
            }
        """
    )

    @Test
    @Disabled("Syntax error at line 4:32 no viable alternative at input")
    @Issue("https://github.com/hashicorp/terraform/tree/v0.12.31/configs/configupgrade/testdata/valid/traversals")
    fun traversals() = assertChanged(
        before = """
            locals {
              simple = "${'$'}{test_instance.foo.bar}"
              splat  = "${'$'}{test_instance.foo.*.bar}"
              index  = "${'$'}{test_instance.foo.1.bar}"
            
              after_simple = "${'$'}{test_instance.foo.bar.0.baz}"
              after_splat  = "${'$'}{test_instance.foo.*.bar.0.baz}"
              after_index  = "${'$'}{test_instance.foo.1.bar.2.baz}"
            
              non_ident_attr = "${'$'}{test_instance.foo.bar.1baz}"
            
              remote_state_output       = "${'$'}{data.terraform_remote_state.foo.bar}"
              remote_state_attr         = "${'$'}{data.terraform_remote_state.foo.backend}"
              remote_state_idx_output   = "${'$'}{data.terraform_remote_state.foo.1.bar}"
              remote_state_idx_attr     = "${'$'}{data.terraform_remote_state.foo.1.backend}"
              remote_state_splat_output = "${'$'}{data.terraform_remote_state.foo.*.bar}"
              remote_state_splat_attr   = "${'$'}{data.terraform_remote_state.foo.*.backend}"
            
              has_index_should   = "${'$'}{test_instance.b.0.id}"
              has_index_shouldnt = "${'$'}{test_instance.c.0.id}"
              no_index_should    = "${'$'}{test_instance.a.id}"
              no_index_shouldnt  = "${'$'}{test_instance.c.id}"
            
              has_index_shouldnt_data = "${'$'}{data.terraform_remote_state.foo.0.backend}"
            }
            
            data "terraform_remote_state" "foo" {
              # This is just here to make sure the schema for this gets loaded to
              # support the remote_state_* checks above.
            }
            
            resource "test_instance" "a" {
              count = 1
            }
            
            resource "test_instance" "b" {
              count = "${'$'}{var.count}"
            }
            
            resource "test_instance" "c" {
            }
        """,
        after = """
            locals {
              simple = test_instance.foo.bar
              splat  = test_instance.foo.*.bar
              index  = test_instance.foo[1].bar
            
              after_simple = test_instance.foo.bar[0].baz
              after_splat  = test_instance.foo.*.bar.0.baz
              after_index  = test_instance.foo[1].bar[2].baz
            
              non_ident_attr = test_instance.foo.bar["1baz"]
            
              remote_state_output       = data.terraform_remote_state.foo.outputs.bar
              remote_state_attr         = data.terraform_remote_state.foo.backend
              remote_state_idx_output   = data.terraform_remote_state.foo[1].outputs.bar
              remote_state_idx_attr     = data.terraform_remote_state.foo[1].backend
              remote_state_splat_output = data.terraform_remote_state.foo.*.outputs.bar
              remote_state_splat_attr   = data.terraform_remote_state.foo.*.backend
            
              has_index_should   = test_instance.b[0].id
              has_index_shouldnt = test_instance.c.id
              no_index_should    = test_instance.a[0].id
              no_index_shouldnt  = test_instance.c.id
            
              has_index_shouldnt_data = data.terraform_remote_state.foo.backend
            }
            
            data "terraform_remote_state" "foo" {
              # This is just here to make sure the schema for this gets loaded to
              # support the remote_state_* checks above.
            }
            
            resource "test_instance" "a" {
              count = 1
            }
            
            resource "test_instance" "b" {
              count = var.count
            }
            
            resource "test_instance" "c" {
            }
        """
    )

}
