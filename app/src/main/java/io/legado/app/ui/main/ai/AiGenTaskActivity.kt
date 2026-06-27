package io.legado.app.ui.main.ai

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.compose.ui.platform.ViewCompositionStrategy
import io.legado.app.base.BaseActivity
import io.legado.app.databinding.ActivityAiGenTaskBinding

class AiGenTaskActivity : BaseActivity<ActivityAiGenTaskBinding>() {

    companion object {
        fun start(context: Context) {
            context.startActivity(Intent(context, AiGenTaskActivity::class.java))
        }
    }

    override val binding get() = ActivityAiGenTaskBinding.inflate(layoutInflater)

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        binding.composeView.apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
            setContent {
                AiGenTaskScreen(onBack = { finish() })
            }
        }
    }
}