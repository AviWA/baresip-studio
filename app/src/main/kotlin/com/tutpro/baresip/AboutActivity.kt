package com.tutpro.baresip

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.OnBackPressedCallback
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLinkStyles
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.fromHtml
import androidx.compose.ui.unit.dp

class AboutActivity : ComponentActivity() {

    private val onBackPressedCallback = object : OnBackPressedCallback(true) {
        override fun handleOnBackPressed() {
            goBack()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {

        super.onCreate(savedInstanceState)

        enableEdgeToEdge()

        Utils.addActivity("about")

        val aboutTitle = String.format(getString(R.string.about_title))
        val aboutText = String.format(getString(R.string.about_text),
            BuildConfig.VERSION_NAME)

        setContent {
            AppTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    AboutContent(aboutTitle, aboutText) { goBack() }
                }
            }
        }

        onBackPressedDispatcher.addCallback(this, onBackPressedCallback)

    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    fun AboutContent(title: String, text: String, navigateBack: () -> Unit) {
        Scaffold(
            modifier = Modifier.safeDrawingPadding(),
            containerColor = LocalCustomColors.current.background,
            topBar = {
                TopAppBar(
                    title = {
                        Text(text = title,
                            color = LocalCustomColors.current.light,
                            fontWeight = FontWeight.Bold
                        )
                    },
                    colors = TopAppBarDefaults.mediumTopAppBarColors(
                        containerColor = LocalCustomColors.current.primary
                    ),
                    navigationIcon = {
                        IconButton(onClick = navigateBack) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Localized description",
                                tint = LocalCustomColors.current.light
                            )
                        }
                    },
                )

            }
        ) { contentPadding ->
            Text(
                text = AnnotatedString.Companion.fromHtml(
                    htmlString = text,
                    linkStyles = TextLinkStyles(
                        style = SpanStyle(color = LocalCustomColors.current.accent)
                    )
                ),
                modifier = Modifier
                    .padding(horizontal = 16.dp, vertical = 16.dp)
                    .padding(contentPadding)
                    .verticalScroll(rememberScrollState())
                    .fillMaxSize()
                    .background(LocalCustomColors.current.background)
            )
        }
    }

    private fun goBack() {
        BaresipService.activities.remove("about")
        setResult(RESULT_CANCELED, Intent())
        finish()
    }

}
