package com.example.expensetracker.ui.network

import com.google.android.gms.common.api.Response
import retrofit2.http.*

interface ApiService {
    @POST("/api/users/signup")
    suspend fun signup(@Body request: SignupRequest): ApiResponse<User>

    @POST("/api/users/login")
    suspend fun login(@Body request: LoginRequest): ApiResponse<User>

    @GET("/api/users/me")
    suspend fun getCurrentUser(@Header("Authorization") token: String): User

    @POST("/api/expenses")
    suspend fun addExpense(
        @Header("Authorization") token: String,
        @Body expense: ExpenseRequest
    ): ApiResponse<Expense>

    @GET("/api/expenses/summary")
    suspend fun getExpenseSummary(
        @Header("Authorization") token: String,
        @Query("start") start: String? = null,
        @Query("end") end: String? = null
    ): Map<String, Any>

    @POST("/api/expenses/balance")
    suspend fun setBalance(
        @Header("Authorization") token: String,
        @Body body: Map<String, Double>
    ): Map<String, Any>

    @GET("/api/expenses/balance")
    suspend fun getBalance(
        @Header("Authorization") token: String
    ): Map<String, Any>

    @GET("api/expenses")
    suspend fun getExpenses(
        @Header("Authorization") token: String,
        @Query("start") start: String? = null,
        @Query("end") end: String? = null
    ): Map<String, Any>

    @POST("/api/chat")
    suspend fun chat(
        @Header("Authorization") token: String,
        @Body req: ChatRequest
    ): ChatResponse


}
