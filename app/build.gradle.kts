plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
    id("org.jetbrains.kotlin.plugin.serialization")
    id("com.google.devtools.ksp")
}

android {
    namespace = "ps.anira.aid"
    compileSdk = 35

    defaultConfig {
        applicationId = "ps.anira.aid"
        // minSdk 26 (Android 8.0) — أعلى قليلاً من 24 عمداً: يسمح باستخدام أيقونة
        // adaptive-icon بصيغة XML/vector بحتة بدون الحاجة لملفات PNG احتياطية
        // (لا يمكنني إنتاج صور PNG حقيقية بهذه البيئة، والأيقونة النصية أوثق من محاولة تلفيقها).
        minSdk = 26
        targetSdk = 35
        versionCode = 7
        versionName = "0.7.0-excel-pdf-stats"
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
        debug {
            isMinifyEnabled = false
            applicationIdSuffix = ".debug"
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }

    buildFeatures {
        compose = true
    }

    testOptions {
        unitTests {
            isIncludeAndroidResources = true
            isReturnDefaultValues = true
        }
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

dependencies {
    val composeBom = platform("androidx.compose:compose-bom:2024.09.00")
    implementation(composeBom)

    implementation("androidx.core:core-ktx:1.13.1")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.6")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.6")
    implementation("androidx.activity:activity-compose:1.9.2")

    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")
    implementation("androidx.navigation:navigation-compose:2.8.1")

    // تخزين محلي — يعادل IndexedDB بالنسخة الويب
    implementation("androidx.room:room-runtime:2.6.1")
    implementation("androidx.room:room-ktx:2.6.1")
    ksp("androidx.room:room-compiler:2.6.1")

    // شبكة — لتكامل تيليجرام فقط (نفس نطاق استخدام الإنترنت بالضبط كما بنسخة الويب)
    implementation("com.squareup.okhttp3:okhttp:4.12.0")

    // حفظ إعدادات تيليجرام (توكن/chat id) محلياً — يعادل localStorage بالويب
    implementation("androidx.datastore:datastore-preferences:1.1.1")

    // بناء نسخة JSON الاحتياطية بأمان (تسلسل مبني على الأنواع) بدل تجميع نص يدوياً عرضة للأخطاء
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.7.3")

    testImplementation("junit:junit:4.13.2")
    testImplementation("org.robolectric:robolectric:4.14")
    testImplementation("androidx.test:core:1.6.1")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.8.1")
    androidTestImplementation("androidx.compose.ui:ui-test-junit4")
    debugImplementation("androidx.compose.ui:ui-tooling")
}
