package me.liam.microsmith.dsl.schemas.core

/**
 * Marker for all schema definitions.
 * Dialects implement this (protobuf, json, avro, etc.).
 */
interface Schema {
    val name: String
}