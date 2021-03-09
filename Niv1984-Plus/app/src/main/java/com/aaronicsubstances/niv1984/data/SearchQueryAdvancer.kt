package com.aaronicsubstances.niv1984.data

class SearchQueryAdvancer(query: String) {
    private val terms: List<String>
    var currentQuery: String
        private set

    // state
    private var treatAsExact: Boolean

    init {
        terms = splitUserQuery(query)
        treatAsExact = true
        currentQuery = transformUserQuery(terms, treatAsExact)
    }

    fun advance(): Boolean {
        if (treatAsExact) {
            treatAsExact = false
        }
        else {
            return false
        }
        currentQuery = transformUserQuery(terms, treatAsExact)
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
            terms: List<String>, treatAsExact: Boolean
        ): String {
            if (terms.isEmpty()) {
                return ""
            }
            // quote terms as phrases or else lowercase them to prevent clash with keywords
            val altQuery = if (treatAsExact) {
                "\"" + terms
                    .joinToString(" ") + "\""
            }
            else {
                terms
                    .joinToString(" NEAR/3 ") { "\"$it\"" }
            }
            return altQuery
        }
    }
}