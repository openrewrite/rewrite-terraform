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
 * @see <a href="https://github.com/hashicorp/terraform/blob/v0.12.31/configs/configupgrade/testdata/valid/provisioner/input/provisioner.tf">https://github.com/hashicorp/terraform/blob/v0.12.31/configs/configupgrade/testdata/valid/provisioner/input/provisioner.tf</a>
 * @see <a href="https://www.terraform.io/upgrade-guides/0-12.html#default-settings-in-connection-blocks">https://www.terraform.io/upgrade-guides/0-12.html#default-settings-in-connection-blocks</a>
 */
@Suppress("RemoveCurlyBracesFromTemplate")
class UseUpdatedConnectionBlockTest : HclRecipeTest {
    override val recipe: Recipe
        get() = UpgradeExpressions()

    @Test
    @Disabled
    fun useUpdatedConnectionBlock() = assertChanged(
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
            variable "login_username" {
            }

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
