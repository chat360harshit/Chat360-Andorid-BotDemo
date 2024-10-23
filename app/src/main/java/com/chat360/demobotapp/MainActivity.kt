package com.chat360.demobotapp
import android.content.Context
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CornerSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import androidx.navigation.compose.*
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import com.chat360.*
import com.chat360.chatbot.common.Chat360
import com.chat360.chatbot.common.CoreConfigs

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MyApp()
        }
    }
}

@Preview(name = "Bot Form")
@Composable
fun MyApp() {
    val navController = rememberNavController()
    NavHost(navController, startDestination = "formScreen") {
        composable("formScreen") { FormScreen(navController) }
        composable("botScreen/{botId}/{appId}/{meta}") { backStackEntry ->
            val botId = backStackEntry.arguments?.getString("botId") ?: ""
            val appId = backStackEntry.arguments?.getString("appId") ?: ""
            val meta = backStackEntry.arguments?.getString("meta") ?: ""
            BotScreen(botId, appId, meta)
        }
    }
}

@Composable
fun FormScreen(navController: NavController) {
    val context = LocalContext.current
    var botId by remember { mutableStateOf("") }
    var appId by remember { mutableStateOf("") }
    var metaKey by remember { mutableStateOf("") }
    var metaValue by remember { mutableStateOf("") }
    var url by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var metaEntries by remember { mutableStateOf(mutableMapOf<String, String>()) }
    Box {
        ElevatedCard(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            colors = CardColors(
                containerColor = Color(0xfff2f2f2),
                contentColor = Color.Black,
                disabledContentColor = Color.White,
                disabledContainerColor = Color.White
            )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp)
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                Text("Chat360 Bot Demo", style = MaterialTheme.typography.headlineLarge)
                Spacer(modifier = Modifier.height(32.dp))

                Column {
                    Text("Bot ID", style = MaterialTheme.typography.titleMedium)
                    OutlinedTextField(
                        value = botId,
                        onValueChange = { botId = it },
                        placeholder = { Text("Enter Bot ID") })
                }

                Spacer(modifier = Modifier.height(16.dp))
                Column {
                    Text("App ID", style = MaterialTheme.typography.titleMedium)
                    OutlinedTextField(
                        value = appId,
                        onValueChange = { appId = it },
                        placeholder = { Text("Enter App ID") })
                }

                Spacer(modifier = Modifier.height(16.dp))

                Column {
                    Text("Metadata", style = MaterialTheme.typography.titleMedium)
                    OutlinedTextField(
                        value = metaKey,
                        onValueChange = { metaKey = it },
                        placeholder = { Text("Key") })
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(

                        value = metaValue,
                        onValueChange = { metaValue = it },
                        placeholder = { Text("Value") })
                    Spacer(modifier = Modifier.height(8.dp))
                    ElevatedButton(
                        colors = ButtonColors(
                            containerColor = Color.Black,
                            contentColor = Color.White,
                            disabledContentColor = Color.White,
                            disabledContainerColor = Color.White
                        ),
                        shape = RoundedCornerShape(
                            corner = CornerSize(12.dp)
                        ),
                        onClick = {
                            if (metaKey.isNotEmpty() && metaValue.isNotEmpty()) {
                                metaEntries[metaKey] = metaValue;
                                metaKey = ""
                                metaValue = ""
                            }
                        }) {
                        Text("Add")
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
                Column {
                    metaEntries
                        .entries.forEachIndexed { index, entry ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp, horizontal = 32.dp),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("${entry.key}: ${entry.value}")
//                                IconButton(onClick = {
//                                    metaEntries.remove(entry.key)
//
//                                }) {
//                                    Icon(imageVector = Icons.Default.Delete, contentDescription = "Delete")
//                                }
                            }
                            
                        }
                }


                Spacer(modifier = Modifier.height(32.dp))

                Column {
                    Text("Extract Data from URL", style = MaterialTheme.typography.titleMedium)
                    OutlinedTextField(
                        value = url,
                        onValueChange = { url = it },
                        placeholder = { Text("Enter URL") })
                    Spacer(modifier = Modifier.height(16.dp))
                    ElevatedButton(
                        colors = ButtonColors(
                            containerColor = Color.Black,
                            contentColor = Color.White,
                            disabledContentColor = Color.White,
                            disabledContainerColor = Color.White
                        ),
                        shape = RoundedCornerShape(
                            corner = CornerSize(12.dp)
                        ),
                        onClick = { extractData(url) { botID: String, appID: String, meta: Map<String,String> ->
                            botId = botID;
                            appId = appID;
                            metaEntries = meta.toMutableMap();
                        } }) {
                        Text("Extract")
                    }
                }

                errorMessage?.let {
                    Text(it, color = Color.Red)
                }

                Spacer(modifier = Modifier.height(20.dp))

                ElevatedButton(
                    colors = ButtonColors(
                        containerColor = Color.Black,
                        contentColor = Color.White,
                        disabledContentColor = Color.White,
                        disabledContainerColor = Color.White
                    ),
                    shape = RoundedCornerShape(
                        corner = CornerSize(12.dp)
                    ),

                    onClick = {
                        launchChatBot(context, botId, metaEntries);
                    }) {
                    Text("Launch Bot")
                }
            }
        }
    }
}

fun launchChatBot(context: Context,  botId: String, metaEntries: Map<String, String>) {
    val chat360 = Chat360().getInstance()
    chat360.coreConfig = CoreConfigs(botId, applicationContext =  context, false, metaEntries)
    chat360.startBot(context);
}

@Composable
fun BotScreen(botId: String, appId: String, meta: String) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text("Bot Screen", style = MaterialTheme.typography.headlineLarge)
        Text("Bot ID: $botId")
        Text("App ID: $appId")
        Text("Meta Data: $meta")
        // Implement your bot logic here
    }
}

fun extractData(url: String, onExtract: (botID: String, appID: String, metaEntries: Map<String,String>) -> Unit) {
    val uri = Uri.parse(url)
    var botId: String = "";
    var appId: String = "";
    var jsonElement: Map<String,String> = mapOf();
    val metaEntries: MutableList<Pair<String,String>> =mutableListOf<Pair<String, String>>();
    if (uri.isHierarchical) {
        val botIdQuery = uri.getQueryParameter("h")
        val appIdQuery = uri.getQueryParameter("appId")
        botIdQuery?.let { botId = it }
        appIdQuery?.let { appId = it }
        val metaQuery = uri.getQueryParameter("meta")
        metaQuery?.let { m ->
            jsonElement = Json.decodeFromString<Map<String,String>>(m)
            onExtract(botId,appId, jsonElement);
        }
        onExtract(botId,appId, jsonElement);
    }
}




