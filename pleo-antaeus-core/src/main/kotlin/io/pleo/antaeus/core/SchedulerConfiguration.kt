package io.pleo.antaeus.core

import dev.inmo.krontab.KronScheduler

data class SchedulerConfiguration(
    val paymentScheduler: KronScheduler,
    val networkFailuresHandler: KronScheduler,
    val transactionFailuresHandler: KronScheduler
) {
}