package com.example.catering.ui.screens.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.catering.data.model.User
import com.example.catering.data.repo.CateringRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import java.util.UUID
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val repository: CateringRepository
) : ViewModel() {

    private val _estadoInicio = MutableStateFlow(EstadoInicioUi())
    val estadoInicio: StateFlow<EstadoInicioUi> = _estadoInicio

    private val idUsuarioActual = MutableStateFlow<String?>(null)

    init {
        viewModelScope.launch {
            repository.ofertasEmpresa.collect { ofertas ->
                val estadoActual = _estadoInicio.value
                _estadoInicio.value = estadoActual.copy(ofertasEmpresa = ofertas)
            }
        }
        viewModelScope.launch {
            repository.solicitudesCliente.collect { solicitudes ->
                val estadoActual = _estadoInicio.value
                _estadoInicio.value = estadoActual.copy(solicitudesCliente = solicitudes)
            }
        }
        viewModelScope.launch {
            repository.perfiles.collect { perfiles ->
                val estadoActual = _estadoInicio.value
                _estadoInicio.value = estadoActual.copy(perfiles = perfiles)
            }
        }
        viewModelScope.launch {
            idUsuarioActual.collect { id ->
                if (id.isNullOrEmpty()) return@collect
                repository.observarBandejaEntrada(id).collect { mensajes ->
                    val estadoActual = _estadoInicio.value
                    _estadoInicio.value = estadoActual.copy(mensajesBandejaEntrada = mensajes)
                }
            }
        }
        viewModelScope.launch {
            idUsuarioActual.collect { id ->
                if (id.isNullOrEmpty()) return@collect
                repository.observarPreferenciasUsuario(id).collect { prefs ->
                    val estadoActual = _estadoInicio.value
                    _estadoInicio.value = estadoActual.copy(
                        idsOfertasEmpresaAceptadas = prefs.idsOfertasEmpresaAceptadas,
                        idsSolicitudesClienteAceptadas = prefs.idsSolicitudesClienteAceptadas
                    )
                }
            }
        }
    }

    fun establecerUsuarioActual(user: User) {
        idUsuarioActual.value = user.uid
        val estadoActual = _estadoInicio.value
        val updatedProfile = estadoActual.perfiles[user.uid]?.actualizarDesdeUsuario(user)
            ?: PerfilPublico.desdeUsuario(user)
        _estadoInicio.value = estadoActual.copy(
            usuarioActual = user,
            perfiles = estadoActual.perfiles + (user.uid to updatedProfile)
        )
        viewModelScope.launch { repository.sincronizarPerfilUsuario(user) }
    }

    fun actualizarPerfil(descripcion: String) {
        val current = _estadoInicio.value.usuarioActual ?: return
        viewModelScope.launch {
            val updatedUser = current.copy(
                profileDescription = descripcion
            )
            val estadoActual = _estadoInicio.value
            val profile = estadoActual.perfiles[current.uid]?.copy(
                descripcion = descripcion,
                nombre = updatedUser.displayName.ifBlank { updatedUser.email },
                correo = updatedUser.email,
                rol = updatedUser.role,
                direccion = updatedUser.address,
                afiliacion = updatedUser.affiliation.orEmpty()
            ) ?: PerfilPublico.desdeUsuario(updatedUser)
            _estadoInicio.value = estadoActual.copy(
                usuarioActual = updatedUser,
                perfiles = estadoActual.perfiles + (current.uid to profile)
            )
            repository.actualizarPerfil(current.uid, descripcion)
        }
    }

    fun crearOfertaEmpresa(input: OfertaEmpresaInput) {
        val user = _estadoInicio.value.usuarioActual ?: return
        viewModelScope.launch {
            repository.crearOfertaEmpresa(user, input)
        }
    }

    fun crearSolicitudCliente(input: SolicitudClienteInput) {
        val user = _estadoInicio.value.usuarioActual ?: return
        viewModelScope.launch {
            repository.crearSolicitudCliente(user, input)
        }
    }

    fun iniciarEdicionOferta(idOferta: String) {
        val oferta = _estadoInicio.value.ofertasEmpresa.firstOrNull { it.id == idOferta } ?: return
        val estadoActual = _estadoInicio.value
        _estadoInicio.value = estadoActual.copy(
            objetivoEdicion = ObjetivoEdicion(tipo = TipoEdicion.OFERTA_EMPRESA, oferta = oferta)
        )
    }

    fun iniciarEdicionSolicitud(idSolicitud: String) {
        val solicitud = _estadoInicio.value.solicitudesCliente.firstOrNull { it.id == idSolicitud } ?: return
        val estadoActual = _estadoInicio.value
        _estadoInicio.value = estadoActual.copy(
            objetivoEdicion = ObjetivoEdicion(tipo = TipoEdicion.SOLICITUD_CLIENTE, solicitud = solicitud)
        )
    }

    fun guardarEdicion(input: DatosEdicion) {
        val objetivo = _estadoInicio.value.objetivoEdicion ?: return
        viewModelScope.launch {
            when (objetivo.tipo) {
                TipoEdicion.OFERTA_EMPRESA -> {
                    val id = objetivo.oferta?.id ?: return@launch
                    repository.actualizarOfertaEmpresa(id, input)
                }

                TipoEdicion.SOLICITUD_CLIENTE -> {
                    val id = objetivo.solicitud?.id ?: return@launch
                    repository.actualizarSolicitudCliente(id, input)
                }
            }
            val estadoActual = _estadoInicio.value
            _estadoInicio.value = estadoActual.copy(objetivoEdicion = null)
        }
    }

    fun finalizarEdicion() {
        val estadoActual = _estadoInicio.value
        _estadoInicio.value = estadoActual.copy(objetivoEdicion = null)
    }

    fun alternarSolicitudAceptada(idSolicitud: String) {
        val usuario = _estadoInicio.value.usuarioActual ?: return
        var agregada = false
        val estadoActual = _estadoInicio.value
        val nuevasAceptadas = estadoActual.idsSolicitudesClienteAceptadas.toMutableSet()
        if (nuevasAceptadas.contains(idSolicitud)) {
            nuevasAceptadas.remove(idSolicitud)
        } else {
            nuevasAceptadas.add(idSolicitud)
            agregada = true
        }
        _estadoInicio.value = estadoActual.copy(idsSolicitudesClienteAceptadas = nuevasAceptadas)
        viewModelScope.launch { repository.alternarSolicitudAceptada(usuario.uid, idSolicitud) }
        if (agregada) {
            enviarSolicitudEjecucionParaSolicitud(idSolicitud)
        }
    }

    fun alternarOfertaAceptada(idOferta: String) {
        val usuario = _estadoInicio.value.usuarioActual ?: return
        var agregada = false
        val estadoActual = _estadoInicio.value
        val nuevasAceptadas = estadoActual.idsOfertasEmpresaAceptadas.toMutableSet()
        if (nuevasAceptadas.contains(idOferta)) {
            nuevasAceptadas.remove(idOferta)
        } else {
            nuevasAceptadas.add(idOferta)
            agregada = true
        }
        _estadoInicio.value = estadoActual.copy(idsOfertasEmpresaAceptadas = nuevasAceptadas)
        viewModelScope.launch { repository.alternarOfertaAceptada(usuario.uid, idOferta) }
        if (agregada) {
            enviarSolicitudEjecucionParaOferta(idOferta)
        }
    }

    fun eliminarOfertaEmpresa(idOferta: String) {
        val estadoActual = _estadoInicio.value
        _estadoInicio.value = estadoActual.copy(
            ofertasEmpresa = estadoActual.ofertasEmpresa.filterNot { it.id == idOferta }
        )
        viewModelScope.launch { repository.eliminarOfertaEmpresa(idOferta) }
    }

    fun eliminarSolicitudCliente(idSolicitud: String) {
        val estadoActual = _estadoInicio.value
        _estadoInicio.value = estadoActual.copy(
            solicitudesCliente = estadoActual.solicitudesCliente.filterNot { it.id == idSolicitud }
        )
        viewModelScope.launch { repository.eliminarSolicitudCliente(idSolicitud) }
    }

    fun adminCrearOfertaEmpresa(author: PerfilPublico, input: OfertaEmpresaInput) {
        viewModelScope.launch {
            repository.crearOfertaEmpresa(author.aUsuario(), input)
        }
    }

    fun adminCrearSolicitudCliente(author: PerfilPublico, input: SolicitudClienteInput) {
        viewModelScope.launch {
            repository.crearSolicitudCliente(author.aUsuario(), input)
        }
    }

    fun adminGuardarUsuario(input: AdminUserInput) {
        viewModelScope.launch {
            val perfilExistente = _estadoInicio.value.perfiles[input.idUsuario]
            val user = User(
                uid = input.idUsuario,
                email = input.email,
                displayName = input.nombreVisible,
                role = input.rol,
                affiliation = input.afiliacion.ifBlank { null },
                address = input.direccion,
                profileDescription = input.descripcion,
                joinedAt = perfilExistente?.fechaRegistro ?: System.currentTimeMillis()
            )
            repository.adminGuardarUsuario(user)
            val estadoActual = _estadoInicio.value
            val perfilPrevio = estadoActual.perfiles[input.idUsuario]
            val perfilActualizado = (perfilPrevio ?: PerfilPublico.desdeUsuario(user)).copy(
                nombre = user.displayName.ifBlank { user.email },
                correo = user.email,
                rol = user.role,
                direccion = user.address,
                descripcion = user.profileDescription,
                afiliacion = user.affiliation.orEmpty(),
                fechaRegistro = user.joinedAt
            )
            _estadoInicio.value = estadoActual.copy(
                perfiles = estadoActual.perfiles + (input.idUsuario to perfilActualizado)
            )
        }
    }

    fun adminEliminarUsuario(idUsuario: String) {
        val estadoActual = _estadoInicio.value
        _estadoInicio.value = estadoActual.copy(perfiles = estadoActual.perfiles - idUsuario)
        viewModelScope.launch { repository.adminEliminarUsuario(idUsuario) }
    }

    fun solicitarEjecucionOferta(idOferta: String) {
        enviarSolicitudEjecucionParaOferta(idOferta)
    }

    fun solicitarEjecucionSolicitud(idSolicitud: String) {
        enviarSolicitudEjecucionParaSolicitud(idSolicitud)
    }

    fun aceptarSolicitudEjecucion(idEntrada: String) {
        responderSolicitudEjecucion(idEntrada, EstadoSolicitudEjecucion.ACEPTADA)
    }

    fun rechazarSolicitudEjecucion(idEntrada: String) {
        responderSolicitudEjecucion(idEntrada, EstadoSolicitudEjecucion.RECHAZADA)
    }

    private fun responderSolicitudEjecucion(idEntrada: String, estado: EstadoSolicitudEjecucion) {
        val usuario = _estadoInicio.value.usuarioActual ?: return
        val mensaje = _estadoInicio.value.mensajesBandejaEntrada.firstOrNull { it.id == idEntrada }
        if (mensaje == null || mensaje.idDestinatario != usuario.uid) return
        if (mensaje.estadoEjecucion != EstadoSolicitudEjecucion.PENDIENTE) return
        viewModelScope.launch { repository.responderSolicitudEjecucion(idEntrada, estado) }
    }

    fun actualizarEstadoOferta(idOferta: String, estado: EstadoPublicacion) {
        val estadoActual = _estadoInicio.value
        val usuario = estadoActual.usuarioActual ?: return
        val oferta = estadoActual.ofertasEmpresa.firstOrNull { it.id == idOferta } ?: return
        viewModelScope.launch {
            repository.actualizarEstadoOferta(idOferta, estado)
            if (usuario.uid != oferta.idAutor) {
                repository.enviarNotificacionEstado(
                    DatosCambioEstado(
                        idPublicacion = oferta.id,
                        tituloPublicacion = oferta.descripcion,
                        tipoPublicacion = TipoPublicacion.OFERTA,
                        estado = estado,
                        idActor = usuario.uid,
                        nombreActor = usuario.displayName.ifBlank { usuario.email },
                        idDestinatario = oferta.idAutor,
                        nombreDestinatario = oferta.nombreAutor
                    )
                )
            }
        }
        if (estado == EstadoPublicacion.FINALIZADA && oferta.idAutor != usuario.uid) {
            val recordatorio = RecordatorioResena(
                idCliente = usuario.uid,
                nombreCliente = usuario.displayName.ifBlank { usuario.email },
                idEmpresa = oferta.idAutor,
                nombreEmpresa = oferta.nombreAutor,
                asunto = "Propuesta finalizada"
            )
            val estadoActual = _estadoInicio.value
            _estadoInicio.value = estadoActual.copy(recordatorioResenaPendiente = recordatorio)
        }
    }

    fun actualizarEstadoSolicitud(idSolicitud: String, estado: EstadoPublicacion) {
        val estadoActual = _estadoInicio.value
        val usuario = estadoActual.usuarioActual ?: return
        val solicitud = estadoActual.solicitudesCliente.firstOrNull { it.id == idSolicitud } ?: return
        viewModelScope.launch {
            repository.actualizarEstadoSolicitud(idSolicitud, estado)
            if (usuario.uid != solicitud.idAutor) {
                repository.enviarNotificacionEstado(
                    DatosCambioEstado(
                        idPublicacion = solicitud.id,
                        tituloPublicacion = solicitud.tipoCatering,
                        tipoPublicacion = TipoPublicacion.SOLICITUD,
                        estado = estado,
                        idActor = usuario.uid,
                        nombreActor = usuario.displayName.ifBlank { usuario.email },
                        idDestinatario = solicitud.idAutor,
                        nombreDestinatario = solicitud.nombreAutor
                    )
                )
            }
        }
        if (estado == EstadoPublicacion.FINALIZADA && solicitud.idAutor != usuario.uid) {
            val recordatorio = RecordatorioResena(
                idCliente = solicitud.idAutor,
                nombreCliente = solicitud.nombreAutor,
                idEmpresa = usuario.uid,
                nombreEmpresa = usuario.displayName.ifBlank { usuario.email },
                asunto = "Solicitud finalizada"
            )
            val estadoActual = _estadoInicio.value
            _estadoInicio.value = estadoActual.copy(recordatorioResenaPendiente = recordatorio)
        }
    }

    fun abrirPerfil(idUsuario: String) {
        val estadoActual = _estadoInicio.value
        val perfil = estadoActual.perfiles[idUsuario]
        _estadoInicio.value = estadoActual.copy(perfilSeleccionado = perfil)
    }

    fun cerrarPerfil() {
        val estadoActual = _estadoInicio.value
        _estadoInicio.value = estadoActual.copy(perfilSeleccionado = null)
    }

    fun descartarAvisoResena() {
        val estadoActual = _estadoInicio.value
        _estadoInicio.value = estadoActual.copy(recordatorioResenaPendiente = null)
    }

    fun enviarResena(perfil: PerfilPublico, rating: Int, comment: String) {
        val estadoActual = _estadoInicio.value
        val usuario = estadoActual.usuarioActual ?: return
        if (usuario.uid == perfil.id) return
        val comentarioRecortado = comment.trim()
        val review = DatosResena(
            idUsuarioEmisor = usuario.uid,
            nombreUsuarioEmisor = usuario.displayName.ifBlank { usuario.email },
            idUsuarioDestino = perfil.id,
            nombreUsuarioDestino = perfil.nombre,
            calificacion = rating,
            comentario = comentarioRecortado
        )
        viewModelScope.launch { repository.enviarResena(review) }

        val nuevaResena = Resena(
            id = UUID.randomUUID().toString(),
            idAutor = usuario.uid,
            nombreAutor = review.nombreUsuarioEmisor,
            calificacion = rating,
            comentario = comentarioRecortado
        )
        val perfilActualizado = perfil.agregarResena(nuevaResena)
        _estadoInicio.value = estadoActual.copy(
            perfiles = estadoActual.perfiles + (perfil.id to perfilActualizado),
            perfilSeleccionado = perfilActualizado
        )
    }

    fun enviarResenasFinales(envio: EnvioResenas) {
        viewModelScope.launch {
            repository.enviarResenasFinales(envio)
        }
        val estadoActual = _estadoInicio.value
        _estadoInicio.value = estadoActual.copy(recordatorioResenaPendiente = null)
    }

    private fun enviarSolicitudEjecucionParaOferta(idOferta: String) {
        val estadoActual = _estadoInicio.value
        val usuario = estadoActual.usuarioActual ?: return
        if (!estadoActual.idsOfertasEmpresaAceptadas.contains(idOferta)) return
        val oferta = estadoActual.ofertasEmpresa.firstOrNull { it.id == idOferta } ?: return
        if (oferta.idAutor == usuario.uid) return
        val datos = DatosSolicitudEjecucion(
            idPublicacion = idOferta,
            tituloPublicacion = oferta.tipoCatering.ifBlank { oferta.descripcion.ifBlank { "Propuesta de ${oferta.nombreAutor}" } },
            tipoPublicacion = TipoPublicacion.OFERTA,
            idSolicitante = usuario.uid,
            nombreSolicitante = usuario.displayName.ifBlank { usuario.email },
            idDestinatario = oferta.idAutor,
            nombreDestinatario = oferta.nombreAutor
        )
        viewModelScope.launch { repository.enviarSolicitudEjecucion(datos) }
    }

    private fun enviarSolicitudEjecucionParaSolicitud(idSolicitud: String) {
        val estadoActual = _estadoInicio.value
        val usuario = estadoActual.usuarioActual ?: return
        if (!estadoActual.idsSolicitudesClienteAceptadas.contains(idSolicitud)) return
        val solicitud = estadoActual.solicitudesCliente.firstOrNull { it.id == idSolicitud } ?: return
        if (solicitud.idAutor == usuario.uid) return
        val datos = DatosSolicitudEjecucion(
            idPublicacion = idSolicitud,
            tituloPublicacion = solicitud.tipoCatering.ifBlank { "Solicitud de ${solicitud.nombreAutor}" },
            tipoPublicacion = TipoPublicacion.SOLICITUD,
            idSolicitante = usuario.uid,
            nombreSolicitante = usuario.displayName.ifBlank { usuario.email },
            idDestinatario = solicitud.idAutor,
            nombreDestinatario = solicitud.nombreAutor
        )
        viewModelScope.launch { repository.enviarSolicitudEjecucion(datos) }
    }
}
