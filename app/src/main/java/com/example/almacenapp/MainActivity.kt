package com.example.almacenapp

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme(colorScheme = darkColorScheme()) {
                AlmacenScreen()
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AlmacenScreen() {
    val scope = rememberCoroutineScope()

    var producto by remember { mutableStateOf("") }
    var aulaSeleccionada by remember { mutableStateOf("Salón 101") }
    var nombreEstudiante by remember { mutableStateOf("") }
    var estadoTexto by remember { mutableStateOf("Listo para solicitar") }
    var enviadoConExito by remember { mutableStateOf(false) }
    var cargando by remember { mutableStateOf(false) }
    var isDropdownExpanded by remember { mutableStateOf(false) }

    val clienteNombre = "Prof. Jeremy Valencia"
    val firebaseURL = "https://cafeteria-colegia-ccb-default-rtdb.firebaseio.com/pedidos.json"

    val opcionesAulas = listOf(
        "Salón 101", "Salón 102", "Salón 103",
        "Salón 201", "Salón 202", "Salón 203",
        "Laboratorio de Ciencias", "Sala de Profesores", "Biblioteca"
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Spacer(modifier = Modifier.height(10.dp))

        Text(
            text = "📦 Solicitud de Útiles",
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFFFF6B00)
        )

        Text(
            text = "Hola, $clienteNombre",
            fontSize = 20.sp,
            fontWeight = FontWeight.SemiBold,
            color = Color.White
        )

        Column(modifier = Modifier.fillMaxWidth()) {
            Text("📍 Ubicación / Aula:", color = Color.Gray, fontSize = 12.sp)
            Spacer(modifier = Modifier.height(4.dp))
            ExposedDropdownMenuBox(
                expanded = isDropdownExpanded,
                onExpandedChange = { isDropdownExpanded = !isDropdownExpanded }
            ) {
                OutlinedTextField(
                    value = aulaSeleccionada,
                    onValueChange = {},
                    readOnly = true,
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = isDropdownExpanded) },
                    modifier = Modifier
                        .menuAnchor()
                        .fillMaxWidth()
                )
                ExposedDropdownMenu(
                    expanded = isDropdownExpanded,
                    onDismissRequest = { isDropdownExpanded = false }
                ) {
                    opcionesAulas.forEach { aula ->
                        DropdownMenuItem(
                            text = { Text(aula) },
                            onClick = {
                                aulaSeleccionada = aula
                                isDropdownExpanded = false
                            }
                        )
                    }
                }
            }
        }

        Column(modifier = Modifier.fillMaxWidth()) {
            Text("🚶‍♂️ Estudiante que recogerá (Opcional):", color = Color.Gray, fontSize = 12.sp)
            Spacer(modifier = Modifier.height(4.dp))
            OutlinedTextField(
                value = nombreEstudiante,
                onValueChange = { nombreEstudiante = it },
                placeholder = { Text("Ej. Camilo Pérez") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
        }

        Column(modifier = Modifier.fillMaxWidth()) {
            Text("✏️ Materiales requeridos:", color = Color.Gray, fontSize = 12.sp)
            Spacer(modifier = Modifier.height(4.dp))
            OutlinedTextField(
                value = producto,
                onValueChange = { producto = it },
                placeholder = { Text("Ej: 2 marcadores negros y 1 resma...") },
                modifier = Modifier.fillMaxWidth()
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        Button(
            onClick = {
                scope.launch {
                    cargando = true
                    enviadoConExito = false
                    estadoTexto = "Enviando a Almacén..."

                    val estudianteFinal = if (nombreEstudiante.trim().isEmpty()) "Docente retira" else nombreEstudiante.trim()
                    val pedidoTexto = producto.trim()

                    val exito = enviarSolicitudFirebase(
                        urlStr = firebaseURL,
                        cliente = clienteNombre,
                        aula = aulaSeleccionada,
                        estudiante = estudianteFinal,
                        pedido = pedidoTexto
                    )

                    cargando = false
                    if (exito) {
                        estadoTexto = "Notificado al Almacén"
                        enviadoConExito = true
                        producto = ""
                        nombreEstudiante = ""
                    } else {
                        estadoTexto = "Error al enviar"
                        enviadoConExito = false
                    }
                }
            },
            enabled = producto.trim().isNotEmpty() && !cargando,
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF6B00)),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier
                .fillMaxWidth()
                .height(54.dp)
        ) {
            if (cargando) {
                CircularProgressIndicator(
                    color = Color.White,
                    modifier = Modifier
                        .size(20.dp)
                        .padding(end = 8.dp),
                    strokeWidth = 2.dp
                )
            }
            Text("Enviar Solicitud a Almacén", fontWeight = FontWeight.Bold, fontSize = 16.sp)
        }

        Text("Estado: $estadoTexto", color = Color.Gray, fontSize = 14.sp)

        if (enviadoConExito) {
            Text("✅ Solicitud recibida en Almacén", color = Color.Green, fontWeight = FontWeight.Bold)
        }
    }
}

suspend fun enviarSolicitudFirebase(
    urlStr: String,
    cliente: String,
    aula: String,
    estudiante: String,
    pedido: String
): Boolean = withContext(Dispatchers.IO) {
    try {
        val url = URL(urlStr)
        val conn = url.openConnection() as HttpURLConnection
        conn.requestMethod = "POST"
        conn.setRequestProperty("Content-Type", "application/json; charset=UTF-8")
        conn.doOutput = true

        val jsonParam = JSONObject().apply {
            put("cliente", cliente)
            put("aula", aula)
            put("estudiante", estudiante)
            put("pedido", pedido)
        }

        val os = OutputStreamWriter(conn.outputStream, "UTF-8")
        os.write(jsonParam.toString())
        os.flush()
        os.close()

        val responseCode = conn.responseCode
        conn.disconnect()
        return@withContext responseCode in 200..299
    } catch (e: Exception) {
        e.printStackTrace()
        return@withContext false
    }
}
