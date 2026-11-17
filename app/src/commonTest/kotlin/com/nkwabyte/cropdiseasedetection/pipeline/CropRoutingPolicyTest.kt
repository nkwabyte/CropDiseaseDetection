package com.nkwabyte.cropdiseasedetection.pipeline

import com.nkwabyte.cropdiseasedetection.common.AppConstants
import com.nkwabyte.cropdiseasedetection.common.model.SupportedCrop
import com.nkwabyte.cropdiseasedetection.common.pipeline.CropRoutingPolicy
import com.nkwabyte.cropdiseasedetection.common.pipeline.RoutingDecision
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

class SupportedCropTest {

    @Test
    fun classLabelsHasExactlyTwentyThreeEntries() {
        assertEquals(23, AppConstants.CLASS_LABELS.size)
        assertEquals(23, SupportedCrop.DETECTOR_CLASS_COUNT)
    }

    @Test
    fun cropRangesMatchThePythonReferenceMapping() {
        // src/classifier/config.py::CROP_TO_YOLO_CLASSES
        assertEquals((0..4).toSet(), SupportedCrop.CORN.classIds)
        assertEquals((5..14).toSet(), SupportedCrop.PEPPER.classIds)
        assertEquals((15..22).toSet(), SupportedCrop.TOMATO.classIds)
    }

    @Test
    fun rangesCoverEveryClassIdExactlyOnce() {
        val all = SupportedCrop.entries.flatMap { it.classIds }
        assertEquals(23, all.size, "ranges overlap or leave a gap")
        assertEquals(AppConstants.CLASS_LABELS.indices.toSet(), all.toSet())
    }

    @Test
    fun everyLabelBelongsToTheCropThatOwnsItsIndex() {
        // Guards against a reordered CLASS_LABELS silently re-pointing every route.
        AppConstants.CLASS_LABELS.forEachIndexed { index, label ->
            val owner = SupportedCrop.forClassId(index)
            assertTrue(
                owner != null && label.startsWith(owner.canonicalLabel),
                "class $index (\"$label\") is not owned by ${owner?.canonicalLabel}",
            )
        }
    }

    @Test
    fun canonicalIdsResolveAndTranslatedNamesDoNot() {
        assertEquals(SupportedCrop.CORN, SupportedCrop.fromCanonicalId("Corn"))
        assertEquals(SupportedCrop.CORN, SupportedCrop.fromCanonicalId("corn"))
        assertEquals(SupportedCrop.CORN, SupportedCrop.fromCanonicalId("CORN"))
        // The display names from values-fr / values-tw / values-ha. Routing must
        // never accept these, or it would start depending on the app's language.
        for (translated in listOf("Maïs", "Aburo", "Masara", "Bli", "Ablona", "Piment", "Tomate")) {
            assertEquals(null, SupportedCrop.fromCanonicalId(translated), translated)
        }
    }

    @Test
    fun rejectionLabelsAndBlanksResolveToNothing() {
        for (id in listOf(null, "", "   ", "Other", "unknown", "Cassava")) {
            assertEquals(null, SupportedCrop.fromCanonicalId(id), "id=$id")
        }
    }
}

class CropRoutingPolicyTest {

    @Test
    fun acceptedCornRoutesOnlyIdsZeroToFour() {
        val decision = CropRoutingPolicy.decide(accepted("Corn"), null)
        assertIs<RoutingDecision.Accepted>(decision)
        assertEquals(SupportedCrop.CORN, decision.crop)
        assertEquals((0..4).toSet(), decision.allowedClassIds)
    }

    @Test
    fun acceptedPepperRoutesOnlyIdsFiveToFourteen() {
        val decision = CropRoutingPolicy.decide(accepted("Pepper"), null)
        assertIs<RoutingDecision.Accepted>(decision)
        assertEquals((5..14).toSet(), decision.allowedClassIds)
    }

    @Test
    fun acceptedTomatoRoutesOnlyIdsFifteenToTwentyTwo() {
        val decision = CropRoutingPolicy.decide(accepted("Tomato"), null)
        assertIs<RoutingDecision.Accepted>(decision)
        assertEquals((15..22).toSet(), decision.allowedClassIds)
    }

    @Test
    fun rejectedClassificationIsNotRoutable() {
        val decision = CropRoutingPolicy.decide(rejected(), "Corn")
        assertIs<RoutingDecision.Rejected>(decision)
        assertEquals("unknown", decision.label)
    }

    @Test
    fun nullClassificationFailsClosedAsUnavailableNotAsRejection() {
        // These must stay distinguishable: "we could not tell" is not "this is
        // not a crop", and the UI says different things for each.
        val decision = CropRoutingPolicy.decide(null, "Corn")
        assertIs<RoutingDecision.ClassifierUnavailable>(decision)
    }

    @Test
    fun acceptedButUnmappedLabelFailsClosed() {
        val decision = CropRoutingPolicy.decide(accepted("Other"), null)
        assertIs<RoutingDecision.ClassifierUnavailable>(decision)
    }

    @Test
    fun blankSelectionUsesTheClassifierCrop() {
        for (blank in listOf(null, "", "   ")) {
            val decision = CropRoutingPolicy.decide(accepted("Tomato"), blank)
            assertIs<RoutingDecision.Accepted>(decision)
            assertEquals(SupportedCrop.TOMATO, decision.crop)
        }
    }

    @Test
    fun conflictingSelectionIsAMismatchCarryingBothCrops() {
        val decision = CropRoutingPolicy.decide(accepted("Tomato", 0.88f), "Corn")
        assertIs<RoutingDecision.SelectionMismatch>(decision)
        assertEquals(SupportedCrop.TOMATO, decision.predicted)
        assertEquals(SupportedCrop.CORN, decision.selected)
        assertEquals(0.88f, decision.confidence)
    }

    @Test
    fun matchingSelectionIsAccepted() {
        val decision = CropRoutingPolicy.decide(accepted("Pepper"), "Pepper")
        assertIs<RoutingDecision.Accepted>(decision)
    }

    @Test
    fun filterKeepsOnlyInRouteClassIndices() {
        val all = (0..22).map { detectionOf(it) }
        val corn = CropRoutingPolicy.filterToRoute(all, SupportedCrop.CORN.classIds)
        assertEquals(listOf(0, 1, 2, 3, 4), corn.map { it.classIndex })

        val pepper = CropRoutingPolicy.filterToRoute(all, SupportedCrop.PEPPER.classIds)
        assertEquals((5..14).toList(), pepper.map { it.classIndex })

        val tomato = CropRoutingPolicy.filterToRoute(all, SupportedCrop.TOMATO.classIds)
        assertEquals((15..22).toList(), tomato.map { it.classIndex })
    }

    @Test
    fun crossCropDetectionsAreRemovedByIndexNotByName() {
        // A Tomato-class detection mislabeled with a Corn-looking name must still
        // be dropped from a Corn route, and vice versa: the index decides.
        val mislabeled = detectionOf(18).copy(className = "Corn Healthy")
        val kept = CropRoutingPolicy.filterToRoute(listOf(mislabeled), SupportedCrop.CORN.classIds)
        assertTrue(kept.isEmpty(), "routing followed the class name instead of the class id")

        val cornIndexTomatoName = detectionOf(2).copy(className = "Tomato Healthy")
        val keptCorn = CropRoutingPolicy.filterToRoute(
            listOf(cornIndexTomatoName), SupportedCrop.CORN.classIds
        )
        assertEquals(1, keptCorn.size)
    }

    @Test
    fun substringSimilarityCannotBypassRouting() {
        // The old filter was className.contains(crop, ignoreCase = true), so a
        // name containing another crop's name leaked across routes. Index-based
        // routing is immune, and this pins that down.
        val sneaky = listOf(
            detectionOf(15).copy(className = "Tomato Corn Hybrid Spot"),
            detectionOf(20).copy(className = "corn"),
            detectionOf(6).copy(className = "CORN_PEPPER"),
        )
        val corn = CropRoutingPolicy.filterToRoute(sneaky, SupportedCrop.CORN.classIds)
        assertTrue(corn.isEmpty(), "a name containing \"corn\" bypassed the Corn route")
    }

    @Test
    fun outOfRangeClassIndexIsNeverRouted() {
        val bogus = listOf(detectionOf(23), detectionOf(-1), detectionOf(999))
        for (crop in SupportedCrop.entries) {
            assertTrue(CropRoutingPolicy.filterToRoute(bogus, crop.classIds).isEmpty())
        }
    }
}
