package com.example.jeudifferences

import kotlinx.serialization.Serializable

@Serializable
data class BestTime(val name: String, val time: TimeConcept)
