package com.aaronicsubstances.niv1984.data

import kotlin.math.ceil

class SearchQueryAdvancer(query: String,
                          isWordSearch: Boolean) {
    private val terms: List<String>
    var currentQuery: String
        private set

    // state
    private var treatAsExact: Boolean
    private var omittedTermCount: Int

    init {
        terms = splitUserQuery(query)
        if (isWordSearch) {
            treatAsExact = false
            omittedTermCount = terms.size - 1
        }
        else {
            treatAsExact = true
            omittedTermCount = 0
        }
        currentQuery = transformUserQuery(terms, treatAsExact, omittedTermCount)
    }

    fun advance(): Boolean {
        if (treatAsExact) {
            treatAsExact = false
        }
        else {
            omittedTermCount++
        }
        // require more than half of terms to proceed
        val maxLimit = ceil(terms.size / 2.0)
        if (omittedTermCount >= maxLimit) {
            return false
        }
        currentQuery = transformUserQuery(terms, treatAsExact, omittedTermCount)
        return true
    }

    companion object {

        internal fun splitUserQuery(rawUserQuery: String): List<String> {
            // Replace all chars which are neither letters nor digits with space.
            val processed = StringBuilder()
            for (i in rawUserQuery.indices) {
                val c = rawUserQuery[i]
                if (Character.isLetterOrDigit(c)) {
                    processed.append(c)
                } else {
                    processed.append(" ")
                }
            }
            val terms = processed.toString().split(" ").filter { it.isNotEmpty() }
            return terms
        }

        internal fun transformUserQuery(
            terms: List<String>, treatAsExact: Boolean, omittedTermCount: Int
        ): String {
            val altLen = terms.size - omittedTermCount
            if (altLen < 1) {
                return ""
            }
            val altQueries = mutableListOf<String>()
            for (i in 0..omittedTermCount) {
                // quote terms as phrases or else lowercase them to prevent clash with keywords
                val altQuery = if (treatAsExact) {
                    "\"" + terms.subList(i, i + altLen)
                        .joinToString(" ") + "\""
                }
                else {
                    terms.subList(i, i + altLen)
                        .joinToString(" NEAR/3 ") { "\"$it\"" }
                }
                altQueries.add(altQuery)
            }
            return altQueries.joinToString(" OR ")
        }
    }
}