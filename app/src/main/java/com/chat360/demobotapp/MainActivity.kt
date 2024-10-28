package com.chat360.demobotapp
import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.core.app.ActivityCompat
import androidx.core.app.ActivityCompat.shouldShowRequestPermissionRationale
import androidx.core.content.ContextCompat
import androidx.navigation.NavController
import androidx.navigation.compose.*
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import com.chat360.*
import com.chat360.chatbot.common.Chat360
import com.chat360.chatbot.common.CoreConfigs
import com.google.android.gms.tasks.OnCompleteListener
import com.google.firebase.messaging.FirebaseMessaging

class MainActivity : ComponentActivity() {
    companion object{
        var fcmToken = ""
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MyApp(askPermission = { askNotificationPermission() })
        }
    }

    // Declare the launcher at the top of your Activity/Fragment:
    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { isGranted: Boolean ->
        if (isGranted) {
            FirebaseMessaging.getInstance().token.addOnCompleteListener(OnCompleteListener { task ->
                if (!task.isSuccessful) {
                    Log.w("Chat360 Demo", "Fetching FCM registration token failed", task.exception)
                    return@OnCompleteListener
                }

                // Get new FCM registration token
                fcmToken = task.result

                // Log and toast
                val msg = getString(R.string.msg_token_fmt, fcmToken)
                Log.d("Chat360 Demo", msg)
                Toast.makeText(baseContext, msg, Toast.LENGTH_SHORT).show()
            })
        } else {
            // TODO: Inform user that that your app will not show notifications.
        }
    }

    private fun askNotificationPermission() {
        // This is only necessary for API level >= 33 (TIRAMISU)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.POST_NOTIFICATIONS ) ==
                PackageManager.PERMISSION_GRANTED
            ) {
                // FCM SDK (and your app) can post notifications.
            } else if (ActivityCompat.shouldShowRequestPermissionRationale(this,android.Manifest.permission.POST_NOTIFICATIONS )) {
            } else {
                // Directly ask for the permission
                requestPermissionLauncher.launch(android.Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }
}

//@Preview(name = "Bot Form")
@Composable
fun MyApp(askPermission: () -> Unit) {
    val navController = rememberNavController()
    NavHost(navController, startDestination = "formScreen") {
        composable("formScreen") { FormScreen(navController, askPermission) }
    }
}

@Composable
fun FormScreen(navController: NavController, askPermission: () -> Unit) {
    val context = LocalContext.current
    var botId by remember { mutableStateOf("") }
    var appId by remember { mutableStateOf("") }
    var metaKey by remember { mutableStateOf("") }
    var metaValue by remember { mutableStateOf("") }
    var url by remember { mutableStateOf("") }
    val isDebug = remember { mutableStateOf(false) }
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
                    Checkbox(
                        checked = isDebug.value,
                        onCheckedChange = { isDebug.value = it }
                    )
                    Text(text = if (isDebug.value) "Debug mode is ON" else "Debug mode is OFF")
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
                        FirebaseMessaging.getInstance().token.addOnCompleteListener(OnCompleteListener { task ->
                            if (!task.isSuccessful) {
                                Log.w("Chat360 Demo", "Fetching FCM registration token failed", task.exception)
                                return@OnCompleteListener
                            }

                            MainActivity.fcmToken = task.result
                            launchChatBot(context, botId,MainActivity.fcmToken, metaEntries);
                            val msg =  MainActivity.fcmToken
                            Log.d("Chat360 Demo", msg)
                        })
                    }) {
                    Text("Launch Bot")
                }

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
                        askPermission();
                    }) {
                    Text("Get Notification Permission")
                }
            }
        }
    }
}

fun launchChatBot(context: Context,  botId: String, fcmToken: String, metaEntries: Map<String, String>) {
    val chat360 = Chat360().getInstance()
    chat360.coreConfig = CoreConfigs(botId, applicationContext =  context, false, metaEntries,  )
    chat360.coreConfig!!.deviceToken = fcmToken;
    chat360.startBot(context);
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




