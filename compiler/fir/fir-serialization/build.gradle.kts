plugins {
    kotlin("jvm")
    id("jps-compatible")
}

repositories {
    maven(url = "https://dl.bintray.com/kotlin/kotlinx")
}

dependencies {
    compile(project(":core:descriptors"))
    compile(project(":core:descriptors.jvm"))
    compile(project(":compiler:fir:cones"))
    compile(project(":compiler:fir:tree"))
    compile(project(":compiler:fir:resolve"))
    compileOnly(project(":compiler:ir.tree"))
    compileOnly(project(":compiler:fir:fir2ir"))
    compile(project(":compiler:frontend"))

    compileOnly(project(":kotlin-reflect-api"))
    compileOnly(intellijCoreDep()) { includeJars("intellij-core", rootProject = rootProject) }
}

sourceSets {
    "main" { projectDefault() }
    "test" { none() }
}
