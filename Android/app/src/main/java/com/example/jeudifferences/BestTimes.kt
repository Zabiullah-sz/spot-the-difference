package com.example.jeudifferences

import kotlinx.serialization.Serializable

@Serializable
data class BestTimes(val firstPlace: BestTime, val secondPlace: BestTime, val thirdPlace: BestTime)
