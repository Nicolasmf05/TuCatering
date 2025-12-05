package com.example.catering.data.repo

import com.example.catering.data.model.User
import com.example.catering.data.model.PreferenciasUsuario
import com.example.catering.ui.screens.home.SolicitudCliente
import com.example.catering.ui.screens.home.SolicitudClienteInput
import com.example.catering.ui.screens.home.OfertaEmpresa
import com.example.catering.ui.screens.home.OfertaEmpresaInput
import com.example.catering.ui.screens.home.DatosEdicion
import com.example.catering.ui.screens.home.DatosSolicitudEjecucion
import com.example.catering.ui.screens.home.EstadoSolicitudEjecucion
import com.example.catering.ui.screens.home.MensajeBandeja
import com.example.catering.ui.screens.home.PerfilPublico
import com.example.catering.ui.screens.home.EstadoPublicacion
import com.example.catering.ui.screens.home.DatosCambioEstado
import com.example.catering.ui.screens.home.EnvioResenas
import com.example.catering.ui.screens.home.DatosResena
import kotlinx.coroutines.flow.Flow

interface CateringRepository {
    val ofertasEmpresa: Flow<List<OfertaEmpresa>>
    val solicitudesCliente: Flow<List<SolicitudCliente>>
    val perfiles: Flow<Map<String, PerfilPublico>>
    fun observarBandejaEntrada(idUsuario: String): Flow<List<MensajeBandeja>>

    fun observarPreferenciasUsuario(idUsuario: String): Flow<PreferenciasUsuario>

    suspend fun sincronizarPerfilUsuario(user: User)

    suspend fun crearOfertaEmpresa(author: User, input: OfertaEmpresaInput)
    suspend fun crearSolicitudCliente(author: User, input: SolicitudClienteInput)
    suspend fun actualizarOfertaEmpresa(idOferta: String, input: DatosEdicion)
    suspend fun actualizarSolicitudCliente(idSolicitud: String, input: DatosEdicion)

    suspend fun actualizarEstadoOferta(idOferta: String, estado: EstadoPublicacion)
    suspend fun actualizarEstadoSolicitud(idSolicitud: String, estado: EstadoPublicacion)

    suspend fun actualizarPerfil(idUsuario: String, descripcion: String)

    suspend fun alternarOfertaAceptada(idUsuario: String, idOferta: String)
    suspend fun alternarSolicitudAceptada(idUsuario: String, idSolicitud: String)

    suspend fun enviarResena(review: DatosResena)
    suspend fun enviarResenasFinales(envio: EnvioResenas)

    suspend fun enviarSolicitudEjecucion(solicitud: DatosSolicitudEjecucion)
    suspend fun responderSolicitudEjecucion(idEntrada: String, estado: EstadoSolicitudEjecucion)
    suspend fun enviarNotificacionEstado(payload: DatosCambioEstado)

    suspend fun eliminarOfertaEmpresa(idOferta: String)
    suspend fun eliminarSolicitudCliente(idSolicitud: String)
    suspend fun adminGuardarUsuario(user: User)
    suspend fun adminEliminarUsuario(idUsuario: String)
}
