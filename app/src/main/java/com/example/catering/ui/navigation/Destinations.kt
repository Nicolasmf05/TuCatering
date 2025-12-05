package com.example.catering.ui.navigation

sealed class Destino(val ruta: String) {
    data object Bienvenida : Destino("welcome")
    data object InicioSesion : Destino("login")
    data object Registro : Destino("register")
    data object Inicio : Destino("home")
    data object Busqueda : Destino("client/search")
    data object Reserva : Destino("client/booking")
    data object PublicarServicio : Destino("company/publish")
    data object SolicitudesEmpresa : Destino("company/requests")
    data object PanelAdmin : Destino("admin/dashboard")
}
