package com.example.leitornoturno

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class ReaderViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(ReaderUiState())
    val uiState: StateFlow<ReaderUiState> = _uiState

    private var pocketDetectionJob: Job? = null

    companion object {
        private const val TEMPO_BOLSO_MS = 3000L
        private const val LUX_LIMITE_ESCURO = 10f
        private const val LUX_LIMITE_SEPIA = 200f
    }

    fun onLightSensorChanged(lux: Float) {
        _uiState.update { estadoAtual ->
            if (estadoAtual.leituraPausada) {
                estadoAtual.copy(luxAtual = lux)
            } else {
                val (tema, brilho) = calcularTemaEBrilho(lux)
                val contraste = calcularContraste(lux)
                estadoAtual.copy(
                    luxAtual = lux,
                    tema = tema,
                    brilho = brilho,
                    contraste = contraste
                )
            }
        }
    }

    private fun calcularTemaEBrilho(lux: Float): Pair<ReaderTheme, Float> {
        return when {
            lux < LUX_LIMITE_ESCURO -> ReaderTheme.ESCURO to 0.15f
            lux < LUX_LIMITE_SEPIA -> ReaderTheme.SEPIA to 0.5f
            else -> ReaderTheme.CLARO to 1.0f
        }
    }

    /**
     * Ambientes muito escuros (pouca luz para os olhos se guiarem) e muito
     * claros (reflexo/ofuscamento na tela) recebem mais contraste no texto:
     * negrito + espaçamento maior entre letras. Ambientes intermediários
     * usam contraste normal.
     */
    private fun calcularContraste(lux: Float): ContrasteConfig {
        return when {
            lux < LUX_LIMITE_ESCURO -> ContrasteConfig(negrito = true, espacamentoLetras = 0.05f)
            lux < LUX_LIMITE_SEPIA -> ContrasteConfig(negrito = false, espacamentoLetras = 0f)
            else -> ContrasteConfig(negrito = true, espacamentoLetras = 0.02f)
        }
    }

    fun onProximitySensorChanged(coberto: Boolean) {
        _uiState.update { it.copy(proximidadeCoberta = coberto) }

        if (coberto) {
            iniciarContagemBolso()
        } else {
            cancelarContagemBolso()
            retomarLeitura()
        }
    }

    private fun iniciarContagemBolso() {
        if (pocketDetectionJob?.isActive == true) return
        pocketDetectionJob = viewModelScope.launch {
            delay(TEMPO_BOLSO_MS)
            pausarLeitura()
        }
    }

    private fun cancelarContagemBolso() {
        pocketDetectionJob?.cancel()
        pocketDetectionJob = null
    }

    private fun pausarLeitura() {
        _uiState.update { it.copy(leituraPausada = true, brilho = 0.05f) }
    }

    private fun retomarLeitura() {
        _uiState.update { estadoAtual ->
            if (!estadoAtual.leituraPausada) return@update estadoAtual
            val (tema, brilho) = calcularTemaEBrilho(estadoAtual.luxAtual)
            val contraste = calcularContraste(estadoAtual.luxAtual)
            estadoAtual.copy(
                leituraPausada = false,
                tema = tema,
                brilho = brilho,
                contraste = contraste
            )
        }
    }

    override fun onCleared() {
        super.onCleared()
        cancelarContagemBolso()
    }
}