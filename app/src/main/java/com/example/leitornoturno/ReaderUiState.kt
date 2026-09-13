package com.example.leitornoturno

import com.example.leitornoturno.ContrasteConfig
import com.example.leitornoturno.ReaderTheme

/**
 * Configuração de contraste do texto, aplicada de forma independente do tema,
 * de acordo com a faixa de lux (ambientes muito escuros ou muito claros pedem
 * mais contraste para manter a legibilidade).
 */
data class ReaderUiState(
    val luxAtual: Float = 0f,
    val tema: ReaderTheme = ReaderTheme.CLARO,
    val brilho: Float = 1.0f,
    val contraste: ContrasteConfig = ContrasteConfig(),
    val leituraPausada: Boolean = false,
    val proximidadeCoberta: Boolean = false
)