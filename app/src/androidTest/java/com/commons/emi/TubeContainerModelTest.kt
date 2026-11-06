package com.commons.emi

import androidx.test.ext.junit.runners.AndroidJUnit4
import org.json.JSONObject
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class TubeContainerModelTest {
    @Test fun onlyPositiveVolumeSingleSampleModelsCanReceiveBagLabels() {
        assertTrue(isTubeContainerModel(JSONObject("""{"is_sample_container":true,"volume":15,"columns":1,"rows":1}""")))
        assertFalse(isTubeContainerModel(JSONObject("""{"is_sample_container":false,"volume":15,"columns":1,"rows":1}""")))
        assertFalse(isTubeContainerModel(JSONObject("""{"is_sample_container":true,"volume":0,"columns":1,"rows":1}""")))
        assertFalse(isTubeContainerModel(JSONObject("""{"is_sample_container":true,"volume":15,"columns":9,"rows":9}""")))
        assertFalse(isTubeContainerModel(JSONObject("""{"is_sample_container":true,"volume":15,"columns":-1,"rows":-1}""")))
        assertFalse(isTubeContainerModel(JSONObject("""{"is_sample_container":true,"volume":15,"columns":1}""")))
    }
    @Test fun unassignedFieldSamplesCanReceiveTheirFirstContainerModel() {
        val unassigned = JSONObject("""{"id":42,"container_model":null}""")
        assertTrue(isRelabelableSampleContainer(unassigned, hasFieldSample = true))
        assertFalse(isRelabelableSampleContainer(unassigned, hasFieldSample = false))
        assertFalse(isRelabelableSampleContainer(JSONObject("""{"id":42}"""), hasFieldSample = true))
    }

    @Test fun existingSingleSampleContainersRemainRelabelableButRacksDoNot() {
        val sample = JSONObject("""{"id":42,"container_model":{"columns":1,"rows":1}}""")
        val rack = JSONObject("""{"id":42,"container_model":{"columns":9,"rows":9}}""")
        val unlimited = JSONObject("""{"id":42,"container_model":{"columns":-1,"rows":-1}}""")
        assertTrue(isRelabelableSampleContainer(sample, hasFieldSample = false))
        assertFalse(isRelabelableSampleContainer(rack, hasFieldSample = true))
        assertFalse(isRelabelableSampleContainer(unlimited, hasFieldSample = true))
    }
}
