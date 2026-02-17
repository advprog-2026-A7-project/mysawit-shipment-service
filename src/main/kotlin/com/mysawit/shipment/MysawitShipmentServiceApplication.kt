package com.mysawit.shipment

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication
class MysawitShipmentServiceApplication

fun main(args: Array<String>) {
    val context = runApplication<MysawitShipmentServiceApplication>(*args)
    if (context.environment.getProperty("app.test.close-context", Boolean::class.java, false)) {
        context.close()
    }
}
