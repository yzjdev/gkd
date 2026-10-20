import li.gkd.gradle.ValidateStringResourcesTask

plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.google.ksp)
    alias(libs.plugins.androidx.room)
    alias(libs.plugins.codeorigin)
    alias(libs.plugins.remap)
}

android {
    namespace = "li.gkd.app"

    buildFeatures {
        compose = true
    }
}

val validateStringResources =
    tasks.register<ValidateStringResourcesTask>("validateStringResources") {
        resourceDirectory.set(layout.projectDirectory.dir("src/main/res"))
    }

tasks.matching { it.name.startsWith("mergeDebugResources") }.configureEach {
    dependsOn(validateStringResources)
}

room3 {
    schemaDirectory("$projectDir/schemas")
}

dependencies {
    api(project(":gkd-aidl"))
    api(libs.androidx.room.runtime)
    api(libs.androidx.room.paging)
    api(libs.androidx.paging.compose)
    api(libs.androidx.lifecycle.runtime.compose)
    api(libs.androidx.lifecycle.service)
    api(libs.androidx.lifecycle.viewmodel.compose)
    api(libs.androidx.lifecycle.viewmodel.navigation3)
    api(libs.androidx.navigation3.runtime)
    api(libs.androidx.navigation3.ui)
    api(libs.androidx.core.ktx)
    api(libs.androidx.annotation)
    api(libs.compose.activity)
    api(libs.androidx.splashscreen)
    api(libs.androidx.sqlite.framework)
    api(libs.kotlinx.coroutines.core)
    api(libs.kotlinx.atomicfu)
    api(libs.kotlinx.serialization.core)
    api(libs.kotlinx.serialization.json)
    api(libs.compose.runtime)
    api(libs.compose.foundation)
    api(libs.compose.animation)
    api(libs.compose.ui)
    api(libs.compose.ui.graphics)
    api(libs.compose.material3)
    api(libs.icons.extended)
    api(libs.coil.compose)
    api(libs.coil.network)
    api(libs.coil.gif)
    api(libs.ktor.client.core)
    api(libs.ktor.client.okhttp)
    api(libs.ktor.client.content.negotiation)
    api(libs.ktor.server.core)
    api(libs.ktor.server.cio)
    api(libs.ktor.server.content.negotiation)
    api(libs.ktor.serialization.kotlinx.json)
    api(libs.telephoto.zoomable)
    api(libs.reorderable)
    api(libs.morph.compose)
    api(libs.priv.kit.ui)
    api(libs.json5)
    api(libs.exp4j)
    api(libs.permissions)
    api(libs.device)
    api(libs.toaster)
    api(libs.google.accompanist.drawablepainter)
    api(libs.rikka.shizuku.api)
    api(libs.lsposed.hiddenapibypass)
    compileOnly(libs.codeorigin)

    ksp(libs.androidx.room.compiler)
    remapApi(project(":gkd-hidden-api"))
}
