package dev.meghlath.switchboard.ksp

import com.google.devtools.ksp.processing.SymbolProcessor
import com.google.devtools.ksp.processing.SymbolProcessorEnvironment
import com.google.devtools.ksp.processing.SymbolProcessorProvider

/**
 * Provider for [SwitchboardProcessor].
 */
public class SwitchboardProcessorProvider : SymbolProcessorProvider {
    override fun create(environment: SymbolProcessorEnvironment): SymbolProcessor {
        return SwitchboardProcessor(
            codeGenerator = environment.codeGenerator,
            logger = environment.logger
        )
    }
}
