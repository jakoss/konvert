package io.mcarle.lib.kmapper.processor.converter.annotated

import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSType
import io.mcarle.lib.kmapper.annotation.KMap
import io.mcarle.lib.kmapper.annotation.KMapTo
import io.mcarle.lib.kmapper.annotation.Priority
import io.mcarle.lib.kmapper.processor.AbstractTypeConverter
import io.mcarle.lib.kmapper.processor.isNullable

class KMapToConverter(
    override val annotation: KMapTo,
    val sourceClassDeclaration: KSClassDeclaration,
    val targetClassDeclaration: KSClassDeclaration,
    val mapKSClassDeclaration: KSClassDeclaration,
    val mapFunctionName: String,
) : AbstractTypeConverter(), AnnotatedConverter<KMapTo> {

    override val priority: Priority = annotation.priority

    private val sourceType: KSType = sourceClassDeclaration.asStarProjectedType()
    private val targetType: KSType = targetClassDeclaration.asStarProjectedType()

    override fun matches(source: KSType, target: KSType): Boolean {
        return sourceType in setOf(
            source,
            source.makeNotNullable()
        ) && targetType in setOf(
            target,
            target.makeNotNullable()
        )
    }

    override fun convert(fieldName: String, source: KSType, target: KSType): String {
        val nc = if (source.isNullable()) "?" else ""
        return "$fieldName$nc.$mapFunctionName()"
    }
}