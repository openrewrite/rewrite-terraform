package org.openrewrite.terraform.search

import org.junit.jupiter.api.Test
import org.openrewrite.Recipe
import org.openrewrite.hcl.HclRecipeTest

class FindResourceTest: HclRecipeTest {
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
