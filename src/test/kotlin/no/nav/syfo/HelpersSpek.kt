package no.nav.syfo

import io.kotest.core.spec.style.FunSpec
import kotlinx.coroutines.runBlocking
import no.nav.syfo.helpers.retry
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldThrow
import java.io.IOException

class HelpersSpek : FunSpec({

    context("Retries") {
        test("Returns result success") {
            val result = runBlocking {
                retry("test_call") {
                    "I'm OK"
                }
            }
            result shouldBeEqualTo "I'm OK"
        }
        test("Subclass of exception should be caught") {
            class SubIOException : IOException("Connection timed out")

            val result = runBlocking {
                var exceptionCount = 1
                retry("test_call") {
                    if (exceptionCount <= 0) {
                        "I'm OK"
                    } else {
                        exceptionCount--
                        throw SubIOException()
                    }
                }
            }
            result shouldBeEqualTo "I'm OK"
        }
        test("Returns result on single IOException") {
            var exceptionCount = 3
            val result = runBlocking {
                retry("test_call") {
                    if (exceptionCount <= 0) {
                        "I'm OK"
                    } else {
                        exceptionCount--
                        throw Exception(IOException("Connection timed out"))
                    }
                }
            }
            exceptionCount shouldBeEqualTo 0
            result shouldBeEqualTo "I'm OK"
        }
        test("Results in exception on non-retrying exceptions") {
            {
                runBlocking {
                    retry<Unit>("test_call") {
                        throw Exception("Unmapped")
                    }
                }
            } shouldThrow Exception::class
        }
    }
})
