package com.thanu.steady.domain

/** Missing observations and missing goals are distinct from measured zero. */
object ChartRules {
    fun progress(value: Double?, target: Double?): Float? {
        if (value == null || !value.isFinite() || value < 0 || target == null || !target.isFinite() || target <= 0) return null
        return (value / target).coerceIn(0.0, 1.0).toFloat()
    }

    fun habitStatus(scheduled: Boolean, suppressed: Boolean, recorded: String?, past: Boolean): String = when {
        !scheduled -> "NOT_DUE"
        suppressed && (recorded == null || recorded == "PENDING") -> "NOT_DUE"
        recorded == null || recorded == "PENDING" -> if (past) "MISSING" else "PENDING"
        else -> recorded
    }
}
