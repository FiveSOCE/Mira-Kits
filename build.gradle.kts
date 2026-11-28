import java.net.URI
import java.security.MessageDigest

plugins { java }

group = "com.mira"
version = "0.1.12-1.18.2"

repositories {
    mavenCentral()
    maven("https://repo.papermc.io/repository/maven-public/")
}

val miraCoreVersion = "0.6.1-1.18.2"
val miraCoreSha256 = "91a1aacad3f1249877afdd5381d83998c33251bf08a30711f2a15c580feaee39"
val miraCoreJar = layout.projectDirectory.file("libs/MiraCore_1.18.2.jar").asFile

val essentialsVersion = "2.22.0"
val essentialsSha256 = "bda4685105977fca2e209820a9f0ad24275bd103390a03236f38e59bfdac58e6"
val essentialsJar = layout.projectDirectory.file("libs/EssentialsX-$essentialsVersion.jar").asFile

fun sha256(file: File): String {
    val digest = MessageDigest.getInstance("SHA-256")
    return digest.digest(file.readBytes()).joinToString("") { byte -> "%02x".format(byte) }
}

fun downloadVerified(url: String, target: File, expectedSha256: String) {
    if (target.exists() && sha256(target) == expectedSha256) return
    target.parentFile.mkdirs()
    URI(url).toURL().openStream().use { input -> target.outputStream().use { output -> input.copyTo(output) } }
    check(sha256(target) == expectedSha256) { "Downloaded dependency failed SHA-256 verification: ${target.name}" }
}

val downloadMiraDependencies by tasks.registering {
    doLast {
        downloadVerified("https://github.com/FiveSOCE/Mira-core/releases/download/dev-1.18.2/MiraCore_1.18.2.jar", miraCoreJar, miraCoreSha256)
        downloadVerified("https://github.com/EssentialsX/Essentials/releases/download/$essentialsVersion/EssentialsX-$essentialsVersion.jar", essentialsJar, essentialsSha256)
    }
}

val paperApiVersion = providers.gradleProperty("paperApiVersion").orElse("1.18.2-R0.1-SNAPSHOT")
val compileJavaVersion = providers.gradleProperty("compileJavaVersion").map(String::toInt).orElse(17)
val bytecodeJavaVersion = providers.gradleProperty("bytecodeJavaVersion").map(String::toInt).orElse(17)

dependencies {
    compileOnly("io.papermc.paper:paper-api:${paperApiVersion.get()}")
    compileOnly(files(miraCoreJar))
    compileOnly(files(essentialsJar))
    testImplementation(platform("org.junit:junit-bom:5.11.4"))
    testImplementation("org.junit.jupiter:junit-jupiter")
}

java { toolchain.languageVersion.set(JavaLanguageVersion.of(compileJavaVersion.get())) }

tasks.withType<JavaCompile>().configureEach {
    dependsOn(downloadMiraDependencies)
    options.encoding = "UTF-8"
    options.release.set(bytecodeJavaVersion.get())
}

tasks.test { useJUnitPlatform() }

tasks.jar { archiveFileName.set("MiraKits_1.18.2.jar") }
