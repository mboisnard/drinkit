package com.drinkit.documentation.processor.ksp

import com.google.devtools.ksp.processing.KSPLogger
import com.google.devtools.ksp.symbol.KSNode

internal class NoOpKSPLogger : KSPLogger {
    override fun logging(message: String, symbol: KSNode?) {}
    override fun info(message: String, symbol: KSNode?) {}
    override fun warn(message: String, symbol: KSNode?) {}
    override fun error(message: String, symbol: KSNode?) {}
    override fun exception(e: Throwable) {}
}
