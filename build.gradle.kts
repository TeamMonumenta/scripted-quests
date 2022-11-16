plugins {
    id("com.palantir.git-version") version "0.12.2"
}

description = "ScriptedQuests"
val gitVersion: groovy.lang.Closure<String> by extra
version = gitVersion()
