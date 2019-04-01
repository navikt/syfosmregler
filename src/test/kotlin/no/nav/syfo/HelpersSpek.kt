package no.nav.syfo

import kotlinx.coroutines.runBlocking
import no.nav.syfo.helpers.isCausedBy
import no.nav.syfo.helpers.retry
import org.amshove.kluent.shouldEqual
import org.amshove.kluent.shouldThrow
import org.spekframework.spek2.Spek
import org.spekframework.spek2.style.specification.describe
import java.io.IOException
import java.lang.RuntimeException

object HelpersSpek : Spek({
    // TODO: Move this to the helper library
    describe("Exception-cause crawler") {
        it("Should find a IOException in a nested Exception") {
            isCausedBy(Exception(IOException("Connection timed out")), 3, arrayOf(IOException::class)) shouldEqual true
        }
        it("Should find not a IOException in a nested Exception") {
            isCausedBy(Exception(IOException("Connection timed out")), 3, arrayOf(RuntimeException::class)) shouldEqual false
        }
        it("Should not find a IOException whenever the cause stack is too deep") {
            isCausedBy(Exception(Exception(Exception(IOException("Connection timed out")))), 3, arrayOf(IOException::class)) shouldEqual false
        }
        it("Should find a IOException whenever the cause stack is 3 deep") {
            isCausedBy(Exception(Exception(IOException("Connection timed out"))), 3, arrayOf(IOException::class)) shouldEqual true
        }
    }

    describe("Retries") {
        it("Returns result success") {
            val result = runBlocking {
                retry("test_call") {
                    "I'm OK"
                }
            }
            result shouldEqual "I'm OK"
        }
        it("Subclass of exception should be caught") {
            class SubIOException : IOException("Connection timed out")
            val result = runBlocking {
                var exceptionCount = 1
                retry("test_call") {
                    if (exceptionCount <= 0) {
                        "I'm OK"
                    } else {
                        exceptionCount --
                        throw SubIOException()
                    }
                }
            }
            result shouldEqual "I'm OK"
        }
        it("Returns result on single IOException") {
            var exceptionCount = 3
            val result = runBlocking {
                retry("test_call") {
                    if (exceptionCount <= 0) {
                        "I'm OK"
                    } else {
                        exceptionCount --
                        throw Exception(IOException("Connection timed out"))
                    }
                }
            }
            exceptionCount shouldEqual 0
            result shouldEqual "I'm OK"
        }
        it("Results in exception on non-retrying exceptions") {
            { runBlocking {
                retry<Unit>("test_call") {
                    throw Exception("Unmapped")
                }
            } } shouldThrow Exception::class
        }
    }
})
