package com.example.example.domain.model

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class GeoBoundingBoxTest {

    private val box = GeoBoundingBox(north = 10.0, south = 0.0, east = 20.0, west = 10.0)

    @Test
    fun `point inside bounding box is contained`() {
        assertTrue(box.contains(Coordinates(latitude = 5.0, longitude = 15.0)))
    }

    @Test
    fun `point outside latitude range is not contained`() {
        assertFalse(box.contains(Coordinates(latitude = 15.0, longitude = 15.0)))
    }

    @Test
    fun `point outside longitude range is not contained`() {
        assertFalse(box.contains(Coordinates(latitude = 5.0, longitude = 25.0)))
    }

    @Test
    fun `point exactly on boundary is contained`() {
        assertTrue(box.contains(Coordinates(latitude = 10.0, longitude = 10.0)))
        assertTrue(box.contains(Coordinates(latitude = 0.0, longitude = 20.0)))
    }
}
