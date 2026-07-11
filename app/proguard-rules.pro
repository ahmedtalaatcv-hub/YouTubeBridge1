# NanoHTTPD
-keep class fi.iki.elonen.** { *; }

# NewPipeExtractor + jsoup
-keep class org.schabi.newpipe.extractor.** { *; }
-keep class org.jsoup.** { *; }
-dontwarn org.mozilla.javascript.**
-dontwarn org.jsoup.**

# ZXing
-keep class com.google.zxing.** { *; }

# Keep model classes used for JSON responses
-keep class com.youtubebridge.app.model.** { *; }
