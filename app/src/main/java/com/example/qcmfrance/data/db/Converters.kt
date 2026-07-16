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

    @TypeConverter
    fun fromVariants(variants: List<QuestionVariant>): String = gson.toJson(variants)

    @TypeConverter
    fun toVariants(json: String): List<QuestionVariant> =
        if (json.isBlank()) emptyList()
        else runCatching { gson.fromJson<List<QuestionVariant>>(json, variantListType) }.getOrNull() ?: emptyList()
}
