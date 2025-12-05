package com.example.catering.ui.screens.admin

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Divider
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.catering.data.model.Role
import com.example.catering.ui.screens.home.AdminUserInput
import com.example.catering.ui.screens.home.SolicitudCliente
import com.example.catering.ui.screens.home.OfertaEmpresa
import com.example.catering.ui.screens.home.PerfilPublico
import com.example.catering.ui.screens.home.EstadoPublicacion
import com.example.catering.ui.screens.home.TipoPublicacion
import java.util.Locale
import java.util.UUID

@Composable
fun AdminDashboard(
    modifier: Modifier = Modifier,
    perfiles: Map<String, PerfilPublico>,
    ofertas: List<OfertaEmpresa>,
    solicitudes: List<SolicitudCliente>,
    onEditarOferta: (String) -> Unit,
    onEditarSolicitud: (String) -> Unit,
    onEliminarOferta: (String) -> Unit,
    onEliminarSolicitud: (String) -> Unit,
    onSaveUser: (AdminUserInput) -> Unit,
    onDeleteUser: (String) -> Unit,
    onCreatePublication: () -> Unit
) {
    val scrollState = rememberScrollState()

    Surface(modifier = modifier.padding(16.dp)) {
        Column(
            verticalArrangement = Arrangement.spacedBy(24.dp),
            modifier = Modifier.verticalScroll(scrollState)
        ) {
            PublicationAdminSection(
                ofertas = ofertas,
                solicitudes = solicitudes,
                onEditarOferta = onEditarOferta,
                onEditarSolicitud = onEditarSolicitud,
                onEliminarOferta = onEliminarOferta,
                onEliminarSolicitud = onEliminarSolicitud,
                onCreatePublication = onCreatePublication
            )
            Divider()
            ProfileAdminSection(
                perfiles = perfiles,
                onSaveUser = onSaveUser,
                onDeleteUser = onDeleteUser
            )
        }
    }
}

@Composable
private fun ProfileAdminSection(
    perfiles: Map<String, PerfilPublico>,
    onSaveUser: (AdminUserInput) -> Unit,
    onDeleteUser: (String) -> Unit
) {
    val listaPerfiles = perfiles.values.sortedBy { it.nombre.lowercase(Locale.getDefault()) }
    var idEdicion by rememberSaveable(listaPerfiles.size) { mutableStateOf(listaPerfiles.firstOrNull()?.id ?: UUID.randomUUID().toString()) }
    var email by rememberSaveable(idEdicion) { mutableStateOf(perfiles[idEdicion]?.correo ?: "") }
    var nombreVisible by rememberSaveable(idEdicion) { mutableStateOf(perfiles[idEdicion]?.nombre ?: "") }
    var rol by rememberSaveable(idEdicion) { mutableStateOf(perfiles[idEdicion]?.rol ?: Role.CLIENT) }
    var afiliacion by rememberSaveable(idEdicion) { mutableStateOf(perfiles[idEdicion]?.afiliacion ?: "") }
    var direccion by rememberSaveable(idEdicion) { mutableStateOf(perfiles[idEdicion]?.direccion ?: "") }
    var descripcion by rememberSaveable(idEdicion) { mutableStateOf(perfiles[idEdicion]?.descripcion ?: "") }
    

    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text("Gestión de perfiles", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
            OutlinedTextField(
                value = idEdicion,
                onValueChange = { idEdicion = it },
                label = { Text("Identificador") },
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = nombreVisible,
                onValueChange = { nombreVisible = it },
                label = { Text("Nombre") },
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = email,
                onValueChange = { email = it },
                label = { Text("Correo") },
                modifier = Modifier.fillMaxWidth()
            )
            Text("Rol", style = MaterialTheme.typography.labelLarge)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Role.values().forEach { option ->
                    FilterChip(
                        selected = rol == option,
                        onClick = { rol = option },
                        label = { Text(rolLabel(option)) }
                    )
                }
            }
            OutlinedTextField(
                value = afiliacion,
                onValueChange = { afiliacion = it },
                label = { Text("Afiliación") },
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = direccion,
                onValueChange = { direccion = it },
                label = { Text("Dirección") },
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = descripcion,
                onValueChange = { descripcion = it },
                label = { Text("Descripción") },
                modifier = Modifier.fillMaxWidth()
            )
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                Button(onClick = {
                    val input = AdminUserInput(
                        idUsuario = idEdicion.trim().ifBlank { UUID.randomUUID().toString() },
                        email = email.trim(),
                        nombreVisible = nombreVisible.trim(),
                        rol = rol,
                        afiliacion = afiliacion.trim(),
                        direccion = direccion.trim(),
                        descripcion = descripcion.trim()
                    )
                    onSaveUser(input)
                }, modifier = Modifier.weight(1f)) {
                    Text("Guardar")
                }
                OutlinedButton(onClick = {
                    val newId = UUID.randomUUID().toString()
                    idEdicion = newId
                    email = ""
                    nombreVisible = ""
                    rol = Role.CLIENT
                    afiliacion = ""
                    direccion = ""
                    descripcion = ""
                }, modifier = Modifier.weight(1f)) {
                    Text("Nuevo perfil")
                }
            }
            OutlinedButton(
                onClick = {
                    if (perfiles.containsKey(idEdicion)) {
                        onDeleteUser(idEdicion)
                        val newId = UUID.randomUUID().toString()
                        idEdicion = newId
                        email = ""
                        nombreVisible = ""
                        rol = Role.CLIENT
                        afiliacion = ""
                        direccion = ""
                        descripcion = ""
                    }
                },
                enabled = perfiles.containsKey(idEdicion)
            ) {
                Text("Eliminar perfil")
            }
        }
        Divider()
        Text("Perfiles existentes", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
        if (listaPerfiles.isEmpty()) {
            Text("No hay perfiles registrados.", style = MaterialTheme.typography.bodyMedium)
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 220.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(listaPerfiles) { profile ->
                    ElevatedCard(onClick = {
                        idEdicion = profile.id
                        email = profile.correo
                        nombreVisible = profile.nombre
                        rol = profile.rol
                        afiliacion = profile.afiliacion
                        direccion = profile.direccion
                        descripcion = profile.descripcion

                    }) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text(profile.nombre, fontWeight = FontWeight.SemiBold)
                            Text(profile.correo.ifBlank { "Sin correo" }, style = MaterialTheme.typography.bodySmall)
                            Text("Rol: ${rolLabel(profile.rol)}", style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PublicationAdminSection(
    ofertas: List<OfertaEmpresa>,
    solicitudes: List<SolicitudCliente>,
    onEditarOferta: (String) -> Unit,
    onEditarSolicitud: (String) -> Unit,
    onEliminarOferta: (String) -> Unit,
    onEliminarSolicitud: (String) -> Unit,
    onCreatePublication: () -> Unit
) {
    val locale = Locale.getDefault()
    val ofertasOrdenadas = ofertas.sortedBy { it.nombreAutor.lowercase(locale) }
    val solicitudesOrdenadas = solicitudes.sortedBy { it.nombreAutor.lowercase(locale) }

    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Text("Gestión de publicaciones", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
        Text(
            "Administra las propuestas de empresas y solicitudes de clientes en la plataforma.",
            style = MaterialTheme.typography.bodyMedium
        )
        Button(onClick = onCreatePublication, modifier = Modifier.fillMaxWidth()) {
            Text("Crear nueva publicación")
        }
        Divider()
        Text("Propuestas de empresas", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
        if (ofertasOrdenadas.isEmpty()) {
            Text("No hay propuestas registradas.", style = MaterialTheme.typography.bodyMedium)
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 320.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(ofertasOrdenadas, key = { it.id }) { oferta ->
                    ElevatedCard(modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(oferta.tipoCatering.ifBlank { "Propuesta" }, fontWeight = FontWeight.SemiBold)
                            Text("Autor: ${oferta.nombreAutor}", style = MaterialTheme.typography.bodyMedium)
                            Text("Estado: ${oferta.estado.label}", style = MaterialTheme.typography.bodySmall)
                            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                Button(onClick = { onEditarOferta(oferta.id) }, modifier = Modifier.weight(1f)) { Text("Editar") }
                                OutlinedButton(onClick = { onEliminarOferta(oferta.id) }, modifier = Modifier.weight(1f)) { Text("Eliminar") }
                            }
                        }
                    }
                }
            }
        }
        Divider()
        Text("Solicitudes de clientes", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
        if (solicitudesOrdenadas.isEmpty()) {
            Text("No hay solicitudes registradas.", style = MaterialTheme.typography.bodyMedium)
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 320.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(solicitudesOrdenadas, key = { it.id }) { solicitud ->
                    ElevatedCard(modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(solicitud.tipoCatering.ifBlank { "Solicitud" }, fontWeight = FontWeight.SemiBold)
                            Text("Cliente: ${solicitud.nombreAutor}", style = MaterialTheme.typography.bodyMedium)
                            Text("Estado: ${solicitud.estado.label}", style = MaterialTheme.typography.bodySmall)
                            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                Button(onClick = { onEditarSolicitud(solicitud.id) }, modifier = Modifier.weight(1f)) { Text("Editar") }
                                OutlinedButton(onClick = { onEliminarSolicitud(solicitud.id) }, modifier = Modifier.weight(1f)) { Text("Eliminar") }
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun rolLabel(rol: Role): String = when (rol) {
    Role.ADMIN -> "Administrador"
    Role.COMPANY -> "Empresa"
    Role.CLIENT -> "Cliente"
}
