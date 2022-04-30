package io.pleo.antaeus.core.services

import dev.inmo.krontab.builder.buildSchedule
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import io.pleo.antaeus.core.SchedulerConfiguration
import io.pleo.antaeus.core.external.PaymentProvider
import io.pleo.antaeus.data.AntaeusDal
import io.pleo.antaeus.models.Currency
import io.pleo.antaeus.models.Invoice
import io.pleo.antaeus.models.InvoiceStatus
import io.pleo.antaeus.models.Money
import kotlinx.coroutines.*
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import kotlin.random.Random

@OptIn(DelicateCoroutinesApi::class)
@ExperimentalCoroutinesApi
class BillingServiceTest {
    private val mockPaymentProvider: PaymentProvider = object : PaymentProvider {
        override fun charge(invoice: Invoice): Boolean {
            return true
        }
    }
    private val testSchedulerConfiguration = getTestSchedulerConfiguration()
    private val mainThreadSurrogate = newSingleThreadContext("TestThread")

    @BeforeEach
    fun setUp() {
        Dispatchers.setMain(mainThreadSurrogate)
    }


    @Test
    fun `payment is paid`() {
        val observableInvoice = testInvoice()
        val dal = mockk<AntaeusDal> {
            every { fetchInvoices() } returns listOf(observableInvoice)
            every { updateInvoice(any()) } answers {}
        }
        val billingService = BillingService(mockPaymentProvider, dal)

        runTest {
            billingService.init(testSchedulerConfiguration)
        }
        Thread.sleep(3000)

        observableInvoice.status = InvoiceStatus.PAID
        verify { dal.updateInvoice(observableInvoice) }
//        assertEquals(observableInvoice.status, InvoiceStatus.PAID)

    }

    private fun testInvoice(): Invoice {
        return Invoice(1, 1, Money(BigDecimal(1.0), Currency.EUR), InvoiceStatus.PENDING)
    }

    private fun getTestSchedulerConfiguration(): SchedulerConfiguration {
        val cron = buildSchedule {
            seconds {
                0 to 1
            }
        }
        val cron2 = buildSchedule {
            seconds {
                0 to 1
            }
        }

        val cron3 = buildSchedule {
            seconds {
                0 to 1
            }
        }
        return SchedulerConfiguration(cron, cron2, cron3)
    }
}