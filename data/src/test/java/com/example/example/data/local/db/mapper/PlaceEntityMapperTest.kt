package com.example.example.data.local.db.mapper

import com.example.example.domain.model.Coordinates
import com.example.example.domain.model.PlaceOfInterest
import org.junit.Assert.assertEquals
import org.junit.Test

class PlaceEntityMapperTest {

    @Test
    fun `round trip preserves fields`() {
        val place = PlaceOfInterest(
            id = "xid1",
            name = "Museum",
            coordinates = Coordinates(latitude = 59.9, longitude = 30.3),
            kinds = "museums",
            distanceMeters = 120.5,
            rating = 2,
            wikidataId = "Q1",
            osmId = "way/1"
        )

        val roundTripped = place.toEntity(offlineAreaId = 7).toDomain()

        assertEquals(place, roundTripped)
    }
}
