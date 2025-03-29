package dev.andrewohara.rhttp

import org.http4k.base64DecodedArray
import org.http4k.base64Encode
import org.http4k.core.*
import org.http4k.format.Json
import org.http4k.format.JsonType
import java.util.*

private const val CLIENT_ID = "clientId"
private const val REQUEST_ID = "requestId"
private const val METHOD = "method"
private const val URI = "uri"
private const val STATUS_CODE = "statusCode"
private const val STATUS_REASON = "statusReason"
private const val HEADERS = "headers"
private const val NAME = "name"
private const val VALUE = "value"
private const val BODY_BASE_64 = "bodyBase64"

data class RedisHttpMessage(
    val clientId: String,
    val requestId: String,
    val method: Method?,
    val uri: String?,
    val statusCode: Int?,
    val statusReason: String?,
    val headers: Headers,
    val bodyBase64: String
) {
    constructor(request: Request, clientId: String, requestId: String = UUID.randomUUID().toString()): this(
        clientId = clientId,
        requestId = requestId,
        method = request.method,
        uri = request.uri.toString(),
        statusCode = null,
        statusReason = null,
        headers = request.headers,
        bodyBase64 = request.body.stream.readAllBytes().base64Encode()
    )

    constructor(response: Response, clientId: String, requestId: String): this(
        clientId = clientId,
        requestId = requestId,
        method = null,
        uri = null,
        statusCode = response.status.code,
        statusReason = response.status.description,
        headers = response.headers,
        bodyBase64 = response.body.stream.readAllBytes().base64Encode()
    )

    companion object
}

fun RedisHttpMessage.toRequest() = Request(method!!, uri!!)
    .headers(headers)
    .body(MemoryBody(bodyBase64.base64DecodedArray()))

fun RedisHttpMessage.toResponse() = Response(Status(statusCode!!, statusReason))
    .headers(headers)
    .body(MemoryBody(bodyBase64.base64DecodedArray()))

fun <NODE> RedisHttpMessage.Companion.parse(message: String, json: Json<NODE>): RedisHttpMessage {
    val node = json.parse(message)

    val props = json {
        fields(node).associate { (name, value) ->
            name to when (name) {
                CLIENT_ID -> text(value)
                REQUEST_ID -> text(value)
                METHOD -> textOrNull(value)?.let { Method.valueOf(it) }
                URI -> textOrNull(value)
                STATUS_CODE -> integerOrNull(value)
                STATUS_REASON -> textOrNull(value)
                HEADERS -> elements(value).map { headers ->
                    fields(headers)
                        .associate { (objName, objValue) -> objName to text(objValue) }
                        .let { it[NAME] to it[VALUE] }
                }.toList()
                BODY_BASE_64 -> textOrNull(value)
                else -> null
            }
        }
    }

    return RedisHttpMessage(
        clientId = props[CLIENT_ID] as String,
        requestId = props[REQUEST_ID] as String,
        method = props[METHOD] as Method?,
        uri = props[URI] as String?,
        statusCode = props[STATUS_CODE] as Int?,
        statusReason = props[STATUS_REASON] as String?,
        headers = props[HEADERS] as Headers,
        bodyBase64 = props[BODY_BASE_64] as String
    )
}

fun <NODE> RedisHttpMessage.toJson(json: Json<NODE>): String = json {
    buildList {
        this += CLIENT_ID to string(clientId)
        this += REQUEST_ID to string(requestId)

        if (method != null) {
            this += METHOD to string(method.toString())
        }

        if (uri != null) {
            this += URI to string(uri.toString())
        }

        if (statusCode != null) {
            this += STATUS_CODE to number(statusCode)
        }

        if (statusReason != null) {
            this += STATUS_REASON to string(statusReason)
        }

        this += HEADERS to array(headers.map { (name, value) ->
            obj(
                NAME to string(name),
                VALUE to if (value == null) nullNode() else string(value)
            )
        })

        this += BODY_BASE_64 to string(bodyBase64)
    }
        .let(::obj)
        .let(::compact)
}

private fun <NODE> Json<NODE>.textOrNull(node: NODE) = when(typeOf(node)) {
    JsonType.String -> text(node)
    else -> null
}

private fun <NODE> Json<NODE>.integerOrNull(node: NODE) = when(typeOf(node)) {
    JsonType.Number -> integer(node).toInt()
    JsonType.Integer -> integer(node).toInt()
    else -> null
}