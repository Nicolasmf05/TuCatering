@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)

package com.example.catering.ui.screens.home

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.AssignmentTurnedIn
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Mail
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.StarBorder
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.RadioButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.catering.data.model.Role
import com.example.catering.data.model.User
import com.example.catering.ui.screens.admin.AdminDashboard
import com.example.catering.ui.screens.common.ProfileScreen
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun PantallaInicio(
    user: User,
    onLogout: () -> Unit,
    viewModel: HomeViewModel = hiltViewModel()
) {
    val state by viewModel.estadoInicio.collectAsState()
    LaunchedEffect(user.uid) { viewModel.establecerUsuarioActual(user) }

    var seccionSeleccionada by rememberSaveable { mutableStateOf(SeccionInicio.INICIO) }
    var mostrarDialogoCreacion by rememberSaveable { mutableStateOf(false) }
    var objetivoCreacionAdmin by remember { mutableStateOf<ObjetivoCreacionAdmin?>(null) }
    var menuExpandido by rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(state.objetivoEdicion) {
        if (state.objetivoEdicion != null) {
            seccionSeleccionada = SeccionInicio.EDICION
        } else if (seccionSeleccionada == SeccionInicio.EDICION) {
            seccionSeleccionada = SeccionInicio.INICIO
        }
    }

    val esAdmin = user.role == Role.ADMIN

    val secciones = remember(state.objetivoEdicion) {
        val base = if (state.objetivoEdicion != null) {
            SeccionInicio.values().toList()
        } else {
            SeccionInicio.values().filterNot { it == SeccionInicio.EDICION }
        }
        base.filterNot { it == SeccionInicio.ADMIN }
    }

    val seccionesMenu = remember(secciones) { secciones.filterNot { it == SeccionInicio.PERFIL } }

    val esEmpresa = user.role == Role.COMPANY

    val cantidadPendientesBuzon = remember(state.mensajesBandejaEntrada, user.uid) {
        state.mensajesBandejaEntrada.count { mensaje ->
            mensaje.tipoEntrada == TipoEntradaBandeja.SOLICITUD_EJECUCION &&
                mensaje.estadoEjecucion == EstadoSolicitudEjecucion.PENDIENTE &&
                mensaje.idDestinatario == user.uid
        }
    }

    val tituloBarraSuperior = seccionSeleccionada.titulo

    val mostrarBotonFlotante = seccionSeleccionada !in setOf(
        SeccionInicio.PERFIL,
        SeccionInicio.BUZON,
        SeccionInicio.EDICION,
        SeccionInicio.BUSQUEDA
    )

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = { Text(tituloBarraSuperior, color = MaterialTheme.colorScheme.onSurface) },
                navigationIcon = {
                    Box {
                        IconButton(onClick = { menuExpandido = true }) {
                            Icon(
                                imageVector = Icons.Filled.Menu,
                                contentDescription = "Abrir menú",
                                tint = MaterialTheme.colorScheme.onSurface
                            )
                        }
                        DropdownMenu(
                            expanded = menuExpandido,
                            onDismissRequest = { menuExpandido = false }
                        ) {
                            seccionesMenu.forEach { seccion ->
                                val esSeleccionada = seccionSeleccionada == seccion
                                val contadorNotificacion = if (seccion == SeccionInicio.BUZON) cantidadPendientesBuzon else 0
                                DropdownMenuItem(
                                    text = {
                                        Row(
                                            modifier = Modifier.widthIn(min = 180.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                                        ) {
                                            Text(
                                                text = seccion.titulo,
                                                color = if (esSeleccionada) {
                                                    MaterialTheme.colorScheme.primary
                                                } else {
                                                    MaterialTheme.colorScheme.onSurface
                                                },
                                                modifier = Modifier.weight(1f)
                                            )
                                            if (contadorNotificacion > 0) {
                                                Text(
                                                    text = contadorNotificacion.coerceAtMost(99).toString(),
                                                    color = MaterialTheme.colorScheme.onTertiaryContainer,
                                                    style = MaterialTheme.typography.labelMedium,
                                                    fontWeight = FontWeight.Bold,
                                                    modifier = Modifier
                                                        .background(
                                                            color = MaterialTheme.colorScheme.tertiaryContainer,
                                                            shape = CircleShape
                                                        )
                                                        .padding(horizontal = 8.dp, vertical = 4.dp)
                                                )
                                            }
                                        }
                                    },
                                    leadingIcon = {
                                        Icon(
                                            imageVector = seccion.icono,
                                            contentDescription = seccion.titulo,
                                            tint = if (esSeleccionada) {
                                                MaterialTheme.colorScheme.primary
                                            } else {
                                                MaterialTheme.colorScheme.onSurfaceVariant
                                            }
                                        )
                                    },
                                    onClick = {
                                        menuExpandido = false
                                        seccionSeleccionada = seccion
                                    }
                                )
                            }
                        }
                    }
                },
                actions = {
                    if (seccionSeleccionada != SeccionInicio.PERFIL) {
                        IconButton(onClick = { seccionSeleccionada = SeccionInicio.PERFIL }) {
                            Icon(
                                imageVector = Icons.Filled.Person,
                                contentDescription = "Abrir perfil"
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface,
                    navigationIconContentColor = MaterialTheme.colorScheme.onSurface,
                    actionIconContentColor = MaterialTheme.colorScheme.onSurface
                )
            )
        },
        floatingActionButton = {
            if (mostrarBotonFlotante) {
                FloatingActionButton(
                    onClick = { mostrarDialogoCreacion = true },
                    containerColor = MaterialTheme.colorScheme.tertiary,
                    contentColor = MaterialTheme.colorScheme.onTertiary
                ) {
                    Icon(imageVector = Icons.Filled.Add, contentDescription = "Crear publicación")
                }
            }
        }
    ) { paddingValues ->
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            color = MaterialTheme.colorScheme.background
        ) {
            when (seccionSeleccionada) {
                SeccionInicio.INICIO -> SeccionFeed(
                    modifier = Modifier.fillMaxSize(),
                    state = state,
                    usuarioActual = user,
                    onAlternarSolicitud = { viewModel.alternarSolicitudAceptada(it) },
                    onAlternarOferta = { viewModel.alternarOfertaAceptada(it) },
                    onVerPerfil = { viewModel.abrirPerfil(it) },
                    onCambioEstado = { id, estado, esOferta ->
                        if (esOferta) viewModel.actualizarEstadoOferta(id, estado) else viewModel.actualizarEstadoSolicitud(id, estado)
                    },
                    onEditarOferta = { viewModel.iniciarEdicionOferta(it) },
                    onEditarSolicitud = { viewModel.iniciarEdicionSolicitud(it) },
                    onEliminarOferta = { viewModel.eliminarOfertaEmpresa(it) },
                    onEliminarSolicitud = { viewModel.eliminarSolicitudCliente(it) }
                )

                SeccionInicio.BUSQUEDA -> SeccionBusqueda(
                    modifier = Modifier.fillMaxSize(),
                    state = state,
                    usuarioActual = user,
                    onAlternarOferta = { viewModel.alternarOfertaAceptada(it) },
                    onAlternarSolicitud = { viewModel.alternarSolicitudAceptada(it) },
                    onVerPerfil = { viewModel.abrirPerfil(it) },
                    onCambioEstadoOferta = { id, estado -> viewModel.actualizarEstadoOferta(id, estado) },
                    onCambioEstadoSolicitud = { id, estado -> viewModel.actualizarEstadoSolicitud(id, estado) }
                )

                SeccionInicio.MIS_PUBLICACIONES -> SeccionMisPublicaciones(
                    modifier = Modifier.fillMaxSize(),
                    state = state,
                    esEmpresa = esEmpresa,
                    usuarioActual = user,
                    onEditarOferta = { viewModel.iniciarEdicionOferta(it) },
                    onEditarSolicitud = { viewModel.iniciarEdicionSolicitud(it) },
                    onCambioEstado = { id, estado, esOferta ->
                        if (esOferta) viewModel.actualizarEstadoOferta(id, estado) else viewModel.actualizarEstadoSolicitud(id, estado)
                    },
                    onVerPerfil = { viewModel.abrirPerfil(it) }
                )

                SeccionInicio.MIS_SOLICITUDES -> SeccionMisSolicitudes(
                    modifier = Modifier.fillMaxSize(),
                    state = state,
                    esEmpresa = esEmpresa,
                    usuarioActual = user,
                    onCambioEstado = { id, estado, esOferta ->
                        if (esOferta) viewModel.actualizarEstadoOferta(id, estado) else viewModel.actualizarEstadoSolicitud(id, estado)
                    },
                    onVerPerfil = { viewModel.abrirPerfil(it) },
                )

                SeccionInicio.ADMIN -> AdminDashboard(
                    modifier = Modifier.fillMaxSize(),
                    perfiles = state.perfiles,
                    ofertas = state.ofertasEmpresa,
                    solicitudes = state.solicitudesCliente,
                    onEditarOferta = { viewModel.iniciarEdicionOferta(it) },
                    onEditarSolicitud = { viewModel.iniciarEdicionSolicitud(it) },
                    onEliminarOferta = { viewModel.eliminarOfertaEmpresa(it) },
                    onEliminarSolicitud = { viewModel.eliminarSolicitudCliente(it) },
                    onSaveUser = { viewModel.adminGuardarUsuario(it) },
                    onDeleteUser = { viewModel.adminEliminarUsuario(it) },
                    onCreatePublication = {
                        objetivoCreacionAdmin = null
                        mostrarDialogoCreacion = true
                    }
                )

                SeccionInicio.PERFIL -> Box(
                    modifier = Modifier.fillMaxSize()
                ) {
                        ProfileScreen(
                            user = user,
                            profile = state.perfiles[user.uid],
                            onLogout = onLogout,
                            onProfileUpdated = { descripcion ->
                                viewModel.actualizarPerfil(descripcion)
                            }
                        )
                }

                SeccionInicio.BUZON -> SeccionBandejaEntrada(
                    modifier = Modifier.fillMaxSize(),
                    messages = state.mensajesBandejaEntrada,
                    usuarioActualId = user.uid,
                    onAccept = { viewModel.aceptarSolicitudEjecucion(it) },
                    onDecline = { viewModel.rechazarSolicitudEjecucion(it) },
                    onVerPerfil = { viewModel.abrirPerfil(it) }
                )

                SeccionInicio.EDICION -> SeccionEditarPublicacion(
                    modifier = Modifier.fillMaxSize(),
                    target = state.objetivoEdicion,
                    onCancel = { viewModel.finalizarEdicion() },
                    onSave = { viewModel.guardarEdicion(it) }
                )
            }
        }
    }

    if (mostrarDialogoCreacion) {
        when {
            esAdmin -> {
                val objetivo = objetivoCreacionAdmin
                val perfiles = state.perfiles.values.sortedBy { it.nombre.lowercase(Locale.getDefault()) }
                if (objetivo == null) {
                    DialogoPropietarioPublicacionAdmin(
                        perfiles = perfiles,
                        onDismiss = {
                            mostrarDialogoCreacion = false
                            objetivoCreacionAdmin = null
                        },
                        onSelect = { selection ->
                            objetivoCreacionAdmin = selection
                        }
                    )
                } else {
                    when (objetivo.tipo) {
                        TipoPublicacion.OFERTA -> {
                            DialogoOfertaEmpresa(
                                onDismiss = {
                                    objetivoCreacionAdmin = null
                                    mostrarDialogoCreacion = false
                                },
                                onCreate = {
                                    viewModel.adminCrearOfertaEmpresa(objetivo.perfil, it)
                                    objetivoCreacionAdmin = null
                                    mostrarDialogoCreacion = false
                                }
                            )
                        }

                        TipoPublicacion.SOLICITUD -> {
                            DialogoSolicitudCliente(
                                onDismiss = {
                                    objetivoCreacionAdmin = null
                                    mostrarDialogoCreacion = false
                                },
                                onCreate = {
                                    viewModel.adminCrearSolicitudCliente(objetivo.perfil, it)
                                    objetivoCreacionAdmin = null
                                    mostrarDialogoCreacion = false
                                }
                            )
                        }
                    }
                }
            }

            esEmpresa -> {
                DialogoOfertaEmpresa(
                    onDismiss = { mostrarDialogoCreacion = false },
                    onCreate = {
                        viewModel.crearOfertaEmpresa(it)
                        mostrarDialogoCreacion = false
                    }
                )
            }

            else -> {
                DialogoSolicitudCliente(
                    onDismiss = { mostrarDialogoCreacion = false },
                    onCreate = {
                        viewModel.crearSolicitudCliente(it)
                        mostrarDialogoCreacion = false
                    }
                )
            }
        }
    }

    state.perfilSeleccionado?.let { perfilSeleccionado ->
        HojaDetallesPerfil(
            profile = perfilSeleccionado,
            currentUserId = user.uid,
            onDismiss = { viewModel.cerrarPerfil() },
            onSubmitReview = { rating, comment ->
                viewModel.enviarResena(perfilSeleccionado, rating, comment)
            }
        )
    }

    state.recordatorioResenaPendiente?.let { reviewPrompt ->
        DialogoResena(
            prompt = reviewPrompt,
            onDismiss = { viewModel.descartarAvisoResena() },
            onConfirm = { viewModel.enviarResenasFinales(it) }
        )
    }
}

private enum class SeccionInicio(val titulo: String, val icono: androidx.compose.ui.graphics.vector.ImageVector) {
    INICIO("Inicio", Icons.Filled.Home),
    BUSQUEDA("Buscar", Icons.Filled.Search),
    MIS_PUBLICACIONES("Publicaciones", Icons.Filled.Description),
    MIS_SOLICITUDES("Solicitudes", Icons.Filled.AssignmentTurnedIn),
    PERFIL("Perfil", Icons.Filled.Person),
    BUZON("Buzón", Icons.Filled.Mail),
    ADMIN("Admin", Icons.Filled.AdminPanelSettings),
    EDICION("Edición", Icons.Filled.Edit)
}

private enum class ModoBusqueda(val etiqueta: String) {
    TODO("Todo"),
    OFERTAS("Ofertas"),
    SOLICITUDES("Solicitudes")
}

private const val ESTADO_BUSQUEDA_TODO = "TODO"

@Composable
private fun SeccionFeed(
    modifier: Modifier = Modifier,
    state: EstadoInicioUi,
    usuarioActual: User,
    onAlternarSolicitud: (String) -> Unit,
    onAlternarOferta: (String) -> Unit,
    onVerPerfil: (String) -> Unit,
    onCambioEstado: (String, EstadoPublicacion, Boolean) -> Unit,
    onEditarOferta: (String) -> Unit,
    onEditarSolicitud: (String) -> Unit,
    onEliminarOferta: (String) -> Unit,
    onEliminarSolicitud: (String) -> Unit
) {
    val esAdmin = usuarioActual.role == Role.ADMIN
    val esEmpresa = usuarioActual.role == Role.COMPANY
    val esCliente = usuarioActual.role == Role.CLIENT

    if (state.ofertasEmpresa.isEmpty() && state.solicitudesCliente.isEmpty()) {
        EstadoVacio(message = "Aún no hay publicaciones disponibles", modifier = modifier)
        return
    }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item { TituloSeccion("Propuestas de empresas") }
        item {
            Divider(color = MaterialTheme.colorScheme.surfaceVariant)
        }
        if (state.ofertasEmpresa.isEmpty()) {
            item {
                Text("No hay propuestas de empresas en este momento.", style = MaterialTheme.typography.bodyMedium)
            }
        } else {
            items(state.ofertasEmpresa, key = { it.id }) { oferta ->
                val esSeleccionada = state.idsOfertasEmpresaAceptadas.contains(oferta.id)
                TarjetaOfertaEmpresa(
                    oferta = oferta,
                    esSeleccionada = esSeleccionada,
                    showAction = esCliente,
                    onToggle = { onAlternarOferta(oferta.id) },
                    canEdit = esAdmin || oferta.idAutor == usuarioActual.uid,
                    onEdit = { onEditarOferta(oferta.id) },
                    onCambioEstado = { estado -> onCambioEstado(oferta.id, estado, true) },
                    onVerPerfil = { onVerPerfil(oferta.idAutor) },
                    estadoEnabled = esAdmin,
                    onDelete = if (esAdmin) { { onEliminarOferta(oferta.id) } } else null
                )
            }
        }

        item { Divider(color = MaterialTheme.colorScheme.surfaceVariant) }
        item { TituloSeccion("Solicitudes de clientes") }
        if (state.solicitudesCliente.isEmpty()) {
            item {
                Text("No hay solicitudes de clientes publicadas.", style = MaterialTheme.typography.bodyMedium)
            }
        } else {
            items(state.solicitudesCliente, key = { it.id }) { solicitud ->
                val esSeleccionada = state.idsSolicitudesClienteAceptadas.contains(solicitud.id)
                TarjetaSolicitudCliente(
                    solicitud = solicitud,
                    esSeleccionada = esSeleccionada,
                    showAction = esEmpresa,
                    onToggle = { onAlternarSolicitud(solicitud.id) },
                    canEdit = esAdmin || solicitud.idAutor == usuarioActual.uid,
                    onEdit = { onEditarSolicitud(solicitud.id) },
                    onCambioEstado = { estado -> onCambioEstado(solicitud.id, estado, false) },
                    onVerPerfil = { onVerPerfil(solicitud.idAutor) },
                    estadoEnabled = esAdmin,
                    onDelete = if (esAdmin) { { onEliminarSolicitud(solicitud.id) } } else null
                )
            }
        }
    }
}

@Composable
private fun TituloSeccion(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleMedium
    )
}

@Composable
private fun DialogoPropietarioPublicacionAdmin(
    perfiles: List<PerfilPublico>,
    onDismiss: () -> Unit,
    onSelect: (ObjetivoCreacionAdmin) -> Unit
) {
    var perfilSeleccionadoId by rememberSaveable(perfiles.size) {
        mutableStateOf(perfiles.firstOrNull()?.id.orEmpty())
    }
    var selectedType by rememberSaveable { mutableStateOf(TipoPublicacion.OFERTA) }
    val scrollState = rememberScrollState()
    val hasProfiles = perfiles.isNotEmpty()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Nueva publicación") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Selecciona el perfil autor", style = MaterialTheme.typography.labelLarge)
                if (!hasProfiles) {
                    Text("No hay perfiles registrados", style = MaterialTheme.typography.bodyMedium)
                } else {
                    Column(
                        modifier = Modifier
                            .heightIn(max = 240.dp)
                            .verticalScroll(scrollState),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        perfiles.forEach { profile ->
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                RadioButton(
                                    selected = perfilSeleccionadoId == profile.id,
                                    onClick = { perfilSeleccionadoId = profile.id }
                                )
                                Column(modifier = Modifier.padding(start = 8.dp)) {
                                    Text(profile.nombre, fontWeight = FontWeight.SemiBold)
                                    if (profile.correo.isNotBlank()) {
                                        Text(profile.correo, style = MaterialTheme.typography.bodySmall)
                                    }
                                }
                            }
                        }
                    }
                }
                Text("Tipo de publicación", style = MaterialTheme.typography.labelLarge)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TipoPublicacion.values().forEach { type ->
                        val selected = selectedType == type
                        FilterChip(
                            selected = selected,
                            onClick = { selectedType = type },
                            label = { Text(if (type == TipoPublicacion.OFERTA) "Propuesta" else "Solicitud") }
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val profile = perfiles.firstOrNull { it.id == perfilSeleccionadoId }
                    if (profile != null) {
                        onSelect(ObjetivoCreacionAdmin(profile, selectedType))
                    }
                },
                enabled = hasProfiles && perfiles.any { it.id == perfilSeleccionadoId }
            ) {
                Text("Continuar")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancelar") }
        }
    )
}

@Composable
private fun SeccionBusqueda(
    modifier: Modifier = Modifier,
    state: EstadoInicioUi,
    usuarioActual: User,
    onAlternarOferta: (String) -> Unit,
    onAlternarSolicitud: (String) -> Unit,
    onVerPerfil: (String) -> Unit,
    onCambioEstadoOferta: (String, EstadoPublicacion) -> Unit,
    onCambioEstadoSolicitud: (String, EstadoPublicacion) -> Unit
) {
    var claveModoBusqueda by rememberSaveable { mutableStateOf(ModoBusqueda.TODO.name) }
    var consultaBusqueda by rememberSaveable { mutableStateOf("") }
    var consultaCiudad by rememberSaveable { mutableStateOf("") }
    var opcionTipoSeleccionada by rememberSaveable { mutableStateOf("") }
    var esTipoPersonalizado by rememberSaveable { mutableStateOf(false) }
    var tipoPersonalizado by rememberSaveable { mutableStateOf("") }
    var claveFiltroEstado by rememberSaveable { mutableStateOf(EstadoPublicacion.ACTIVA.name) }

    val modoBusqueda = remember(claveModoBusqueda) { ModoBusqueda.valueOf(claveModoBusqueda) }
    val filtroTipo = if (esTipoPersonalizado) tipoPersonalizado else opcionTipoSeleccionada
    val locale = Locale.getDefault()
    val consultaNormalizada = consultaBusqueda.trim().lowercase(locale)
    val ciudadNormalizada = consultaCiudad.trim().lowercase(locale)
    val tipoNormalizado = filtroTipo.trim().lowercase(locale)

    val filtroEstado = claveFiltroEstado.takeIf { it != ESTADO_BUSQUEDA_TODO }
        ?.let { EstadoPublicacion.valueOf(it) }

    val mostrarOfertas = modoBusqueda != ModoBusqueda.SOLICITUDES
    val mostrarSolicitudes = modoBusqueda != ModoBusqueda.OFERTAS

    val resultadosOfertas = remember(
        state.perfiles,
        state.ofertasEmpresa,
        consultaNormalizada,
        ciudadNormalizada,
        tipoNormalizado,
        filtroEstado,
        mostrarOfertas
    ) {
        if (!mostrarOfertas) emptyList() else {
            state.ofertasEmpresa
                .filter { oferta ->
                    val matchesStatus = when (filtroEstado) {
                        null -> oferta.estado != EstadoPublicacion.CANCELADA
                        else -> oferta.estado == filtroEstado
                    }

                    val profile = state.perfiles[oferta.idAutor]
                    val searchableText = buildString {
                        append(oferta.nombreAutor)
                        append(' ')
                        append(oferta.descripcion)
                        append(' ')
                        append(oferta.rangoUbicacion)
                        append(' ')
                        append(oferta.rangoPersonas)
                        append(' ')
                        append(oferta.rangoPrecio)
                        append(' ')
                    profile?.let {
                        append(it.nombre)
                        append(' ')
                        append(it.direccion)
                        append(' ')
                        append(it.afiliacion)
                    }
                    }.lowercase(locale)

                    val nameMatches = consultaNormalizada.isBlank() || searchableText.contains(consultaNormalizada)
                    val cityMatches = ciudadNormalizada.isBlank() || searchableText.contains(ciudadNormalizada)
                    val typeMatches = tipoNormalizado.isBlank() ||
                        oferta.tipoCatering.lowercase(locale).contains(tipoNormalizado) ||
                        oferta.descripcion.lowercase(locale).contains(tipoNormalizado)

                    matchesStatus && nameMatches && cityMatches && typeMatches
                }
                .sortedBy { it.nombreAutor.lowercase(locale) }
        }
    }

    val resultadosSolicitudes = remember(
        state.perfiles,
        state.solicitudesCliente,
        consultaNormalizada,
        ciudadNormalizada,
        tipoNormalizado,
        filtroEstado,
        mostrarSolicitudes
    ) {
        if (!mostrarSolicitudes) emptyList() else {
            state.solicitudesCliente
                .filter { solicitud ->
                    val matchesStatus = when (filtroEstado) {
                        null -> solicitud.estado != EstadoPublicacion.CANCELADA
                        else -> solicitud.estado == filtroEstado
                    }

                    val profile = state.perfiles[solicitud.idAutor]
                    val searchableText = buildString {
                        append(solicitud.nombreAutor)
                        append(' ')
                        append(solicitud.notas)
                        append(' ')
                        append(solicitud.tipoCatering)
                        append(' ')
                        append(solicitud.rangoPrecio)
                        append(' ')
                        append(solicitud.ubicacion)
                        append(' ')
                        append(solicitud.servicios.joinToString(","))
                        append(' ')
                    profile?.let {
                        append(it.nombre)
                        append(' ')
                        append(it.direccion)
                        append(' ')
                        append(it.afiliacion)
                    }
                    }.lowercase(locale)

                    val nameMatches = consultaNormalizada.isBlank() || searchableText.contains(consultaNormalizada)
                    val cityMatches = ciudadNormalizada.isBlank() || searchableText.contains(ciudadNormalizada)
                    val typeMatches = tipoNormalizado.isBlank() ||
                        solicitud.tipoCatering.lowercase(locale).contains(tipoNormalizado) ||
                        solicitud.servicios.any { it.lowercase(locale).contains(tipoNormalizado) }

                    matchesStatus && nameMatches && cityMatches && typeMatches
                }
                .sortedBy { it.nombreAutor.lowercase(locale) }
        }
    }

    val resultsLabel = listOfNotNull(
        if (mostrarOfertas) "Ofertas: ${resultadosOfertas.size}" else null,
        if (mostrarSolicitudes) "Solicitudes: ${resultadosSolicitudes.size}" else null
    ).joinToString(" • ")

    val canClientAct = usuarioActual.role == Role.CLIENT
    val canCompanyAct = usuarioActual.role == Role.COMPANY
    val adminControlsEnabled = usuarioActual.role == Role.ADMIN

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Búsqueda y filtros", style = MaterialTheme.typography.titleMedium)
                OutlinedTextField(
                    value = consultaBusqueda,
                    onValueChange = { consultaBusqueda = it },
                    label = { Text("Nombre, servicio o palabra clave") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                OutlinedTextField(
                    value = consultaCiudad,
                    onValueChange = { consultaCiudad = it },
                    label = { Text("Ciudad o zona") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                Text("Tipo de catering", style = MaterialTheme.typography.labelLarge)
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected = !esTipoPersonalizado && opcionTipoSeleccionada.isBlank(),
                        onClick = {
                            opcionTipoSeleccionada = ""
                            esTipoPersonalizado = false
                        },
                        label = { Text("Todos") }
                    )
                    CATERING_TYPE_OPTIONS.forEach { option ->
                        val esSeleccionada = !esTipoPersonalizado && opcionTipoSeleccionada.equals(option, ignoreCase = true)
                        FilterChip(
                            selected = esSeleccionada,
                            onClick = {
                                if (esSeleccionada) {
                                    opcionTipoSeleccionada = ""
                                } else {
                                    opcionTipoSeleccionada = option
                                }
                                esTipoPersonalizado = false
                            },
                            label = { Text(option) }
                        )
                    }
                    FilterChip(
                        selected = esTipoPersonalizado,
                        onClick = {
                            esTipoPersonalizado = !esTipoPersonalizado
                            if (esTipoPersonalizado) {
                                opcionTipoSeleccionada = ""
                            }
                        },
                        label = { Text("Otro") }
                    )
                }
                if (esTipoPersonalizado) {
                    OutlinedTextField(
                        value = tipoPersonalizado,
                        onValueChange = { tipoPersonalizado = it },
                        label = { Text("Especifica el tipo de catering") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                }

                Text("Resultados a mostrar", style = MaterialTheme.typography.labelLarge)
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    ModoBusqueda.values().forEach { mode ->
                        FilterChip(
                            selected = modoBusqueda == mode,
                            onClick = { claveModoBusqueda = mode.name },
                            label = { Text(mode.etiqueta) }
                        )
                    }
                }

                Text("Estado de las publicaciones", style = MaterialTheme.typography.labelLarge)
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val statusOptions = listOf(
                        EstadoPublicacion.ACTIVA to "Activas",
                        EstadoPublicacion.EN_PROGRESO to "En progreso",
                        EstadoPublicacion.FINALIZADA to "Finalizadas",
                        null to "Todas (sin canceladas)"
                    )
                    statusOptions.forEach { (status, label) ->
                        val key = status?.name ?: ESTADO_BUSQUEDA_TODO
                        FilterChip(
                            selected = claveFiltroEstado == key,
                            onClick = { claveFiltroEstado = key },
                            label = { Text(label) }
                        )
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    AssistChip(
                        onClick = {
                            consultaBusqueda = ""
                            consultaCiudad = ""
                            opcionTipoSeleccionada = ""
                            esTipoPersonalizado = false
                            tipoPersonalizado = ""
                            claveModoBusqueda = ModoBusqueda.TODO.name
                            claveFiltroEstado = EstadoPublicacion.ACTIVA.name
                        },
                        label = { Text("Limpiar filtros") }
                    )
                }
            }
        }

        item {
            Text(
                text = "Resultados • $resultsLabel",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
        }

        val hasResults = (mostrarOfertas && resultadosOfertas.isNotEmpty()) || (mostrarSolicitudes && resultadosSolicitudes.isNotEmpty())
        if (!hasResults) {
            item {
                Text(
                    text = "No encontramos coincidencias con los filtros actuales.",
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }

        if (mostrarOfertas && resultadosOfertas.isNotEmpty()) {
            item { TituloSeccion("Propuestas de empresas (${resultadosOfertas.size})") }
            items(resultadosOfertas, key = { it.id }) { oferta ->
                TarjetaOfertaEmpresa(
                    oferta = oferta,
                    esSeleccionada = state.idsOfertasEmpresaAceptadas.contains(oferta.id),
                    showAction = canClientAct,
                    onToggle = { onAlternarOferta(oferta.id) },
                    canEdit = false,
                    onEdit = {},
                    onCambioEstado = { estado -> onCambioEstadoOferta(oferta.id, estado) },
                    onVerPerfil = { onVerPerfil(oferta.idAutor) },
                    estadoEnabled = adminControlsEnabled
                )
            }
        }

        if (mostrarSolicitudes && resultadosSolicitudes.isNotEmpty()) {
            item { TituloSeccion("Solicitudes de clientes (${resultadosSolicitudes.size})") }
            items(resultadosSolicitudes, key = { it.id }) { solicitud ->
                TarjetaSolicitudCliente(
                    solicitud = solicitud,
                    esSeleccionada = state.idsSolicitudesClienteAceptadas.contains(solicitud.id),
                    showAction = canCompanyAct,
                    onToggle = { onAlternarSolicitud(solicitud.id) },
                    canEdit = false,
                    onEdit = {},
                    onCambioEstado = { estado -> onCambioEstadoSolicitud(solicitud.id, estado) },
                    onVerPerfil = { onVerPerfil(solicitud.idAutor) },
                    estadoEnabled = adminControlsEnabled
                )
            }
        }
    }
}

@Composable
private fun SeccionMisPublicaciones(
    modifier: Modifier = Modifier,
    state: EstadoInicioUi,
    esEmpresa: Boolean,
    usuarioActual: User,
    onEditarOferta: (String) -> Unit,
    onEditarSolicitud: (String) -> Unit,
    onCambioEstado: (String, EstadoPublicacion, Boolean) -> Unit,
    onVerPerfil: (String) -> Unit
) {
    if (esEmpresa) {
        val ofertas = state.ofertasEmpresa.filter { it.idAutor == usuarioActual.uid }
        ListaOfertasEmpresa(
            ofertas = ofertas,
            acceptedIds = emptySet(),
            onToggle = {},
            showAction = false,
            emptyMessage = "Aún no has creado propuestas de catering",
            modifier = modifier,
            usuarioActualId = usuarioActual.uid,
            onEdit = onEditarOferta,
            onCambioEstado = { oferta, estado -> onCambioEstado(oferta.id, estado, true) },
            onVerPerfil = { onVerPerfil(it.idAutor) },
            estadoEnabled = true
        )
    } else {
        val solicitudes = state.solicitudesCliente.filter { it.idAutor == usuarioActual.uid }
        ListaSolicitudesCliente(
            solicitudes = solicitudes,
            acceptedIds = emptySet(),
            onToggle = {},
            showAction = false,
            emptyMessage = "Aún no has publicado solicitudes",
            modifier = modifier,
            usuarioActualId = usuarioActual.uid,
            onEdit = onEditarSolicitud,
            onCambioEstado = { solicitud, estado -> onCambioEstado(solicitud.id, estado, false) },
            onVerPerfil = { onVerPerfil(it.idAutor) },
            estadoEnabled = true
        )
    }
}

@Composable
private fun SeccionMisSolicitudes(
    modifier: Modifier = Modifier,
    state: EstadoInicioUi,
    esEmpresa: Boolean,
    usuarioActual: User,
    onCambioEstado: (String, EstadoPublicacion, Boolean) -> Unit,
    onVerPerfil: (String) -> Unit
) {
    if (esEmpresa) {
        val solicitudes = state.solicitudesCliente.filter { state.idsSolicitudesClienteAceptadas.contains(it.id) }
        ListaSolicitudesCliente(
            solicitudes = solicitudes,
            acceptedIds = state.idsSolicitudesClienteAceptadas,
            onToggle = {},
            showAction = false,
            emptyMessage = "No has marcado solicitudes todavía",
            modifier = modifier,
            usuarioActualId = usuarioActual.uid,
            onEdit = null,
            onCambioEstado = { solicitud, estado -> onCambioEstado(solicitud.id, estado, false) },
            onVerPerfil = { onVerPerfil(it.idAutor) },
            estadoEnabled = true
        )
    } else {
        val ofertas = state.ofertasEmpresa.filter { state.idsOfertasEmpresaAceptadas.contains(it.id) }
        ListaOfertasEmpresa(
            ofertas = ofertas,
            acceptedIds = state.idsOfertasEmpresaAceptadas,
            onToggle = {},
            showAction = false,
            emptyMessage = "No has guardado propuestas",
            modifier = modifier,
            usuarioActualId = usuarioActual.uid,
            onEdit = null,
            onCambioEstado = { oferta, estado -> onCambioEstado(oferta.id, estado, true) },
            onVerPerfil = { onVerPerfil(it.idAutor) },
            estadoEnabled = true
        )
    }
}

@Composable
private fun ListaSolicitudesCliente(
    solicitudes: List<SolicitudCliente>,
    acceptedIds: Set<String>,
    onToggle: (String) -> Unit,
    showAction: Boolean,
    emptyMessage: String,
    modifier: Modifier = Modifier,
    usuarioActualId: String,
    onEdit: ((String) -> Unit)?,
    onCambioEstado: (SolicitudCliente, EstadoPublicacion) -> Unit,
    onVerPerfil: (SolicitudCliente) -> Unit,
    estadoEnabled: Boolean
) {
    if (solicitudes.isEmpty()) {
        EstadoVacio(message = emptyMessage, modifier = modifier)
        return
    }
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        items(solicitudes, key = { it.id }) { solicitud ->
            TarjetaSolicitudCliente(
                solicitud = solicitud,
                esSeleccionada = acceptedIds.contains(solicitud.id),
                showAction = showAction,
                onToggle = { onToggle(solicitud.id) },
                canEdit = onEdit != null && solicitud.idAutor == usuarioActualId,
                onEdit = { onEdit?.invoke(solicitud.id) },
                onCambioEstado = { estado -> onCambioEstado(solicitud, estado) },
                onVerPerfil = { onVerPerfil(solicitud) },
                estadoEnabled = estadoEnabled
            )
        }
    }
}

@Composable
private fun ListaOfertasEmpresa(
    ofertas: List<OfertaEmpresa>,
    acceptedIds: Set<String>,
    onToggle: (String) -> Unit,
    showAction: Boolean,
    emptyMessage: String,
    modifier: Modifier = Modifier,
    usuarioActualId: String,
    onEdit: ((String) -> Unit)?,
    onCambioEstado: (OfertaEmpresa, EstadoPublicacion) -> Unit,
    onVerPerfil: (OfertaEmpresa) -> Unit,
    estadoEnabled: Boolean
) {
    if (ofertas.isEmpty()) {
        EstadoVacio(message = emptyMessage, modifier = modifier)
        return
    }
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        items(ofertas, key = { it.id }) { oferta ->
            TarjetaOfertaEmpresa(
                oferta = oferta,
                esSeleccionada = acceptedIds.contains(oferta.id),
                showAction = showAction,
                onToggle = { onToggle(oferta.id) },
                canEdit = onEdit != null && oferta.idAutor == usuarioActualId,
                onEdit = { onEdit?.invoke(oferta.id) },
                onCambioEstado = { estado -> onCambioEstado(oferta, estado) },
                onVerPerfil = { onVerPerfil(oferta) },
                estadoEnabled = estadoEnabled
            )
        }
    }
}

@Composable
private fun TarjetaSolicitudCliente(
    solicitud: SolicitudCliente,
    esSeleccionada: Boolean,
    showAction: Boolean,
    onToggle: () -> Unit,
    canEdit: Boolean,
    onEdit: () -> Unit,
    onCambioEstado: (EstadoPublicacion) -> Unit,
    onVerPerfil: () -> Unit,
    estadoEnabled: Boolean,
    onDelete: (() -> Unit)? = null
) {
    var menuExpandido by remember { mutableStateOf(false) }
    ElevatedCard(
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(solicitud.tipoCatering, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                    Text("Cliente: ${solicitud.nombreAutor}", style = MaterialTheme.typography.bodyMedium)
                }
                DesplegableEstado(
                    currentStatus = solicitud.estado,
                    onCambioEstado = onCambioEstado,
                    expanded = menuExpandido,
                    onExpandedChange = { menuExpandido = it },
                    enabled = estadoEnabled
                )
                if (canEdit) {
                    IconButton(onClick = onEdit) {
                        Icon(imageVector = Icons.Filled.Edit, contentDescription = "Editar solicitud")
                    }
                }
                if (onDelete != null) {
                    IconButton(onClick = onDelete) {
                        Icon(imageVector = Icons.Filled.Delete, contentDescription = "Eliminar solicitud")
                    }
                }
            }
            Spacer(Modifier.height(8.dp))
            Text("Rango de precio: ${solicitud.rangoPrecio}", style = MaterialTheme.typography.bodyMedium)
            Text("Personas: ${solicitud.cantidadPersonas}", style = MaterialTheme.typography.bodyMedium)
            Text("Ubicación: ${solicitud.ubicacion}", style = MaterialTheme.typography.bodyMedium)
            Text("Fecha: ${solicitud.fechaEvento}", style = MaterialTheme.typography.bodyMedium)
            if (solicitud.notas.isNotBlank()) {
                Spacer(Modifier.height(8.dp))
                Text(solicitud.notas, style = MaterialTheme.typography.bodySmall)
            }
            if (solicitud.servicios.isNotEmpty()) {
                Spacer(Modifier.height(8.dp))
                Text("Servicios requeridos:", style = MaterialTheme.typography.labelLarge)
                Text(solicitud.servicios.joinToString(", "), style = MaterialTheme.typography.bodyMedium)
            }
            Spacer(Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                TextButton(onClick = onVerPerfil, modifier = Modifier.fillMaxWidth()) {
                    Text("Ver perfil")
                }
            }
            if (showAction) {
                Spacer(Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Button(onClick = onToggle, modifier = Modifier.fillMaxWidth()) {
                        Text(if (esSeleccionada) "Solicitud enviada" else "Solicitar ejecución")
                    }
                }
            }
        }
    }
}

@Composable
private fun TarjetaOfertaEmpresa(
    oferta: OfertaEmpresa,
    esSeleccionada: Boolean,
    showAction: Boolean,
    onToggle: () -> Unit,
    canEdit: Boolean,
    onEdit: () -> Unit,
    onCambioEstado: (EstadoPublicacion) -> Unit,
    onVerPerfil: () -> Unit,
    estadoEnabled: Boolean,
    onDelete: (() -> Unit)? = null
) {
    var menuExpandido by remember { mutableStateOf(false) }
    ElevatedCard(
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    val headline = oferta.tipoCatering.takeIf { it.isNotBlank() }
                        ?: "Propuesta de ${oferta.nombreAutor}"
                    Text(headline, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                    Text("Empresa: ${oferta.nombreAutor}", style = MaterialTheme.typography.bodyMedium)
                    if (oferta.rangoUbicacion.isNotBlank()) {
                        Text("Cobertura: ${oferta.rangoUbicacion}", style = MaterialTheme.typography.bodyMedium)
                    }
                }
                DesplegableEstado(
                    currentStatus = oferta.estado,
                    onCambioEstado = onCambioEstado,
                    expanded = menuExpandido,
                    onExpandedChange = { menuExpandido = it },
                    enabled = estadoEnabled
                )
                if (canEdit) {
                    IconButton(onClick = onEdit) {
                        Icon(imageVector = Icons.Filled.Edit, contentDescription = "Editar propuesta")
                    }
                }
                if (onDelete != null) {
                    IconButton(onClick = onDelete) {
                        Icon(imageVector = Icons.Filled.Delete, contentDescription = "Eliminar propuesta")
                    }
                }
            }
            Spacer(Modifier.height(8.dp))
            Text("Rango de precio: ${oferta.rangoPrecio}", style = MaterialTheme.typography.bodyMedium)
            Text("Personas: ${oferta.rangoPersonas}", style = MaterialTheme.typography.bodyMedium)
            if (oferta.descripcion.isNotBlank()) {
                Spacer(Modifier.height(8.dp))
                Text(oferta.descripcion, style = MaterialTheme.typography.bodySmall)
            }
            Spacer(Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                TextButton(onClick = onVerPerfil, modifier = Modifier.fillMaxWidth()) {
                    Text("Ver perfil")
                }
            }
            if (showAction) {
                Spacer(Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Button(onClick = onToggle, modifier = Modifier.fillMaxWidth()) {
                        Text(if (esSeleccionada) "Solicitud enviada" else "Solicitar ejecución")
                    }
                }
            }
        }
    }
}

@Composable
private fun DesplegableEstado(
    currentStatus: EstadoPublicacion,
    onCambioEstado: (EstadoPublicacion) -> Unit,
    expanded: Boolean,
    onExpandedChange: (Boolean) -> Unit,
    enabled: Boolean
) {
    Column(horizontalAlignment = Alignment.End) {
        AssistChip(onClick = { if (enabled) onExpandedChange(true) }, label = { Text(currentStatus.label) }, enabled = enabled)
        DropdownMenu(expanded = expanded, onDismissRequest = { onExpandedChange(false) }) {
            EstadoPublicacion.values().forEach { estado ->
                DropdownMenuItem(
                    text = { Text(estado.label) },
                    onClick = {
                        onExpandedChange(false)
                        onCambioEstado(estado)
                    }
                )
            }
        }
    }
}

@Composable
private fun EstadoVacio(message: String, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Text(text = message, style = MaterialTheme.typography.bodyMedium)
    }
}

private data class ObjetivoCreacionAdmin(val perfil: PerfilPublico, val tipo: TipoPublicacion)

private val CATERING_TYPE_OPTIONS = listOf(
    "Empresarial",
    "Social",
    "Banquetes",
    "Cóctel",
    "Gourmet",
    "Escolar",
    "Sanitario",
    "Eventos deportivos",
    "A domicilio",
    "Ecológico"
)

@Composable
private fun SeccionEditarPublicacion(
    modifier: Modifier = Modifier,
    target: ObjetivoEdicion?,
    onCancel: () -> Unit,
    onSave: (DatosEdicion) -> Unit
) {
    val objetivo = target ?: run {
        EstadoVacio(message = "Selecciona una publicación para editar", modifier = modifier)
        return
    }
    val editorScrollState = rememberScrollState()
    Surface(modifier = modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(editorScrollState)
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = if (objetivo.tipo == TipoEdicion.OFERTA_EMPRESA) "Editar propuesta" else "Editar solicitud",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.SemiBold
            )
            var rangoPrecio by remember(objetivo) { mutableStateOf(objetivo.oferta?.rangoPrecio ?: objetivo.solicitud?.rangoPrecio.orEmpty()) }
            var rangoPersonas by remember(objetivo) { mutableStateOf(objetivo.oferta?.rangoPersonas ?: objetivo.solicitud?.cantidadPersonas?.toString().orEmpty()) }
            var rangoUbicacion by remember(objetivo) { mutableStateOf(objetivo.oferta?.rangoUbicacion ?: objetivo.solicitud?.ubicacion.orEmpty()) }
            var descripcion by remember(objetivo) { mutableStateOf(objetivo.oferta?.descripcion ?: objetivo.solicitud?.notas.orEmpty()) }
            var servicios by remember(objetivo) { mutableStateOf(objetivo.solicitud?.servicios?.joinToString(", ") ?: "") }
            var fechaEvento by remember(objetivo) { mutableStateOf(objetivo.solicitud?.fechaEvento ?: "") }
            val initialCateringType = objetivo.oferta?.tipoCatering ?: objetivo.solicitud?.tipoCatering ?: ""
            val initialTypeOption = remember(objetivo) {
                CATERING_TYPE_OPTIONS.firstOrNull { it.equals(initialCateringType, ignoreCase = true) }
            }
            var tipoCatering by remember(objetivo) { mutableStateOf(initialCateringType) }
            var selectedCateringOption by remember(objetivo) { mutableStateOf(initialTypeOption) }
            var isCustomCatering by remember(objetivo) { mutableStateOf(initialTypeOption == null && initialCateringType.isNotBlank()) }
            var customCateringType by remember(objetivo) { mutableStateOf(if (initialTypeOption == null) initialCateringType else "") }

            OutlinedTextField(
                value = rangoPrecio,
                onValueChange = { rangoPrecio = it },
                label = { Text("Rango de precio") },
                modifier = Modifier.fillMaxWidth()
            )
            if (objetivo.tipo == TipoEdicion.OFERTA_EMPRESA) {
                OutlinedTextField(
                    value = rangoPersonas,
                    onValueChange = { rangoPersonas = it },
                    label = { Text("Rango de personas") },
                    modifier = Modifier.fillMaxWidth()
                )
            } else {
                OutlinedTextField(
                    value = rangoPersonas,
                    onValueChange = { rangoPersonas = it.filter { ch -> ch.isDigit() } },
                    label = { Text("Número de personas") },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = servicios,
                    onValueChange = { servicios = it },
                    label = { Text("Servicios (coma)") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
            OutlinedTextField(
                value = rangoUbicacion,
                onValueChange = { rangoUbicacion = it },
                label = { Text(if (objetivo.tipo == TipoEdicion.OFERTA_EMPRESA) "Cobertura geográfica" else "Localización") },
                modifier = Modifier.fillMaxWidth()
            )
            Text("Tipo de catering", style = MaterialTheme.typography.labelLarge)
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                CATERING_TYPE_OPTIONS.forEach { option ->
                    val esSeleccionada = !isCustomCatering && selectedCateringOption?.equals(option, ignoreCase = true) == true
                    FilterChip(
                        selected = esSeleccionada,
                        onClick = {
                            if (esSeleccionada) {
                                selectedCateringOption = null
                                tipoCatering = ""
                            } else {
                                selectedCateringOption = option
                                tipoCatering = option
                            }
                            isCustomCatering = false
                        },
                        label = { Text(option) }
                    )
                }
                FilterChip(
                    selected = isCustomCatering,
                    onClick = {
                        isCustomCatering = !isCustomCatering
                        if (isCustomCatering) {
                            selectedCateringOption = null
                            tipoCatering = customCateringType
                        } else {
                            tipoCatering = ""
                        }
                    },
                    label = { Text("Otro") }
                )
            }
            if (isCustomCatering) {
                OutlinedTextField(
                    value = customCateringType,
                    onValueChange = {
                        customCateringType = it
                        tipoCatering = it
                    },
                    label = { Text("Especifica el tipo de catering") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
            if (objetivo.tipo == TipoEdicion.SOLICITUD_CLIENTE) {
                OutlinedTextField(
                    value = fechaEvento,
                    onValueChange = { fechaEvento = it },
                    label = { Text("Fecha del evento") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
            OutlinedTextField(
                value = descripcion,
                onValueChange = { descripcion = it },
                label = { Text(if (objetivo.tipo == TipoEdicion.OFERTA_EMPRESA) "Descripción" else "Notas") },
                modifier = Modifier.fillMaxWidth()
            )
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                TextButton(onClick = onCancel) { Text("Cancelar") }
                Button(onClick = {
                    val serviciosList = servicios.split(',').mapNotNull { it.trim().takeIf(String::isNotEmpty) }
                    val input = DatosEdicion(
                        rangoPrecio = rangoPrecio.trim(),
                        rangoPersonas = rangoPersonas.trim(),
                        rangoUbicacion = rangoUbicacion.trim(),
                        descripcion = descripcion.trim(),
                        servicios = serviciosList,
                        cantidadPersonas = rangoPersonas.toIntOrNull() ?: objetivo.solicitud?.cantidadPersonas ?: 0,
                        fechaEvento = fechaEvento.trim(),
                        tipoCatering = tipoCatering.trim()
                    )
                    onSave(input)
                }) {
                    Text("Guardar cambios")
                }
            }
        }
    }
}

@Composable
private fun SeccionBandejaEntrada(
    modifier: Modifier = Modifier,
    messages: List<MensajeBandeja>,
    usuarioActualId: String,
    onAccept: (String) -> Unit,
    onDecline: (String) -> Unit,
    onVerPerfil: (String) -> Unit
) {
    if (messages.isEmpty()) {
        EstadoVacio(message = "Tu buzón está vacío", modifier = modifier)
        return
    }
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(messages, key = { it.id }) { message ->
            ElevatedCard(
                colors = CardDefaults.elevatedCardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainerLow
                )
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(message.titulo, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Medium)
                    if (message.tituloPublicacion.isNotBlank()) {
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = "Publicación: ${message.tituloPublicacion}",
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                    Spacer(Modifier.height(4.dp))
                    Text(message.cuerpo, style = MaterialTheme.typography.bodyMedium)
                    Spacer(Modifier.height(8.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        val timestampText = formatearMarcaTemporal(message.marcaTemporal)
                        if (timestampText.isNotBlank()) {
                            Text(timestampText, style = MaterialTheme.typography.labelMedium)
                        }
                        when (message.tipoEntrada) {
                            TipoEntradaBandeja.CAMBIO_ESTADO -> {
                                message.estadoPublicacion?.let { estado ->
                                    AssistChip(onClick = {}, label = { Text(estado.label) })
                                }
                            }

                            TipoEntradaBandeja.SOLICITUD_EJECUCION, TipoEntradaBandeja.RESPUESTA_EJECUCION -> {
                                message.estadoEjecucion?.let { estado ->
                                    AssistChip(onClick = {}, label = { Text(estado.aEtiqueta()) })
                                }
                            }
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                    TextButton(onClick = { onVerPerfil(message.idActor) }) {
                        Text("Ver perfil de ${message.nombreActor}")
                    }
                    val pendingForUser = message.tipoEntrada == TipoEntradaBandeja.SOLICITUD_EJECUCION &&
                        message.estadoEjecucion == EstadoSolicitudEjecucion.PENDIENTE &&
                        message.idDestinatario == usuarioActualId
                    if (pendingForUser) {
                        Spacer(Modifier.height(8.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Button(onClick = { onAccept(message.id) }, modifier = Modifier.weight(1f)) {
                                Text("Aceptar")
                            }
                            OutlinedButton(onClick = { onDecline(message.id) }, modifier = Modifier.weight(1f)) {
                                Text("Rechazar")
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun formatearMarcaTemporal(timestamp: Long): String {
    if (timestamp <= 0L) return ""
    val formatter = SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.getDefault())
    return formatter.format(Date(timestamp))
}

private fun EstadoSolicitudEjecucion.aEtiqueta(): String = when (this) {
    EstadoSolicitudEjecucion.PENDIENTE -> "Pendiente"
    EstadoSolicitudEjecucion.ACEPTADA -> "Aceptada"
    EstadoSolicitudEjecucion.RECHAZADA -> "Rechazada"
}

@Composable
private fun DialogoOfertaEmpresa(
    onDismiss: () -> Unit,
    onCreate: (OfertaEmpresaInput) -> Unit
) {
    var rangoPrecio by remember { mutableStateOf("") }
    var rangoPersonas by remember { mutableStateOf("") }
    var rangoUbicacion by remember { mutableStateOf("") }
    var descripcion by remember { mutableStateOf("") }
    var tipoCatering by remember { mutableStateOf("") }
    var opcionTipoSeleccionada by remember { mutableStateOf<String?>(null) }
    var esTipoPersonalizado by remember { mutableStateOf(false) }
    var customCateringType by remember { mutableStateOf("") }

    val isValid = rangoPrecio.isNotBlank() && rangoPersonas.isNotBlank() &&
        rangoUbicacion.isNotBlank() && tipoCatering.isNotBlank()

    val dialogScrollState = rememberScrollState()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Nueva propuesta de catering") },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 420.dp)
                    .verticalScroll(dialogScrollState),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedTextField(
                    value = rangoPrecio,
                    onValueChange = { rangoPrecio = it },
                    label = { Text("Rango de precio") },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = rangoPersonas,
                    onValueChange = { rangoPersonas = it },
                    label = { Text("Rango de personas") },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = rangoUbicacion,
                    onValueChange = { rangoUbicacion = it },
                    label = { Text("Cobertura geográfica") },
                    modifier = Modifier.fillMaxWidth()
                )
                Text("Tipo de catering", style = MaterialTheme.typography.labelLarge)
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    CATERING_TYPE_OPTIONS.forEach { option ->
                        val esSeleccionada = !esTipoPersonalizado && opcionTipoSeleccionada?.equals(option, ignoreCase = true) == true
                        FilterChip(
                            selected = esSeleccionada,
                            onClick = {
                                if (esSeleccionada) {
                                    opcionTipoSeleccionada = null
                                    tipoCatering = ""
                                } else {
                                    opcionTipoSeleccionada = option
                                    tipoCatering = option
                                }
                                esTipoPersonalizado = false
                            },
                            label = { Text(option) }
                        )
                    }
                    FilterChip(
                        selected = esTipoPersonalizado,
                        onClick = {
                            esTipoPersonalizado = !esTipoPersonalizado
                            if (esTipoPersonalizado) {
                                opcionTipoSeleccionada = null
                                tipoCatering = customCateringType
                            } else {
                                tipoCatering = ""
                            }
                        },
                        label = { Text("Otro") }
                    )
                }
                if (esTipoPersonalizado) {
                    OutlinedTextField(
                        value = customCateringType,
                        onValueChange = {
                            customCateringType = it
                            tipoCatering = it
                        },
                        label = { Text("Especifica el tipo de catering") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                OutlinedTextField(
                    value = descripcion,
                    onValueChange = { descripcion = it },
                    label = { Text("Descripción (opcional)") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    onCreate(
                        OfertaEmpresaInput(
                            rangoPrecio = rangoPrecio.trim(),
                            rangoPersonas = rangoPersonas.trim(),
                            rangoUbicacion = rangoUbicacion.trim(),
                            descripcion = descripcion.trim(),
                            tipoCatering = tipoCatering.trim()
                        )
                    )
                },
                enabled = isValid
            ) {
                Text("Guardar")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancelar") }
        }
    )
}

@Composable
private fun DialogoSolicitudCliente(
    onDismiss: () -> Unit,
    onCreate: (SolicitudClienteInput) -> Unit
) {
    var rangoPrecio by remember { mutableStateOf("") }
    var cantidadPersonas by remember { mutableStateOf("") }
    var servicios by remember { mutableStateOf("") }
    var tipoCatering by remember { mutableStateOf("") }
    var opcionTipoSeleccionada by remember { mutableStateOf<String?>(null) }
    var esTipoPersonalizado by remember { mutableStateOf(false) }
    var customCateringType by remember { mutableStateOf("") }
    var ubicacion by remember { mutableStateOf("") }
    var fechaEvento by remember { mutableStateOf("") }
    var notas by remember { mutableStateOf("") }

    val peopleNumber = cantidadPersonas.toIntOrNull()
    val isValid = rangoPrecio.isNotBlank() && peopleNumber != null && tipoCatering.isNotBlank() &&
        ubicacion.isNotBlank() && fechaEvento.isNotBlank()

    val requestScrollState = rememberScrollState()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Nueva solicitud de evento") },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 420.dp)
                    .verticalScroll(requestScrollState),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedTextField(
                    value = rangoPrecio,
                    onValueChange = { rangoPrecio = it },
                    label = { Text("Presupuesto disponible") },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = cantidadPersonas,
                    onValueChange = { cantidadPersonas = it.filter { ch -> ch.isDigit() } },
                    label = { Text("Número de personas") },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = servicios,
                    onValueChange = { servicios = it },
                    label = { Text("Servicios necesarios (separados por coma)") },
                    modifier = Modifier.fillMaxWidth()
                )
                Text("Tipo de catering", style = MaterialTheme.typography.labelLarge)
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    CATERING_TYPE_OPTIONS.forEach { option ->
                        val esSeleccionada = !esTipoPersonalizado && opcionTipoSeleccionada?.equals(option, ignoreCase = true) == true
                        FilterChip(
                            selected = esSeleccionada,
                            onClick = {
                                if (esSeleccionada) {
                                    opcionTipoSeleccionada = null
                                    tipoCatering = ""
                                } else {
                                    opcionTipoSeleccionada = option
                                    tipoCatering = option
                                }
                                esTipoPersonalizado = false
                            },
                            label = { Text(option) }
                        )
                    }
                    FilterChip(
                        selected = esTipoPersonalizado,
                        onClick = {
                            esTipoPersonalizado = !esTipoPersonalizado
                            if (esTipoPersonalizado) {
                                opcionTipoSeleccionada = null
                                tipoCatering = customCateringType
                            } else {
                                tipoCatering = ""
                            }
                        },
                        label = { Text("Otro") }
                    )
                }
                if (esTipoPersonalizado) {
                    OutlinedTextField(
                        value = customCateringType,
                        onValueChange = {
                            customCateringType = it
                            tipoCatering = it
                        },
                        label = { Text("Especifica el tipo de catering") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                OutlinedTextField(
                    value = ubicacion,
                    onValueChange = { ubicacion = it },
                    label = { Text("Localización") },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = fechaEvento,
                    onValueChange = { fechaEvento = it },
                    label = { Text("Fecha del evento") },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = notas,
                    onValueChange = { notas = it },
                    label = { Text("Notas adicionales (opcional)") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    onCreate(
                        SolicitudClienteInput(
                            rangoPrecio = rangoPrecio.trim(),
                            cantidadPersonas = peopleNumber ?: 0,
                            servicios = servicios.split(',').mapNotNull { it.trim().takeIf(String::isNotEmpty) },
                            tipoCatering = tipoCatering.trim(),
                            ubicacion = ubicacion.trim(),
                            fechaEvento = fechaEvento.trim(),
                            notas = notas.trim()
                        )
                    )
                },
                enabled = isValid
            ) {
                Text("Publicar")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancelar") }
        }
    )
}

@Composable
private fun HojaDetallesPerfil(
    profile: PerfilPublico,
    currentUserId: String,
    onDismiss: () -> Unit,
    onSubmitReview: (Int, String) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val joinDateText = remember(profile.fechaRegistro) {
        profile.fechaRegistro.takeIf { it > 0L }?.let {
            val formatter = SimpleDateFormat("d 'de' MMMM yyyy", Locale.getDefault())
            formatter.format(Date(it))
        } ?: "Sin información"
    }
    val ratingText = if (profile.cantidadResenas > 0) {
        "${"%.1f".format(profile.calificacionPromedio)} (${profile.cantidadResenas} reseñas)"
    } else {
        "Sin reseñas"
    }
    val roleLabel = when (profile.rol) {
        Role.ADMIN -> "Administrador"
        Role.CLIENT -> "Cliente"
        Role.COMPANY -> "Empresa"
    }
    var rating by remember(profile.id) { mutableStateOf(5) }
    var comment by remember(profile.id) { mutableStateOf("") }
    val isSelfProfile = profile.id == currentUserId

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        dragHandle = { BottomSheetDefaults.DragHandle() }
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(120.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.surfaceVariant),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Person,
                            contentDescription = "Perfil",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(56.dp)
                        )
                    }
                    Text(profile.nombre, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                    Text("Rol: $roleLabel", style = MaterialTheme.typography.bodyMedium)
                    Text("Afiliación: ${profile.afiliacion.ifBlank { "Sin afiliación" }}", style = MaterialTheme.typography.bodyMedium)
                    Text("Ubicación: ${profile.direccion.ifBlank { "Sin información" }}", style = MaterialTheme.typography.bodyMedium)
                    Text("Miembro desde: $joinDateText", style = MaterialTheme.typography.bodyMedium)
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(imageVector = Icons.Filled.Star, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Text(ratingText, style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }
            if (!isSelfProfile) {
                item {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            "Tu reseña",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        SelectorCalificacion(
                            rating = rating,
                            onRatingSelected = { rating = it }
                        )
                        OutlinedTextField(
                            value = comment,
                            onValueChange = { comment = it },
                            label = { Text("Comentario (opcional)") },
                            modifier = Modifier.fillMaxWidth()
                        )
                        Button(
                            onClick = {
                                onSubmitReview(rating, comment)
                                comment = ""
                            },
                            modifier = Modifier.align(Alignment.End)
                        ) {
                            Text("Enviar reseña")
                        }
                    }
                }
            }
            if (profile.descripcion.isNotBlank()) {
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("Descripción", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                        Text(profile.descripcion, style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }
            item {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Trabajos previos", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                    if (profile.trabajosPrevios.isEmpty()) {
                        Text("No hay trabajos previos registrados", style = MaterialTheme.typography.bodyMedium)
                    } else {
                        profile.trabajosPrevios.forEach { work ->
                            Column(modifier = Modifier.padding(vertical = 8.dp)) {
                                Text(work.titulo, fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.bodyLarge)
                                if (work.descripcion.isNotBlank()) {
                                    Text(work.descripcion, style = MaterialTheme.typography.bodyMedium)
                                }
                            }
                        }
                    }
                }
            }
            item {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Reseñas recientes", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                    if (profile.resenasRecientes.isEmpty()) {
                        Text("Aún no hay reseñas para este perfil", style = MaterialTheme.typography.bodyMedium)
                    } else {
                        profile.resenasRecientes.forEachIndexed { index, review ->
                            Column(modifier = Modifier.padding(vertical = 6.dp)) {
                                Text("${review.nombreAutor} - ${review.calificacion}/5", fontWeight = FontWeight.SemiBold)
                                if (review.comentario.isNotBlank()) {
                                    Text(review.comentario, style = MaterialTheme.typography.bodyMedium)
                                }
                            }
                            if (index < profile.resenasRecientes.lastIndex) {
                                Divider()
                            }
                        }
                    }
                }
            }
            item {
                TextButton(
                    onClick = onDismiss,
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                ) {
                    Text("Cerrar")
                }
            }
        }
    }
}

@Composable
private fun DialogoResena(
    prompt: RecordatorioResena,
    onDismiss: () -> Unit,
    onConfirm: (EnvioResenas) -> Unit
) {
    var calificacionCliente by remember { mutableStateOf(5) }
    var comentarioCliente by remember { mutableStateOf("") }
    var calificacionEmpresa by remember { mutableStateOf(5) }
    var comentarioEmpresa by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
                confirmButton = {
                    TextButton(onClick = {
                        val envio = EnvioResenas(
                            resenaCliente = DatosResena(
                                idUsuarioEmisor = prompt.idCliente,
                                nombreUsuarioEmisor = prompt.nombreCliente,
                                idUsuarioDestino = prompt.idEmpresa,
                                nombreUsuarioDestino = prompt.nombreEmpresa,
                                calificacion = calificacionCliente,
                                comentario = comentarioCliente.trim()
                            ),
                            resenaEmpresa = DatosResena(
                                idUsuarioEmisor = prompt.idEmpresa,
                                nombreUsuarioEmisor = prompt.nombreEmpresa,
                                idUsuarioDestino = prompt.idCliente,
                                nombreUsuarioDestino = prompt.nombreCliente,
                                calificacion = calificacionEmpresa,
                                comentario = comentarioEmpresa.trim()
                            )
                        )
                        onConfirm(envio)
            }) {
                Text("Guardar reseñas")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancelar") }
        },
        title = { Text(prompt.asunto) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Text("Reseña del cliente hacia la empresa", fontWeight = FontWeight.SemiBold)
                SelectorCalificacion(rating = calificacionCliente, onRatingSelected = { calificacionCliente = it })
                OutlinedTextField(
                    value = comentarioCliente,
                    onValueChange = { comentarioCliente = it },
                    label = { Text("Comentario del cliente") },
                    modifier = Modifier.fillMaxWidth()
                )
                Divider()
                Text("Reseña de la empresa hacia el cliente", fontWeight = FontWeight.SemiBold)
                SelectorCalificacion(rating = calificacionEmpresa, onRatingSelected = { calificacionEmpresa = it })
                OutlinedTextField(
                    value = comentarioEmpresa,
                    onValueChange = { comentarioEmpresa = it },
                    label = { Text("Comentario de la empresa") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    )
}

@Composable
private fun SelectorCalificacion(rating: Int, onRatingSelected: (Int) -> Unit) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        (1..5).forEach { value ->
            IconButton(onClick = { onRatingSelected(value) }) {
                Icon(
                    imageVector = if (value <= rating) Icons.Filled.Star else Icons.Outlined.StarBorder,
                    contentDescription = "Calificación $value"
                )
            }
        }
    }
}
