package com.example.catering.ui.screens.home

import com.example.catering.data.model.Role
import com.example.catering.data.model.User
data class EstadoInicioUi(
    val usuarioActual: User? = null,
    val ofertasEmpresa: List<OfertaEmpresa> = emptyList(),
    val solicitudesCliente: List<SolicitudCliente> = emptyList(),
    val idsOfertasEmpresaAceptadas: Set<String> = emptySet(),
    val idsSolicitudesClienteAceptadas: Set<String> = emptySet(),
    val perfiles: Map<String, PerfilPublico> = emptyMap(),
    val perfilSeleccionado: PerfilPublico? = null,
    val objetivoEdicion: ObjetivoEdicion? = null,
    val mensajesBandejaEntrada: List<MensajeBandeja> = emptyList(),
    val recordatorioResenaPendiente: RecordatorioResena? = null
)

data class OfertaEmpresa(
    val id: String,
    val idAutor: String,
    val nombreAutor: String,
    val rangoPrecio: String,
    val rangoPersonas: String,
    val rangoUbicacion: String,
    val descripcion: String,
    val tipoCatering: String,
    val estado: EstadoPublicacion
)

data class SolicitudCliente(
    val id: String,
    val idAutor: String,
    val nombreAutor: String,
    val rangoPrecio: String,
    val cantidadPersonas: Int,
    val servicios: List<String>,
    val tipoCatering: String,
    val ubicacion: String,
    val fechaEvento: String,
    val notas: String,
    val estado: EstadoPublicacion
)

data class OfertaEmpresaInput(
    val rangoPrecio: String,
    val rangoPersonas: String,
    val rangoUbicacion: String,
    val descripcion: String,
    val tipoCatering: String
)

data class SolicitudClienteInput(
    val rangoPrecio: String,
    val cantidadPersonas: Int,
    val servicios: List<String>,
    val tipoCatering: String,
    val ubicacion: String,
    val fechaEvento: String,
    val notas: String
)

data class DatosEdicion(
    val rangoPrecio: String,
    val rangoPersonas: String,
    val rangoUbicacion: String,
    val descripcion: String,
    val servicios: List<String>,
    val cantidadPersonas: Int,
    val fechaEvento: String,
    val tipoCatering: String
)

data class ObjetivoEdicion(
    val tipo: TipoEdicion,
    val oferta: OfertaEmpresa? = null,
    val solicitud: SolicitudCliente? = null
)

enum class TipoEdicion { OFERTA_EMPRESA, SOLICITUD_CLIENTE }

data class RecordatorioResena(
    val idCliente: String,
    val nombreCliente: String,
    val idEmpresa: String,
    val nombreEmpresa: String,
    val asunto: String
)

data class AdminUserInput(
    val idUsuario: String,
    val email: String,
    val nombreVisible: String,
    val rol: Role,
    val afiliacion: String,
    val direccion: String,
    val descripcion: String
)

data class EnvioResenas(
    val resenaCliente: DatosResena,
    val resenaEmpresa: DatosResena
)

data class DatosResena(
    val idUsuarioEmisor: String,
    val nombreUsuarioEmisor: String,
    val idUsuarioDestino: String,
    val nombreUsuarioDestino: String,
    val calificacion: Int,
    val comentario: String
)

data class PerfilPublico(
    val id: String,
    val nombre: String,
    val rol: Role,
    val correo: String,
    val descripcion: String,
    val direccion: String,
    val afiliacion: String,
    val calificacionPromedio: Double,
    val cantidadResenas: Int,
    val resenasRecientes: List<Resena>,
    val trabajosPrevios: List<TrabajoAnterior>,
    val fechaRegistro: Long
) {
    fun actualizarDesdeUsuario(usuario: User) = copy(
        nombre = usuario.displayName.ifBlank { usuario.email },
        rol = usuario.role,
        correo = usuario.email,
        descripcion = usuario.profileDescription,
        direccion = usuario.address,
        afiliacion = usuario.affiliation.orEmpty(),
        fechaRegistro = usuario.joinedAt.takeIf { it > 0L } ?: fechaRegistro
    )

    fun agregarResena(resena: Resena): PerfilPublico {
        val nuevaCantidad = cantidadResenas + 1
        val nuevoPromedio = if (cantidadResenas == 0) {
            resena.calificacion.toDouble()
        } else {
            ((calificacionPromedio * cantidadResenas) + resena.calificacion) / nuevaCantidad
        }
        val recientes = (listOf(resena) + resenasRecientes).take(5)
        return copy(
            calificacionPromedio = nuevoPromedio,
            cantidadResenas = nuevaCantidad,
            resenasRecientes = recientes
        )
    }

    companion object {
        fun desdeUsuario(usuario: User) = PerfilPublico(
            id = usuario.uid,
            nombre = usuario.displayName.ifBlank { usuario.email },
            rol = usuario.role,
            correo = usuario.email,
            descripcion = usuario.profileDescription,
            direccion = usuario.address,
            afiliacion = usuario.affiliation.orEmpty(),
            calificacionPromedio = 0.0,
            cantidadResenas = 0,
            resenasRecientes = emptyList(),
            trabajosPrevios = emptyList(),
            fechaRegistro = usuario.joinedAt
        )

        fun desdePredeterminado(id: String, nombre: String, rol: Role) = PerfilPublico(
            id = id,
            nombre = nombre,
            rol = rol,
            correo = "",
            descripcion = "",
            direccion = "",
            afiliacion = "",
            calificacionPromedio = 0.0,
            cantidadResenas = 0,
            resenasRecientes = emptyList(),
            trabajosPrevios = emptyList(),
            fechaRegistro = 0L
        )
    }
}

fun PerfilPublico.aUsuario(): User = User(
    uid = id,
    email = correo,
    displayName = nombre,
    role = rol,
    affiliation = afiliacion.ifBlank { null },
    address = direccion,
    profileDescription = descripcion,
    joinedAt = fechaRegistro
)

data class Resena(
    val id: String,
    val idAutor: String,
    val nombreAutor: String,
    val calificacion: Int,
    val comentario: String
)

data class TrabajoAnterior(
    val titulo: String,
    val descripcion: String
)

data class MensajeBandeja(
    val id: String,
    val tipoEntrada: TipoEntradaBandeja,
    val titulo: String,
    val cuerpo: String,
    val marcaTemporal: Long,
    val idActor: String,
    val nombreActor: String,
    val idDestinatario: String,
    val nombreDestinatario: String,
    val idPublicacion: String,
    val tituloPublicacion: String,
    val tipoPublicacion: TipoPublicacion,
    val estadoEjecucion: EstadoSolicitudEjecucion? = null,
    val estadoPublicacion: EstadoPublicacion? = null
)

enum class TipoEntradaBandeja {
    CAMBIO_ESTADO,
    SOLICITUD_EJECUCION,
    RESPUESTA_EJECUCION
}

enum class EstadoSolicitudEjecucion {
    PENDIENTE,
    ACEPTADA,
    RECHAZADA
}

enum class TipoPublicacion {
    OFERTA,
    SOLICITUD
}

data class DatosSolicitudEjecucion(
    val idPublicacion: String,
    val tituloPublicacion: String,
    val tipoPublicacion: TipoPublicacion,
    val idSolicitante: String,
    val nombreSolicitante: String,
    val idDestinatario: String,
    val nombreDestinatario: String
)

data class DatosCambioEstado(
    val idPublicacion: String,
    val tituloPublicacion: String,
    val tipoPublicacion: TipoPublicacion,
    val estado: EstadoPublicacion,
    val idActor: String,
    val nombreActor: String,
    val idDestinatario: String,
    val nombreDestinatario: String
)

enum class EstadoPublicacion(val label: String) {
    ACTIVA("Activa"),
    CANCELADA("Cancelada"),
    EN_PROGRESO("En progreso"),
    FINALIZADA("Finalizada")
}
