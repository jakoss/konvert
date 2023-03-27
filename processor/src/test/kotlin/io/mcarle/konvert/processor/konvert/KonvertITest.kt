package io.mcarle.konvert.processor.konvert

import com.tschuchort.compiletesting.SourceFile
import io.mcarle.konvert.api.Mapping
import io.mcarle.konvert.converter.SameTypeConverter
import io.mcarle.konvert.converter.api.DEFAULT_KONVERTER_PRIORITY
import io.mcarle.konvert.converter.api.DEFAULT_KONVERT_PRIORITY
import io.mcarle.konvert.converter.api.TypeConverterRegistry
import io.mcarle.konvert.processor.KonverterITest
import io.mcarle.konvert.processor.generatedSourceFor
import org.jetbrains.kotlin.utils.addToStdlib.firstIsInstanceOrNull
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import kotlin.test.assertContains
import kotlin.test.assertEquals
import kotlin.test.assertNotNull


class KonvertITest : KonverterITest() {

    @Test
    fun converter() {
        val (compilation) = super.compileWith(
            listOf(SameTypeConverter()),
            SourceFile.kotlin(
                name = "TestCode.kt",
                contents =
                """
import io.mcarle.konvert.api.Konverter
import io.mcarle.konvert.api.Konvert
import io.mcarle.konvert.api.Mapping

class SourceClass(
    val sourceProperty: String
)
class TargetClass(
    val targetProperty: String
)

@Konverter
interface Mapper {
    @Konvert(mappings = [Mapping(source="sourceProperty",target="targetProperty")])
    fun toTarget(source: SourceClass): TargetClass
}
                """.trimIndent()
            )
        )
        val mapperCode = compilation.generatedSourceFor("MapperKonverter.kt")
        println(mapperCode)

        val converter = TypeConverterRegistry.firstIsInstanceOrNull<KonvertTypeConverter>()
        assertNotNull(converter, "No KonverterTypeConverter registered")
        assertEquals("toTarget", converter.mapFunctionName)
        assertEquals("source", converter.paramName)
        assertEquals("SourceClass", converter.sourceClassDeclaration.simpleName.asString())
        assertEquals("TargetClass", converter.targetClassDeclaration.simpleName.asString())
        assertEquals("Mapper", converter.mapKSClassDeclaration.simpleName.asString())
        assertEquals(true, converter.enabledByDefault)
        assertEquals(listOf(Mapping(source = "sourceProperty", target = "targetProperty")), converter.annotation?.mappings)
        assertEquals(DEFAULT_KONVERT_PRIORITY, converter.priority)
    }

    @Test
    fun defaultMappingsOnMissingKonvertAnnotation() {
        val (compilation) = super.compileWith(
            listOf(SameTypeConverter()),
            SourceFile.kotlin(
                name = "TestCode.kt",
                contents =
                """
import io.mcarle.konvert.api.Konverter

class SourceClass(
    val property: String
)
class TargetClass(
    val property: String
)

@Konverter
interface Mapper {
    fun toTarget(source: SourceClass): TargetClass
}
                """.trimIndent()
            )
        )
        val mapperCode = compilation.generatedSourceFor("MapperKonverter.kt")
        println(mapperCode)

        val converter = TypeConverterRegistry.firstIsInstanceOrNull<KonvertTypeConverter>()
        assertNotNull(converter, "No KonverterTypeConverter registered")
        assertEquals("toTarget", converter.mapFunctionName)
        assertEquals("source", converter.paramName)
        assertEquals("SourceClass", converter.sourceClassDeclaration.simpleName.asString())
        assertEquals("TargetClass", converter.targetClassDeclaration.simpleName.asString())
        assertEquals("Mapper", converter.mapKSClassDeclaration.simpleName.asString())
        assertEquals(true, converter.enabledByDefault)
        assertEquals(listOf(), converter.annotation?.mappings)
        assertEquals(DEFAULT_KONVERT_PRIORITY, converter.priority)
    }

    @Test
    fun registerConverterForImplementedFunctions() {
        val (compilation) = super.compileWith(
            listOf(SameTypeConverter()),
            SourceFile.kotlin(
                name = "TestCode.kt",
                contents =
                """
import io.mcarle.konvert.api.Konverter

class SourceClass(
    val sourceProperty: String
)
class TargetClass(
    val targetProperty: String
)

@Konverter
interface Mapper {
    fun toTarget(source: SourceClass): TargetClass {
        return TargetClass(source.sourceProperty)
    }
}
                """.trimIndent()
            )
        )
        assertThrows<IllegalArgumentException> { compilation.generatedSourceFor("MapperKonverter.kt") }

        val converter = TypeConverterRegistry.firstIsInstanceOrNull<KonvertTypeConverter>()
        assertNotNull(converter, "No KonverterTypeConverter registered")
        assertEquals("toTarget", converter.mapFunctionName)
        assertEquals("source", converter.paramName)
        assertEquals("SourceClass", converter.sourceClassDeclaration.simpleName.asString())
        assertEquals("TargetClass", converter.targetClassDeclaration.simpleName.asString())
        assertEquals("Mapper", converter.mapKSClassDeclaration.simpleName.asString())
        assertEquals(true, converter.enabledByDefault)
        assertEquals(null, converter.annotation)
        assertEquals(DEFAULT_KONVERTER_PRIORITY, converter.priority)
    }

    @Test
    fun useOtherMapper() {
        val (compilation) = super.compileWith(
            listOf(SameTypeConverter()),
            SourceFile.kotlin(
                name = "TestCode.kt",
                contents =
                """
import io.mcarle.konvert.api.Konverter
import io.mcarle.konvert.api.Konvert
import io.mcarle.konvert.api.Mapping

class SourceClass(
    val sourceProperty: SourceProperty
)
class TargetClass(
    val targetProperty: TargetProperty
)

@Konverter
interface Mapper {
    @Konvert(mappings = [Mapping(source="sourceProperty",target="targetProperty")])
    fun toTarget(source: SourceClass): TargetClass
}

data class SourceProperty(val value: String)
data class TargetProperty(val value: String)

@Konverter
interface OtherMapper {
    fun toTarget(source: SourceProperty): TargetProperty
}
                """.trimIndent()
            )
        )
        val mapperCode = compilation.generatedSourceFor("MapperKonverter.kt")
        println(mapperCode)

        assertContains(mapperCode, "Konverter.get<OtherMapper>().toTarget(")
    }

}