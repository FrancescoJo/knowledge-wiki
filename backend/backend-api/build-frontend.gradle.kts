/**
 * build-frontend.gradle.kts
 *
 * Builds frontend npm modules and copies their IIFE bundles into the
 * processResources output so they are served as static assets.
 *
 * Applied via: apply(from = "build-frontend.gradle.kts")
 *
 * $Since: 2026-05-27
 */

fun readPackageVersion(dir: File): String {
    val raw = dir.resolve("package.json").readText()
    return Regex(""""version"\s*:\s*"([^"]+)"""").find(raw)!!.groupValues[1]
}

fun npmVersionTag(version: String): String = "v${version.replace(".", "_")}"

fun Project.registerNpmBundleBuild(name: String, dir: File): TaskProvider<Exec> =
    tasks.register<Exec>(name) {
        workingDir(dir)
        val shell = System.getenv("SHELL") ?: "/bin/bash"
        commandLine(shell, "-l", "-c", "npm run build:bundle")
        inputs.dir(dir.resolve("src"))
        inputs.files(dir.resolve("package.json"), dir.resolve("vite.bundle.config.ts"))
        outputs.dir(dir.resolve("dist-bundle"))
    }

val texteditDir = rootProject.projectDir.parentFile.resolve("frontend/common-libs/textedit")
val omnimemoDir = rootProject.projectDir.parentFile.resolve("frontend/omnimemo")

val texteditFileName = "textedit-${npmVersionTag(readPackageVersion(texteditDir))}.js"
val omnimemoFileName = "omnimemo-${npmVersionTag(readPackageVersion(omnimemoDir))}.js"

val buildTextedit = registerNpmBundleBuild("buildTextedit", texteditDir)
val buildOmnimemo = registerNpmBundleBuild("buildOmnimemo", omnimemoDir)

tasks.named<ProcessResources>("processResources") {
    dependsOn(buildTextedit, buildOmnimemo)
    from(texteditDir.resolve("dist-bundle")) { include(texteditFileName); into("static/lib") }
    from(omnimemoDir.resolve("dist-bundle")) { include(omnimemoFileName); into("static/lib") }
    filesMatching("templates/fragments/head.html") {
        filter { line ->
            line.replace("@textedit.script@", "/lib/$texteditFileName")
                .replace("@omnimemo.script@", "/lib/$omnimemoFileName")
        }
    }
}
