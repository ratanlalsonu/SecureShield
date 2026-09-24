# SecureShield
**ML-Based Android App Security and Storage Isolation**  
*"Run Unknown Apps. Stay Safe. Keep Your Storage Private."*

---

## 1. Project Overview
SecureShield is an Android security framework designed to protect user devices against untrusted and potentially malicious applications installed from external sources (such as third-party websites, Telegram, WhatsApp, file managers, and direct APK downloads). Instead of relying on manual file picking, SecureShield operates as a real system APK handler via Android Intent filtering. When an APK is opened from any external source, SecureShield automatically intercepts the APK URI, performs in-depth static bytecode and archive analysis, extracts a 30-dimensional behavioral feature vector, runs on-device Machine Learning risk inference trained on real Android malware characteristics (CICMalDroid 2020), presents a detailed security report with actionable findings, and enables genuine Android isolated execution via Android Managed Profiles (Work Profile) and Scoped Storage.

---

## 2. Problem Statement
Android users frequently download APKs from non-Play Store sources—including beta testing channels, mod repositories, chat messaging applications, and third-party app stores. These applications often:
- Request sensitive runtime permissions (SMS, Camera, Microphone, Fine Location, Contacts, Background Services).
- Utilize legacy `targetSdkVersion` to bypass modern Scoped Storage restrictions and runtime permission prompts.
- Contain undisclosed native `.so` libraries that execute unmanaged machine code.
- Possess overlay capabilities (`SYSTEM_ALERT_WINDOW`) and secondary installer permissions (`REQUEST_INSTALL_PACKAGES`) that enable phishing, tapjacking, or silent payload drops.

Standard Android PackageInstaller presents only a generic permission prompt without behavioral risk evaluation or execution isolation options. SecureShield bridges this critical security gap by providing pre-installation machine learning risk scoring and genuine profile-level storage protection.

---

## 3. Core Architecture & Workflow

### Interception & Inspection Flow:
```
External Application (Telegram / Chrome / WhatsApp / Files)
       │
       ▼  User taps downloaded APK
Android Intent Dispatch (ACTION_VIEW / ACTION_SEND)
       │  MIME: application/vnd.android.package-archive
       ▼
SecureShield Interception & Safe Ingestion
       │  • Path Traversal Prevention
       │  • Streaming SHA-256 Digest Calculation
       │  • ZIP/APK Header Signature Verification
       ▼
Static APK Archive & Bytecode Parsing
       │  • PackageManager Archive Parser
       │  • AndroidManifest Extraction (Permissions, Activities, Services, Receivers)
       │  • ZIP Inspection (Native .so libraries in lib/, DEX counts)
       │  • X.509 Certificate & Self-Signed Signing Detection
       ▼
Feature Extraction Pipeline (FeatureSchema)
       │  • 30-dimensional standardized feature vector
       ▼
On-Device ML Risk Classifier
       │  • Regularized Logistic Risk Model (Trained on CICMalDroid 2020 distributions)
       │  • Sigmoid Probability Inference P(malicious | x)
       │  • Risk Score (0-100) & Model Confidence %
       ▼
Security Report & Key Findings
       │  • Real extracted findings (Overlay, Boot persistence, Native libs, Legacy SDK)
       ▼
Installation Environment Recommendation
       │
       ├──► Option A: Secure Environment (Android Managed Profile / Isolated Storage)
       └──► Option B: Normal Installation (Android PackageInstaller via FileProvider)
       ▼
Post-Installation Lifecycle & Telemetry
       • Broadcast Package Monitoring (ACTION_PACKAGE_ADDED / REMOVED)
       • Real-time Security Alerts on Sensitive Permissions
       • Persistent Scan History in Local Room Database
```

---

## 4. Machine Learning Methodology & Feature Schema

SecureShield uses a 30-dimensional feature schema standardized to match the feature distribution profiles of Android malware datasets (such as CICMalDroid 2020 and Drebin):

| Index | Feature Name | Type | Description |
|---|---|---|---|
| 0 | `permission_count` | Numeric | Total number of requested permissions |
| 1 | `dangerous_permission_count` | Numeric | Runtime dangerous permissions requested |
| 2 | `has_sms_permissions` | Binary | READ_SMS, SEND_SMS, RECEIVE_SMS |
| 3 | `has_call_phone_permissions` | Binary | CALL_PHONE, READ_CALL_LOG, WRITE_CALL_LOG |
| 4 | `has_contacts_permissions` | Binary | READ_CONTACTS, WRITE_CONTACTS, GET_ACCOUNTS |
| 5 | `has_location_permissions` | Binary | ACCESS_FINE_LOCATION, ACCESS_COARSE_LOCATION |
| 6 | `has_camera_permission` | Binary | CAMERA |
| 7 | `has_audio_record_permission`| Binary | RECORD_AUDIO |
| 8 | `has_system_alert_window` | Binary | SYSTEM_ALERT_WINDOW (Overlay capability) |
| 9 | `has_request_install_packages`| Binary | REQUEST_INSTALL_PACKAGES (Secondary dropper) |
| 10 | `has_receive_boot_completed` | Binary | RECEIVE_BOOT_COMPLETED (Boot persistence) |
| 11 | `has_internet_permission` | Binary | INTERNET |
| 12 | `has_external_storage_write` | Binary | WRITE_EXTERNAL_STORAGE, MANAGE_EXTERNAL_STORAGE |
| 13 | `has_read_phone_state` | Binary | READ_PHONE_STATE (Device identifiers / IMEI) |
| 14 | `comb_storage_and_network` | Binary | Storage write + Internet connectivity |
| 15 | `comb_location_and_network`| Binary | Location tracking + Internet transmission |
| 16 | `comb_camera_and_mic` | Binary | Simultaneous visual and acoustic capture |
| 17 | `comb_sms_and_network` | Binary | SMS interception + Network exfiltration |
| 18 | `comb_boot_and_service` | Binary | Startup persistence + Background service execution |
| 19 | `comb_install_and_network` | Binary | Package installation + Network connectivity |
| 20 | `activity_count` | Numeric | Total registered activity components |
| 21 | `service_count` | Numeric | Total registered background services |
| 22 | `receiver_count` | Numeric | Total registered broadcast receivers |
| 23 | `provider_count` | Numeric | Total registered content providers |
| 24 | `exported_component_count` | Numeric | Attack surface (exported components) |
| 25 | `native_lib_count` | Numeric | Count of packaged `.so` shared libraries |
| 26 | `has_native_libs` | Binary | Indicator of native machine code presence |
| 27 | `dex_count` | Numeric | Count of DEX files (multidex / packing indicator) |
| 28 | `target_sdk_is_legacy` | Binary | targetSdk < 30 (bypassing Scoped Storage) |
| 29 | `cert_is_self_signed` | Binary | Certificate self-signed / debug keystore |

### Model Training Pipeline:
- The included script `train_model.py` trains the regularized classifier and generates the artifact `app/src/main/assets/model_weights.json`.
- Inference executes strictly on-device using exact normalized linear combination and sigmoid activation $P(\text{malicious} \mid \mathbf{x}) = \frac{1}{1 + e^{-z}}$.
- Metrics: Evaluated with Accuracy, Precision, Recall, F1-Score, and Confusion Matrix.

---

## 5. Storage Isolation & Android Security Model

### Genuine Android Isolation vs. Simulated Sandboxes
SecureShield **does not** create a fake "sandbox folder" on external storage. A directory on the filesystem does not isolate Linux process memory or prevent cross-process read access. SecureShield relies on the genuine Android security architecture:
1. **Linux UID Sandboxing:** Android assigns every installed application a unique Linux user ID (e.g., `u0_a145`). Process memory, internal data directories (`/data/data/<package>/`), and sockets are strictly isolated by Linux kernel permissions.
2. **SELinux Policy Enforcement:** Mandatory Access Control (MAC) policies restrict what domains and system capabilities apps can access.
3. **Android Scoped Storage (API 30+):** Apps target modern media and document providers, preventing arbitrary global access to `/sdcard/`.
4. **Android Managed Profiles (Work Profile):** SecureShield inspects device support for Managed Profiles via `UserManager` and `DevicePolicyManager`. In a Managed Profile, apps run under an entirely separate Linux user profile (User 10) with encrypted, profile-specific storage (`/data/user/10/`) and isolated IPC channels.

---

## 6. Privacy & Offline Guarantee
- **100% On-Device:** All APK decompression, hashing, manifest parsing, feature extraction, and ML risk classification occur strictly locally on the Android device.
- **No Cloud Uploads:** APK files and user metadata are never transmitted to any remote cloud servers.
- **Local Room Database:** Scan history, security alerts, and logs are maintained in an offline Room SQLite database on the device.

---

## 7. Setup & Build Instructions

### Prerequisites:
- Android Studio Ladybug or later / Gradle 9.x
- Android SDK 36 (minSdk 24)
- JDK 17 / 21

### Building the Project:
```bash
# Verify unit and Robolectric tests
./gradlew testDebugUnitTest

# Assemble debug APK
./gradlew assembleDebug
```

---

## 8. Final-Year Academic & Engineering Note
Developed as an engineering project demonstrating practical mobile systems security, static analysis, machine learning classification on real threat indicators, and Android enterprise profile architecture.
