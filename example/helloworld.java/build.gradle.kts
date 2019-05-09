plugins {
  id("org.metaborg.gradle.config.java-application")
}

application {
  mainClassName = "mb.pie.example.helloworld.java.Main"
}

dependencies {
  compile(platform(project(":pie.depconstraints")))

  compile(project(":pie.runtime"))
  compile(project(":pie.store.lmdb"))

  compileOnly("org.checkerframework:checker-qual-android")
}

metaborg {
  javaCreatePublication = false // Do not publish example application.
}
