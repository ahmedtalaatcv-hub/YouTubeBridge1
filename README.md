# YouTube Bridge

تطبيق Android (Kotlin) يحوّل الهاتف إلى سيرفر HTTP محلي، يستقبل رابط يوتيوب ويعيد
رابط بث مباشر (JSON) لاستخدامه في مشغل IPTV، بدون Termux/Node/Python/VPS.

```
GET http://<IP_PHONE>:3000/youtube?url=https://youtube.com/watch?v=xxxx
->
{
  "youtube": "...",
  "title": "...",
  "stream": "...",
  "updated": 123456789
}
```

## للحصول على ملف APK جاهز (بدون تثبيت Android Studio)
هذه البيئة لا تملك Android SDK ولا وصولاً لمستودعات Google Maven، لذا لا يمكنني
بناء الـ APK هنا مباشرة. أسهل طريقة عملية للحصول عليه دون تثبيت أي برنامج على
جهازك هي عبر GitHub Actions (مجاني)، وهذا المشروع يتضمن ملف الأتمتة جاهزاً:
`.github/workflows/build.yml`.

الخطوات:
1. أنشئ مستودع (repository) جديد فارغ على GitHub.
2. ارفع محتويات هذا المجلد إليه (Upload files أو `git push`).
3. بمجرد الرفع سيبدأ GitHub تلقائياً ببناء المشروع (تبويب Actions).
4. بعد اكتمال البناء (Build APK ✅)، افتح تشغيلة الـ workflow، وستجد في أسفل
   الصفحة ملف Artifact باسم `YouTubeBridge-debug-apk` — نزّله، ففيه `app-debug.apk`
   جاهز للتثبيت مباشرة على الهاتف.

## أو: البناء يدوياً عبر Android Studio
1. افتح المجلد في Android Studio (Hedgehog أو أحدث).
2. اتركه يزامن Gradle (يحتاج اتصال إنترنت لتنزيل التبعيات من google()/mavenCentral()/jitpack).
3. Build > Build App Bundle(s) / APK(s) > Build APK(s).

## القرار التقني الأهم: لماذا NewPipeExtractor وليس yt-dlp؟
`yt-dlp` برنامج Python، ولا توجد طريقة لتشغيله كعملية Python أصلية داخل تطبيق
Android بدون تضمين مترجم بايثون كامل (عبر أدوات مثل Chaquopy)، وهو حل ثقيل،
صعب الصيانة، ويحتاج تحديثاً متكرراً بنفس معدل تحديث yt-dlp نفسه.

البديل العملي المستخدم في هذا المشروع هو **NewPipeExtractor**
(https://github.com/TeamNewPipe/NewPipeExtractor) — نفس المكتبة التي يعتمد
عليها تطبيق NewPipe الشهير. وهي:
- مكتوبة بالكامل بـ Java/Kotlin، تعمل native داخل Android بدون أي عملية خارجية.
- تحلّل استجابة player الداخلية ليوتيوب وتستخرج روابط بث مباشرة (progressive
  streams تصلح مباشرة لعنصر `<video>` أو أغلب مشغلات IPTV).
- مضمّنة كمكتبة Gradle عادية (لا تحتاج Node/Python/سيرفر خارجي).

### قيود يجب معرفتها (تنطبق على أي حل استخراج غير رسمي، بما فيه yt-dlp)
- يوتيوب يغيّر بنية الـ player بين حين وآخر، فقد ينقطع الاستخراج مؤقتاً حتى
  تُحدَّث مكتبة NewPipeExtractor (تحديثها = رفع نسخة تطبيق جديدة بإصدار أحدث
  من المكتبة، لأنها مُجمّعة داخل الـ APK وليست عملية تعمل بشكل منفصل).
- روابط البث المُستخرجة عادة موقّعة (signed) وتنتهي صلاحيتها خلال ساعات قليلة؛
  لهذا يحتوي الرد على حقل `updated` ليعرف مشغل الـ IPTV متى يعيد الطلب.
- الفيديوهات المقيدة عمرياً أو جغرافياً أو المحمية بـ DRM قد تفشل في الاستخراج.
- هذا الأسلوب (تماماً مثل yt-dlp/NewPipe) خارج نطاق شروط استخدام يوتيوب
  الرسمية لواجهة الـ API؛ الاستخدام هنا شخصي وغير تجاري.

## الأمان
السيرفر يرفض أي طلب لا يأتي من عنوان IP ضمن نطاق الشبكة المحلية (RFC1918:
10.x / 172.16-31.x / 192.168.x أو loopback/link-local)، ويُرجع 403 لأي اتصال
من خارج الشبكة.

## بنية المشروع
```
app/src/main/java/com/youtubebridge/app/
├── App.kt                      # Application + notification channel
├── MainActivity.kt             # يربط الـ Service ويستضيف واجهة Compose
├── model/Models.kt             # StreamResult / LogEntry / ServerUiState
├── util/
│   ├── Prefs.kt                # إعدادات دائمة (المنفذ، auto-start، الوضع الليلي)
│   ├── NetworkUtils.kt         # اكتشاف IP + فحص أن الطلب من نفس الشبكة
│   └── QrCodeGenerator.kt      # توليد QR عبر ZXing
├── extractor/
│   ├── OkHttpDownloaderImpl.kt # Downloader لـ NewPipeExtractor
│   └── YoutubeStreamExtractor.kt
├── server/YouTubeBridgeServer.kt  # NanoHTTPD: /youtube, /status, CORS
├── service/ServerForegroundService.kt  # Foreground Service + إشعار دائم
├── receiver/BootReceiver.kt    # إعادة تشغيل تلقائي بعد إعادة تشغيل الهاتف
└── ui/                          # Compose Material 3 (MainScreen, ViewModel, Theme)
```

## ملاحظة حول "إعادة التشغيل بعد إعادة تشغيل الهاتف"
الصلاحية `RECEIVE_BOOT_COMPLETED` + `BootReceiver` تُشغّل السيرفر تلقائياً بعد
إقلاع الهاتف إذا كان خيار "تشغيل تلقائي" مفعّلاً. لضمان عدم إيقاف النظام
للخدمة، اطلب من المستخدم استثناء التطبيق من تحسين البطارية (يظهر طلب ذلك
تلقائياً عند أول فتح للتطبيق)، وبعض الأجهزة (Xiaomi/Huawei/Oppo..) تتطلب أيضاً
تفعيل "Autostart" يدوياً من إعدادات الشركة المصنعة — هذا قيد على مستوى نظام
Android/OEM وليس شيئاً يمكن للتطبيق تجاوزه برمجياً بالكامل.
