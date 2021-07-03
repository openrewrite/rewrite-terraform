package org.openrewrite.terraform

import org.junit.jupiter.api.Test
import org.openrewrite.Recipe
import org.openrewrite.hcl.HclRecipeTest

class AddConfigurationTest: HclRecipeTest {
    override val recipe: Recipe
        get() = AddConfiguration(
            "aws_ebs_volume",
            "encrypted = true"
        )

    @Test
    fun addEncryptedToEbsVolume() = assertChanged(
        before = """
            resource "aws_ebs_volume" {
              size = 1
            }
            
            resource "aws_ebs_volume" {
              # leave this one alone
              encrypted = false
            }
        """.trimIndent(),
        after = """
            resource "aws_ebs_volume" {
              size      = 1
              encrypted = true
            }
            
            resource "aws_ebs_volume" {
              # leave this one alone
              encrypted = false
            }
        """.trimIndent()
    )
}
