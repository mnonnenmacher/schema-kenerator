package io.github.smiley4.schemakenerator.swagger.steps

import io.github.smiley4.schemakenerator.core.annotations.Example
import io.github.smiley4.schemakenerator.core.data.BaseTypeData
import io.github.smiley4.schemakenerator.core.data.Bundle
import io.github.smiley4.schemakenerator.core.data.PropertyData
import io.github.smiley4.schemakenerator.swagger.data.SwaggerSchema
import io.github.smiley4.schemakenerator.swagger.steps.SwaggerSchemaAnnotationUtils.iterateProperties
import io.swagger.v3.oas.models.media.Schema

/**
 * Adds example-values from [Example]-annotations.
 */
class SwaggerSchemaCoreAnnotationExamplesStep {

    fun process(bundle: Bundle<SwaggerSchema>): Bundle<SwaggerSchema> {
        return bundle.also { schema ->
            process(schema.data)
            schema.supporting.forEach { process(it) }
        }
    }

    private fun process(schema: SwaggerSchema) {
        if (schema.swagger.examples == null) {
            determineExamples(schema.typeData)?.also {
                @Suppress("UNCHECKED_CAST")
                (schema.swagger as Schema<Any?>).example = it
            }
        }
        iterateProperties(schema) { prop, data ->
            determineExamples(data)?.also {
                @Suppress("UNCHECKED_CAST")
                (prop as Schema<Any?>).example = it
            }
        }
    }

    private fun determineExamples(typeData: PropertyData): String? {
        return typeData.annotations
            .filter { it.name == Example::class.qualifiedName }
            .map { it.values["example"] as String }
            .firstOrNull()
    }

    private fun determineExamples(typeData: BaseTypeData): String? {
        return typeData.annotations
            .filter { it.name == Example::class.qualifiedName }
            .map { it.values["example"] as String }
            .firstOrNull()
    }

}
