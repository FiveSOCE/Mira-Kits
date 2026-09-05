import java.net.URI
import java.security.MessageDigest

plugins { java }

group = "com.mira"
version = "0.1.9"

repositories {
    mavenCentral()
    maven("https://repo.papermc.io/repository/maven-public/")
}

val miraCoreVersion = "0.2.0"
val miraCoreSha256 = "66433a266a76088d2a2de90ac1beb1a5a183c26891ee8f394827b47830195b03"
val miraCoreJar = layout.projectDirectory.file("libs/MiraCore-$miraCoreVersion.jar").asFile

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
        downloadVerified("https://github.com/FiveSOCE/MIra-core/releases/download/v$miraCoreVersion/MiraCore-$miraCoreVersion.jar", miraCoreJar, miraCoreSha256)
        downloadVerified("https://github.com/EssentialsX/Essentials/releases/download/$essentialsVersion/EssentialsX-$essentialsVersion.jar", essentialsJar, essentialsSha256)
    }
}

dependencies {
    compileOnly("io.papermc.paper:paper-api:1.21.11-R0.1-SNAPSHOT")
    compileOnly(files(miraCoreJar))
    compileOnly(files(essentialsJar))
    testImplementation(platform("org.junit:junit-bom:5.11.4"))
    testImplementation("org.junit.jupiter:junit-jupiter")
}

java { toolchain.languageVersion.set(JavaLanguageVersion.of(21)) }

tasks.withType<JavaCompile>().configureEach {
    dependsOn(downloadMiraDependencies)
    options.encoding = "UTF-8"
    options.release.set(21)
}

tasks.test { useJUnitPlatform() }

tasks.jar { archiveFileName.set("MiraKits-${project.version}.jar") }
