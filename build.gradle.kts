plugins {
    id("org.openrewrite.build.recipe-library") version "latest.release"
}

group = "org.openrewrite.recipe"
description = "Refactor Terraform. Automatically."

val rewriteVersion = rewriteRecipe.rewriteVersion.get()

dependencies {
    implementation("org.openrewrite:rewrite-hcl:${rewriteVersion}")
}
