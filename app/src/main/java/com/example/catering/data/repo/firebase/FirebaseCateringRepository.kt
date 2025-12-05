package com.example.catering.data.repo.firebase

import com.example.catering.data.model.Role
import com.example.catering.data.model.User
import com.example.catering.data.model.PreferenciasUsuario
import com.example.catering.data.repo.CateringRepository
import com.example.catering.ui.screens.home.SolicitudCliente
import com.example.catering.ui.screens.home.SolicitudClienteInput
import com.example.catering.ui.screens.home.OfertaEmpresa
import com.example.catering.ui.screens.home.OfertaEmpresaInput
import com.example.catering.ui.screens.home.DatosSolicitudEjecucion
import com.example.catering.ui.screens.home.EstadoSolicitudEjecucion
import com.example.catering.ui.screens.home.DatosEdicion
import com.example.catering.ui.screens.home.TipoEntradaBandeja
import com.example.catering.ui.screens.home.MensajeBandeja
import com.example.catering.ui.screens.home.PerfilPublico
import com.example.catering.ui.screens.home.EstadoPublicacion
import com.example.catering.ui.screens.home.TipoPublicacion
import com.example.catering.ui.screens.home.Resena
import com.example.catering.ui.screens.home.DatosResena
import com.example.catering.ui.screens.home.EnvioResenas
import com.example.catering.ui.screens.home.DatosCambioEstado
import com.google.firebase.firestore.CollectionReference
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.example.catering.ui.screens.home.TrabajoAnterior
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

@Singleton
class FirebaseCateringRepository @Inject constructor(
    private val firestore: FirebaseFirestore
) : CateringRepository {

    private val coleccionOfertas: CollectionReference
        get() = firestore.collection(COLECCION_OFERTAS)
    private val coleccionSolicitudes: CollectionReference
        get() = firestore.collection(COLECCION_SOLICITUDES)
    private val coleccionUsuarios: CollectionReference
        get() = firestore.collection(COLECCION_USUARIOS)
    private val coleccionPreferencias: CollectionReference
        get() = firestore.collection(COLECCION_PREFERENCIAS)
    private val coleccionBandejaEntrada: CollectionReference
        get() = firestore.collection(COLECCION_BANDEJA_ENTRADA)

    override val ofertasEmpresa: Flow<List<OfertaEmpresa>> =
        coleccionOfertas.comoFlujoLista { doc ->
            val estadoName = doc.getString("status") ?: EstadoPublicacion.ACTIVA.name
            val estado = runCatching { EstadoPublicacion.valueOf(estadoName) }
                .getOrDefault(EstadoPublicacion.ACTIVA)
            OfertaEmpresa(
                id = doc.id,
                idAutor = doc.getString("authorId") ?: return@comoFlujoLista null,
                nombreAutor = doc.getString("authorName") ?: "",
                rangoPrecio = doc.getString("priceRange") ?: "",
                rangoPersonas = doc.getString("peopleRange") ?: "",
                rangoUbicacion = doc.getString("locationRange") ?: "",
                descripcion = doc.getString("description") ?: "",
                tipoCatering = doc.getString("cateringType") ?: "",
                estado = estado
            )
        }

    override val solicitudesCliente: Flow<List<SolicitudCliente>> =
        coleccionSolicitudes.comoFlujoLista { doc ->
            val estadoName = doc.getString("status") ?: EstadoPublicacion.ACTIVA.name
            val estado = runCatching { EstadoPublicacion.valueOf(estadoName) }
                .getOrDefault(EstadoPublicacion.ACTIVA)
            SolicitudCliente(
                id = doc.id,
                idAutor = doc.getString("authorId") ?: return@comoFlujoLista null,
                nombreAutor = doc.getString("authorName") ?: "",
                rangoPrecio = doc.getString("priceRange") ?: "",
                cantidadPersonas = doc.getLong("peopleCount")?.toInt() ?: 0,
                servicios = doc.obtenerListaStrings("services"),
                tipoCatering = doc.getString("cateringType") ?: "",
                ubicacion = doc.getString("location") ?: "",
                fechaEvento = doc.getString("eventDate") ?: "",
                notas = doc.getString("notes") ?: "",
                estado = estado
            )
        }

    override val perfiles: Flow<Map<String, PerfilPublico>> = callbackFlow {
        val registration = coleccionUsuarios.addSnapshotListener { snapshot, error ->
            if (error != null) {
                trySend(emptyMap())
                return@addSnapshotListener
            }
            val perfiles = snapshot?.documents.orEmpty().mapNotNull { doc ->
                doc.toPerfilPublico()
            }.associateBy { it.id }
            trySend(perfiles)
        }
        awaitClose { registration.remove() }
    }

    override fun observarBandejaEntrada(idUsuario: String): Flow<List<MensajeBandeja>> = callbackFlow {
        val query = coleccionBandejaEntrada
            .whereArrayContains("participants", idUsuario)
        val registration = query.addSnapshotListener { snapshot, error ->
            if (error != null) {
                trySend(emptyList())
                return@addSnapshotListener
            }
            val mensajes = snapshot?.documents.orEmpty()
                .mapNotNull { doc -> doc.toMensajeBandeja() }
                .sortedByDescending { it.marcaTemporal }
            trySend(mensajes)
        }
        awaitClose { registration.remove() }
    }

    override fun observarPreferenciasUsuario(idUsuario: String): Flow<PreferenciasUsuario> = callbackFlow {
        val docRef = coleccionPreferencias.document(idUsuario)
        val registration = docRef.addSnapshotListener { snapshot, _ ->
            val ofertasAceptadas = snapshot?.obtenerListaStrings("acceptedOffers")?.toSet() ?: emptySet()
            val solicitudesAceptadas = snapshot?.obtenerListaStrings("acceptedRequests")?.toSet() ?: emptySet()
            trySend(PreferenciasUsuario(ofertasAceptadas, solicitudesAceptadas))
        }
        awaitClose { registration.remove() }
    }

    override suspend fun sincronizarPerfilUsuario(user: User) {
        val joinedAt = user.joinedAt.takeIf { it > 0L } ?: System.currentTimeMillis()
        val data = mapOf(
            "email" to user.email,
            "displayName" to user.displayName.ifBlank { user.email },
            "role" to user.role.name,
            "affiliation" to user.affiliation,
            "address" to user.address,
            "profileDescription" to user.profileDescription,
            "joinedAt" to joinedAt
        )
        coleccionUsuarios.document(user.uid).set(data, SetOptions.merge()).await()
    }

    override suspend fun crearOfertaEmpresa(author: User, input: OfertaEmpresaInput) {
        val docRef = coleccionOfertas.document()
        val payload = mapOf(
            "authorId" to author.uid,
            "authorName" to author.displayName.ifBlank { author.email },
            "priceRange" to input.rangoPrecio,
            "peopleRange" to input.rangoPersonas,
            "locationRange" to input.rangoUbicacion,
            "description" to input.descripcion,
            "cateringType" to input.tipoCatering,
            "status" to EstadoPublicacion.ACTIVA.name,
            "createdAt" to System.currentTimeMillis()
        )
        docRef.set(payload).await()
    }

    override suspend fun crearSolicitudCliente(author: User, input: SolicitudClienteInput) {
        val docRef = coleccionSolicitudes.document()
        val payload = mapOf(
            "authorId" to author.uid,
            "authorName" to author.displayName.ifBlank { author.email },
            "priceRange" to input.rangoPrecio,
            "peopleCount" to input.cantidadPersonas,
            "services" to input.servicios,
            "cateringType" to input.tipoCatering,
            "location" to input.ubicacion,
            "eventDate" to input.fechaEvento,
            "notes" to input.notas,
            "status" to EstadoPublicacion.ACTIVA.name,
            "createdAt" to System.currentTimeMillis()
        )
        docRef.set(payload).await()
    }
    override suspend fun actualizarOfertaEmpresa(idOferta: String, input: DatosEdicion) {
        val data = mapOf(
            "priceRange" to input.rangoPrecio,
            "peopleRange" to input.rangoPersonas,
            "locationRange" to input.rangoUbicacion,
            "description" to input.descripcion,
            "cateringType" to input.tipoCatering
        )
        coleccionOfertas.document(idOferta).set(data, SetOptions.merge()).await()
    }

    override suspend fun actualizarSolicitudCliente(idSolicitud: String, input: DatosEdicion) {
        val data = mapOf(
            "priceRange" to input.rangoPrecio,
            "peopleCount" to input.cantidadPersonas,
            "services" to input.servicios,
            "cateringType" to input.tipoCatering,
            "location" to input.rangoUbicacion,
            "eventDate" to input.fechaEvento,
            "notes" to input.descripcion
        )
        coleccionSolicitudes.document(idSolicitud).set(data, SetOptions.merge()).await()
    }

    override suspend fun actualizarEstadoOferta(idOferta: String, estado: EstadoPublicacion) {
        coleccionOfertas.document(idOferta)
            .set(mapOf("status" to estado.name), SetOptions.merge())
            .await()
    }

    override suspend fun actualizarEstadoSolicitud(idSolicitud: String, estado: EstadoPublicacion) {
        coleccionSolicitudes.document(idSolicitud)
            .set(mapOf("status" to estado.name), SetOptions.merge())
            .await()
    }

    override suspend fun actualizarPerfil(idUsuario: String, descripcion: String) {
        val data = mapOf("profileDescription" to descripcion)
        coleccionUsuarios.document(idUsuario).set(data, SetOptions.merge()).await()
    }

    override suspend fun alternarOfertaAceptada(idUsuario: String, idOferta: String) {
        alternarValorArreglo(idUsuario, "acceptedOffers", idOferta)
    }

    override suspend fun alternarSolicitudAceptada(idUsuario: String, idSolicitud: String) {
        alternarValorArreglo(idUsuario, "acceptedRequests", idSolicitud)
    }

    override suspend fun enviarResena(review: DatosResena) {
        firestore.runTransaction { transaction ->
            aplicarResena(transaction, review)
            null
        }.await()
    }

    override suspend fun enviarResenasFinales(envio: EnvioResenas) {
        firestore.runTransaction { transaction ->
            aplicarResena(transaction, envio.resenaCliente)
            aplicarResena(transaction, envio.resenaEmpresa)
            null
        }.await()
    }

    override suspend fun enviarSolicitudEjecucion(solicitud: DatosSolicitudEjecucion) {
        val participants = listOf(solicitud.idSolicitante, solicitud.idDestinatario).distinct()
        val publicationTitle = solicitud.tituloPublicacion.ifBlank {
            when (solicitud.tipoPublicacion) {
                TipoPublicacion.OFERTA -> "Propuesta de ${solicitud.nombreDestinatario}".trim()
                TipoPublicacion.SOLICITUD -> "Solicitud de ${solicitud.nombreDestinatario}".trim()
            }
        }
        val body = "${solicitud.nombreSolicitante} solicitó ejecutar \"$publicationTitle\""
        val payload = mapOf(
            "entryType" to TipoEntradaBandeja.SOLICITUD_EJECUCION.name,
            "title" to "Solicitud de ejecución",
            "body" to body,
            "publicationId" to solicitud.idPublicacion,
            "publicationTitle" to publicationTitle,
            "publicationType" to solicitud.tipoPublicacion.name,
            "actorId" to solicitud.idSolicitante,
            "actorName" to solicitud.nombreSolicitante,
            "recipientId" to solicitud.idDestinatario,
            "recipientName" to solicitud.nombreDestinatario,
            "executionStatus" to EstadoSolicitudEjecucion.PENDIENTE.name,
            "timestamp" to System.currentTimeMillis(),
            "participants" to participants
        )
        coleccionBandejaEntrada.document().set(payload).await()
    }

    override suspend fun responderSolicitudEjecucion(idEntrada: String, estado: EstadoSolicitudEjecucion) {
        val docRef = coleccionBandejaEntrada.document(idEntrada)
        firestore.runTransaction { transaction ->
            val snapshot = transaction.get(docRef)
            if (!snapshot.exists()) {
                return@runTransaction null
            }
            val actorId = snapshot.getString("actorId") ?: return@runTransaction null
            val actorName = snapshot.getString("actorName") ?: ""
            val recipientId = snapshot.getString("recipientId") ?: return@runTransaction null
            val recipientName = snapshot.getString("recipientName") ?: ""
            val publicationId = snapshot.getString("publicationId") ?: return@runTransaction null
            val publicationTitle = snapshot.getString("publicationTitle") ?: ""
            val publicationTypeName = snapshot.getString("publicationType") ?: TipoPublicacion.SOLICITUD.name
            val publicationType = runCatching { TipoPublicacion.valueOf(publicationTypeName) }
                .getOrDefault(TipoPublicacion.SOLICITUD)
            val participants = (snapshot.get("participants") as? List<*>)
                ?.mapNotNull { it as? String }
                ?.ifEmpty { null }
                ?: listOf(actorId, recipientId).distinct()
            val body = when (estado) {
                EstadoSolicitudEjecucion.ACEPTADA -> "$recipientName aceptó la ejecución de \"$publicationTitle\""
                EstadoSolicitudEjecucion.RECHAZADA -> "$recipientName rechazó la ejecución de \"$publicationTitle\""
                EstadoSolicitudEjecucion.PENDIENTE -> "$recipientName actualizó la solicitud"
            }
            val publicationCollection = when (publicationType) {
                TipoPublicacion.OFERTA -> coleccionOfertas
                TipoPublicacion.SOLICITUD -> coleccionSolicitudes
            }
            val publicationStatus = when (estado) {
                EstadoSolicitudEjecucion.ACEPTADA -> EstadoPublicacion.EN_PROGRESO
                EstadoSolicitudEjecucion.RECHAZADA -> EstadoPublicacion.ACTIVA
                EstadoSolicitudEjecucion.PENDIENTE -> null
            }
            val updatePayload = mapOf(
                "executionStatus" to estado.name,
                "body" to body,
                "timestamp" to System.currentTimeMillis(),
                "resolvedAt" to System.currentTimeMillis()
            )
            transaction.set(docRef, updatePayload, SetOptions.merge())
            if (publicationStatus != null) {
                transaction.set(
                    publicationCollection.document(publicationId),
                    mapOf("status" to publicationStatus.name),
                    SetOptions.merge()
                )
            }
            val responsePayload = mapOf(
                "entryType" to TipoEntradaBandeja.RESPUESTA_EJECUCION.name,
                "title" to "Respuesta a solicitud",
                "body" to body,
                "publicationId" to publicationId,
                "publicationTitle" to publicationTitle,
                "publicationType" to publicationTypeName,
                "actorId" to recipientId,
                "actorName" to recipientName,
                "recipientId" to actorId,
                "recipientName" to actorName,
                "executionStatus" to estado.name,
                "timestamp" to System.currentTimeMillis(),
                "participants" to participants
            )
            transaction.set(coleccionBandejaEntrada.document(), responsePayload)
            null
        }.await()
    }

    override suspend fun enviarNotificacionEstado(payload: DatosCambioEstado) {
        val participants = listOf(payload.idActor, payload.idDestinatario).distinct()
        val publicationTitle = payload.tituloPublicacion.ifBlank {
            when (payload.tipoPublicacion) {
                TipoPublicacion.OFERTA -> "Propuesta"
                TipoPublicacion.SOLICITUD -> "Solicitud"
            }
        }
        val body = "${payload.nombreActor} cambió el estado a ${payload.estado.label}"
        val entry = mapOf(
            "entryType" to TipoEntradaBandeja.CAMBIO_ESTADO.name,
            "title" to "Cambio de estado",
            "body" to body,
            "publicationId" to payload.idPublicacion,
            "publicationTitle" to publicationTitle,
            "publicationType" to payload.tipoPublicacion.name,
            "actorId" to payload.idActor,
            "actorName" to payload.nombreActor,
            "recipientId" to payload.idDestinatario,
            "recipientName" to payload.nombreDestinatario,
            "publicationStatus" to payload.estado.name,
            "timestamp" to System.currentTimeMillis(),
            "participants" to participants
        )
        coleccionBandejaEntrada.document().set(entry).await()
    }

    override suspend fun eliminarOfertaEmpresa(idOferta: String) {
        coleccionOfertas.document(idOferta).delete().await()
    }

    override suspend fun eliminarSolicitudCliente(idSolicitud: String) {
        coleccionSolicitudes.document(idSolicitud).delete().await()
    }

    override suspend fun adminGuardarUsuario(user: User) {
        val payload = mapOf(
            "email" to user.email,
            "displayName" to user.displayName.ifBlank { user.email },
            "role" to user.role.name,
            "affiliation" to user.affiliation,
            "address" to user.address,
            "profileDescription" to user.profileDescription,
            "joinedAt" to (user.joinedAt.takeIf { it > 0L } ?: System.currentTimeMillis())
        )
        coleccionUsuarios.document(user.uid).set(payload, SetOptions.merge()).await()
    }

    override suspend fun adminEliminarUsuario(idUsuario: String) {
        coleccionUsuarios.document(idUsuario).delete().await()
    }

    private suspend fun alternarValorArreglo(idUsuario: String, field: String, value: String) {
        val docRef = coleccionPreferencias.document(idUsuario)
        firestore.runTransaction { transaction ->
            val snapshot = transaction.get(docRef)
            val current = snapshot.get(field) as? List<*> ?: emptyList<String>()
            val entries = current.filterIsInstance<String>().toMutableSet()
            if (entries.contains(value)) {
                entries.remove(value)
            } else {
                entries.add(value)
            }
            if (!snapshot.exists()) {
                transaction.set(
                    docRef,
                    mapOf(
                        "acceptedOffers" to emptyList<String>(),
                        "acceptedRequests" to emptyList<String>()
                    ),
                    SetOptions.merge()
                )
            }
            transaction.set(docRef, mapOf(field to entries.toList()), SetOptions.merge())
            null
        }.await()
    }

    private fun aplicarResena(
        transaction: com.google.firebase.firestore.Transaction,
        review: DatosResena
    ) {
        val docRef = coleccionUsuarios.document(review.idUsuarioDestino)
        val snapshot = transaction.get(docRef)
        val currentAverage = snapshot.getDouble("averageRating") ?: 0.0
        val currentCount = snapshot.getLong("reviewCount")?.toInt() ?: 0
        val newCount = currentCount + 1
        val newAverage = if (currentCount == 0) {
            review.calificacion.toDouble()
        } else {
            ((currentAverage * currentCount) + review.calificacion) / newCount
        }
        val existingReviews = snapshot.get("recentReviews") as? List<*>
        val mapped = existingReviews?.mapNotNull { it.aEntradaResena() } ?: emptyList()
        val newEntry = Resena(
            id = UUID.randomUUID().toString(),
            idAutor = review.idUsuarioEmisor,
            nombreAutor = review.nombreUsuarioEmisor,
            calificacion = review.calificacion,
            comentario = review.comentario
        )
        val updatedRecent = listOf(newEntry) + mapped
        val payload = mapOf(
            "averageRating" to newAverage,
            "reviewCount" to newCount,
            "recentReviews" to updatedRecent.take(5).map { it.toMap() }
        )
        if (!snapshot.exists()) {
            transaction.set(
                docRef,
                mapOf(
                    "displayName" to review.nombreUsuarioDestino,
                    "role" to Role.CLIENT.name,
                    "email" to ""
                ),
                SetOptions.merge()
            )
        }
        transaction.set(docRef, payload, SetOptions.merge())
    }

    private fun DocumentSnapshot.toPerfilPublico(): PerfilPublico? {
        val roleName = getString("role") ?: return null
        val rol = runCatching { Role.valueOf(roleName) }.getOrDefault(Role.CLIENT)
        val resenasRecientes = (get("recentReviews") as? List<*>)
            ?.mapNotNull { it.aEntradaResena() } ?: emptyList()
        val trabajosPrevios = (get("previousWorks") as? List<*>)
            ?.mapNotNull { it.aTrabajoPrevio() } ?: emptyList()
        return PerfilPublico(
            id = id,
            nombre = getString("displayName") ?: getString("email") ?: "",
            rol = rol,
            correo = getString("email") ?: "",
            descripcion = getString("profileDescription") ?: "",
            direccion = getString("address") ?: "",
            afiliacion = getString("affiliation") ?: "",
            calificacionPromedio = getDouble("averageRating") ?: 0.0,
            cantidadResenas = getLong("reviewCount")?.toInt() ?: 0,
            resenasRecientes = resenasRecientes,
            trabajosPrevios = trabajosPrevios,
            fechaRegistro = obtenerFechaRegistro()
        )
    }

    companion object {
        private const val COLECCION_OFERTAS = "offers"
        private const val COLECCION_SOLICITUDES = "requests"
        private const val COLECCION_USUARIOS = "users"
        private const val COLECCION_PREFERENCIAS = "userPreferences"
        private const val COLECCION_BANDEJA_ENTRADA = "inbox"
    }
}

private fun <T> CollectionReference.comoFlujoLista(
    mapper: (DocumentSnapshot) -> T?
): Flow<List<T>> = callbackFlow {
    val registration = addSnapshotListener { snapshot, _ ->
        val items = snapshot?.documents.orEmpty().mapNotNull(mapper)
        trySend(items)
    }
    awaitClose { registration.remove() }
}

private fun DocumentSnapshot.obtenerListaStrings(field: String): List<String> {
    val raw = get(field) as? List<*>
    return raw?.mapNotNull { it as? String } ?: emptyList()
}

private fun DocumentSnapshot.obtenerMapaStrings(field: String): Map<String, String> {
    val raw = get(field) as? Map<*, *> ?: return emptyMap()
    return raw.mapNotNull { (key, value) ->
        val k = key as? String
        val v = value as? String
        if (k != null && v != null) k to v else null
    }.toMap()
}

private fun DocumentSnapshot.obtenerMapaEnteros(field: String): Map<String, Int> {
    val raw = get(field) as? Map<*, *> ?: return emptyMap()
    return raw.mapNotNull { (key, value) ->
        val k = key as? String ?: return@mapNotNull null
        val intValue = when (value) {
            is Number -> value.toInt()
            is String -> value.toIntOrNull()
            else -> null
        }
        if (intValue != null) k to intValue else null
    }.toMap()
}

private fun DocumentSnapshot.obtenerFechaRegistro(): Long {
    val raw = get("joinedAt")
    return when (raw) {
        is Number -> raw.toLong()
        is com.google.firebase.Timestamp -> raw.toDate().time
        else -> 0L
    }
}

private fun Any?.aEntradaResena(): Resena? {
    val map = this as? Map<*, *> ?: return null
    val authorId = map["authorId"] as? String ?: return null
    val authorName = map["authorName"] as? String ?: return null
    val rating = when (val value = map["rating"]) {
        is Number -> value.toInt()
        is String -> value.toIntOrNull()
        else -> null
    } ?: return null
    val comment = map["comment"] as? String ?: ""
    val id = map["id"] as? String ?: UUID.randomUUID().toString()
    return Resena(
        id = id,
        idAutor = authorId,
        nombreAutor = authorName,
        calificacion = rating,
        comentario = comment
    )
}

private fun Any?.aTrabajoPrevio(): TrabajoAnterior? {
    val map = this as? Map<*, *> ?: return null
    val title = map["title"] as? String ?: return null
    val description = map["description"] as? String ?: ""
    return TrabajoAnterior(
        titulo = title,
        descripcion = description
    )
}

private fun DocumentSnapshot.toMensajeBandeja(): MensajeBandeja? {
    val typeName = getString("entryType") ?: return null
    val type = runCatching { TipoEntradaBandeja.valueOf(typeName) }.getOrElse { return null }
    val publicationTypeName = getString("publicationType") ?: TipoPublicacion.SOLICITUD.name
    val publicationType = runCatching { TipoPublicacion.valueOf(publicationTypeName) }
        .getOrDefault(TipoPublicacion.SOLICITUD)
    val executionStatus = getString("executionStatus")?.let {
        runCatching { EstadoSolicitudEjecucion.valueOf(it) }.getOrNull()
    }
    val publicationStatus = getString("publicationStatus")?.let {
        runCatching { EstadoPublicacion.valueOf(it) }.getOrNull()
    }
    return MensajeBandeja(
        id = id,
        tipoEntrada = type,
        titulo = getString("title") ?: "",
        cuerpo = getString("body") ?: "",
        marcaTemporal = getLong("timestamp") ?: 0L,
        idActor = getString("actorId") ?: "",
        nombreActor = getString("actorName") ?: "",
        idDestinatario = getString("recipientId") ?: "",
        nombreDestinatario = getString("recipientName") ?: "",
        idPublicacion = getString("publicationId") ?: "",
        tituloPublicacion = getString("publicationTitle") ?: "",
        tipoPublicacion = publicationType,
        estadoEjecucion = executionStatus,
        estadoPublicacion = publicationStatus
    )
}

private fun Resena.toMap(): Map<String, Any?> = mapOf(
    "id" to id,
    "authorId" to idAutor,
    "authorName" to nombreAutor,
    "rating" to calificacion,
    "comment" to comentario
)
