package com.example

import com.example.data.repository.CrewRepository
import org.junit.Assert.assertEquals
import org.junit.Test

class RatingAggregationTest {

    @Test
    fun computeSummaryFromRatings_computesExactRoundedAverageAndCount() {
        val empty = CrewRepository.computeSummaryFromRatings(emptyList())
        assertEquals(0.0, empty.ratingAverage, 0.001)
        assertEquals(0L, empty.ratingCount)

        val single = CrewRepository.computeSummaryFromRatings(listOf(5))
        assertEquals(5.0, single.ratingAverage, 0.001)
        assertEquals(1L, single.ratingCount)

        val multi = CrewRepository.computeSummaryFromRatings(listOf(5, 4, 5))
        assertEquals(4.7, multi.ratingAverage, 0.001)
        assertEquals(3L, multi.ratingCount)

        val ignoresOutOfRange = CrewRepository.computeSummaryFromRatings(listOf(0, 4, 6, 2))
        assertEquals(3.0, ignoresOutOfRange.ratingAverage, 0.001)
        assertEquals(2L, ignoresOutOfRange.ratingCount)
    }

    @Test
    fun computeIncrementalSummary_handlesCreateUpdateAndDelete() {
        val first = CrewRepository.computeIncrementalSummary(
            currentAverage = 0.0,
            currentCount = 0L,
            previousRating = null,
            newRating = 5
        )
        assertEquals(5.0, first.ratingAverage, 0.001)
        assertEquals(1L, first.ratingCount)

        val addedSecond = CrewRepository.computeIncrementalSummary(
            currentAverage = 5.0,
            currentCount = 1L,
            previousRating = null,
            newRating = 3
        )
        assertEquals(4.0, addedSecond.ratingAverage, 0.001)
        assertEquals(2L, addedSecond.ratingCount)

        val updatedSecond = CrewRepository.computeIncrementalSummary(
            currentAverage = 4.0,
            currentCount = 2L,
            previousRating = 3,
            newRating = 5
        )
        assertEquals(5.0, updatedSecond.ratingAverage, 0.001)
        assertEquals(2L, updatedSecond.ratingCount)

        val deletedOneOfTwo = CrewRepository.computeIncrementalSummary(
            currentAverage = 4.5,
            currentCount = 2L,
            previousRating = 4,
            newRating = null
        )
        assertEquals(5.0, deletedOneOfTwo.ratingAverage, 0.001)
        assertEquals(1L, deletedOneOfTwo.ratingCount)

        val deletedLast = CrewRepository.computeIncrementalSummary(
            currentAverage = 5.0,
            currentCount = 1L,
            previousRating = 5,
            newRating = null
        )
        assertEquals(0.0, deletedLast.ratingAverage, 0.001)
        assertEquals(0L, deletedLast.ratingCount)
    }
}
