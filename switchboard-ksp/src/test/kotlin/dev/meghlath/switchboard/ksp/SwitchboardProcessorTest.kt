package dev.meghlath.switchboard.ksp

import com.tschuchort.compiletesting.KotlinCompilation
import com.tschuchort.compiletesting.SourceFile
import com.tschuchort.compiletesting.symbolProcessorProviders
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.io.File

class SwitchboardProcessorTest {

    private fun compile(vararg source: SourceFile): Pair<KotlinCompilation, KotlinCompilation.Result> {
        val compilation = KotlinCompilation().apply {
            sources = source.toList()
            symbolProcessorProviders = listOf(SwitchboardProcessorProvider())
            inheritClassPath = true
            messageOutputStream = System.out
        }
        return compilation to compilation.compile()
    }

    private fun getKspGeneratedFile(compilation: KotlinCompilation, fileName: String): String {
        val kspSourcesDir = compilation.workingDir.resolve("ksp/sources/kotlin")
        val file = kspSourcesDir.walkTopDown().find { it.name == fileName }
            ?: throw IllegalStateException("Generated file $fileName not found. Files found: ${kspSourcesDir.walkTopDown().map { it.name }.toList()}")
        return file.readText()
    }

    @Test
    fun testBooleanFlagGeneration() {
        val (compilation, result) = compile(SourceFile.kotlin("Flags.kt", """
            package test
            import dev.meghlath.switchboard.annotations.*
            @Flags
            interface MyFlags {
                @BooleanFlag(default = true) val myFlag: Boolean
            }
        """))
        assertEquals(KotlinCompilation.ExitCode.OK, result.exitCode)
        val generated = getKspGeneratedFile(compilation, "MyFlagsAccessors.kt")
        assertTrue(generated.contains("resolveBoolean(\"myFlag\", true)"))
    }

    @Test
    fun testIntFlagGeneration() {
        val (compilation, result) = compile(SourceFile.kotlin("Flags.kt", """
            package test
            import dev.meghlath.switchboard.annotations.*
            @Flags
            interface MyFlags {
                @IntFlag(default = 42) val myFlag: Int
            }
        """))
        assertEquals(KotlinCompilation.ExitCode.OK, result.exitCode)
        val generated = getKspGeneratedFile(compilation, "MyFlagsAccessors.kt")
        assertTrue(generated.contains("resolveInt(\"myFlag\", 42)"))
    }

    @Test
    fun testLongFlagGeneration() {
        val (compilation, result) = compile(SourceFile.kotlin("Flags.kt", """
            package test
            import dev.meghlath.switchboard.annotations.*
            @Flags
            interface MyFlags {
                @LongFlag(default = 1000L) val myFlag: Long
            }
        """))
        assertEquals(KotlinCompilation.ExitCode.OK, result.exitCode)
        val generated = getKspGeneratedFile(compilation, "MyFlagsAccessors.kt")
        assertTrue(generated.contains("resolveLong(\"myFlag\", 1000L)"))
    }

    @Test
    fun testFloatFlagGeneration() {
        val (compilation, result) = compile(SourceFile.kotlin("Flags.kt", """
            package test
            import dev.meghlath.switchboard.annotations.*
            @Flags
            interface MyFlags {
                @FloatFlag(default = 1.5f) val myFlag: Float
            }
        """))
        assertEquals(KotlinCompilation.ExitCode.OK, result.exitCode)
        val generated = getKspGeneratedFile(compilation, "MyFlagsAccessors.kt")
        assertTrue(generated.contains("resolveFloat(\"myFlag\", 1.5f)"))
    }

    @Test
    fun testDoubleFlagGeneration() {
        val (compilation, result) = compile(SourceFile.kotlin("Flags.kt", """
            package test
            import dev.meghlath.switchboard.annotations.*
            @Flags
            interface MyFlags {
                @DoubleFlag(default = 3.14) val myFlag: Double
            }
        """))
        assertEquals(KotlinCompilation.ExitCode.OK, result.exitCode)
        val generated = getKspGeneratedFile(compilation, "MyFlagsAccessors.kt")
        assertTrue(generated.contains("resolveDouble(\"myFlag\", 3.14)"))
    }

    @Test
    fun testStringFlagGeneration() {
        val (compilation, result) = compile(SourceFile.kotlin("Flags.kt", """
            package test
            import dev.meghlath.switchboard.annotations.*
            @Flags
            interface MyFlags {
                @StringFlag(default = "hello") val myFlag: String
            }
        """))
        assertEquals(KotlinCompilation.ExitCode.OK, result.exitCode)
        val generated = getKspGeneratedFile(compilation, "MyFlagsAccessors.kt")
        assertTrue(generated.contains("resolveString(\"myFlag\", \"hello\")"))
    }

    @Test
    fun testEnumFlagGeneration() {
        val (compilation, result) = compile(
            SourceFile.kotlin("Variant.kt", """
                package test
                enum class Variant { A, B }
            """),
            SourceFile.kotlin("Flags.kt", """
                package test
                import dev.meghlath.switchboard.annotations.*
                @Flags
                interface MyFlags {
                    @EnumFlag(default = "A", enumClass = Variant::class) val myFlag: Variant
                }
            """)
        )
        assertEquals(KotlinCompilation.ExitCode.OK, result.exitCode)
        val generated = getKspGeneratedFile(compilation, "MyFlagsAccessors.kt")
        assertTrue(generated.contains("resolveEnum(\"myFlag\", Variant.A)"))
    }

    @Test
    fun testEnumFlagInvalidDefault() {
        val (compilation, result) = compile(
            SourceFile.kotlin("Variant.kt", """
                package test
                enum class Variant { A, B }
            """),
            SourceFile.kotlin("Flags.kt", """
                package test
                import dev.meghlath.switchboard.annotations.*
                @Flags
                interface MyFlags {
                    @EnumFlag(default = "INVALID", enumClass = Variant::class) val myFlag: Variant
                }
            """)
        )
        assertEquals(KotlinCompilation.ExitCode.COMPILATION_ERROR, result.exitCode)
        assertTrue(result.messages.contains("EnumFlag default \"INVALID\" is not a valid entry in Variant"))
    }

    @Test
    fun testEnumFlagTypeMismatch() {
        val (compilation, result) = compile(
            SourceFile.kotlin("Variant.kt", """
                package test
                enum class Variant { A, B }
            """),
            SourceFile.kotlin("Flags.kt", """
                package test
                import dev.meghlath.switchboard.annotations.*
                @Flags
                interface MyFlags {
                    @EnumFlag(default = "A", enumClass = Variant::class) val myFlag: String
                }
            """)
        )
        assertEquals(KotlinCompilation.ExitCode.COMPILATION_ERROR, result.exitCode)
        assertTrue(result.messages.contains("does not match property type"))
    }

    @Test
    fun testBooleanFlagOnIntProperty() {
        val (compilation, result) = compile(SourceFile.kotlin("Flags.kt", """
            package test
            import dev.meghlath.switchboard.annotations.*
            @Flags
            interface MyFlags {
                @BooleanFlag(default = true) val myFlag: Int
            }
        """))
        assertEquals(KotlinCompilation.ExitCode.COMPILATION_ERROR, result.exitCode)
        assertTrue(result.messages.contains("BooleanFlag cannot be applied to kotlin.Int property 'myFlag'"))
    }

    @Test
    fun testIntFlagOnStringProperty() {
        val (compilation, result) = compile(SourceFile.kotlin("Flags.kt", """
            package test
            import dev.meghlath.switchboard.annotations.*
            @Flags
            interface MyFlags {
                @IntFlag(default = 42) val myFlag: String
            }
        """))
        assertEquals(KotlinCompilation.ExitCode.COMPILATION_ERROR, result.exitCode)
        assertTrue(result.messages.contains("IntFlag cannot be applied to kotlin.String property 'myFlag'"))
    }

    @Test
    fun testDuplicateKeysSameContainer() {
        val (compilation, result) = compile(SourceFile.kotlin("Flags.kt", """
            package test
            import dev.meghlath.switchboard.annotations.*
            @Flags
            interface MyFlags {
                @BooleanFlag(default = true) val myFlag: Boolean
                @IntFlag(default = 42) val myFlag: Int
            }
        """))
        assertEquals(KotlinCompilation.ExitCode.COMPILATION_ERROR, result.exitCode)
    }

    @Test
    fun testDuplicateKeysAcrossContainers() {
        val (compilation, result) = compile(
            SourceFile.kotlin("Flags1.kt", """
                package test
                import dev.meghlath.switchboard.annotations.*
                @Flags
                interface Flags1 {
                    @BooleanFlag(default = true) val myFlag: Boolean
                }
            """),
            SourceFile.kotlin("Flags2.kt", """
                package test
                import dev.meghlath.switchboard.annotations.*
                @Flags
                interface Flags2 {
                    @IntFlag(default = 42) val myFlag: Int
                }
            """)
        )
        assertEquals(KotlinCompilation.ExitCode.COMPILATION_ERROR, result.exitCode)
        assertTrue(result.messages.contains("Duplicate flag key 'myFlag'"))
    }

    @Test
    fun testAggregatorGeneration() {
        val (compilation, result) = compile(SourceFile.kotlin("Flags.kt", """
            package test
            import dev.meghlath.switchboard.annotations.*
            @Flags
            interface MyFlags {
                @BooleanFlag(default = true) val myFlag: Boolean
            }
        """))
        assertEquals(KotlinCompilation.ExitCode.OK, result.exitCode)
        val generatedFlags = getKspGeneratedFile(compilation, "SwitchboardFlags.kt")
        assertTrue(generatedFlags.contains("\"myFlag\""))

        val generatedRegistry = getKspGeneratedFile(compilation, "SwitchboardRegistryImpl.kt")
        assertTrue(generatedRegistry.contains("object SwitchboardRegistryImpl"))
        assertTrue(generatedRegistry.contains("\"myFlag\""))
    }
}
