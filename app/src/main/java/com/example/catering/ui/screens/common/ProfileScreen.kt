package com.example.catering.ui.screens.common

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Button
import androidx.compose.material3.Divider
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.catering.data.model.Role
import com.example.catering.data.model.User
import com.example.catering.ui.screens.home.PerfilPublico
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun ProfileScreen(
    user: User,
    profile: PerfilPublico?,
    onLogout: () -> Unit,
    onProfileUpdated: (String) -> Unit
) {
    Surface(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            verticalArrangement = Arrangement.Top
        ) {
            Text(
                text = "Perfil",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(24.dp))

            var description by rememberSaveable(user.uid) { mutableStateOf(user.profileDescription) }
            var selectedTab by rememberSaveable(profile?.id ?: user.uid) { mutableStateOf(ProfileTab.WORKS) }

    val displayName = profile?.nombre?.takeIf { it.isNotBlank() }
        ?: user.displayName.ifBlank { user.email }
    val affiliation = profile?.afiliacion?.takeIf { it.isNotBlank() }
        ?: user.affiliation?.takeIf { it.isNotBlank() }
        ?: "Sin afiliación"
    val address = profile?.direccion?.takeIf { it.isNotBlank() } ?: user.address
    val joinedAt = profile?.fechaRegistro?.takeIf { it > 0L } ?: user.joinedAt
            val joinDateText = joinedAt.takeIf { it > 0L }?.let {
                val formatter = remember { SimpleDateFormat("d 'de' MMMM yyyy", Locale.getDefault()) }
                formatter.format(Date(it))
            } ?: "Sin información"
    val ratingText = profile?.let { pub ->
        if (pub.cantidadResenas > 0) {
            "${"%.1f".format(pub.calificacionPromedio)} (${pub.cantidadResenas} reseñas)"
        } else {
            "Sin reseñas"
        }
    } ?: "Sin reseñas"
    val publicDescription = when {
        !profile?.descripcion.isNullOrBlank() -> profile?.descripcion.orEmpty()
        description.isNotBlank() -> description
        else -> ""
    }

            LaunchedEffect(user.profileDescription) { description = user.profileDescription }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(24.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(104.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.surfaceVariant),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Filled.Person,
                        contentDescription = "Perfil",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(48.dp)
                    )
                }
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = displayName,
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.SemiBold
                    )
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Star,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Text(ratingText, style = MaterialTheme.typography.bodyMedium)
                    }
                    Text(
                        text = "Afiliación: $affiliation",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Text(
                        text = "Dirección: ${address.ifBlank { "Sin definir" }}",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Text(
                        text = "Miembro desde: $joinDateText",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }

            if (publicDescription.isNotBlank()) {
                Spacer(modifier = Modifier.height(12.dp))
                Text(publicDescription, style = MaterialTheme.typography.bodyMedium)
            }

            Spacer(modifier = Modifier.height(16.dp))
            Divider()

            if (profile != null) {
                Spacer(modifier = Modifier.height(16.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    ProfileToggleButton(
                        text = "Trabajos previos",
                        selected = selectedTab == ProfileTab.WORKS,
                        onClick = { selectedTab = ProfileTab.WORKS },
                        modifier = Modifier.weight(1f)
                    )
                    ProfileToggleButton(
                        text = "Reseñas",
                        selected = selectedTab == ProfileTab.REVIEWS,
                        onClick = { selectedTab = ProfileTab.REVIEWS },
                        modifier = Modifier.weight(1f)
                    )
                }
                Spacer(modifier = Modifier.height(16.dp))
                when (selectedTab) {
                    ProfileTab.WORKS -> {
                        if (profile.trabajosPrevios.isEmpty()) {
                            Text("No hay trabajos previos registrados", style = MaterialTheme.typography.bodyMedium)
                        } else {
                            profile.trabajosPrevios.forEach { work ->
                                Column(modifier = Modifier.padding(vertical = 12.dp)) {
                                    Text(
                                        work.titulo,
                                        fontWeight = FontWeight.SemiBold,
                                        style = MaterialTheme.typography.titleMedium
                                    )
                                    if (work.descripcion.isNotBlank()) {
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(work.descripcion, style = MaterialTheme.typography.bodyMedium)
                                    }
                                }
                                Divider()
                            }
                        }
                    }

                    ProfileTab.REVIEWS -> {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Star,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = ratingText,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Medium
                            )
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                        if (profile.resenasRecientes.isEmpty()) {
                            Text("Aún no hay reseñas publicadas", style = MaterialTheme.typography.bodyMedium)
                        } else {
                            profile.resenasRecientes.forEach { review ->
                                Column(modifier = Modifier.padding(vertical = 8.dp)) {
                                    Text(
                                        "${review.nombreAutor} - ${review.calificacion}/5",
                                        fontWeight = FontWeight.SemiBold
                                    )
                                    if (review.comentario.isNotBlank()) {
                                        Text(review.comentario, style = MaterialTheme.typography.bodyMedium)
                                    }
                                }
                                Divider()
                            }
                        }
                    }
                }
            } else {
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "Tu perfil público se mostrará aquí cuando completes tu información.",
                    style = MaterialTheme.typography.bodyMedium
                )
            }

            Spacer(modifier = Modifier.height(24.dp))
            Divider()
            Spacer(modifier = Modifier.height(16.dp))

            Text("Datos de cuenta", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            Spacer(modifier = Modifier.height(12.dp))
            ProfileItem(label = "Nombre", value = user.displayName.ifBlank { "Sin especificar" })
            ProfileItem(label = "Correo", value = user.email)
            ProfileItem(label = "Rol", value = roleLabel(user.role))
            ProfileItem(label = "Afiliación", value = affiliation)
            ProfileItem(label = "Dirección", value = user.address.ifBlank { "Sin definir" })

            OutlinedTextField(
                value = description,
                onValueChange = { description = it },
                label = { Text("Descripción personal") },
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(16.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Button(
                    onClick = {
                        onProfileUpdated(description.trim())
                    },
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Guardar perfil")
                }
                OutlinedButton(onClick = onLogout, modifier = Modifier.weight(1f)) {
                    Text("Cerrar sesión")
                }
            }
        }
    }
}

@Composable
private fun ProfileToggleButton(
    text: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    if (selected) {
        FilledTonalButton(onClick = onClick, modifier = modifier) {
            Text(text)
        }
    } else {
        OutlinedButton(onClick = onClick, modifier = modifier) {
            Text(text)
        }
    }
}

@Composable
private fun ProfileItem(label: String, value: String) {
    Column(modifier = Modifier.padding(bottom = 16.dp)) {
        Text(text = label, style = MaterialTheme.typography.labelLarge)
        Text(text = value, style = MaterialTheme.typography.bodyLarge)
    }
}

private enum class ProfileTab { WORKS, REVIEWS }

private fun roleLabel(role: Role): String = when (role) {
    Role.COMPANY -> "Empresa"
    Role.CLIENT -> "Cliente"
    Role.ADMIN -> "Administrador"
}
