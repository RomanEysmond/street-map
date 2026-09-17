package com.example.example.data.local.db.mapper

import com.example.example.domain.model.GeoBoundingBox
import com.example.example.domain.model.OfflineArea
import org.junit.Assert.assertEquals
import org.junit.Test

class OfflineAreaEntityMapperTest {

    @Test
    fun `round trip preserves fields`() {
        val area = OfflineArea(
            id = 5,
            name = "Downtown",
            boundingBox = GeoBoundingBox(north = 10.1, south = 10.0, east = 20.1, west = 20.0),
            minZoom = 14,
            maxZoom = 17,
            createdAtMillis = 1_700_000_000_000,
            tileCount = 42,
            estimatedSizeBytes = 630_000
        )

        val roundTripped = area.toEntity().toDomain()

        assertEquals(area, roundTripped)
    }
}
