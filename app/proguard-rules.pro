# Add project specific ProGuard rules here.

# --- Gson ---------------------------------------------------------------
# Gson lit/écrit les champs par réflexion et s'appuie sur les signatures
# génériques (TypeToken) pour List<Question>, List<QuestionVariant>,
# Map<Int, String>, List<Int>, FichesData.
-keepattributes Signature
-keep class com.google.gson.reflect.TypeToken { *; }
-keep class * extends com.google.gson.reflect.TypeToken

# Modèles (dé)sérialisés par Gson. R8 **full mode** (défaut AGP 8+) considère
# qu'une classe jamais instanciée par le code — créée uniquement par Gson via
# réflexion, comme QuestionVariant ou Fiche — est morte : il peut supprimer ses
# constructeurs et remplacer l'accès à ses membres par null/throw, même si les
# champs sont gardés par -keepclassmembers. Il faut donc garder les classes
# ENTIÈRES (constructeurs compris), pas seulement leurs champs.
# (Symptôme corrigé : crash en release au chargement de l'examen/entraînement
# dès qu'une question à variantes était matérialisée, alors que le debug était sain.)
-keep class com.example.qcmfrance.data.model.** { *; }
