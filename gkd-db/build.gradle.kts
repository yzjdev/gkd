plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.android.kotlin.multiplatform.library)
    alias(libs.plugins.androidx.room)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.google.ksp)
}

kotlin {
    android {
        namespace = "li.gkd.db"
    }

    sourceSets {
        commonMain.dependencies {
            api(libs.androidx.room.runtime)
            api(libs.androidx.room.paging)
            api(libs.kotlinx.serialization.core)
            implementation(libs.kotlinx.serialization.json)
        }
        androidMain.dependencies {
            implementation(libs.androidx.sqlite.framework)
        }
    }
}

room3 {
    schemaDirectory("$projectDir/schemas")
}

dependencies {
    add("kspAndroid", libs.androidx.room.compiler)
}
