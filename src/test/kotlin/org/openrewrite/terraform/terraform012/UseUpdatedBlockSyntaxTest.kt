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
 * @see <a href="___">___</a>
 */
@Suppress("RemoveCurlyBracesFromTemplate")
class UseUpdatedBlockSyntaxTest : HclRecipeTest {
    override val recipe: Recipe
        get() = UseFirstClassExpressions()

    @Test
    @Disabled
    @Issue("https://github.com/hashicorp/terraform/tree/v0.12.31/configs/configupgrade/testdata/valid/block-as-list-attr")
    fun blockAsListAttr() = assertChanged(
        before = """
            resource "test_instance" "foo" {
              network = [
                {
                  cidr_block = "10.1.0.0/16"
                },
                {
                  cidr_block = "10.2.0.0/16"
                },
              ]
            }
        """,
        after = """
            resource "test_instance" "foo" {
              network {
                cidr_block = "10.1.0.0/16"
              }
              network {
                cidr_block = "10.2.0.0/16"
              }
            }
        """
    )

    @Test
    @Disabled
    @Issue("https://github.com/hashicorp/terraform/tree/v0.12.31/configs/configupgrade/testdata/valid/block-as-list-dynamic-item")
    fun blockAsListDynamicItem() = assertChanged(
        before = """
            resource "test_instance" "foo" {
              network = [
                {
                  cidr_block = "10.1.2.0/24"
                },
                "${'$'}{var.baz}"
              ]
            }
        """,
        after = """
          resource "test_instance" "foo" {
          network {
            cidr_block = "10.1.2.0/24"
          }
          dynamic "network" {
            for_each = [var.baz]
            content {
              # TF-UPGRADE-TODO: The automatic upgrade tool can't predict
              # which keys might be set in maps assigned here, so it has
              # produced a comprehensive set here. Consider simplifying
              # this after confirming which keys can be set in practice.
        
              cidr_block = lookup(network.value, "cidr_block", null)
        
              dynamic "subnet" {
                for_each = lookup(network.value, "subnet", [])
                content {
                  number = subnet.value.number
                }
              }
            }
          }
            }
        """
    )

    @Test
    @Disabled
    @Issue("https://github.com/hashicorp/terraform/tree/v0.12.31/configs/configupgrade/testdata/valid/block-as-list-dynamic-nested")
    fun blockAsListDynamicNested() = assertChanged(
        before = """
            resource "test_instance" "foo" {
              network {
                subnet = "${'$'}{var.baz}"
              }
            }
        """,
        after = """
            resource "test_instance" "foo" {
              network {
                dynamic "subnet" {
                  for_each = var.baz
                  content {
                    # TF-UPGRADE-TODO: The automatic upgrade tool can't predict
                    # which keys might be set in maps assigned here, so it has
                    # produced a comprehensive set here. Consider simplifying
                    # this after confirming which keys can be set in practice.
            
                    number = subnet.value.number
                  }
                }
              }
            }
        """
    )

    @Test
    @Disabled
    @Issue("https://github.com/hashicorp/terraform/tree/v0.12.31/configs/configupgrade/testdata/valid/block-as-list-dynamic-nested")
    fun blockAsListDynamic() = assertChanged(
        before = """
            resource "test_instance" "foo" {
              network = "${'$'}{var.baz}"
            }
        """,
        after = """
            resource "test_instance" "foo" {
              dynamic "network" {
                for_each = var.baz
                content {
                  # TF-UPGRADE-TODO: The automatic upgrade tool can't predict
                  # which keys might be set in maps assigned here, so it has
                  # produced a comprehensive set here. Consider simplifying
                  # this after confirming which keys can be set in practice.
            
                  cidr_block = lookup(network.value, "cidr_block", null)
            
                  dynamic "subnet" {
                    for_each = lookup(network.value, "subnet", [])
                    content {
                      number = subnet.value.number
                    }
                  }
                }
              }
            }
        """
    )

    @Test
    @Disabled
    @Issue("https://github.com/hashicorp/terraform/tree/v0.12.31/configs/configupgrade/testdata/valid/block-as-map-attr")
    fun blockAsMapAttr() = assertChanged(
        before = """
            resource "test_instance" "foo" {
              network = {
                cidr_block = "10.1.0.0/16"
              }
            }
        """,
        after = """
            resource "test_instance" "foo" {
              network {
                cidr_block = "10.1.0.0/16"
              }
            }
        """
    )

    @Test
    @Disabled
    @Issue("https://github.com/hashicorp/terraform/tree/v0.12.31/configs/configupgrade/testdata/valid/list-of-obj-as-block")
    fun listOfObjectsAsBlock() = assertChanged(
        before = """
            resource "test_instance" "from_list" {
              list_of_obj = [
                {},
                {},
              ]
            }

            resource "test_instance" "already_blocks" {
              list_of_obj {}
              list_of_obj {}
            }

            resource "test_instance" "empty" {
              list_of_obj = []
            }
        """,
        after = """
            resource "test_instance" "from_list" {
              list_of_obj {
              }
              list_of_obj {
              }
            }

            resource "test_instance" "already_blocks" {
              list_of_obj {
              }
              list_of_obj {
              }
            }

            resource "test_instance" "empty" {
              list_of_obj = []
            }
        """
    )

    @Test
    @Disabled
    @Issue("https://github.com/hashicorp/terraform/tree/v0.12.31/configs/configupgrade/testdata/valid/map-attr-as-block")
    fun mapAttributeAsBlock() = assertChanged(
        before = """
            resource "test_instance" "foo" {
              type  = "z1.weedy"
              image = "image-abcd"
              tags {
                name = "boop"
              }
            }
        """,
        after = """
            resource "test_instance" "foo" {
              type  = "z1.weedy"
              image = "image-abcd"
              tags = {
                name = "boop"
              }
            }
        """
    )


}
