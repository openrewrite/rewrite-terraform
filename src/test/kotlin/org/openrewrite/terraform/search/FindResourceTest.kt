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
package org.openrewrite.terraform.search

import org.junit.jupiter.api.Test
import org.openrewrite.Recipe
import org.openrewrite.hcl.HclRecipeTest

class FindResourceTest : HclRecipeTest {
    override val recipe: Recipe
        get() = FindResource("aws_ebs_volume")

    @Test
    fun findResource() = assertChanged(
        before = """
            resource "aws_ebs_volume" {
              size      = 1
              encrypted = true
            }
            
            resource "azure_storage_volume" {
              size = 1
            }
        """.trimIndent(),
        after = """
            /*~~>*/resource "aws_ebs_volume" {
              size      = 1
              encrypted = true
            }
            
            resource "azure_storage_volume" {
              size = 1
            }
        """.trimIndent()
    )
}
