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
package org.openrewrite.terraform

import org.openrewrite.InMemoryExecutionContext
import org.openrewrite.Recipe
import org.openrewrite.SourceFile
import org.openrewrite.config.Environment
import org.openrewrite.hcl.HclParser
import org.openrewrite.terraform.search.FindResource
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.nio.file.attribute.BasicFileAttributes
import java.util.function.BiPredicate
import kotlin.io.path.ExperimentalPathApi
import kotlin.streams.toList

object RewriteTerraformProjectOnDisk {
    @ExperimentalPathApi
    @JvmStatic
    fun main(args: Array<String>) {
        val srcDir = Paths.get(args[0])
//        val recipe: Recipe = Class.forName(args[1]).getDeclaredConstructor().newInstance() as Recipe
//        val recipe = FindResource("aws_ebs_volume")

        val recipe = Environment.builder()
            .scanRuntimeClasspath()
            .build()
            .activateRecipes("org.openrewrite.terraform.aws.AWSBestPractices")

        val predicate = BiPredicate<Path, BasicFileAttributes> { p, bfa ->
            bfa.isRegularFile && p.fileName.toString().endsWith(".tf")
        }

        val paths = Files.find(srcDir, 999, predicate)
            .limit(if (args.size > 2) args[2].toLong() else Long.MAX_VALUE)
            .toList()

        val parser: HclParser = HclParser.builder().build()

        val sourceFiles: List<SourceFile> = parser.parse(paths, srcDir, InMemoryExecutionContext())
        recipe.run(sourceFiles, InMemoryExecutionContext { t -> t.printStackTrace() }).map {
            println(it.diff())
            if (System.getenv("rewrite.autofix")?.equals("true") == true) {
                it.after!!.sourcePath.toFile().writeText(it.after!!.printAll(), Charsets.UTF_8)
            }
        }
    }
}
