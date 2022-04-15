package io.pleo.antaeus.core.services

import dev.inmo.krontab.KronScheduler
import dev.inmo.krontab.doWhile
import io.pleo.antaeus.core.external.PaymentProvider

class BillingService(
    private val paymentProvider: PaymentProvider,
    private val invoiceService: InvoiceService,
    private val customerService: CustomerService
) {

    suspend fun init(scheduler: KronScheduler){
        scheduler.doWhile {
            chargePayments()
            true // true - repeat on next time
        }
    }
    fun chargePayments() {
        val invoices = invoiceService.fetchAll();
        invoices.forEach { paymentProvider.charge(it) }
    }
}
