package com.priorizedev.brizza.data.api

import retrofit2.http.*
import retrofit2.Response
import com.priorizedev.brizza.data.model.*

data class SignUpRequestBody(
    val email: String,
    val password: String
)

interface SupabaseApi {
    @POST("auth/v1/signup")
    suspend fun signUp(
        @Header("apikey") apiKey: String,
        @Header("Authorization") authHeader: String,
        @Body body: SignUpRequestBody
    ): Response<okhttp3.ResponseBody>

    @POST("auth/v1/token")
    suspend fun signIn(
        @Header("apikey") apiKey: String,
        @Header("Authorization") authHeader: String,
        @Query("grant_type") grantType: String,
        @Body body: SignUpRequestBody
    ): Response<okhttp3.ResponseBody>

    @GET("rest/v1/usuarios")
    suspend fun getUsuarioByEmail(
        @Header("apikey") apiKey: String,
        @Header("Authorization") authHeader: String,
        @Query("email") emailFilter: String
    ): Response<List<Usuario>>

    @POST("rest/v1/clientes")
    @Headers("Prefer: resolution=merge-duplicates")
    suspend fun upsertClientes(
        @Header("apikey") apiKey: String,
        @Header("Authorization") authHeader: String,
        @Body body: List<Cliente>
    ): Response<Unit>

    @POST("rest/v1/ambientes")
    @Headers("Prefer: resolution=merge-duplicates")
    suspend fun upsertAmbientes(
        @Header("apikey") apiKey: String,
        @Header("Authorization") authHeader: String,
        @Body body: List<Ambiente>
    ): Response<Unit>

    @POST("rest/v1/equipamentos")
    @Headers("Prefer: resolution=merge-duplicates")
    suspend fun upsertEquipamentos(
        @Header("apikey") apiKey: String,
        @Header("Authorization") authHeader: String,
        @Body body: List<Equipamento>
    ): Response<Unit>

    @POST("rest/v1/tecnicos")
    @Headers("Prefer: resolution=merge-duplicates")
    suspend fun upsertTecnicos(
        @Header("apikey") apiKey: String,
        @Header("Authorization") authHeader: String,
        @Body body: List<Tecnico>
    ): Response<Unit>

    @POST("rest/v1/ordens_servico")
    @Headers("Prefer: resolution=merge-duplicates")
    suspend fun upsertOrdensServico(
        @Header("apikey") apiKey: String,
        @Header("Authorization") authHeader: String,
        @Body body: List<OrdemServico>
    ): Response<Unit>

    @POST("rest/v1/pmoc_reports")
    @Headers("Prefer: resolution=merge-duplicates")
    suspend fun upsertPmocReports(
        @Header("apikey") apiKey: String,
        @Header("Authorization") authHeader: String,
        @Body body: List<PmocReport>
    ): Response<Unit>

    @POST("rest/v1/pmoc_logbook_entries")
    @Headers("Prefer: resolution=merge-duplicates")
    suspend fun upsertPmocLogbookEntries(
        @Header("apikey") apiKey: String,
        @Header("Authorization") authHeader: String,
        @Body body: List<PmocLogbookEntry>
    ): Response<Unit>

    @POST("rest/v1/usuarios")
    @Headers("Prefer: resolution=merge-duplicates")
    suspend fun upsertUsuarios(
        @Header("apikey") apiKey: String,
        @Header("Authorization") authHeader: String,
        @Body body: List<Usuario>
    ): Response<Unit>

    @POST("rest/v1/marcas_equipamentos")
    @Headers("Prefer: resolution=merge-duplicates")
    suspend fun upsertMarcasEquipamentos(
        @Header("apikey") apiKey: String,
        @Header("Authorization") authHeader: String,
        @Body body: List<MarcaEquipamento>
    ): Response<Unit>

    @GET("rest/v1/marcas_equipamentos")
    suspend fun getMarcasEquipamentos(
        @Header("apikey") apiKey: String,
        @Header("Authorization") authHeader: String,
        @Query("or") orFilter: String
    ): Response<List<MarcaEquipamento>>

    @GET("rest/v1/clientes")
    suspend fun getClientes(
        @Header("apikey") apiKey: String,
        @Header("Authorization") authHeader: String,
        @Query("usuario_email") emailFilter: String
    ): Response<List<Cliente>>

    @GET("rest/v1/ambientes")
    suspend fun getAmbientes(
        @Header("apikey") apiKey: String,
        @Header("Authorization") authHeader: String,
        @Query("usuario_email") emailFilter: String
    ): Response<List<Ambiente>>

    @GET("rest/v1/equipamentos")
    suspend fun getEquipamentos(
        @Header("apikey") apiKey: String,
        @Header("Authorization") authHeader: String,
        @Query("usuario_email") emailFilter: String
    ): Response<List<Equipamento>>

    @GET("rest/v1/tecnicos")
    suspend fun getTecnicos(
        @Header("apikey") apiKey: String,
        @Header("Authorization") authHeader: String,
        @Query("usuario_email") emailFilter: String
    ): Response<List<Tecnico>>

    @GET("rest/v1/ordens_servico")
    suspend fun getOrdensServico(
        @Header("apikey") apiKey: String,
        @Header("Authorization") authHeader: String,
        @Query("usuario_email") emailFilter: String
    ): Response<List<OrdemServico>>

    @GET("rest/v1/pmoc_reports")
    suspend fun getPmocReports(
        @Header("apikey") apiKey: String,
        @Header("Authorization") authHeader: String,
        @Query("usuario_email") emailFilter: String
    ): Response<List<PmocReport>>

    @GET("rest/v1/pmoc_logbook_entries")
    suspend fun getPmocLogbookEntries(
        @Header("apikey") apiKey: String,
        @Header("Authorization") authHeader: String
        // logbook entries can be synced globally or fetched if loaded
    ): Response<List<PmocLogbookEntry>>

    @GET("rest/v1/pecas")
    suspend fun getPecas(
        @Header("apikey") apiKey: String,
        @Header("Authorization") authHeader: String,
        @Query("usuario_email") emailFilter: String
    ): Response<List<Peca>>

    @POST("rest/v1/pecas")
    @Headers("Prefer: resolution=merge-duplicates")
    suspend fun upsertPecas(
        @Header("apikey") apiKey: String,
        @Header("Authorization") authHeader: String,
        @Body body: List<Peca>
    ): Response<Unit>

    @GET("rest/v1/programas_preventivos")
    suspend fun getProgramasPreventivos(
        @Header("apikey") apiKey: String,
        @Header("Authorization") authHeader: String,
        @Query("usuario_email") emailFilter: String
    ): Response<List<ProgramaPreventivo>>

    @POST("rest/v1/programas_preventivos")
    @Headers("Prefer: resolution=merge-duplicates")
    suspend fun upsertProgramasPreventivos(
        @Header("apikey") apiKey: String,
        @Header("Authorization") authHeader: String,
        @Body body: List<ProgramaPreventivo>
    ): Response<Unit>

    @POST("storage/v1/object/fotos_tecnicas/{path}")
    suspend fun uploadFotoTecnica(
        @Header("apikey") apiKey: String,
        @Header("Authorization") authHeader: String,
        @Header("Content-Type") contentType: String,
        @Path("path") path: String,
        @Body fileBody: okhttp3.RequestBody
    ): Response<okhttp3.ResponseBody>

    @DELETE("rest/v1/clientes")
    suspend fun deleteCliente(
        @Header("apikey") apiKey: String,
        @Header("Authorization") authHeader: String,
        @Query("id") idFilter: String
    ): Response<Unit>

    @DELETE("rest/v1/ambientes")
    suspend fun deleteAmbiente(
        @Header("apikey") apiKey: String,
        @Header("Authorization") authHeader: String,
        @Query("id") idFilter: String
    ): Response<Unit>

    @DELETE("rest/v1/equipamentos")
    suspend fun deleteEquipamento(
        @Header("apikey") apiKey: String,
        @Header("Authorization") authHeader: String,
        @Query("id") idFilter: String
    ): Response<Unit>

    @DELETE("rest/v1/tecnicos")
    suspend fun deleteTecnico(
        @Header("apikey") apiKey: String,
        @Header("Authorization") authHeader: String,
        @Query("id") idFilter: String
    ): Response<Unit>

    @DELETE("rest/v1/ordens_servico")
    suspend fun deleteOrdemServico(
        @Header("apikey") apiKey: String,
        @Header("Authorization") authHeader: String,
        @Query("id") idFilter: String
    ): Response<Unit>

    @DELETE("rest/v1/pmoc_reports")
    suspend fun deletePmocReport(
        @Header("apikey") apiKey: String,
        @Header("Authorization") authHeader: String,
        @Query("id") idFilter: String
    ): Response<Unit>

    @DELETE("rest/v1/pecas")
    suspend fun deletePeca(
        @Header("apikey") apiKey: String,
        @Header("Authorization") authHeader: String,
        @Query("id") idFilter: String
    ): Response<Unit>

    @DELETE("rest/v1/programas_preventivos")
    suspend fun deleteProgramaPreventivo(
        @Header("apikey") apiKey: String,
        @Header("Authorization") authHeader: String,
        @Query("id") idFilter: String
    ): Response<Unit>

    @DELETE("rest/v1/marcas_equipamentos")
    suspend fun deleteMarcaEquipamento(
        @Header("apikey") apiKey: String,
        @Header("Authorization") authHeader: String,
        @Query("id") idFilter: String
    ): Response<Unit>

    @GET("rest/v1/log_versao")
    suspend fun getLogsVersao(
        @Header("apikey") apiKey: String,
        @Header("Authorization") authHeader: String,
        @Query("order") order: String = "id.desc"
    ): Response<List<LogVersao>>
}
