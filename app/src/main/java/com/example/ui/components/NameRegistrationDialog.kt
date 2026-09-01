package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Public
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties

data class CountryOption(val code: String, val name: String, val flag: String)

val popularCountries = listOf(
    CountryOption("TR", "Türkiye", "🇹🇷"),
    CountryOption("AZ", "Azerbaycan", "🇦🇿"),
    CountryOption("DE", "Almanya", "🇩🇪"),
    CountryOption("US", "ABD", "🇺🇸"),
    CountryOption("GB", "Birleşik Krallık", "🇬🇧"),
    CountryOption("FR", "Fransa", "🇫🇷"),
    CountryOption("NL", "Hollanda", "🇳🇱"),
    CountryOption("JP", "Japonya", "🇯🇵"),
    CountryOption("BR", "Brezilya", "🇧🇷"),
    CountryOption("KR", "Güney Kore", "🇰🇷")
)

@Composable
fun NameRegistrationDialog(
    initialCountry: String = "TR",
    isChecking: Boolean = false,
    errorMessage: String? = null,
    onSubmit: (name: String, country: String) -> Unit
) {
    var usernameInput by remember { mutableStateOf("") }
    var selectedCountry by remember { mutableStateOf(initialCountry) }

    Dialog(
        onDismissRequest = { /* Non-dismissable on first launch until a name is picked */ },
        properties = DialogProperties(dismissOnBackPress = false, dismissOnClickOutside = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp)
                .clip(RoundedCornerShape(24.dp))
                .border(1.dp, Color(0xFF38BDF8).copy(alpha = 0.5f), RoundedCornerShape(24.dp)),
            color = Color(0xFF0F172A),
            tonalElevation = 8.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp)
                    .testTag("name_registration_dialog"),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Header Icon
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF1E293B))
                        .border(1.dp, Color(0xFF38BDF8), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.EmojiEvents,
                        contentDescription = null,
                        tint = Color(0xFF38BDF8),
                        modifier = Modifier.size(28.dp)
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "SANAL TAŞ'A HOŞ GELDİN",
                    style = MaterialTheme.typography.titleMedium.copy(
                        color = Color(0xFFF8FAFC),
                        fontWeight = FontWeight.Black,
                        letterSpacing = 1.sp
                    )
                )

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = "Gerçek tıklamaların küresel liderlik tablosunda yer alması için benzersiz bir oyuncu adı seçin.",
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = Color(0xFF94A3B8),
                        fontSize = 12.5.sp,
                        lineHeight = 18.sp
                    ),
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )

                Spacer(modifier = Modifier.height(20.dp))

                // Username Input Field
                OutlinedTextField(
                    value = usernameInput,
                    onValueChange = { input ->
                        if (input.length <= 20 && !input.contains("\n")) {
                            usernameInput = input
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("username_input_field"),
                    label = { Text("Oyuncu Rumuzu (Benzersiz)", color = Color(0xFF94A3B8)) },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Person,
                            contentDescription = null,
                            tint = Color(0xFF38BDF8)
                        )
                    },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(
                        capitalization = KeyboardCapitalization.None,
                        imeAction = ImeAction.Done
                    ),
                    keyboardActions = KeyboardActions(
                        onDone = {
                            if (usernameInput.trim().length >= 3 && !isChecking) {
                                onSubmit(usernameInput.trim(), selectedCountry)
                            }
                        }
                    ),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color(0xFFF8FAFC),
                        unfocusedTextColor = Color(0xFFF8FAFC),
                        focusedBorderColor = Color(0xFF38BDF8),
                        unfocusedBorderColor = Color(0xFF334155),
                        focusedContainerColor = Color(0xFF1E293B),
                        unfocusedContainerColor = Color(0xFF1E293B)
                    ),
                    shape = RoundedCornerShape(12.dp),
                    supportingText = {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "En az 3 karakter",
                                style = MaterialTheme.typography.labelSmall.copy(color = Color(0xFF64748B))
                            )
                            Text(
                                text = "${usernameInput.length}/20",
                                style = MaterialTheme.typography.labelSmall.copy(color = Color(0xFF64748B))
                            )
                        }
                    }
                )

                Spacer(modifier = Modifier.height(14.dp))

                // Country Selector Section
                Column(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(bottom = 6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Public,
                            contentDescription = null,
                            tint = Color(0xFF94A3B8),
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Temsil Ettiğin Ülke:",
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = Color(0xFF94A3B8),
                                fontWeight = FontWeight.Bold
                            )
                        )
                    }

                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        items(popularCountries) { country ->
                            val isSelected = selectedCountry == country.code
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(if (isSelected) Color(0xFF38BDF8) else Color(0xFF1E293B))
                                    .border(
                                        1.dp,
                                        if (isSelected) Color(0xFF38BDF8) else Color(0xFF334155),
                                        RoundedCornerShape(10.dp)
                                    )
                                    .clickable { selectedCountry = country.code }
                                    .padding(horizontal = 10.dp, vertical = 6.dp)
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(text = country.flag, fontSize = 14.sp)
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = country.code,
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            color = if (isSelected) Color(0xFF0F172A) else Color(0xFFF8FAFC),
                                            fontWeight = FontWeight.Bold
                                        )
                                    )
                                }
                            }
                        }
                    }
                }

                // Error Message Notice
                AnimatedVisibility(
                    visible = !errorMessage.isNullOrEmpty(),
                    enter = fadeIn(),
                    exit = fadeOut()
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 14.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color(0xFFF43F5E).copy(alpha = 0.15f))
                            .border(1.dp, Color(0xFFF43F5E), RoundedCornerShape(8.dp))
                            .padding(horizontal = 10.dp, vertical = 8.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Error,
                                contentDescription = null,
                                tint = Color(0xFFF43F5E),
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = errorMessage ?: "",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    color = Color(0xFFF43F5E),
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 11.5.sp
                                )
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Submit Button
                Button(
                    onClick = {
                        val trimmed = usernameInput.trim()
                        if (trimmed.length >= 3 && !isChecking) {
                            onSubmit(trimmed, selectedCountry)
                        }
                    },
                    enabled = usernameInput.trim().length >= 3 && !isChecking,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("submit_username_button"),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF38BDF8),
                        contentColor = Color(0xFF0F172A),
                        disabledContainerColor = Color(0xFF1E293B),
                        disabledContentColor = Color(0xFF64748B)
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    if (isChecking) {
                        CircularProgressIndicator(
                            color = Color(0xFF0F172A),
                            modifier = Modifier.size(20.dp),
                            strokeWidth = 2.dp
                        )
                    } else {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Taş Dünyasına Katıl",
                                style = MaterialTheme.typography.titleSmall.copy(
                                    fontWeight = FontWeight.Black,
                                    letterSpacing = 0.5.sp
                                )
                            )
                        }
                    }
                }
            }
        }
    }
}
