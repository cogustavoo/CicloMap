/* * Arquivo: model/Ponto.kt
 * Descrição: Este é o nosso modelo de dados. Objetos desta classe serão salvos e lidos do Firestore.
 * É uma "data class" para que o Kotlin nos dê várias funções úteis (equals, hashCode, etc.)
 */
package com.gustavo.ciclomap.model

import com.google.firebase.firestore.GeoPoint
import com.google.firebase.Timestamp

data class Ponto(

    @get:com.google.firebase.firestore.Exclude
    @set:com.google.firebase.firestore.Exclude
    var id: String = "",

    val type: String = "",
    val notes: String = "",

    val location: GeoPoint? = null,
    val userId: String = "",
    val createdAt: Timestamp = Timestamp.now()
) {
    constructor() : this("", "", "", null, "", Timestamp.now())
}