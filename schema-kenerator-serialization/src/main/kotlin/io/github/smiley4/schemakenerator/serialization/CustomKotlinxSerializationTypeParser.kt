package io.github.smiley4.schemakenerator.serialization

import io.github.smiley4.schemakenerator.core.parser.CustomTypeParser
import kotlinx.serialization.descriptors.SerialDescriptor

interface CustomKotlinxSerializationTypeParser : CustomTypeParser<SerialDescriptor>