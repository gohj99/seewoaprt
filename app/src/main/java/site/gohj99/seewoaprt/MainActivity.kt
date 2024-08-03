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

class MainActivity : ComponentActivity() {
    private var doneStr = mutableStateOf("")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            seewoaprtTheme {
                SplashMainScreen(
                    onDoneClick = { text ->
                        lifecycleScope.launch {
                            doneStr.value = getString(R.string.loading)
                            val recoveredPassword = seewoAssistantPasswordRecovery(text)
                            if (recoveredPassword != null) {
                                doneStr.value = recoveredPassword
                            } else {
                                doneStr.value = getString(R.string.failed)
                            }
                        }
                    },
                    doneStr = doneStr
                )
            }
        }
        doneStr.value = getString(R.string.count)
    }

    private suspend fun seewoAssistantPasswordRecovery(LockPasswordV2: String): String? = withContext(Dispatchers.Default) {
        val hugo = toMD5("hugo")
        for (i in 0 until 1000000) {
            val pwd = "%06d".format(i)
            val enc = toMD5(toMD5(pwd) + hugo).substring(8, 24)
            if (enc == LockPasswordV2) {
                return@withContext pwd
            }
        }
        return@withContext null
    }

    private fun toMD5(input: String): String {
        val bytes = MessageDigest.getInstance("MD5").digest(input.toByteArray())
        return bytes.joinToString("") { "%02x".format(it) }
    }
}

@Composable
fun CustomButton(
    onClick: () -> Unit,
    text: String,
    modifier: Modifier = Modifier,
    shape: androidx.compose.foundation.shape.RoundedCornerShape = androidx.compose.foundation.shape.RoundedCornerShape(10.dp)
) {
    var isPressed by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(if (isPressed) 0.9f else 1f)

    Box(
        modifier = modifier
            .scale(scale)
            .size(width = 148.dp, height = 33.dp)
            .clip(shape)
            .background(Color(0xFF2397D3))
            .pointerInput(Unit) {
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
    doneStr: MutableState<String>
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
                    shape = androidx.compose.foundation.shape.RoundedCornerShape(4.dp)
                )
                .clip(androidx.compose.foundation.shape.RoundedCornerShape(4.dp))
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
            text = doneStr.value
        )
    }
}

@Preview(showBackground = true)
@Composable
fun GreetingPreview() {
    seewoaprtTheme {
        SplashMainScreen(
            onDoneClick = { /*TODO*/ },
            doneStr = mutableStateOf("")
        )
    }
}
