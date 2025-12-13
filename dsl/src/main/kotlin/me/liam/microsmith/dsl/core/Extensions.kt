package me.liam.microsmith.dsl.core

/**
 * Marker interface for all extension payloads.
 *
 * Extensions are typed data objects that plugins attach to the [MicrosmithModel].
 * They represent domain-specific concepts (e.g. services, endpoints, schemas).
 */
interface ModelExtension

/**
 * Marker for extensions that live at the root, i.e. microsmith, level of the DSL.
 *
 * Example: a `ServicesExtension` that holds all declared services.
 */
interface MicrosmithExtension : ModelExtension