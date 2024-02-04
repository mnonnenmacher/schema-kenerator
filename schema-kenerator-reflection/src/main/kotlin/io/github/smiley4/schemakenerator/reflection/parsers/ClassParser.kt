package io.github.smiley4.schemakenerator.reflection.parsers

import io.github.smiley4.schemakenerator.core.parser.CollectionTypeData
import io.github.smiley4.schemakenerator.core.parser.ContextTypeRef
import io.github.smiley4.schemakenerator.core.parser.EnumTypeData
import io.github.smiley4.schemakenerator.core.parser.InlineTypeRef
import io.github.smiley4.schemakenerator.core.parser.MapTypeData
import io.github.smiley4.schemakenerator.core.parser.ObjectTypeData
import io.github.smiley4.schemakenerator.core.parser.PrimitiveTypeData
import io.github.smiley4.schemakenerator.core.parser.PropertyData
import io.github.smiley4.schemakenerator.core.parser.PropertyType
import io.github.smiley4.schemakenerator.core.parser.TypeId
import io.github.smiley4.schemakenerator.core.parser.TypeParameterData
import io.github.smiley4.schemakenerator.core.parser.TypeRef
import io.github.smiley4.schemakenerator.core.parser.Visibility
import io.github.smiley4.schemakenerator.reflection.ClassType
import kotlin.reflect.KClass
import kotlin.reflect.KType

class ClassParser(private val typeParser: ReflectionTypeParser) {

    fun parse(type: KType, clazz: KClass<*>, providedTypeParameters: Map<String, TypeParameterData>): TypeRef {

        // resolve all type parameters
        val resolvedTypeParameters = typeParser.getTypeParameterParser().parse(type, clazz, providedTypeParameters)

        // check if the same type with the same type parameters has already been resolved -> reuse existing
        val id = TypeId.build(clazz.qualifiedName ?: "?", resolvedTypeParameters.values.map { it.type })
        if (!typeParser.config.inline && typeParser.context.has(id)) {
            return ContextTypeRef(id)
        }

        // check custom parsers
        typeParser.parseCustom(id, clazz)?.also {
            if (typeParser.config.inline) {
                return@parse InlineTypeRef(it)
            } else {
                return@parse typeParser.context.add(it)
            }
        }

        // add placeholder to break out of some infinite recursions
        if (!typeParser.config.inline) {
            typeParser.context.reserve(id)
        }

        // determine type (object, primitive, map, collection, ...)
        val classType = typeParser.config.typeDecider.determineType(typeParser.config, clazz, id)

        // collect information about supertypes
        val supertypes = if (classType == ClassType.OBJECT) {
            typeParser.getSupertypeParser().parse(clazz, resolvedTypeParameters)
        } else {
            emptyList()
        }

        // collect information about enum constants
        val enumValues = if (classType == ClassType.ENUM) {
            typeParser.getEnumValueParser().parse(clazz)
        } else {
            emptyList()
        }

        // collect member information
        val members = if (classType == ClassType.OBJECT) {
            typeParser.getPropertyParser().parse(clazz, resolvedTypeParameters, supertypes)
        } else {
            emptyList()
        }

        // add type to context and return its id
        return when (classType) {
            ClassType.PRIMITIVE -> PrimitiveTypeData(
                id = id,
                simpleName = clazz.simpleName!!,
                qualifiedName = clazz.qualifiedName!!,
                typeParameters = resolvedTypeParameters,
            ).let { typeParser.asRef(it) }
            ClassType.OBJECT -> ObjectTypeData(
                id = id,
                simpleName = clazz.simpleName!!,
                qualifiedName = clazz.qualifiedName!!,
                typeParameters = resolvedTypeParameters,
                subtypes = emptyList(),
                supertypes = supertypes,
                members = members,
            ).let { typeParser.asRef(it) }
            ClassType.ENUM -> EnumTypeData(
                id = id,
                simpleName = clazz.simpleName!!,
                qualifiedName = clazz.qualifiedName!!,
                typeParameters = resolvedTypeParameters,
                subtypes = emptyList(),
                supertypes = supertypes,
                members = members,
                enumConstants = enumValues
            ).let { typeParser.asRef(it) }
            ClassType.COLLECTION -> CollectionTypeData(
                id = id,
                simpleName = clazz.simpleName!!,
                qualifiedName = clazz.qualifiedName!!,
                typeParameters = resolvedTypeParameters,
                subtypes = emptyList(),
                supertypes = supertypes,
                members = members,
                itemType = resolvedTypeParameters["E"]?.let {
                    PropertyData(
                        name = "item",
                        type = it.type,
                        nullable = it.nullable,
                        visibility = Visibility.PUBLIC,
                        kind = PropertyType.PROPERTY
                    )
                } ?: resolvedTypeParameters["T"]?.let {
                    PropertyData(
                        name = "item",
                        type = it.type,
                        nullable = it.nullable,
                        visibility = Visibility.PUBLIC,
                        kind = PropertyType.PROPERTY
                    )
                }
                ?: unknownPropertyData("item")
            ).let { typeParser.asRef(it) }
            ClassType.MAP -> MapTypeData(
                id = id,
                simpleName = clazz.simpleName!!,
                qualifiedName = clazz.qualifiedName!!,
                typeParameters = resolvedTypeParameters,
                subtypes = emptyList(),
                supertypes = supertypes,
                members = members,
                keyType = resolvedTypeParameters["K"]?.let {
                    PropertyData(
                        name = "key",
                        type = it.type,
                        nullable = it.nullable,
                        visibility = Visibility.PUBLIC,
                        kind = PropertyType.PROPERTY
                    )
                } ?: unknownPropertyData("item"),
                valueType = resolvedTypeParameters["V"]?.let {
                    PropertyData(
                        name = "value",
                        type = it.type,
                        nullable = it.nullable,
                        visibility = Visibility.PUBLIC,
                        kind = PropertyType.PROPERTY
                    )
                } ?: unknownPropertyData("item")
            ).let { typeParser.asRef(it) }
        }
    }

    private fun unknownPropertyData(name: String) = PropertyData(
        name = name,
        type = ContextTypeRef(TypeId.wildcard()),
        nullable = false,
        visibility = Visibility.PUBLIC,
        kind = PropertyType.PROPERTY
    )

}