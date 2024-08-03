package site.gohj99.seewoaprt

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import site.gohj99.seewoaprt.ui.theme.seewoaprtTheme
import java.security.MessageDigest
import java.util.concurrent.atomic.AtomicBoolean

class MainActivity : ComponentActivity() {
    private var doneStr = mutableStateOf("")
    private val md5Digest: MessageDigest = MessageDigest.getInstance("MD5")
    private var isComputing = mutableStateOf(false)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            seewoaprtTheme {
                SplashMainScreen(
                    onDoneClick = { text ->
                        lifecycleScope.launch {
                            isComputing.value = true
                            doneStr.value = getString(R.string.loading)
                            val recoveredPassword = seewoAssistantPasswordRecovery(
                                lockPasswordV2 = text
                            )
                            if (recoveredPassword != null) {
                                doneStr.value = getString(R.string.result) + recoveredPassword
                            } else {
                                doneStr.value = getString(R.string.failed)
                            }
                            isComputing.value = false
                        }
                    },
                    doneStr = doneStr,
                    isComputing = isComputing
                )
            }
        }
        doneStr.value = getString(R.string.count)
    }

    private suspend fun seewoAssistantPasswordRecovery(lockPasswordV2: String): String? = withContext(Dispatchers.Default) {
        if (lockPasswordV2 == "" || lockPasswordV2.length != 16) {
            //println("Invalid lockPasswordV2: $lockPasswordV2")
            return@withContext null
        }
        val hugo = toMD5("hugo")
        //println("MD5 of 'hugo': $hugo")

        val totalParts = 2000
        val rangeSize = (1000000 + totalParts - 1) / totalParts  // 向上取整
        val found = AtomicBoolean(false)
        val deferredResults = (0 until totalParts).map { part ->
            async {
                val start = part * rangeSize
                val end = minOf(start + rangeSize, 1000000)  // 确保 end 不超过 1000000
                for (i in start until end) {
                    if (found.get()) {
                        return@async null
                    }
                    val pwd = "%06d".format(i)
                    val pwdHash = toMD5(pwd)
                    val combinedHash = toMD5(pwdHash + hugo)
                    val enc = combinedHash.substring(8, 24)

                    /* 调试
                    if (i > 999988) { // 每隔100000次迭代打印一次调试信息
                        println("Trying password: $pwd -> pwdHash: $pwdHash -> combinedHash: $combinedHash -> enc: $enc")
                    }
                    */

                    if (enc == lockPasswordV2) {
                        //println("Found matching password: $pwd")
                        found.set(true)
                        return@async pwd
                    }
                }
                return@async null
            }
        }
        deferredResults.awaitAll().firstOrNull { it != null }
    }

    private fun toMD5(input: String): String {
        val digest = MessageDigest.getInstance("MD5")
        val bytes = digest.digest(input.toByteArray())
        val hexString = StringBuilder(bytes.size * 2)
        for (byte in bytes) {
            val hex = (byte.toInt() and 0xFF).toString(16)
            if (hex.length == 1) {
                hexString.append('0')
            }
            hexString.append(hex)
        }
        return hexString.toString()
    }
}

@Composable
fun CustomButton(
    onClick: () -> Unit,
    text: String,
    modifier: Modifier = Modifier,
    shape: RoundedCornerShape = RoundedCornerShape(10.dp),
    enabled: Boolean = true
) {
    var isPressed by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(if (isPressed && enabled) 0.9f else 1f, label = "")

    Box(
        modifier = modifier
            .scale(scale)
            .size(width = 148.dp, height = 33.dp)
            .clip(shape)
            .background(if (enabled) Color(0xFF2397D3) else Color.Gray)
            .pointerInput(Unit) {
                if (enabled) {
                    detectTapGestures(
                        onPress = {
                            isPressed = true
                            try {
                                awaitRelease()
                            } finally {
                                isPressed = false
                            }
                        },
                        onTap = {
                            onClick()
                        }
                    )
                }
            },
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            color = Color.White
        )
    }
}

@Composable
fun SplashMainScreen(
    onDoneClick: (String) -> Unit,
    doneStr: MutableState<String>,
    isComputing: MutableState<Boolean>
) {
    val passwordHint: String = stringResource(id = R.string.PasswardV2)
    var text by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = stringResource(id = R.string.app_name),
            color = Color.White,
            fontSize = 20.sp,
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 5.dp),
            textAlign = TextAlign.Center
        )

        Box(
            modifier = Modifier
                .height(40.dp)
                .padding(start = 30.dp, end = 30.dp)
                .background(
                    color = Color.Gray,
                    shape = RoundedCornerShape(4.dp)
                )
                .clip(RoundedCornerShape(4.dp))
        ) {
            BasicTextField(
                value = text,
                onValueChange = { text = it },
                singleLine = true,
                textStyle = TextStyle(
                    color = Color.LightGray,
                    textAlign = TextAlign.Center,
                    fontSize = 14.sp
                ),
                cursorBrush = SolidColor(Color.White),
                modifier = Modifier
                    .fillMaxHeight()
                    .padding(horizontal = 8.dp)
            ) { innerTextField ->
                Box(
                    modifier = Modifier
                        .fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    if (text.isEmpty()) {
                        Text(
                            text = passwordHint,
                            color = Color.LightGray,
                            textAlign = TextAlign.Center,
                            fontSize = 14.sp
                        )
                    }
                    innerTextField()
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        CustomButton(
            onClick = {
                onDoneClick(text)
            },
            text = doneStr.value,
            enabled = !isComputing.value
        )
    }
}

@Preview(showBackground = true)
@Composable
fun GreetingPreview() {
    seewoaprtTheme {
        SplashMainScreen(
            onDoneClick = { /*TODO*/ },
            doneStr = mutableStateOf(""),
            isComputing = mutableStateOf(false)
        )
    }
}
