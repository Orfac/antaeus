package io.pleo.antaeus.core.services

import dev.inmo.krontab.KronScheduler
import dev.inmo.krontab.doWhile
import io.pleo.antaeus.core.exceptions.CurrencyMismatchException
import io.pleo.antaeus.core.exceptions.CustomerNotFoundException
import io.pleo.antaeus.core.exceptions.NetworkException
import io.pleo.antaeus.core.external.PaymentProvider
import io.pleo.antaeus.models.Invoice
import io.pleo.antaeus.models.InvoiceStatus
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch

class BillingService(
    private val paymentProvider: PaymentProvider,
    private val invoiceService: InvoiceService,
    private val customerService: CustomerService
) {

    private val failedInvoices: ArrayList<Invoice> = ArrayList()

    suspend fun init(scheduler: KronScheduler) {
        coroutineScope {
            launch {
                scheduler.doWhile {
                    chargePayments()
                    true // true - repeat on next time
                }
            }
        }
    }

    private fun chargePayments() {
        val invoices = invoiceService.fetchAll();
        invoices.forEach { processInvoice(it) }
    }

    private fun processInvoice(invoice: Invoice) {
        try {
            val result = paymentProvider.charge(invoice)
            if (result) {
                val paidInvoice = invoice.copy(status = InvoiceStatus.PAID)
                invoiceService.updateInvoice(paidInvoice)
            } else {
                failedInvoices.add(invoice)
            }
        } catch (customerNotFoundException: CustomerNotFoundException) {
            return
        } catch (currencyMismatchException: CurrencyMismatchException) {
            return
        } catch (networkException: NetworkException) {
            failedInvoices.add(invoice)
        }
    }


}
