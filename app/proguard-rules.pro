# PDFBox (manual text extraction) loads fonts and resources reflectively.
-keep class com.tom_roush.pdfbox.** { *; }
-keep class com.tom_roush.fontbox.** { *; }
-keep class com.tom_roush.harmony.** { *; }
-dontwarn com.gemalto.jp2.**
-dontwarn com.tom_roush.pdfbox.**
-dontwarn org.bouncycastle.**
-dontwarn javax.xml.**

# Keep line numbers for readable crash reports.
-keepattributes SourceFile,LineNumberTable
