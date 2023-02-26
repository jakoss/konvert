package io.mcarle.lib.kmapper.processor.converter

import io.mcarle.lib.kmapper.processor.TypeConverter
import org.junit.jupiter.api.assertDoesNotThrow
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import java.time.*
import java.time.temporal.ChronoField
import java.util.*
import kotlin.reflect.KCallable
import kotlin.reflect.KClass
import kotlin.test.assertEquals

class XToTemporalConverterITest : ConverterITest() {

    companion object {
        @JvmStatic
        fun converterList(): List<Arguments> = listOf(
            StringToInstantConverter(),
            StringToZonedDateTimeConverter(),
            StringToOffsetDateTimeConverter(),
            StringToLocalDateTimeConverter(),
            StringToLocalDateConverter(),
            LongToInstantConverter(),
        ).toConverterTestArguments {
            it.sourceClass.qualifiedName to it.targetClass.qualifiedName
        }
    }

    @ParameterizedTest()
    @MethodSource("converterList")
    override fun converterTest(converter: TypeConverter, sourceTypeName: String, targetTypeName: String) {
        super.converterTest(converter, sourceTypeName, targetTypeName)
    }

    override fun verifyMapper(
        sourceTypeName: String,
        targetTypeName: String,
        mapperInstance: Any,
        mapperFunction: KCallable<*>,
        sourceKClass: KClass<*>,
        targetKClass: KClass<*>
    ) {
        val instant = when {
            targetTypeName.startsWith("java.time.Instant") -> Instant.now()
            targetTypeName.startsWith("java.time.ZonedDateTime") -> ZonedDateTime.now()
            targetTypeName.startsWith("java.time.OffsetDateTime") -> OffsetDateTime.now()
            targetTypeName.startsWith("java.time.LocalDateTime") -> LocalDateTime.now()
            targetTypeName.startsWith("java.time.LocalDate") -> LocalDate.now()
            else -> LocalDate.now()
        }

        val sourceInstance = sourceKClass.constructors.first().call(
            when {
                sourceTypeName.startsWith("kotlin.String") -> instant.toString()
                sourceTypeName.startsWith("kotlin.Long") -> instant.getLong(ChronoField.INSTANT_SECONDS) * 1000 + instant.getLong(ChronoField.MILLI_OF_SECOND)
                else -> null
            }
        )

        val targetInstance = mapperFunction.call(mapperInstance, sourceInstance)

        val targetValue = assertDoesNotThrow {
            targetKClass.members.first { it.name == "test" }.call(targetInstance)
        }
        when {
            targetTypeName.startsWith("java.time.Instant") -> when {
                sourceTypeName.startsWith("kotlin.String") -> assertEquals(instant, targetValue)
                sourceTypeName.startsWith("kotlin.Long") -> assertEquals(Instant.ofEpochMilli((instant as Instant).toEpochMilli()), targetValue)
            }
            targetTypeName.startsWith("java.time.ZonedDateTime") -> assertEquals(instant, targetValue)
            targetTypeName.startsWith("java.time.OffsetDateTime") -> assertEquals(instant, targetValue)
            targetTypeName.startsWith("java.time.LocalDateTime") -> assertEquals(instant, targetValue)
            targetTypeName.startsWith("java.time.LocalDate") -> assertEquals(instant, targetValue)
        }
    }

}

