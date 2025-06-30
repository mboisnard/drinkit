package com.drinkit.event.sourcing

data class SequenceId(val value: Long = 0) : Comparable<SequenceId> {

    companion object {
        private const val INTERVAL_BETWEEN_SEQUENCES = 10
    }

    fun next() = SequenceId(value + INTERVAL_BETWEEN_SEQUENCES)

    override fun compareTo(other: SequenceId) = value.compareTo(other.value)
}