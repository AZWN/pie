import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
  id("org.metaborg.gradle.config.kotlin-library")
}

dependencies {
  api(project(":pie.api"))
  implementation(kotlin("stdlib-jdk8"))
  implementation(kotlin("reflect"))
  api("org.junit.jupiter:junit-jupiter-api:5.2.0")
  api("com.nhaarman:mockito-kotlin:1.5.0")
  implementation("com.google.jimfs:jimfs:1.1")
}

tasks.withType<KotlinCompile>().all {
  kotlinOptions.apiVersion = "1.2"
  kotlinOptions.languageVersion = "1.2"
}
