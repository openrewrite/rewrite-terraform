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
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.openrewrite.Issue
import org.openrewrite.Recipe
import org.openrewrite.hcl.HclRecipeTest

@Suppress("RemoveCurlyBracesFromTemplate")
@Issue("https://github.com/openrewrite/rewrite-terraform/issues/6")
class UpgradeExpressionsTest : HclRecipeTest {
    override val recipe: Recipe
        get() = UpgradeExpressions()

    @Test
    fun updateQuotedTemplateWhenValueIsOnlyExpression() = assertChanged(
        before = """
            resource "aws_instance" "example" {
              ami                     = "before ${'$'}{var.ami}"
              instance_type           = "${'$'}{var.instance_type} after"

              vpc_security_group_ids  = "${'$'}{var.vpc_security_group_ids}"
            }
        """,
        after = """
            resource "aws_instance" "example" {
              ami                     = "before ${'$'}{var.ami}"
              instance_type           = "${'$'}{var.instance_type} after"

              vpc_security_group_ids  = var.vpc_security_group_ids
            }
        """
    )

    @Test
    fun doNotChangeExistingUpdatedExpressionSyntax() = assertUnchanged(
        before = """
            resource "aws_instance" "example" {
              ami                    = var.ami
              instance_type          = var.instance_type

              vpc_security_group_ids = var.vpc_security_group_ids
            }
        """
    )

    /**
     * https://github.com/hashicorp/terraform/tree/v0.12.31/configs/configupgrade/testdata/valid/nested-exprs-in-hcl
     */
    @Test
    fun nestedExpressions() = assertChanged(
        before = """
            locals {
              in_map = {
                foo = "${'$'}{var.baz}"
              }
              in_list = [
                "${'$'}{var.baz}",
                "${'$'}{var.bar}",
              ]
              in_list_one_line = ["${'$'}{var.baz}", "${'$'}{var.bar}"]
              in_map_of_list = {
                foo = ["${'$'}{var.baz}"]
              }
              in_list_of_map = [
                {
                  foo = "${'$'}{var.baz}"
                }
              ]
            }
        """,
        after = """
            locals {
              in_map = {
                foo = var.baz
              }
              in_list = [
                var.baz,
                var.bar,
              ]
              in_list_one_line = [var.baz, var.bar]
              in_map_of_list = {
                foo = [var.baz]
              }
              in_list_of_map = [
                {
                  foo = var.baz
                }
              ]
            }
        """
    )

    @Test
    fun keepTemplateInterpolationsWhenCombinedWithOtherExpressions() = assertChanged(
        before = """
            locals {
              formatted = "${'$'}{format("%s-${'$'}{var.subnet_suffix}", var.name)}"
            }
        """,
        after = """
            locals {
              formatted = format("%s-${'$'}{var.subnet_suffix}", var.name)
            }
        """
    )

    @Test
    @Disabled("Syntax error at line 2:64 no viable alternative at input") // https://github.com/terraform-aws-modules/terraform-aws-vpc/pull/265/files#diff-dc46acf24afd63ef8c556b77c126ccc6e578bc87e3aa09a931f33d9bf2532fbbR512
    fun multipleConditionals() = assertChanged(
        before = """
            resource "aws_network_acl_rule" "public_outbound" {
              count = "${'$'}{var.create_vpc && var.public_dedicated_network_acl && length(var.public_subnets) > 0 ? length(var.public_outbound_acl_rules) : 0}"
            }
        """,
        after = """
            resource "aws_network_acl_rule" "public_outbound" {
              count = var.create_vpc && var.public_dedicated_network_acl && length(var.public_subnets) > 0 ? length(var.public_outbound_acl_rules) : 0
            }
        """
    )

    @Test
    fun functions() = assertChanged(
        before = """
            locals {
              tags = "${'$'}{merge(var.common_tags, var.specific_tags)}"
            }
        """,
        after = """
            locals {
              tags = merge(var.common_tags, var.specific_tags)
            }
        """
    )

    @Test
    fun arithmetic() = assertChanged(
        before = """
            locals {
              add             = "${'$'}{1 + 2}"
              sub             = "${'$'}{1 - 2}"
              mul             = "${'$'}{1 * 2}"
              mod             = "${'$'}{4 % 2}"
              and             = "${'$'}{true && true}"
              or              = "${'$'}{true || true}"
              equal           = "${'$'}{1 == 2}"
              not_equal       = "${'$'}{1 != 2}"
              less_than       = "${'$'}{1 < 2}"
              greater_than    = "${'$'}{1 > 2}"
              less_than_eq    = "${'$'}{1 <= 2}"
              greater_than_eq = "${'$'}{1 >= 2}"
              neg             = "${'$'}{-local.add}"
            }
        """,
        after = """
            locals {
              add             = 1 + 2
              sub             = 1 - 2
              mul             = 1 * 2
              mod             = 4 % 2
              and             = true && true
              or              = true || true
              equal           = 1 == 2
              not_equal       = 1 != 2
              less_than       = 1 < 2
              greater_than    = 1 > 2
              less_than_eq    = 1 <= 2
              greater_than_eq = 1 >= 2
              neg             = -local.add
            }
        """
    )

    @Test
    fun methodCalls() = assertChanged(
        before = """
            locals {
              call_no_args  = "${'$'}{foo()}"
              call_one_arg  = "${'$'}{foo(1)}"
              call_two_args = "${'$'}{foo(1, 2)}"
            }
        """,
        after = """
            locals {
              call_no_args  = foo()
              call_one_arg  = foo(1)
              call_two_args = foo(1, 2)
            }
        """
    )

    @Test
    fun conditionals() = assertChanged(
        before = """
            locals {
              cond = "${'$'}{true ? 1 : 2}"
            }
        """,
        after = """
            locals {
              cond = true ? 1 : 2
            }
        """
    )

    @Test
    fun indexes() = assertChanged(
        before = """
            locals {
              index_str = "${'$'}{foo["a"]}"
              index_num = "${'$'}{foo[1]}"
            }
        """,
        after = """
            locals {
              index_str = foo["a"]
              index_num = foo[1]
            }
        """
    )

    @Test
    fun variableAccess() = assertChanged(
        before = """
            locals {
              var_access_single = "${'$'}{foo}"
              var_access_dot    = "${'$'}{foo.bar}"
              var_access_splat  = "${'$'}{foo.bar.*.baz}"
            }
        """,
        after = """
            locals {
              var_access_single = foo
              var_access_dot    = foo.bar
              var_access_splat  = foo.bar.*.baz
            }
        """
    )

    @Nested
    inner class UseUpdatedArgumentCommasTest {
        /**
         * https://github.com/hashicorp/terraform/tree/v0.12.31/configs/configupgrade/testdata/valid/argument-commas
         */
        @Test
        @Disabled
        fun useUpdatedArgumentCommas() = assertChanged(
            before = """
                locals {
                  foo = "bar", baz = "beep"
                }

                resource "test_instance" "foo" {
                  image = "b", type = "d"
                }
            """,
            after = """
                locals {
                  foo = "bar"
                  baz = "beep"
                }

                resource "test_instance" "foo" {
                  image = "b"
                  type  = "d"
                }
            """
        )
    }

    @Nested
    inner class UseUpdatedBlockSyntaxTest {
        /**
         * https://github.com/hashicorp/terraform/tree/v0.12.31/configs/configupgrade/testdata/valid/block-as-list-attr
         */
        @Test
        @Disabled
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

        /**
         * https://github.com/hashicorp/terraform/tree/v0.12.31/configs/configupgrade/testdata/valid/block-as-list-dynamic-item
         */
        @Test
        @Disabled
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

        /**
         * https://github.com/hashicorp/terraform/tree/v0.12.31/configs/configupgrade/testdata/valid/block-as-list-dynamic-nested
         */
        @Test
        @Disabled
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

        /**
         * https://github.com/hashicorp/terraform/tree/v0.12.31/configs/configupgrade/testdata/valid/block-as-list-dynamic-nested
         */
        @Test
        @Disabled
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

        /**
         * https://github.com/hashicorp/terraform/tree/v0.12.31/configs/configupgrade/testdata/valid/block-as-map-attr
         */
        @Test
        @Disabled
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

        /**
         * https://github.com/hashicorp/terraform/tree/v0.12.31/configs/configupgrade/testdata/valid/list-of-obj-as-block
         */
        @Test
        @Disabled
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

        /**
         * https://github.com/hashicorp/terraform/tree/v0.12.31/configs/configupgrade/testdata/valid/map-attr-as-block
         */
        @Test
        @Disabled
        fun mapAttributeAsBlock() = assertChanged(
            before = """
                resource "test_instance" "foo" {
                  type  = "z1.weedy"
                  image = "image-abc"
                  tags {
                    name = "beep"
                  }
                }
            """,
            after = """
                resource "test_instance" "foo" {
                  type  = "z1.weedy"
                  image = "image-abc"
                  tags = {
                    name = "beep"
                  }
                }
            """
        )

    }

    @Nested
    inner class UseUpdatedConnectionBlockTest {
        /**
         * https://github.com/hashicorp/terraform/blob/v0.12.31/configs/configupgrade/testdata/valid/provisioner/input/provisioner.tf
         * https://www.terraform.io/upgrade-guides/0-12.html#default-settings-in-connection-blocks
         */
        @Test
        @Disabled
        fun useUpdatedConnectionBlock() = assertChanged(
            expectedCyclesThatMakeChanges = 2, // todo
            before = """
                variable "login_username" {}

                resource "aws_instance" "foo" {
                  connection {
                    user = "${'$'}{var.login_username}"
                  }

                  provisioner "test" {
                    commands = "${'$'}{list("a", "b", "c")}"

                    when       = "create"
                    on_failure = "fail"

                    connection {
                      type = "winrm"
                      user = "${'$'}{var.login_username}"
                    }
                  }
                }
            """,
            after = """
                variable "login_username" {}

                resource "aws_instance" "foo" {
                  connection {
                    host = coalesce(self.public_ip, self.private_ip)
                    type = "ssh"
                    user = var.login_username
                  }

                  provisioner "test" {
                    commands = ["a", "b", "c"]

                    when       = create
                    on_failure = fail

                    connection {
                      host = coalesce(self.public_ip, self.private_ip)
                      type = "winrm"
                      user = var.login_username
                    }
                  }
                }
            """
        )
    }

    /**
     * https://github.com/hashicorp/terraform/blob/v0.12.31/configs/configupgrade/testdata/valid/funcs-replaced/want/funcs-replaced.tf
     */
    @Nested
    inner class UseUpdatedFunctionsTest {
        @Test
        @Issue("https://github.com/openrewrite/rewrite-terraform/issues/4")
        fun doNotChangeExistingListTupleSyntax() = assertUnchanged(
            before = """
                locals {
                  list = ["a", "b", "c"]
                }
            """
        )

        @Test
        @Issue("https://github.com/openrewrite/rewrite-terraform/issues/4")
        fun listTupleSyntax() = assertChanged(
            expectedCyclesThatMakeChanges = 2, // todo
            before = """
                locals {
                  list        = "${'$'}{list("a", "b", "c")}"
                  list_concat = "${'$'}{concat(list("a", "b"), list("c"))}"
                  list_empty  = "${'$'}{list()}"
                }
            """,
            after = """
                locals {
                  list        = ["a", "b", "c"]
                  list_concat = concat(["a", "b"], ["c"])
                  list_empty  = []
                }
            """
        )

        @Test
        @Issue("https://github.com/openrewrite/rewrite-terraform/issues/7")
        fun doNotChangeExistingMapObjectSyntax() = assertUnchanged(
            before = """
                locals {
                  tags0 = {
                    name = "Mabel"
                    age  = 52
                  }
                  tags1 = { name = "Mabel", age = 52 }
                }
            """
        )

        @Test
        @Issue("https://github.com/openrewrite/rewrite-terraform/issues/7")
        fun mapObjectSyntax() = assertChanged(
            expectedCyclesThatMakeChanges = 2, // todo
            before = """
                locals {
                  map         = "${'$'}{map("a", "b", "c", "d")}"
                  map_merge   = "${'$'}{merge(map("a", "b"), map("c", "d"))}"
                  map_empty   = "${'$'}{map()}"
                  map_invalid = "${'$'}{map("a", "b", "c")}"
                }
            """,
            after = """
                locals {
                  map         = {"a"="b","c"="d"}
                  map_merge   = merge({"a"="b"}, {"c"="d"})
                  map_empty   = {}
                  map_invalid = map("a", "b", "c")
                }
            """
        )

        @Test
        @Issue("https://github.com/openrewrite/rewrite-terraform/issues/8")
        fun doNotChangeExistingLookupSyntax() = assertUnchanged(
            before = """
                resource "aws_network_acl_rule" "private_inbound" {
                  count    = 5
                  protocol = var.private_inbound_acl_rules[count.index]["protocol"]
                }
            """
        )

        @Test
        @Issue("https://github.com/openrewrite/rewrite-terraform/issues/8")
        fun doNotChangeLookupSyntaxWithThreeArguments() = assertChanged(
            before = """
                locals {
                  lookup_ref = "${'$'}{lookup(local.map, "a", "default")}"
                }
            """,
            after = """
                locals {
                  lookup_ref = lookup(local.map, "a", "default")
                }
            """
        )

        @Test
        @Issue("https://github.com/openrewrite/rewrite-terraform/issues/8")
        fun lookupSyntaxLiteral() = assertChanged(
            expectedCyclesThatMakeChanges = 2, // todo
            before = """
                locals {
                  lookup_literal = "${'$'}{lookup({ a = "b" }, "a")}"
                }
            """,
            after = """
                locals {
                  lookup_literal = { a = "b" }["a"]
                }
            """
        )

        @Test
        @Issue("https://github.com/openrewrite/rewrite-terraform/issues/8")
        fun lookupSyntaxReference() = assertChanged(
            expectedCyclesThatMakeChanges = 2, // todo
            before = """
                locals {
                  lookup_ref = "${'$'}{lookup(local.map, "a")}"
                }
            """,
            after = """
                locals {
                  lookup_ref = local.map["a"]
                }
            """
        )

        @Test
        fun intermixedFunctionCalls() = assertChanged(
            expectedCyclesThatMakeChanges = 2, // todo
            before = """
                locals {
                  list_of_map = "${'$'}{list(map("a", "b"))}"
                  map_of_list = "${'$'}{map("a", list("b"))}"
                }
            """,
            after = """
                locals {
                  list_of_map = [{"a"="b"}]
                  map_of_list = {"a"=["b"]}
                }
            """
        )

        @Test
        @Disabled
        fun updateUndocumentedHILFunctions() = assertChanged(
            expectedCyclesThatMakeChanges = 2, // todo
            before = """
                locals {
                  # Undocumented HIL implementation details that some users nonetheless relied on.
                  conv_bool_to_string  = "${'$'}{__builtin_BoolToString(true)}"
                  conv_float_to_int    = "${'$'}{__builtin_FloatToInt(1.5)}"
                  conv_float_to_string = "${'$'}{__builtin_FloatToString(1.5)}"
                  conv_int_to_float    = "${'$'}{__builtin_IntToFloat(1)}"
                  conv_int_to_string   = "${'$'}{__builtin_IntToString(1)}"
                  conv_string_to_int   = "${'$'}{__builtin_StringToInt("1")}"
                  conv_string_to_float = "${'$'}{__builtin_StringToFloat("1.5")}"
                  conv_string_to_bool  = "${'$'}{__builtin_StringToBool("true")}"
                }
            """,
            after = """
                locals {
                  # Undocumented HIL implementation details that some users nonetheless relied on.
                  conv_bool_to_string  = tostring(tobool(true))
                  conv_float_to_int    = floor(1.5)
                  conv_float_to_string = tostring(tonumber(1.5))
                  conv_int_to_float    = floor(1)
                  conv_int_to_string   = tostring(floor(1))
                  conv_string_to_int   = floor(tostring("1"))
                  conv_string_to_float = tonumber(tostring("1.5"))
                  conv_string_to_bool  = tobool(tostring("true"))
                }
            """
        )
    }

    @Nested
    inner class UseUpdatedIndexSplatSyntaxTest {
        /**
         * https://github.com/hashicorp/terraform/tree/v0.12.31/configs/configupgrade/testdata/valid/indexed-splat
         */
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

        /**
         * https://github.com/hashicorp/terraform/tree/v0.12.31/configs/configupgrade/testdata/valid/traversals
         */
        @Test
        @Disabled("Syntax error at line 4:32 no viable alternative at input")
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
                  has_index_should_not = "${'$'}{test_instance.c.0.id}"
                  no_index_should    = "${'$'}{test_instance.a.id}"
                  no_index_should_not  = "${'$'}{test_instance.c.id}"

                  has_index_should_not_data = "${'$'}{data.terraform_remote_state.foo.0.backend}"
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
                  has_index_should_not = test_instance.c.id
                  no_index_should    = test_instance.a[0].id
                  no_index_should_not  = test_instance.c.id

                  has_index_should_not_data = data.terraform_remote_state.foo.backend
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

    @Nested
    inner class UseUpdatedListSyntaxTest {
        @Test
        @Disabled
        fun expressionsWithListsAndMaps() = assertChanged(
            expectedCyclesThatMakeChanges = 2, // todo
            before = """
                locals {
                  vpc_security_group_ids = "${'$'}{var.security_group_id != "" ? list(var.security_group_id) : list()}"
                }
            """,
            after = """
                locals {
                  vpc_security_group_ids = var.security_group_id != "" ? [var.security_group_id] : []
                }
            """
        )

        /**
         * https://github.com/hashicorp/terraform/tree/v0.12.31/configs/configupgrade/testdata/valid/redundant-list
         */
        @Test
        @Disabled
        fun redundantList() = assertChanged(
            expectedCyclesThatMakeChanges = 2, // todo
            before = """
                variable "list_example" {
                  type = "list"
                }

                resource "test_instance" "other" {
                  count = 2
                }

                resource "test_instance" "bad1" {
                  security_groups = ["${'$'}{test_instance.other.*.id}"]
                }

                resource "test_instance" "bad2" {
                  security_groups = ["${'$'}{var.list_example}"]
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
                  security_groups = ["not_a_list"]
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
                variable "list_example" {
                  type = list(string)
                }

                resource "test_instance" "other" {
                  count = 2
                }

                resource "test_instance" "bad1" {
                  security_groups = test_instance.other.*.id
                }

                resource "test_instance" "bad2" {
                  security_groups = var.list_example
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
                  security_groups = ["not_a_list"]
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

    @Nested
    inner class UseUpdatedNumberLiteralsTest {
        @Test
        @Disabled("Syntax error at line 6:23 extraneous input '=' expecting {'{', Identifier, QUOTE}.")
        @Issue("https://github.com/openrewrite/rewrite-terraform/issues/13")
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

    @Nested
    inner class UseUpdatedResourceCountReferencesTest {
        /**
         * https://github.com/hashicorp/terraform/tree/v0.12.31/configs/configupgrade/testdata/valid/resource-count-ref
         */
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

    @Nested
    @Issue("https://github.com/openrewrite/rewrite-terraform/issues/5")
    inner class UseUpdatedVariableTypesTest {
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

}
