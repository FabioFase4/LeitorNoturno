package com.example.leitornoturno

import android.graphics.Typeface
import android.os.Bundle
import android.view.View
import android.view.WindowManager
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.example.leitornoturno.databinding.ActivityMainBinding
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private val viewModel: ReaderViewModel by viewModels()
    private lateinit var sensorProvider: SensorManagerProvider

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        sensorProvider = SensorManagerProvider(this)

        if (!sensorProvider.sensoresDisponiveis()) {
            binding.textStatus.text = getString(R.string.sensores_indisponiveis)
        }

        binding.textLeitura.text = getString(R.string.texto_exemplo)

        observarEstado()
        coletarSensores()
    }

    private fun observarEstado() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { estado ->
                    aplicarTema(estado.tema)
                    aplicarBrilho(estado.brilho)
                    aplicarContraste(estado.contraste)
                    atualizarInterface(estado)
                }
            }
        }
    }

    private fun coletarSensores() {
        lifecycleScope.launch {
            // Coleta dados do sensor de luz enquanto a tela estiver visível
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    sensorProvider.getLightSensorFlow().collect { lux ->
                        viewModel.onLightSensorChanged(lux)
                    }
                }
                launch {
                    sensorProvider.getProximitySensorFlow().collect { coberto ->
                        viewModel.onProximitySensorChanged(coberto)
                    }
                }
            }
        }
    }

    private fun aplicarTema(tema: ReaderTheme) {
        val (corFundoRes, corTextoRes) = when (tema) {
            ReaderTheme.CLARO -> R.color.fundo_claro to R.color.texto_claro
            ReaderTheme.SEPIA -> R.color.fundo_sepia to R.color.texto_sepia
            ReaderTheme.ESCURO -> R.color.fundo_escuro to R.color.texto_escuro
        }
        val corFundo = ContextCompat.getColor(this, corFundoRes)
        val corTexto = ContextCompat.getColor(this, corTextoRes)

        binding.root.setBackgroundColor(corFundo)
        binding.textLeitura.setTextColor(corTexto)
        binding.textStatus.setTextColor(corTexto)
    }

    private fun aplicarBrilho(brilho: Float) {
        val params: WindowManager.LayoutParams = window.attributes
        params.screenBrightness = brilho.coerceIn(0.01f, 1.0f)
        window.attributes = params
    }

    /*
    * Aplica o ajuste de contraste (negrito + espaçamento de letras) no texto lido.
    * */
    private fun aplicarContraste(contraste: ContrasteConfig) {
        binding.textLeitura.setTypeface(
            binding.textLeitura.typeface,
            if (contraste.negrito) Typeface.BOLD else Typeface.NORMAL
        )
        binding.textLeitura.letterSpacing = contraste.espacamentoLetras
    }

    private fun atualizarInterface(estado: ReaderUiState) {
        binding.textLeitura.visibility = if (estado.leituraPausada) View.INVISIBLE else View.VISIBLE
        binding.overlayPausa.visibility = if (estado.leituraPausada) View.VISIBLE else View.GONE

        val nomeTema = when (estado.tema) {
            ReaderTheme.CLARO -> getString(R.string.tema_claro)
            ReaderTheme.SEPIA -> getString(R.string.tema_sepia)
            ReaderTheme.ESCURO -> getString(R.string.tema_escuro)
        }

        binding.textStatus.text = getString(
            R.string.status_formato,
            estado.luxAtual,
            nomeTema,
            (estado.brilho * 100).toInt()
        )
    }
}
