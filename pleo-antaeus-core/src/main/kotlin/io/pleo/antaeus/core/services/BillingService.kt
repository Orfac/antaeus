package io.pleo.antaeus.core.services

import dev.inmo.krontab.KronScheduler
import dev.inmo.krontab.doWhile
import io.pleo.antaeus.core.exceptions.CurrencyMismatchException
import io.pleo.antaeus.core.exceptions.CustomerNotFoundException
import io.pleo.antaeus.core.exceptions.NetworkException
import io.pleo.antaeus.core.external.PaymentProvider
import io.pleo.antaeus.models.Invoice
import io.pleo.antaeus.models.InvoiceStatus

class BillingService(
    private val paymentProvider: PaymentProvider,
    private val invoiceService: InvoiceService,
    private val customerService: CustomerService
) {

    private val failedInvoices: ArrayList<Invoice> = ArrayList()

    suspend fun init(scheduler: KronScheduler) {
        scheduler.doWhile {
            chargePayments()
            true // true - repeat on next time
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
                val paidInvoice = Invoice(
                    id = invoice.id,
                    customerId = invoice.customerId,
                    amount = invoice.amount,
                    status = InvoiceStatus.PAID
                )
                invoiceService.updateInvoice(paidInvoice)
            } else {
                val paidInvoice = Invoice(
                    id = invoice.id,
                    customerId = invoice.customerId,
                    amount = invoice.amount,
                    status = InvoiceStatus.PENDING
                )
                invoiceService.updateInvoice(paidInvoice)
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
