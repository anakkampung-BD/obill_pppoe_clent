package com.ribminet.obill.data.remote

sealed class ApiResult<out T> {
    data class Ok<T>(val data: T) : ApiResult<T>()
    data class Err(
        val message: String,
        val code: String? = null,
        val httpCode: Int? = null,
        val retryAfterSeconds: Int? = null,
        val remainingAttempts: Int? = null,
    ) : ApiResult<Nothing>()
}

inline fun <T> ApiResult<T>.onOk(block: (T) -> Unit): ApiResult<T> {
    if (this is ApiResult.Ok) block(data)
    return this
}

inline fun <T> ApiResult<T>.onErr(block: (ApiResult.Err) -> Unit): ApiResult<T> {
    if (this is ApiResult.Err) block(this)
    return this
}
