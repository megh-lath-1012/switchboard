package services.pixelpulse.switchboard.ksp

import com.google.devtools.ksp.processing.CodeGenerator
import com.google.devtools.ksp.processing.Dependencies
import com.google.devtools.ksp.processing.KSPLogger
import com.google.devtools.ksp.processing.Resolver
import com.google.devtools.ksp.processing.SymbolProcessor
import com.google.devtools.ksp.symbol.ClassKind
import com.google.devtools.ksp.symbol.KSAnnotated
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSFile
import com.google.devtools.ksp.symbol.KSPropertyDeclaration
import com.google.devtools.ksp.symbol.KSType
import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.FileSpec
import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.KModifier
import com.squareup.kotlinpoet.PropertySpec
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import com.squareup.kotlinpoet.TypeSpec
import com.squareup.kotlinpoet.asClassName
import com.squareup.kotlinpoet.asTypeName
import com.squareup.kotlinpoet.ksp.toClassName
import com.squareup.kotlinpoet.ksp.writeTo

/**
 * Processor for Switchboard annotations.
 */
public class SwitchboardProcessor(
    private val codeGenerator: CodeGenerator,
    private val logger: KSPLogger
) : SymbolProcessor {

    private val flagAnnotationNames = setOf(
        "BooleanFlag", "IntFlag", "LongFlag", "FloatFlag", "DoubleFlag", "StringFlag", "EnumFlag"
    )

    private val allDiscoveredKeys = mutableMapOf<String, KSPropertyDeclaration>()
    private val allDiscoveredFlags = mutableListOf<FlagMetadata>()
    private val allSourceFiles = mutableSetOf<KSFile>()

    override fun process(resolver: Resolver): List<KSAnnotated> {
        val containers = resolver.getSymbolsWithAnnotation("services.pixelpulse.switchboard.annotations.Flags")
            .filterIsInstance<KSClassDeclaration>()

        containers.forEach { container ->
            processContainer(container)
        }

        return emptyList()
    }

    override fun finish() {
        if (allDiscoveredKeys.isNotEmpty()) {
            generateAggregator()
        }
    }

    private fun processContainer(container: KSClassDeclaration) {
        val containerFile = container.containingFile ?: return
        allSourceFiles.add(containerFile)

        val flags = container.getAllProperties()
            .mapNotNull { prop -> resolveFlag(prop) }
            .toList()

        if (flags.isNotEmpty()) {
            generateAccessors(container, flags)
        }
    }

    private fun resolveFlag(property: KSPropertyDeclaration): FlagMetadata? {
        val annotation = property.annotations.find { it.shortName.asString() in flagAnnotationNames } ?: return null
        val annotationName = annotation.shortName.asString()
        
        val key = property.simpleName.asString()
        val existing = allDiscoveredKeys[key]
        if (existing != null) {
            logger.error("Duplicate flag key '$key' found in ${property.location}. " +
                    "Already defined in ${existing.location}.", property)
            return null
        }
        allDiscoveredKeys[key] = property

        val defaultArg = annotation.arguments.find { it.name?.asString() == "default" }
        val defaultValue = defaultArg?.value

        val description = annotation.arguments.find { it.name?.asString() == "description" }?.value as? String ?: ""
        val category = annotation.arguments.find { it.name?.asString() == "category" }?.value as? String ?: ""

        val metadata = FlagMetadata(
            key = key,
            defaultValue = defaultValue,
            annotationName = annotationName,
            description = description,
            category = category,
            property = property
        )

        if (!validateFlag(metadata, annotation)) {
            return null
        }

        allDiscoveredFlags.add(metadata)
        return metadata
    }

    private fun validateFlag(metadata: FlagMetadata, annotation: com.google.devtools.ksp.symbol.KSAnnotation): Boolean {
        val property = metadata.property
        val propType = property.type.resolve()
        val propTypeQualified = propType.declaration.qualifiedName?.asString()

        when (metadata.annotationName) {
            "BooleanFlag" -> if (propTypeQualified != "kotlin.Boolean") {
                logger.error("BooleanFlag cannot be applied to $propTypeQualified property '${metadata.key}'.", property)
                return false
            }
            "IntFlag" -> if (propTypeQualified != "kotlin.Int") {
                logger.error("IntFlag cannot be applied to $propTypeQualified property '${metadata.key}'.", property)
                return false
            }
            "LongFlag" -> if (propTypeQualified != "kotlin.Long") {
                logger.error("LongFlag cannot be applied to $propTypeQualified property '${metadata.key}'.", property)
                return false
            }
            "FloatFlag" -> if (propTypeQualified != "kotlin.Float") {
                logger.error("FloatFlag cannot be applied to $propTypeQualified property '${metadata.key}'.", property)
                return false
            }
            "DoubleFlag" -> if (propTypeQualified != "kotlin.Double") {
                logger.error("DoubleFlag cannot be applied to $propTypeQualified property '${metadata.key}'.", property)
                return false
            }
            "StringFlag" -> if (propTypeQualified != "kotlin.String") {
                logger.error("StringFlag cannot be applied to $propTypeQualified property '${metadata.key}'.", property)
                return false
            }
            "EnumFlag" -> {
                val enumClassArg = annotation.arguments.find { it.name?.asString() == "enumClass" }?.value as? KSType
                if (enumClassArg == null) {
                    logger.error("EnumFlag requires 'enumClass' argument.", property)
                    return false
                }
                if (enumClassArg != propType) {
                    logger.error("EnumFlag enumClass $enumClassArg does not match property type $propType.", property)
                    return false
                }
                val enumDecl = enumClassArg.declaration as? KSClassDeclaration
                if (enumDecl?.classKind != ClassKind.ENUM_CLASS) {
                    logger.error("EnumFlag.enumClass must reference an enum class, got ${enumDecl?.classKind}.", property)
                    return false
                }
                val defaultName = metadata.defaultValue as? String
                if (defaultName == null) {
                    logger.error("EnumFlag requires 'default' string argument.", property)
                    return false
                }
                val entryNames = enumDecl.declarations
                    .filterIsInstance<KSClassDeclaration>()
                    .filter { it.classKind == ClassKind.ENUM_ENTRY }
                    .map { it.simpleName.asString() }
                    .toList()
                if (defaultName !in entryNames) {
                    logger.error("EnumFlag default \"$defaultName\" is not a valid entry in ${enumDecl.simpleName.asString()}. " +
                            "Valid entries: ${entryNames.joinToString()}", property)
                    return false
                }
                metadata.enumEntries = entryNames
            }
        }
        return true
    }

    private fun generateAccessors(container: KSClassDeclaration, flags: List<FlagMetadata>) {
        val packageName = container.packageName.asString()
        val containerName = container.simpleName.asString()
        val fileName = "${containerName}Accessors"

        val switchboardClassName = ClassName("services.pixelpulse.switchboard.core", "Switchboard")

        val fileSpec = FileSpec.builder(packageName, fileName)
            .addFileComment("GENERATED CODE — DO NOT EDIT\nSource: ${container.qualifiedName?.asString()}")

        flags.forEach { flag ->
            val funName = flag.key
            val resolveMethod = when (flag.annotationName) {
                "BooleanFlag" -> "resolveBoolean"
                "IntFlag" -> "resolveInt"
                "LongFlag" -> "resolveLong"
                "FloatFlag" -> "resolveFloat"
                "DoubleFlag" -> "resolveDouble"
                "StringFlag" -> "resolveString"
                "EnumFlag" -> "resolveEnum"
                else -> throw IllegalStateException("Unknown annotation ${flag.annotationName}")
            }

            val propType = flag.property.type.resolve().toClassName()
            val propertySpec = PropertySpec.builder(funName, propType)
                .receiver(switchboardClassName)
                .getter(FunSpec.getterBuilder()
                    .addCode("return %T.%L(%S, %L)", switchboardClassName, resolveMethod, flag.key, formatDefaultValue(flag))
                    .build())
                .build()

            fileSpec.addProperty(propertySpec)
        }

        fileSpec.build().writeTo(codeGenerator, Dependencies(false, container.containingFile!!))
    }

    private fun formatDefaultValue(flag: FlagMetadata): String {
        if (flag.annotationName == "EnumFlag") {
            val type = flag.property.type.resolve()
            val className = type.toClassName()
            return "${className.simpleName}.${flag.defaultValue}"
        }
        return when (flag.defaultValue) {
            is String -> "\"${flag.defaultValue}\""
            is Float -> "${flag.defaultValue}f"
            is Long -> "${flag.defaultValue}L"
            else -> flag.defaultValue.toString()
        }
    }

    private fun generateAggregator() {
        val flagsFileSpec = FileSpec.builder("services.pixelpulse.switchboard", "SwitchboardFlags")
            .addFileComment("GENERATED CODE — DO NOT EDIT")

        val setType = Set::class.asTypeName().parameterizedBy(String::class.asTypeName())

        val typeSpec = TypeSpec.objectBuilder("SwitchboardFlags")
            .addProperty(PropertySpec.builder("allKeys", setType)
                .initializer("setOf(%L)", allDiscoveredKeys.keys.joinToString { "\"$it\"" })
                .build())
            .build()

        flagsFileSpec.addType(typeSpec)
        flagsFileSpec.build().writeTo(codeGenerator, Dependencies(true, *allSourceFiles.toTypedArray()))

        // Generate SwitchboardRegistryImpl
        val registryClassName = ClassName("services.pixelpulse.switchboard.core", "SwitchboardRegistry")
        val registeredFlagClassName = ClassName("services.pixelpulse.switchboard.core", "RegisteredFlag")
        val flagTypeClassName = ClassName("services.pixelpulse.switchboard.core", "FlagType")

        val listType = List::class.asTypeName().parameterizedBy(registeredFlagClassName)

        val registryObjectBuilder = TypeSpec.objectBuilder("SwitchboardRegistryImpl")
            .addSuperinterface(registryClassName)

        val codeBlock = com.squareup.kotlinpoet.CodeBlock.builder()
        codeBlock.add("listOf(\n")
        allDiscoveredFlags.forEach { flag ->
            val flagType = when (flag.annotationName) {
                "BooleanFlag" -> "BOOLEAN"
                "IntFlag" -> "INT"
                "LongFlag" -> "LONG"
                "FloatFlag" -> "FLOAT"
                "DoubleFlag" -> "DOUBLE"
                "StringFlag" -> "STRING"
                "EnumFlag" -> "ENUM"
                else -> "STRING"
            }
            val defaultStr = flag.defaultValue?.toString() ?: ""
            codeBlock.add("    %T(\n", registeredFlagClassName)
            codeBlock.add("        key = %S,\n", flag.key)
            codeBlock.add("        type = %T.%L,\n", flagTypeClassName, flagType)
            codeBlock.add("        defaultValue = %S,\n", defaultStr)
            codeBlock.add("        description = %S,\n", flag.description)
            codeBlock.add("        category = %S,\n", flag.category)
            if (flag.enumEntries.isNotEmpty()) {
                codeBlock.add("        enumEntries = listOf(${flag.enumEntries.joinToString { "%S" }})\n", *flag.enumEntries.toTypedArray())
            } else {
                codeBlock.add("        enumEntries = emptyList()\n")
            }
            codeBlock.add("    ),\n")
        }
        codeBlock.add(")\n")

        val flagsPropertySpec = PropertySpec.builder("flags", listType, KModifier.OVERRIDE)
            .initializer(codeBlock.build())
            .build()

        registryObjectBuilder.addProperty(flagsPropertySpec)

        val registryFileSpec = FileSpec.builder("services.pixelpulse.switchboard", "SwitchboardRegistryImpl")
            .addFileComment("GENERATED CODE — DO NOT EDIT")
            .addType(registryObjectBuilder.build())
            .build()

        registryFileSpec.writeTo(codeGenerator, Dependencies(true, *allSourceFiles.toTypedArray()))
    }

    private data class FlagMetadata(
        val key: String,
        val defaultValue: Any?,
        val annotationName: String,
        val description: String,
        val category: String,
        val property: KSPropertyDeclaration,
        var enumEntries: List<String> = emptyList()
    )
}
