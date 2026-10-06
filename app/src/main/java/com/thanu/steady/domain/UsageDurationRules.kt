package com.thanu.steady.domain

data class UsageTransition(val packageName: String, val at: Long, val resumed: Boolean)
object UsageDurationRules {
    fun durations(start: Long, end: Long, events: List<UsageTransition>): Map<String, Long> {
        require(end >= start)
        val open = mutableMapOf<String, Long>(); val spans = mutableMapOf<String, MutableList<WallInterval>>()
        fun close(name: String, at: Long) {
            val begin = open.remove(name) ?: return
            val boundedStart = maxOf(begin, start); val boundedEnd = minOf(at, end)
            if(boundedEnd > boundedStart) spans.getOrPut(name) { mutableListOf() }.add(WallInterval(boundedStart, boundedEnd))
        }
        events.filter { it.at < end && it.packageName.isNotBlank() }.sortedBy { it.at }.forEach {
            if(it.resumed) open.putIfAbsent(it.packageName, it.at) else close(it.packageName, it.at)
        }
        open.keys.toList().forEach { close(it, end) }
        return spans.mapValues { ActivityTotals.unionMillis(it.value) }
    }
}
