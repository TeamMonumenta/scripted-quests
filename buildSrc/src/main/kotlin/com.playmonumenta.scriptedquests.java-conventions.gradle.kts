plugins {
    `java-library`
    `maven-publish`
    checkstyle
    pmd
}

repositories {
    mavenLocal()
    maven {
        url = uri("https://papermc.io/repo/repository/maven-public/")
    }

    maven {
        url = uri("https://jitpack.io")
    }

    maven {
        url = uri("https://repo.mikeprimm.com/")
    }

    maven {
        url = uri("https://raw.githubusercontent.com/TeamMonumenta/monumenta-redis-sync/master/mvn-repo/")
    }

    maven {
        url = uri("https://libraries.minecraft.net")
    }

    maven {
        url = uri("https://repo.maven.apache.org/maven2/")
    }

    // NBT API
    maven {
        url = uri("https://repo.codemc.org/repository/maven-public/")
    }
}

group = "com.playmonumenta.scriptedquests"
java.sourceCompatibility = JavaVersion.VERSION_16
java.targetCompatibility = JavaVersion.VERSION_16

tasks.withType<JavaCompile>() {
    options.encoding = "UTF-8"
}

pmd {
    isConsoleOutput = true
    toolVersion = "6.41.0"
    ruleSets = listOf("$rootDir/pmd-ruleset.xml")
    setIgnoreFailures(true)
}
