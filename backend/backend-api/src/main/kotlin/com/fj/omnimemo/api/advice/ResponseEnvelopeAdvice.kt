/*
 * ResponseEnvelopeAdvice.kt
 *
 * $Since: 2026-05-28T00:00:00Z
 */
package com.fj.omnimemo.api.advice

import com.fj.omnimemo.api.response.ResponseEnvelope
import jakarta.servlet.http.HttpServletResponse
import org.springframework.core.MethodParameter
import org.springframework.http.MediaType
import org.springframework.http.converter.HttpMessageConverter
import org.springframework.http.server.ServerHttpRequest
import org.springframework.http.server.ServerHttpResponse
import org.springframework.http.server.ServletServerHttpResponse
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.bind.annotation.RestControllerAdvice
import org.springframework.web.servlet.mvc.method.annotation.ResponseBodyAdvice
import java.time.Instant
import java.time.temporal.ChronoUnit

/**
 * Wraps every JSON response from [RestController] and [RestControllerAdvice] beans
 * in a [ResponseEnvelope], setting [ResponseEnvelope.Type.OK] for 2xx and [ResponseEnvelope.Type.ERR]
 * for all other status codes.
 *
 * View controllers returning [org.springframework.web.servlet.ModelAndView] are
 * unaffected because they bypass the message converter pipeline entirely.
 *
 * @author Francesco Jo
 * @since 0.1.1
 */
@RestControllerAdvice
class ResponseEnvelopeAdvice : ResponseBodyAdvice<Any> {
    override fun supports(
        returnType: MethodParameter,
        converterType: Class<out HttpMessageConverter<*>>,
    ): Boolean {
        val cls = returnType.containingClass
        if (cls.packageName.startsWith("org.springdoc")) return false
        return cls.isAnnotationPresent(RestController::class.java) ||
                cls.isAnnotationPresent(RestControllerAdvice::class.java)
    }

    override fun beforeBodyWrite(
        body: Any?,
        returnType: MethodParameter,
        selectedContentType: MediaType,
        selectedConverterType: Class<out HttpMessageConverter<*>>,
        request: ServerHttpRequest,
        response: ServerHttpResponse,
    ): Any? {
        if (body is ResponseEnvelope<*>) return body
        val status = (response as? ServletServerHttpResponse)
            ?.servletResponse?.status
            ?: HttpServletResponse.SC_OK
        val type = if (status in 200..299) ResponseEnvelope.Type.OK else ResponseEnvelope.Type.ERR
        return ResponseEnvelope(
            type = type,
            body = body,
            timestamp = Instant.now().truncatedTo(ChronoUnit.SECONDS).toString(),
        )
    }
}
