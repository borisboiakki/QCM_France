# Add project specific ProGuard rules here.

# --- Gson ---------------------------------------------------------------
# Gson lit/écrit les champs par réflexion et s'appuie sur les signatures
# génériques (TypeToken) pour List<Question>, Map<Int, String>, List<Int>.
-keepattributes Signature
-keep class com.google.gson.reflect.TypeToken { *; }
-keep class * extends com.google.gson.reflect.TypeToken

# Modèles sérialisés par Gson (seed questions.json, examen en pause, cycle) :
# les noms de champs doivent correspondre aux clés JSON.
-keepclassmembers class com.example.qcmfrance.data.model.** {
    <fields>;
}
