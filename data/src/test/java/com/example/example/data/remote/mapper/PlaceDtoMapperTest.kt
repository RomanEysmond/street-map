package com.example.example.data.remote.mapper

import com.example.example.data.remote.dto.FeatureDto
import com.example.example.data.remote.dto.GeometryDto
import com.example.example.data.remote.dto.PropertiesDto
import org.junit.Assert.assertEquals
import org.junit.Test

class PlaceDtoMapperTest {

    @Test
    fun `geojson coordinates map to latitude and longitude correctly`() {
        // GeoJSON order is [longitude, latitude]
        val dto = FeatureDto(
            geometry = GeometryDto(coordinates = listOf(30.3, 59.9), type = "Point"),
            id = "node/1",
            properties = PropertiesDto(dist = 42.0, name = "Test place", rate = 3, xid = "abc123"),
            type = "Feature"
        )

        val domain = dto.toDomain()

        assertEquals(59.9, domain.coordinates.latitude, 0.0001)
        assertEquals(30.3, domain.coordinates.longitude, 0.0001)
        assertEquals("abc123", domain.id)
        assertEquals("Test place", domain.name)
    }

    @Test
    fun `falls back to feature id when xid is missing`() {
        val dto = FeatureDto(
            geometry = GeometryDto(coordinates = listOf(1.0, 2.0), type = "Point"),
            id = "node/1",
            properties = PropertiesDto(dist = 0.0, rate = 0, xid = null),
            type = "Feature"
        )

        val domain = dto.toDomain()

        assertEquals("node/1", domain.id)
    }
}
