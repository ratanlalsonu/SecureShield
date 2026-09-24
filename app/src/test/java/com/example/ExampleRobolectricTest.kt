package com.example

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.AppDatabase
import com.example.data.local.ScanResultEntity
import com.example.domain.model.ApkMetadata
import com.example.domain.model.PermissionDetail
import com.example.ml.FeatureExtractor
import com.example.ml.MLRiskClassifier
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ExampleRobolectricTest {

    @Test
    fun `read string from context`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("SecureShield", appName)
    }

    @Test
    fun `feature extractor produces 30 features`() {
        val dummyMetadata = ApkMetadata(
            fileName = "sample.apk",
            appName = "SampleApp",
            packageName = "com.sample.app",
            versionName = "1.0",
            versionCode = 1,
            minSdk = 24,
            targetSdk = 34,
            fileSize = 1024L * 1024L,
            sha256 = "dummy_sha256_hash",
            sourceApp = "Telegram",
            permissions = listOf(
                PermissionDetail(
                    name = "android.permission.INTERNET",
                    simpleName = "INTERNET",
                    isDangerous = false,
                    group = "Network",
                    description = "Full network access"
                ),
                PermissionDetail(
                    name = "android.permission.CAMERA",
                    simpleName = "CAMERA",
                    isDangerous = true,
                    group = "Camera",
                    description = "Take photos"
                )
            )
        )

        val features = FeatureExtractor.extractFeatures(dummyMetadata)
        assertEquals(30, features.size)
        assertEquals(2f, features[0]) // total perms
        assertEquals(1f, features[1]) // dangerous perms
        assertEquals(1f, features[6]) // camera perm
        assertEquals(1f, features[11]) // internet perm
    }

    @Test
    fun `ml risk classifier returns valid score in range`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val classifier = MLRiskClassifier(context)

        val dummyMetadata = ApkMetadata(
            fileName = "sample.apk",
            appName = "SampleApp",
            packageName = "com.sample.app",
            versionName = "1.0",
            versionCode = 1,
            minSdk = 24,
            targetSdk = 34,
            fileSize = 2048L,
            sha256 = "sample_sha256",
            sourceApp = "Web Browser"
        )

        val eval = classifier.evaluate(dummyMetadata)
        assertTrue(eval.score in 0..100)
        assertTrue(eval.confidence in 50..100)
        assertTrue(eval.probability in 0f..1f)
        assertNotNull(eval.level)
        assertTrue(eval.recommendation.isNotBlank())
    }

    @Test
    fun `room database stores and retrieves scan records`() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java).build()
        val dao = db.scanResultDao()

        val record = ScanResultEntity(
            apkSha256 = "abc123sha256",
            packageName = "com.test.target",
            appName = "Target App",
            versionName = "2.1.0",
            versionCode = 21,
            fileSize = 4096000L,
            scanTimestamp = System.currentTimeMillis(),
            riskScore = 75,
            riskLevel = "HIGH",
            modelConfidence = 84,
            detectedPermissionsCount = 14,
            dangerousPermissionsCount = 6,
            sensitiveFindingsJson = "[\"Requests Camera & Mic\"]",
            sourceApp = "WhatsApp",
            certSha256 = "cert_fingerprint",
            recommendation = "Run in Secure Environment"
        )

        val insertedId = dao.insertScan(record)
        assertTrue(insertedId > 0)

        val retrieved = dao.getScanBySha256("abc123sha256")
        assertNotNull(retrieved)
        assertEquals("Target App", retrieved?.appName)
        assertEquals(75, retrieved?.riskScore)

        val count = dao.getScanCount().first()
        assertEquals(1, count)

        val allScans = dao.getAllScansList()
        assertEquals(1, allScans.size)
        assertEquals("com.test.target", allScans[0].packageName)

        val alertDao = db.securityAlertDao()
        val alertId = alertDao.insertAlert(
            com.example.data.local.SecurityAlertEntity(
                packageName = "com.test.target",
                appName = "Target App",
                alertType = "HIGH_RISK_APP_INSTALLED",
                severity = "HIGH",
                timestamp = System.currentTimeMillis(),
                description = "High risk app installed"
            )
        )
        assertTrue(alertId > 0)
        assertEquals(1, alertDao.getAlertCount().first())

        db.close()
    }

    @Test
    fun `theme preferences manager persists and restores theme configuration`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val prefs = com.example.ui.theme.ThemePreferencesManager(context)

        // Initial default
        val initial = prefs.getThemeConfig()
        assertEquals(com.example.ui.theme.AppThemePalette.CYBER_BLUE, initial.palette)
        assertEquals(com.example.ui.theme.ThemeMode.SYSTEM, initial.themeMode)
        assertEquals(false, initial.amoledPureBlack)

        // Save new settings
        prefs.savePalette(com.example.ui.theme.AppThemePalette.EMERALD_SENTINEL)
        prefs.saveThemeMode(com.example.ui.theme.ThemeMode.DARK)
        prefs.saveAmoledPureBlack(true)

        val updated = prefs.getThemeConfig()
        assertEquals(com.example.ui.theme.AppThemePalette.EMERALD_SENTINEL, updated.palette)
        assertEquals(com.example.ui.theme.ThemeMode.DARK, updated.themeMode)
        assertEquals(true, updated.amoledPureBlack)

        // Verify color scheme resolution with AMOLED
        val scheme = com.example.ui.theme.resolveColorScheme(
            palette = updated.palette,
            darkTheme = true,
            amoledPureBlack = updated.amoledPureBlack,
            context = context
        )
        assertEquals(androidx.compose.ui.graphics.Color.Black, scheme.background)
    }
}
