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
@Issue("https://github.com/openrewrite/rewrite-terraform/issues/6")
class UseFirstClassExpressionsTest : HclRecipeTest {
    override val recipe: Recipe
        get() = UseFirstClassExpressions()

    @Test
    fun removeCurlyBracesFromTemplate() = assertChanged(
        before = """
            variable "ami" {}
            variable "instance_type" {}
            variable "vpc_security_group_ids" {
              type = "list"
            }

            resource "aws_instance" "example" {
              ami                     = "${'$'}{var.ami}"
              instance_type           = "${'$'}{var.instance_type}"

              vpc_security_group_ids  = "${'$'}{var.vpc_security_group_ids}"
            }
        """,
        after = """
            variable "ami" {}
            variable "instance_type" {}
            variable "vpc_security_group_ids" {
              type = "list"
            }

            resource "aws_instance" "example" {
              ami                     = var.ami
              instance_type           = var.instance_type

              vpc_security_group_ids  = var.vpc_security_group_ids
            }
        """
    )

    @Test
    fun doNotChangeExistingUpdatedExpressionSyntax() = assertUnchanged(
        before = """
            variable "ami" {}
            variable "instance_type" {}
            variable "vpc_security_group_ids" {
              type = "list"
            }

            resource "aws_instance" "example" {
              ami                    = var.ami
              instance_type          = var.instance_type

              vpc_security_group_ids = var.vpc_security_group_ids
            }
        """
    )

    @Test
    @Disabled
    @Issue("https://github.com/openrewrite/rewrite-terraform/issues/7")
    fun removeCurlyBracesFromFunctions() = assertChanged(
        before = """
            tags = "${'$'}{merge(map("Name", "example"), var.common_tags)}"
        """,
        after = """
            tags = merge({ Name = "example" }, var.common_tags)
        """
    )

    @Test
    @Disabled
    fun expressionsWithListsAndMaps() = assertChanged(
        before = """
            resource "aws_instance" "example" {
              # The following works because the list structure is static
              vpc_security_group_ids = ["${'$'}{var.security_group_1}", "${'$'}{var.security_group_2}"]

              # The following doesn't work, because the [...] syntax isn't known to the interpolation language
              vpc_security_group_ids = "${'$'}{var.security_group_id != "" ? [var.security_group_id] : []}"

              # Instead, it's necessary to use the list() function
              vpc_security_group_ids = "${'$'}{var.security_group_id != "" ? list(var.security_group_id) : list()}"
            }
        """,
        after = """
            resource "aws_instance" "example" {
              vpc_security_group_ids = var.security_group_id != "" ? [var.security_group_id] : []
            }
        """
    )

}
