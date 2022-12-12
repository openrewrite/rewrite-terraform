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

import org.openrewrite.RecipeRun;
import org.openrewrite.Result;
import org.openrewrite.hcl.HclParser;
import org.openrewrite.hcl.tree.Hcl;

import java.util.List;

public class Example {
    public static void main(String[] args) {
        HclParser hclParser = HclParser.builder().build();

        // there are other forms of parse that take file paths, etc.
        List<Hcl.ConfigFile> tfs = hclParser.parse("" +
                "resource \"random_id\" \"random\" {\n" +
                "  byte_length = 11\n" +
                "}");

        RecipeRun recipeRun = new SecureRandom(20).run(tfs);

        for (Result result : recipeRun.getResults()) {
            // you could overwrite the original source with this string
            // result.getAfter().print();

            System.out.println(result.diff());
        }
    }
}
