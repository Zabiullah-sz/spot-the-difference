package com.example.jeudifferences

import okhttp3.MultipartBody
import okhttp3.RequestBody
import okhttp3.ResponseBody
import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.Part
import retrofit2.http.Path


interface UserApi {
    @Multipart
    @POST("users/upload-profile-image")
    fun uploadProfileImage(@Part("userId") userId: RequestBody, @Part profileImage: MultipartBody.Part): Call<UploadResponse>


    @POST("users/signup")
    fun signUp(@Body request: SignupRequest): Call<SignupResponse>

    @POST("users/login")
    fun login(@Body request: LoginRequest): Call<LoginResponse>

    @POST("users/forgot-password/{email}")
    fun forgotPassword(@Path("email") email: String): Call<EmailResponse>

    @POST("users/update-password")
    fun resetPassword(@Body request: ResetPasswordRequest): Call<ResetPasswordResponse>

    @GET("users/friend-requests/{userId}")
    fun getFriendRequests(@Path("userId") userId: String): Call<ArrayList<FriendRequest>>

    @GET("users/logout")
    fun logout(): Call<LogoutResponse>

    @POST("users/change-username")
    fun changeUsername(@Body request: ChangUserNameRequest): Call<ChangUserNameResponse>

    @GET("users/get-public-profile-image/{userId}")
    fun getPublicProfileImage(@Path("userId") userId: String): Call<ResponseBody>
}

data class ChangUserNameRequest(val userId: String, val newUsername:String)
data class ChangUserNameResponse( val status: String, val msg: String)
data class ResetPasswordRequest(val token: String, val newPassword: String)

data class LogoutResponse(val msg: String)
data class ResetPasswordResponse(
    val status: String,
    val msg: String,
)

data class EmailResponse(
    val status: String,
    val msg: String,
)
data class SignupRequest(val password: String, val username: String,val email:String)
data class LoginRequest(val username: String, val password: String)
data class SignupResponse(val msg: String, val userId: String, val username: String)
data class LoginResponse(val user: User, val msg: String)
data class UploadResponse(
    val status: String,
    val msg: String
)
 // Adjust fields as per your API response

