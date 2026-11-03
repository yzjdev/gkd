import li.gkd.gradle.ValidateStringResourcesTask

plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.android.kotlin.multiplatform.library)
    alias(libs.plugins.cmp)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.codeorigin)
    alias(libs.plugins.remap)
}

compose.resources {
    publicResClass = true
    packageOfResClass = "li.gkd.app.resources"
}

val validateStringResources =
    tasks.register<ValidateStringResourcesTask>("validateStringResources") {
        resourceDirectory.set(layout.projectDirectory.dir("src/commonMain/composeResources"))
    }

tasks
    .matching { it.name == "prepareComposeResourcesTaskForCommonMain" }
    .configureEach {
        dependsOn(validateStringResources)
    }

kotlin {
    android {
        namespace = "li.gkd.app"
        androidResources.enable = true
    }
    sourceSets {
        commonMain.dependencies {
            compileOnly(libs.codeorigin)
            api(libs.cmp.resources)
            api(project(":gkd-db"))
            api(project(":gkd-selector"))
            api(libs.kotlinx.coroutines.core)
            api(libs.kotlinx.atomicfu)
            implementation(libs.expression)
            api(libs.androidx.paging.compose)
            api(libs.coil.compose)
            api(libs.coil.network)
            api(libs.ktor.client.okhttp)
            api(libs.ktor.server.core)
            api(libs.ktor.server.cio)
            api(libs.ktor.server.content.negotiation)
            api(libs.ktor.client.content.negotiation)
            api(libs.ktor.serialization.kotlinx.json)
            api(libs.telephoto.zoomable)
            api(libs.reorderable)
            api(libs.kmp.viewmodel)
            api(libs.kmp.lifecycle)
            api(libs.kmp.navigation3)
            api(libs.androidx.navigation3.runtime)
            api(libs.kmp.viewmodel.navigation3)
            // AndroidX Runtime provides real implementations for both Android and JVM.
            api(libs.compose.runtime)
            api(libs.cmp.foundation)
            api(libs.cmp.ui)
            api(libs.cmp.ui.graphics)
            api(libs.cmp.animation)
            api(libs.cmp.material3)
            api(libs.cmp.icons)
            api(libs.morph.compose)
            api(libs.priv.kit.ui)
            api(libs.json5)
            api(libs.kotlinx.serialization.json)
        }
        androidMain.dependencies {
            api(libs.priv.kit.input)
            api(project(":gkd-aidl"))
            api(libs.androidx.lifecycle.service)
            api(libs.compose.activity)
            api(libs.rikka.shizuku.api)
            api(libs.rikka.shizuku.provider)
            api(libs.lsposed.hiddenapibypass)
            api(libs.google.accompanist.drawablepainter)
            api(libs.androidx.splashscreen)
            api(libs.coil.gif)
            api(libs.toaster)
            api(libs.permissions)
            api(libs.device)
            api(libs.androidx.core.ktx)
            api(libs.sweetline)
            // Control Android Compose independently of CMP; export the same versions to the app.
            api(libs.compose.animation)
            api(libs.compose.foundation)
            api(libs.compose.material3)
            api(libs.compose.ui)
            api(libs.compose.ui.graphics)
        }
    }
}

dependencies {
    remapApi(project(":gkd-hidden-api"))
}
