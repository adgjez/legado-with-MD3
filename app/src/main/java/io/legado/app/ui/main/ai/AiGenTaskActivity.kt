package io.legado.app.ui.main.ai

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.compose.setContent
import io.legado.app.base.BaseComposeActivity

class AiGenTaskActivity : BaseComposeActivity() {
    companion object {
        fun start(context: Context) {
            context.startActivity(Intent(context, AiGenTaskActivity::class.java))
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            AiGenTaskScreen(onBack = { finish() })
        }
    }
}