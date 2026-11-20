# اصحى واحسب — تطبيق أندرويد أصلي (Kotlin + Jetpack Compose)

تطبيق منبّه ذكي يعمل على **Android 9 (API 28) وما فوق حتى Android 15 (API 35)** بواجهة عربية كاملة من اليمين إلى اليسار (RTL).

## الآليات الأصلية المستخدمة في التطبيق

1. **العمل في الخلفية حتى بعد مسح التطبيق من التطبيقات الأخيرة:**
   - جدولة دقيقة عبر `AlarmManager.setAlarmClock()` داخل `AlarmScheduler.kt` لإيقاظ الجهاز من وضع Doze العميق.
   - تخزين دائم للمنبّهات في قاعدة بيانات **Room** (`AlarmDatabase.kt`).
   - بدء خدمة أمامية `AlarmRingingService` من نوع `mediaPlayback` عند استلام البث في `AlarmReceiver`.
2. **استعادة المنبّهات بعد إعادة تشغيل الهاتف:**
   - مستقبل `BootReceiver` مفعّل بـ `android:directBootAware="true"` يستمع إلى:
     - `LOCKED_BOOT_COMPLETED` (قبل فتح قفل الشاشة)
     - `BOOT_COMPLETED`
     - `TIME_SET` و `TIMEZONE_CHANGED` و `MY_PACKAGE_REPLACED`
     - `SCHEDULE_EXACT_ALARM_PERMISSION_STATE_CHANGED`
   - قاعدة البيانات مبنية فوق `createDeviceProtectedStorageContext()` لضمان قراءتها حتى في وضع Direct Boot.
3. **الصوت وشاشة القفل ومنع الإيقاف إلا بحل الرياضيات:**
   - تشغيل الصوت عبر `MediaPlayer` بمسار `AudioAttributes.USAGE_ALARM` + طلب `AudioFocusRequest` + رفع تدريجي للصوت + اهتزاز مستمر عبر `VibratorManager`/`Vibrator`.
   - إشعار عالي الأولوية (`IMPORTANCE_HIGH`) مع `setFullScreenIntent(...)` و `setOngoing(true)` بدون زر إغلاق.
   - شاشة `AlarmLockScreenActivity` تستخدم `setShowWhenLocked(true)` و `setTurnScreenOn(true)` وتعطّل زر الرجوع (`OnBackPressedCallback`) حتى يحل المستخدم المسألة الرياضية بنجاح، ثم تُرسل `ACTION_SOLVED_DISMISS` لإيقاف الخدمة.

## طريقة البناء والحصول على ملف APK

### الطريقة الأولى: عبر Android Studio (على الكمبيوتر)
1. افتح مجلد `isha-wahsib-android` داخل **Android Studio**.
2. انتظر انتهاء مزامنة Gradle وتحميل المكتبات.
3. لاستخراج ملف APK وتثبيته على الجوال:
   - من القائمة العلوية: **Build > Build Bundle(s) / APK(s) > Build APK(s)**
   - ستجد الملف جاهزاً في: `app/build/outputs/apk/debug/app-debug.apk`
   - انقل الملف إلى هاتفك وافتحه لتثبيت التطبيق (أو وصّل الهاتف بكابل USB مع تفعيل خيار مطوّري البرامج واضغط **Run**).

### الطريقة الثانية: استخراج APK سحابياً عبر GitHub (بدون تثبيت Android Studio)
1. أنشئ مستودعاً جديداً على **GitHub** وارفع إليه محتويات مجلد `isha-wahsib-android` (متضمناً مجلد `.github/workflows/build-apk.yml`).
2. افتح تبويب **Actions** في المستودع، وسيبدأ بناء ملف `app-debug.apk` تلقائياً خلال دقيقتين.
3. بعد اكتمال العملية، نزّل الملف المضغوط `isha-wahsib-apk` من أسفل صفحة النتيجة، وفك الضغط لتثبيت `app-debug.apk` على جوالك مباشرة.

