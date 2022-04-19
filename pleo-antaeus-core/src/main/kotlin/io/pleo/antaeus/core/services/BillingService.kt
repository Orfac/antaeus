package io.pleo.antaeus.core.services

import dev.inmo.krontab.doWhile
import io.pleo.antaeus.core.SchedulerConfiguration
import io.pleo.antaeus.core.exceptions.CurrencyMismatchException
import io.pleo.antaeus.core.exceptions.CustomerNotFoundException
import io.pleo.antaeus.core.exceptions.NetworkException
import io.pleo.antaeus.core.external.PaymentProvider
import io.pleo.antaeus.models.Invoice
import io.pleo.antaeus.models.InvoiceStatus
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch

class BillingService(
    private val paymentProvider: PaymentProvider,
    private val invoiceService: InvoiceService,
    private val customerService: CustomerService
) {

    private val failedTransactionInvoices: ArrayList<Invoice> = ArrayList()
    private val failedNetworkInvoices: ArrayList<Invoice> = ArrayList()

    suspend fun init(schedulerConfiguration: SchedulerConfiguration) {
        val (paymentScheduler, networkFailuresHandler, transactionFailuresHandler) = schedulerConfiguration
        coroutineScope {
            launch {
                paymentScheduler.doWhile {
                    chargePayments()
                    true // true - repeat on next time
                }
                networkFailuresHandler.doWhile {
                    chargePayments(failedNetworkInvoices)
                    true // true - repeat on next time
                }
                transactionFailuresHandler.doWhile {
                    chargePayments(failedTransactionInvoices)
                    true // true - repeat on next time
                }
            }
        }
    }

    private fun chargePayments(invoices: List<Invoice> = arrayListOf()) {
        if (invoices.isEmpty()) {
            val invoicesNew = invoiceService.fetchAll()
            invoicesNew.forEach { processInvoice(it) }
        } else {
            invoices.forEach { processInvoice(it) }
        }
    }

    private fun processInvoice(invoice: Invoice) {
        try {
            val result = paymentProvider.charge(invoice)
            if (result) {
                val paidInvoice = invoice.copy(status = InvoiceStatus.PAID)
                invoiceService.updateInvoice(paidInvoice)
            } else {
                failedTransactionInvoices.add(invoice)
            }
        } catch (customerNotFoundException: CustomerNotFoundException) {
            return
        } catch (currencyMismatchException: CurrencyMismatchException) {
            return
        } catch (networkException: NetworkException) {
            failedNetworkInvoices.add(invoice)
        }
    }


}
