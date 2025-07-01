package com.gustavo.ciclomap

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.core.content.ContextCompat
import androidx.navigation.NavController
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.*
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.CollectionReference
import com.google.firebase.firestore.GeoPoint
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.gustavo.ciclomap.model.Ponto
import com.gustavo.ciclomap.ui.theme.CicloMapTheme
import com.gustavo.ciclomap.utils.vectorToBitmap
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.*

val tiposDePontoMap = mapOf(
    "buraco_via" to "Buraco na via",
    "ponto_perigoso" to "Ponto Perigoso",
    "sem_iluminacao" to "Trecho sem Iluminação",
    "oficina" to "Oficina/Bicicletaria"
)

sealed class Screen(val route: String, val title: String, val icon: ImageVector? = null) {
    object Splash : Screen("splash", "Splash")
    object Map : Screen("map", "Mapa", Icons.Default.Map)
    object Profile : Screen("profile", "Perfil", Icons.Default.Person)
    object Main : Screen("main", "Main") // Rota para o grupo de ecrãs principais
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            CicloMapTheme {
                RootNavigation()
            }
        }
    }
}

@Composable
fun RootNavigation() {
    val navController = rememberNavController()
    NavHost(navController = navController, startDestination = Screen.Splash.route) {
        composable(Screen.Splash.route) {
            SplashScreen(navController = navController)
        }
        composable(Screen.Main.route) {
            MainScreen()
        }
    }
}

@Composable
fun SplashScreen(navController: NavController) {
    LaunchedEffect(key1 = true) {
        delay(2500L) // Aguarda 2.5 segundos
        navController.navigate(Screen.Main.route) {
            popUpTo(Screen.Splash.route) {
                inclusive = true
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White),
        contentAlignment = Alignment.Center
    ) {

        Image(
            painter = painterResource(id = R.drawable.ciclomap_logo),
            contentDescription = "Logo do CicloMap",
            modifier = Modifier.size(150.dp)
        )
    }
}


@Composable
fun MainScreen() {
    val navController = rememberNavController()
    val contexto = LocalContext.current
    val TAG = "MainScreen"

    var pontos by remember { mutableStateOf<List<Ponto>>(emptyList()) }
    var pontosCollectionRef by remember { mutableStateOf<CollectionReference?>(null) }
    val currentUserId by remember { derivedStateOf { Firebase.auth.currentUser?.uid } }

    LaunchedEffect(Unit) {
        val auth = Firebase.auth
        if (auth.currentUser == null) {
            auth.signInAnonymously()
        }

        val db = Firebase.firestore
        val appId = "ciclomap-android-app"
        val collection = db.collection("artifacts").document(appId).collection("public").document("data").collection("pontos")
        pontosCollectionRef = collection

        collection.addSnapshotListener { snapshot, error ->
            if (error != null) {
                Log.w(TAG, "Erro ao ouvir por mudanças no Firestore.", error)
                return@addSnapshotListener
            }

            if (snapshot != null) {
                pontos = snapshot.documents.mapNotNull { doc ->
                    doc.toObject(Ponto::class.java)?.apply { id = doc.id }
                }
            }
        }
    }


    Scaffold(
        bottomBar = { BottomNavigationBar(navController) }
    ) { innerPadding ->
        NavigationGraph(
            navController = navController,
            modifier = Modifier.padding(innerPadding),
            pontos = pontos,
            currentUserId = currentUserId,
            pontosCollectionRef = pontosCollectionRef
        )
    }
}

@Composable
fun BottomNavigationBar(navController: NavController) {
    val items = listOf(
        Screen.Map,
        Screen.Profile
    )
    NavigationBar {
        val navBackStackEntry by navController.currentBackStackEntryAsState()
        val currentRoute = navBackStackEntry?.destination?.route

        items.forEach { screen ->
            screen.icon?.let {
                NavigationBarItem(
                    icon = { Icon(it, contentDescription = screen.title) },
                    label = { Text(screen.title) },
                    selected = currentRoute == screen.route,
                    onClick = {
                        navController.navigate(screen.route) {
                            popUpTo(navController.graph.startDestinationId) {
                                saveState = true
                            }
                            launchSingleTop = true
                            restoreState = true
                        }
                    }
                )
            }
        }
    }
}

@Composable
fun NavigationGraph(
    navController: NavHostController,
    modifier: Modifier = Modifier,
    pontos: List<Ponto>,
    currentUserId: String?,
    pontosCollectionRef: CollectionReference?
) {
    NavHost(navController, startDestination = Screen.Map.route, modifier = modifier) {
        composable(Screen.Map.route) {
            MapScreen(
                pontos = pontos,
                pontosCollectionRef = pontosCollectionRef,
                currentUserId = currentUserId
            )
        }
        composable(Screen.Profile.route) {
            val userPontos = pontos.filter { it.userId == currentUserId }
            ProfileScreen(
                userPontos = userPontos,
                currentUserId = currentUserId
            )
        }
    }
}

@Composable
fun ProfileScreen(userPontos: List<Ponto>, currentUserId: String?) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(text = "Perfil do Utilizador", style = MaterialTheme.typography.headlineMedium)
        Spacer(modifier = Modifier.height(16.dp))

        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Seu ID de Utilizador:", style = MaterialTheme.typography.titleMedium)
                Text(currentUserId ?: "A carregar...", style = MaterialTheme.typography.bodySmall)
                Spacer(modifier = Modifier.height(8.dp))
                Text("Mais funcionalidades (login, rotas) em implementação futura.", style = MaterialTheme.typography.bodyMedium)
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        Text(text = "Meus Pontos Adicionados", style = MaterialTheme.typography.titleLarge)
        Spacer(modifier = Modifier.height(8.dp))

        if (userPontos.isEmpty()) {
            Text(text = "Você ainda não adicionou nenhum ponto.", modifier = Modifier.padding(16.dp))
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(userPontos) { ponto ->
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                text = tiposDePontoMap[ponto.type] ?: "Ponto Desconhecido",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            if (ponto.notes.isNotBlank()) {
                                Text(
                                    text = ponto.notes,
                                    style = MaterialTheme.typography.bodyMedium
                                )
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Adicionado em: ${SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(ponto.createdAt.toDate())}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                            )
                        }
                    }
                }
            }
        }
    }
}


@Composable
fun MapScreen(
    pontos: List<Ponto>,
    pontosCollectionRef: CollectionReference?,
    currentUserId: String?
) {
    val contexto = LocalContext.current

    val localizacaoInicial = LatLng(-26.9935, -48.6346)
    val cameraState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(localizacaoInicial, 14f)
    }

    var temPermissao by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(contexto, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
        )
    }

    var mostrarDialogoTipoPonto by remember { mutableStateOf(false) }
    var tipoPontoParaAdicionar by remember { mutableStateOf<String?>(null) }
    var mostrarFormularioObs by remember { mutableStateOf(false) }
    var modoDePosicionamento by remember { mutableStateOf(false) }
    val novoPontoState = rememberMarkerState()

    var pontoSelecionado by remember { mutableStateOf<Ponto?>(null) }
    var pontoParaEditar by remember { mutableStateOf<Ponto?>(null) }
    var mostrarDialogoConfirmacaoExclusao by remember { mutableStateOf<Ponto?>(null) }


    val solicitadorPermissao = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { concedido -> temPermissao = concedido }
    )

    LaunchedEffect(Unit) {
        if (!temPermissao) {
            solicitadorPermissao.launch(Manifest.permission.ACCESS_FINE_LOCATION)
        }
    }

    fun adicionarPonto(tipo: String, observacoes: String, latLng: LatLng) {
        pontosCollectionRef?.let { collection ->
            val userId = currentUserId ?: "anonymous_${System.currentTimeMillis()}"
            val novoPonto = Ponto(
                type = tipo,
                notes = observacoes,
                location = GeoPoint(latLng.latitude, latLng.longitude),
                userId = userId
            )
            collection.add(novoPonto)
                .addOnSuccessListener {
                    Toast.makeText(contexto, "Ponto adicionado!", Toast.LENGTH_SHORT).show()
                }
                .addOnFailureListener {
                    Toast.makeText(contexto, "Erro ao guardar o ponto.", Toast.LENGTH_SHORT).show()
                }
        }
    }

    fun excluirPonto(pontoId: String) {
        pontosCollectionRef?.document(pontoId)?.delete()
            ?.addOnSuccessListener {
                Toast.makeText(contexto, "Ponto excluído!", Toast.LENGTH_SHORT).show()
                mostrarDialogoConfirmacaoExclusao = null
            }
            ?.addOnFailureListener {
                Toast.makeText(contexto, "Erro ao excluir o ponto.", Toast.LENGTH_SHORT).show()
            }
    }

    fun editarPonto(pontoId: String, novasObservacoes: String) {
        pontosCollectionRef?.document(pontoId)?.update("notes", novasObservacoes)
            ?.addOnSuccessListener {
                Toast.makeText(contexto, "Ponto atualizado!", Toast.LENGTH_SHORT).show()
                pontoParaEditar = null
            }
            ?.addOnFailureListener {
                Toast.makeText(contexto, "Erro ao atualizar o ponto.", Toast.LENGTH_SHORT).show()
            }
    }


    Scaffold(
        floatingActionButton = {
            AnimatedVisibility(!modoDePosicionamento) {
                FloatingActionButton(onClick = { mostrarDialogoTipoPonto = true }) {
                    Icon(Icons.Filled.Add, contentDescription = "Adicionar ponto")
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
        ) {
            GoogleMap(
                modifier = Modifier.fillMaxSize(),
                cameraPositionState = cameraState,
                properties = MapProperties(isMyLocationEnabled = temPermissao),
                uiSettings = MapUiSettings(zoomControlsEnabled = true, myLocationButtonEnabled = temPermissao),
                onMapClick = {
                    pontoSelecionado = null
                },
                contentPadding = PaddingValues(bottom = 72.dp)
            ) {
                pontos.forEach { ponto ->
                    ponto.location?.let { geoPoint ->
                        val iconResId = when (ponto.type) {
                            "buraco_via" -> R.drawable.ic_marker_buraco
                            "ponto_perigoso" -> R.drawable.ic_marker_perigo
                            "sem_iluminacao" -> R.drawable.ic_marker_luz
                            "oficina" -> R.drawable.ic_marker_oficina
                            else -> R.drawable.ic_marker_default
                        }
                        Marker(
                            state = MarkerState(position = LatLng(geoPoint.latitude, geoPoint.longitude)),
                            icon = vectorToBitmap(contexto, iconResId),
                            onClick = {
                                if (!modoDePosicionamento) {
                                    pontoSelecionado = ponto
                                }
                                true
                            }
                        )
                    }
                }

                Marker(
                    state = novoPontoState,
                    visible = modoDePosicionamento,
                    draggable = true,
                    icon = BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_CYAN),
                    title = "Posicione o Ponto de Alerta"
                )
            }

            if (modoDePosicionamento) {
                Column(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.95f))
                        .padding(16.dp)
                        .fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Arraste o pino para o local exato e confirme",
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Row(
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Button(
                            onClick = { mostrarFormularioObs = true },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF28a745))
                        ) {
                            Text("Confirmar Local")
                        }
                        Button(
                            onClick = {
                                modoDePosicionamento = false
                                tipoPontoParaAdicionar = null
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
                        ) {
                            Text("Cancelar")
                        }
                    }
                }
            }
        }

        if (mostrarDialogoTipoPonto) {
            DialogoSelecaoTipoPonto(
                onDismissRequest = { mostrarDialogoTipoPonto = false },
                onTypeSelected = { tipoId ->
                    mostrarDialogoTipoPonto = false
                    tipoPontoParaAdicionar = tipoId
                    modoDePosicionamento = true
                    novoPontoState.position = cameraState.position.target
                }
            )
        }

        if (mostrarFormularioObs) {
            tipoPontoParaAdicionar?.let { tipo ->
                FormularioObservacaoDialog(
                    tipoPonto = tipo,
                    onDismissRequest = { mostrarFormularioObs = false },
                    onSave = { observacoes ->
                        adicionarPonto(
                            tipo = tipo,
                            observacoes = observacoes,
                            latLng = novoPontoState.position
                        )
                        mostrarFormularioObs = false
                        modoDePosicionamento = false
                        tipoPontoParaAdicionar = null
                    }
                )
            }
        }

        pontoSelecionado?.let { ponto ->
            DialogoDetalhesPonto(
                ponto = ponto,
                currentUserId = currentUserId,
                onDismissRequest = { pontoSelecionado = null },
                onExcluirClick = {
                    pontoSelecionado = null
                    mostrarDialogoConfirmacaoExclusao = ponto
                },
                onEditarClick = {
                    pontoSelecionado = null
                    pontoParaEditar = ponto
                }
            )
        }

        mostrarDialogoConfirmacaoExclusao?.let { pontoParaExcluir ->
            AlertDialog(
                onDismissRequest = { mostrarDialogoConfirmacaoExclusao = null },
                title = { Text("Excluir Ponto") },
                text = { Text("Tem a certeza de que deseja excluir este ponto? Esta ação não pode ser desfeita.") },
                confirmButton = {
                    Button(
                        onClick = { excluirPonto(pontoParaExcluir.id) },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                    ) { Text("Excluir") }
                },
                dismissButton = {
                    TextButton(onClick = { mostrarDialogoConfirmacaoExclusao = null }) {
                        Text("Cancelar")
                    }
                }
            )
        }

        pontoParaEditar?.let { ponto ->
            FormularioObservacaoDialog(
                tipoPonto = ponto.type,
                observacoesIniciais = ponto.notes,
                onDismissRequest = { pontoParaEditar = null },
                onSave = { novasObservacoes ->
                    editarPonto(ponto.id, novasObservacoes)
                }
            )
        }
    }
}

@Composable
fun DialogoSelecaoTipoPonto(
    onDismissRequest: () -> Unit,
    onTypeSelected: (tipoId: String) -> Unit
) {
    Dialog(onDismissRequest = onDismissRequest) {
        Card(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            shape = RoundedCornerShape(16.dp),
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "Adicionar Novo Ponto",
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.padding(bottom = 16.dp)
                )
                Text(
                    text = "Qual tipo de ponto você deseja adicionar?",
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(bottom = 16.dp)
                )
                tiposDePontoMap.forEach { (id, nome) ->
                    Text(
                        text = nome,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onTypeSelected(id) }
                            .padding(vertical = 12.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun FormularioObservacaoDialog(
    tipoPonto: String,
    observacoesIniciais: String = "",
    onDismissRequest: () -> Unit,
    onSave: (observacoes: String) -> Unit
) {
    var observacoes by remember { mutableStateOf(observacoesIniciais) }
    val nomeDoTipo = tiposDePontoMap[tipoPonto] ?: "Ponto"

    Dialog(onDismissRequest = onDismissRequest) {
        Card(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            shape = RoundedCornerShape(16.dp),
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "Editar: $nomeDoTipo",
                    style = MaterialTheme.typography.titleLarge
                )

                OutlinedTextField(
                    value = observacoes,
                    onValueChange = { observacoes = it },
                    label = { Text("Observações (opcional)") },
                    placeholder = { Text("Ex: Buraco grande no meio da rua") },
                    modifier = Modifier.fillMaxWidth()
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onClick = onDismissRequest) {
                        Text("Cancelar")
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(onClick = { onSave(observacoes) }) {
                        Text("Guardar")
                    }
                }
            }
        }
    }
}

@Composable
fun DialogoDetalhesPonto(
    ponto: Ponto,
    currentUserId: String?,
    onDismissRequest: () -> Unit,
    onExcluirClick: () -> Unit,
    onEditarClick: () -> Unit
) {
    val isOwner = ponto.userId.isNotBlank() && ponto.userId == currentUserId

    Dialog(onDismissRequest = onDismissRequest) {
        Card(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            shape = RoundedCornerShape(16.dp),
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = tiposDePontoMap[ponto.type] ?: "Detalhes do Ponto",
                    style = MaterialTheme.typography.titleLarge
                )
                Spacer(modifier = Modifier.height(8.dp))
                if (ponto.notes.isNotBlank()) {
                    Text(
                        text = ponto.notes,
                        style = MaterialTheme.typography.bodyMedium
                    )
                } else {
                    Text(
                        text = "Nenhuma observação adicionada.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                }
                Spacer(modifier = Modifier.height(16.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (isOwner) {
                        IconButton(onClick = onEditarClick) {
                            Icon(Icons.Default.Edit, contentDescription = "Editar Ponto")
                        }
                        IconButton(onClick = onExcluirClick) {
                            Icon(Icons.Default.Delete, contentDescription = "Excluir Ponto", tint = MaterialTheme.colorScheme.error)
                        }
                        Spacer(modifier = Modifier.weight(1f))
                    }

                    TextButton(onClick = onDismissRequest) {
                        Text("Fechar")
                    }
                }
            }
        }
    }
}
