package com.example.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Public
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.SpotifyBlack
import com.example.ui.theme.SpotifyGreen

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.filled.QrCode2
import androidx.compose.material.icons.filled.PlayArrow

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoginScreen(
    onLogin: (String, String, String) -> Unit,
    onGuestAccess: () -> Unit,
    errorMessage: String?,
    modifier: Modifier = Modifier
) {
    var serverUrl by remember { mutableStateOf("") }
    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }

    val gradient = Brush.verticalGradient(
        colors = listOf(
            Color(0xFF2E2E2E), // slightly lighter top
            SpotifyBlack
        )
    )

    val qrGradient = remember {
        Brush.horizontalGradient(
            colors = listOf(Color(0xFF00E5FF), Color(0xFF00E676))
        )
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(gradient),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // "Logo" representation
            Box(
                modifier = Modifier
                    .size(70.dp)
                    .clip(MaterialTheme.shapes.extraLarge)
                    .background(SpotifyGreen),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Public,
                    contentDescription = null,
                    modifier = Modifier.size(42.dp),
                    tint = SpotifyBlack
                )
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Text(
                "JellyMusic Fiesta",
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )

            Spacer(modifier = Modifier.height(20.dp))

            // Guest Banner for QR Scanners
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFF161C2C)),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        "🎉 ¿Escaneaste el QR de una Fiesta?",
                        color = Color.White,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        "Pide cualquier canción sin necesidad de iniciar sesión",
                        color = Color(0xFFB0BEC5),
                        fontSize = 11.sp
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Button(
                        onClick = onGuestAccess,
                        colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
                        contentPadding = PaddingValues(0.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(42.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(qrGradient)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.QrCode2, contentDescription = null, tint = Color.Black)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Pedir Canción como Invitado", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            OutlinedTextField(
                value = serverUrl,
                onValueChange = { serverUrl = it },
                label = { Text("Server URL (Jellyfin)") },
                leadingIcon = { Icon(Icons.Default.Public, null) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri),
                modifier = Modifier.fillMaxWidth().testTag("server_input"),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = SpotifyGreen,
                    focusedLabelColor = SpotifyGreen
                )
            )

            Spacer(modifier = Modifier.height(12.dp))

            OutlinedTextField(
                value = username,
                onValueChange = { username = it },
                label = { Text("Username") },
                leadingIcon = { Icon(Icons.Default.Person, null) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth().testTag("username_input"),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = SpotifyGreen,
                    focusedLabelColor = SpotifyGreen
                )
            )

            Spacer(modifier = Modifier.height(12.dp))

            OutlinedTextField(
                value = password,
                onValueChange = { password = it },
                label = { Text("Password") },
                leadingIcon = { Icon(Icons.Default.Lock, null) },
                visualTransformation = PasswordVisualTransformation(),
                singleLine = true,
                modifier = Modifier.fillMaxWidth().testTag("password_input"),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = SpotifyGreen,
                    focusedLabelColor = SpotifyGreen
                )
            )

            if (errorMessage != null) {
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = errorMessage,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodyMedium
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            Button(
                onClick = { onLogin(serverUrl, username, password) },
                colors = ButtonDefaults.buttonColors(containerColor = SpotifyGreen, contentColor = Color.White),
                shape = MaterialTheme.shapes.large,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
                    .testTag("login_button")
            ) {
                Text("Iniciar Sesión", fontSize = 15.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}
