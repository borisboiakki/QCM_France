package com.example.qcmfrance.data.db

import androidx.room.TypeConverter
import com.example.qcmfrance.data.model.QuestionVariant
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

/**
 * Convertisseurs Room. Sérialise la liste des [QuestionVariant] d'une
 * [com.example.qcmfrance.data.model.Question] en JSON pour la stocker dans la colonne `variants`
 * (aucune table dédiée, donc aucune migration au-delà de l'ajout de la colonne).
 */
class Converters {
    private val gson = Gson()
    private val variantListType = object : TypeToken<List<QuestionVariant>>() {}.type

    // Paramètre nullable obligatoire : Gson instancie les Question du seed sans passer par le
    // constructeur Kotlin (allocation Unsafe), donc `variants` est null en mémoire pour toute
    // question dont le JSON n'a pas de clé "variants", malgré le type non-null de l'entité.
    // Sans cette tolérance, l'insertAll du seed plante en NullPointerException.
    @TypeConverter
    fun fromVariants(variants: List<QuestionVariant>?): String =
        if (variants == null) "[]" else gson.toJson(variants)

    @TypeConverter
    fun toVariants(json: String): List<QuestionVariant> =
        if (json.isBlank()) emptyList()
        else runCatching { gson.fromJson<List<QuestionVariant>>(json, variantListType) }.getOrNull() ?: emptyList()
}
