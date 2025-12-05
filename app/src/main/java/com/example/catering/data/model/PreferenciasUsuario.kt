package com.example.catering.data.model

data class PreferenciasUsuario(
    val idsOfertasEmpresaAceptadas: Set<String> = emptySet(),
    val idsSolicitudesClienteAceptadas: Set<String> = emptySet()
)
