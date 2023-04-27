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
package org.openrewrite.terraform;

import org.junit.jupiter.api.Test;
import org.openrewrite.DocumentExample;
import org.openrewrite.terraform.AddConfiguration;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;

import static org.openrewrite.hcl.Assertions.hcl;

class AddConfigurationTest implements RewriteTest {
    @Override
    public void defaults(RecipeSpec spec) {
        spec.recipe(new AddConfiguration("aws_ebs_volume",
            "encrypted = true"));
    }

    @DocumentExample
    @Test
    void addEncryptedToEbsVolume() {
        rewriteRun(
          hcl(
            """
              resource "aws_ebs_volume" {
                size = 1
              }

              resource "aws_ebs_volume" {
                # leave this one alone
                encrypted = false
              }
              """,
            """
              resource "aws_ebs_volume" {
                size      = 1
                encrypted = true
              }

              resource "aws_ebs_volume" {
                # leave this one alone
                encrypted = false
              }
              """
          )
        );
    }

    @Test
    void addPointInTimeRecoveryBlock() {
        rewriteRun(
          spec -> spec.recipe(new AddConfiguration(
            "aws_dynamodb_table",
            """
              point_in_time_recovery {
                enabled = true
              }
              """
          )),
          hcl(
            """
              resource "aws_dynamodb_table" {
                name = "GameScores"
              }
              """,
            """
              resource "aws_dynamodb_table" {
                name = "GameScores"
                point_in_time_recovery {
                  enabled = true
                }
              }
              """
          )
        );
    }
}
