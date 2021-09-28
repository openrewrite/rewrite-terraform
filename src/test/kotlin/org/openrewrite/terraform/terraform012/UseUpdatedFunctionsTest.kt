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
 * @see <a href="https://github.com/hashicorp/terraform/blob/v0.12.31/configs/configupgrade/testdata/valid/funcs-replaced/want/funcs-replaced.tf">https://github.com/hashicorp/terraform/blob/v0.12.31/configs/configupgrade/testdata/valid/funcs-replaced/want/funcs-replaced.tf</a>
 */
@Suppress("RemoveCurlyBracesFromTemplate")
@Issue("https://github.com/openrewrite/rewrite-terraform/issues/7")
class UseUpdatedFunctionsTest : HclRecipeTest {
    override val recipe: Recipe
        get() = UpgradeExpressions()

    @Test
    @Disabled
    fun removeCurlyBracesFromFunctions() = assertChanged(
        before = """
            tags = "${'$'}{merge(map("Name", "example"), var.common_tags)}"
        """,
        after = """
            tags = merge({ Name = "example" }, var.common_tags)
        """
    )

    @Test
    @Issue("https://github.com/openrewrite/rewrite-terraform/issues/4")
    fun doNotChangeExistingListBracketSyntax() = assertUnchanged(
        before = """
            locals {
              list = ["a", "b", "c"]
            }
        """
    )

    @Test
    @Issue("https://github.com/openrewrite/rewrite-terraform/issues/4")
    fun listSyntax() = assertChanged(
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
    @Disabled
    fun mapSyntax() = assertChanged(
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
              map = {
                "a" = "b"
                "c" = "d"
              }
              map_merge = merge(
                {
                  "a" = "b"
                },
                {
                  "c" = "d"
                },
              )
              map_empty   = {}
              map_invalid = map("a", "b", "c")
            }
        """
    )

    @Test
    @Issue("https://github.com/openrewrite/rewrite-terraform/issues/8")
    @Disabled
    fun lookupSyntax() = assertChanged(
        expectedCyclesThatMakeChanges = 2, // todo
        before = """
            locals {
              lookup_literal = "${'$'}{lookup(map("a", "b"), "a")}"
              lookup_ref     = "${'$'}{lookup(local.map, "a")}"
            }
        """,
        after = """
            locals {
              lookup_literal = {
                a = "b"
              }["a"]
              lookup_ref = local.map["a"]
            }
        """
    )

    @Test
    @Disabled
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
              list_of_map = [
                {
                  "a" = "b"
                },
              ]
              map_of_list = {
                "a" = ["b"]
              }
            }
        """
    )

    @Test
    @Disabled
    fun updateUndocumentedHILFunctions() = assertChanged(
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
