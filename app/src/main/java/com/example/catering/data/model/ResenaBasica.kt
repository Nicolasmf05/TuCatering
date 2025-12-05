package com.example.catering.data.model

data class ResenaBasica(
    val id: String = "",
    val idUsuarioEmisor: String = "",
    val idUsuarioDestino: String = "",
    val calificacion: Int = 0,
    val comentario: String = ""
)
