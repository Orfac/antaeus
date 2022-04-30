package io.pleo.antaeus.core.services

import dev.inmo.krontab.doWhile
import io.pleo.antaeus.core.SchedulerConfiguration
import io.pleo.antaeus.core.exceptions.CurrencyMismatchException
import io.pleo.antaeus.core.exceptions.CustomerNotFoundException
import io.pleo.antaeus.core.exceptions.NetworkException
import io.pleo.antaeus.core.external.PaymentProvider
import io.pleo.antaeus.data.AntaeusDal
import io.pleo.antaeus.models.Invoice
import io.pleo.antaeus.models.InvoiceStatus
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import java.util.concurrent.ConcurrentLinkedQueue

class BillingService(
    private val paymentProvider: PaymentProvider,
    private val dal: AntaeusDal
) {

    private val failedTransactionInvoices: ArrayList<Invoice> = ArrayList()
    private val failedNetworkInvoices: ArrayList<Invoice> = ArrayList()
    private val invoices: ConcurrentLinkedQueue<Invoice> = ConcurrentLinkedQueue()

    @OptIn(DelicateCoroutinesApi::class)
    suspend fun init(schedulerConfiguration: SchedulerConfiguration) {
        val (paymentScheduler, networkFailuresHandler, transactionFailuresHandler) = schedulerConfiguration
        GlobalScope.launch {
            paymentScheduler.doWhile {
                chargePayments()
                true
            }
//            networkFailuresHandler.doWhile {
//                chargePayments(failedNetworkInvoices)
//                true
//            }
//            transactionFailuresHandler.doWhile {
//                chargePayments(failedTransactionInvoices)
//                true
//            }
        }
    }

    private fun chargePayments(invoices: List<Invoice> = arrayListOf()) {
        if (invoices.isEmpty()) {
            val invoicesNew = dal.fetchInvoices()
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
                dal.updateInvoice(paidInvoice)
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
