# قواعد النشر والتحديثات (Release & Versioning Rules)

لضمان عدم حدوث أي تشابه أو تكرار لرسائل التحديث مستقبلاً، يجب على أي مطور أو مساعد ذكاء اصطناعي الالتزام الصارم بالقواعد التالية عند إصدار أي تحديث:

## 1. القاعدة الذهبية: الاعتماد على `versionCode` (رقم البناء الرقمي للأندرويد)
- نظام التحقق من التحديثات داخل التطبيق في `GitHubUpdateService` يعتمد أولاً ومطلقاً على `versionCode`.
- **في كل تحديث جديد، يجب زيادة `versionCode` بمقدار 1 دائماً وبدون استثناء** (مثلاً: 22 -> 23 -> 24).
- مهما كان اسم الإصدار النصي، إذا كان `remote.versionCode <= current.versionCode`، فلن يظهر للمستخدم أي إشعار تحديث بالخطأ أبداً.

## 2. توحيد `versionName` و `tag_name` بدقة تامة:
- لا تضع أصفاراً زائدة أو تخلط بين الصيغ (مثل تسمية التاج `v2.10` ووضع الاسم في Gradle `2.1.0`).
- يجب أن يكون اسم الإصدار متطابقاً في جميع الملفات التالية في نفس اللحظة:
  1. `app/build.gradle.kts` -> `versionName = "2.XX"` و `versionCode = YY`
  2. `update.json` -> `"versionName": "2.XX"`, `"versionCode": YY`, `"tag_name": "v2.XX"`
  3. `FinancialViewModel.kt` -> القيمة الافتراضية للـ fallback
  4. وسم Git (Git tag) -> `v2.XX`

## 3. منع التخزين المؤقت (Cache-Busting):
- عند قراءة `update.json` عبر الرابط الخام لـ GitHub، تم تفعيل إضافة وسم منع الكاش الزمني `?nocache=timestamp` مع ترويسة `Cache-Control: no-cache` لتجنب قراءة Fastly CDN القديمة.
