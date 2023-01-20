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


import org.openrewrite.InMemoryExecutionContext;
import org.openrewrite.Recipe;
import org.openrewrite.SourceFile;
import org.openrewrite.config.Environment;
import org.openrewrite.hcl.HclParser;
import org.openrewrite.hcl.tree.Hcl;
import org.openrewrite.terraform.search.FindResource;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.List;
import java.util.function.BiPredicate;
import java.util.stream.Collectors;


public class RewriteTerraformProjectOnDisk {
    public static void main(String[] args) throws Exception {
        Path srcDir = Paths.get(args[0]);

        Recipe recipe = Environment.builder()
          .scanRuntimeClasspath()
          .build()
          .activateRecipes("org.openrewrite.terraform.aws.AWSBestPractices");

        BiPredicate<Path, BasicFileAttributes> predicate = (Path p, BasicFileAttributes bfa) ->
          bfa.isRegularFile() && p.getFileName().toString().endsWith(".tf");

        List<Path> paths = Files.find(srcDir, 999, predicate)
          .limit((args.length > 2) ? Long.parseLong(args[2]) : Long.MAX_VALUE)
          .collect(Collectors.toList());

        HclParser parser = HclParser.builder().build();
        InMemoryExecutionContext ctx = new InMemoryExecutionContext(Throwable::printStackTrace);
        List<Hcl.ConfigFile> sourceFiles = parser.parse(paths, srcDir, ctx);
        recipe.run(sourceFiles, ctx).getResults().forEach(it -> {
            System.out.println(it.diff());
            if(System.getenv("rewrite.autofix").equals("true")) {
                Charset charset = it.getAfter().getCharset() == null ? StandardCharsets.UTF_8 : it.getAfter().getCharset();
                try (BufferedWriter sourceFileWriter = Files.newBufferedWriter(it.getAfter().getSourcePath(), charset)) {
                    sourceFileWriter.write(new String(it.getAfter().printAll().getBytes(charset), charset));
                } catch (IOException e) {
                    throw new UncheckedIOException("Unable to rewrite source files", e);
                }
            }
        });

    }
}
