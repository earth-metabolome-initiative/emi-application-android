package com.commons.emi

import androidx.test.ext.junit.runners.AndroidJUnit4
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ContainerModelCapacityTest {
    @Test
    fun modelDimensionsOverrideStaleContainerDimensions() {
        val container = JSONObject("""{"is_finite":false,"columns":1,"rows":1,
            "container_model":{"columns":9,"rows":9}}""")
        assertEquals(81, DatabaseManager.capacityFromContainerModel(container))
    }

    @Test
    fun modelUnlimitedMarkerOverridesCopiedFiniteContainerFields() {
        val container = JSONObject("""{"is_finite":true,"columns":9,"rows":9,
            "container_model":{"columns":-1,"rows":-1}}""")
        assertEquals(-1, DatabaseManager.capacityFromContainerModel(container))
    }

    @Test
    fun missingOrIncompleteModelCannotUseCopiedFields() {
        assertEquals(0, DatabaseManager.capacityFromContainerModel(
            JSONObject("""{"columns":9,"rows":9,"is_finite":true}""")))
        assertEquals(0, DatabaseManager.capacityFromContainerModel(
            JSONObject("""{"columns":9,"rows":9,"container_model":{"columns":9}}""")))
    }
}
