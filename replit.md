# مشروع اختصاراتي

## نظرة عامة

هذا مشروع Android أصلي مكتوب بـ Kotlin وJetpack Compose. يتكون التطبيق من:

- شاشة حديثة لإدارة اختصارات النصوص مع البحث والتثبيت والمعاينة.
- خدمة Text Expander تعتمد على Android Accessibility.
- قاعدة بيانات Room محلية لتخزين الاختصارات.

## التشغيل والبناء

المشروع ليس تطبيق ويب، لذلك لا يحتاج إلى Workflow لعرض Preview. استخدم Android
Studio أو GitHub Actions مع Android SDK Platform 36. أمر البناء المحلي:

```bash
./gradlew assembleDebug
```

الناتج هو `app/build/outputs/apk/debug/app-debug.apk`. ملف
`.github/workflows/android.yml` يبني APK تلقائياً باستخدام Gradle 9.3.1 ويرفعه
كـ artifact باسم `app-debug`.

## متطلبات التشغيل على الهاتف

- تفعيل خدمة Text Expander من إعدادات إمكانية الوصول.
- قد يلزم السماح بالإعدادات المقيدة في Android 13/14 عند تثبيت APK يدوياً.

## تفضيلات المشروع

- الحفاظ على بنية Android الحالية وعدم تحويل المشروع إلى تطبيق ويب أو نقل قاعدة
  البيانات.
- إبقاء واجهة المستخدم باللغة العربية واتجاهها RTL.
- عدم تخزين مفاتيح API أو كلمات المرور داخل المستودع؛ استخدم Secrets/بيئة
  البناء عند الحاجة.
