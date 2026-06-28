package com.priorizedev.brizza.util

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.pdf.PdfDocument
import android.net.Uri
import android.util.Base64
import android.widget.Toast
import androidx.core.content.FileProvider
import com.priorizedev.brizza.data.model.Ambiente
import com.priorizedev.brizza.data.model.Cliente
import com.priorizedev.brizza.data.model.Equipamento
import com.priorizedev.brizza.data.model.OrdemServico
import com.priorizedev.brizza.data.model.PmocReport
import com.priorizedev.brizza.data.model.Tecnico
import com.priorizedev.brizza.data.model.PmocLogbookEntry
import com.priorizedev.brizza.ui.screens.deserializeSelectedPecas
import com.priorizedev.brizza.ui.screens.serializeSelectedPecas
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object ReportUtils {

    fun base64ToBitmap(base64Str: String): Bitmap? {
        if (base64Str.isEmpty()) return null
        return try {
            val cleaned = base64Str.substringAfter(",") // bypass any data:image/png;base64 header
            val decodedBytes = Base64.decode(cleaned, Base64.DEFAULT)
            BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.size)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    fun bitmapToBase64(bitmap: Bitmap): String {
        val outputStream = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.PNG, 85, outputStream)
        val byteArray = outputStream.toByteArray()
        return Base64.encodeToString(byteArray, Base64.NO_WRAP)
    }

    private fun formatDate(timestamp: Long): String {
        val sdf = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
        return sdf.format(Date(timestamp))
    }

    private fun formatNumeroOrdem(numero: String): String {
        val digits = numero.filter { it.isDigit() }
        return if (digits.isNotEmpty()) {
            val value = digits.toLongOrNull() ?: 1L
            val formatted = String.format(Locale.getDefault(), "%05d", value % 100000)
            "${formatted.substring(0, 4)}-${formatted.substring(4)}"
        } else {
            "0000-0"
        }
    }

    fun generateOrdemServicoPdf(
        context: Context,
        os: OrdemServico,
        cliente: Cliente,
        tecnico: Tecnico?,
        equipamento: Equipamento?,
        ambiente: Ambiente? = null
    ): File? {
        val sharedPrefs = context.getSharedPreferences("climagest_prefs", Context.MODE_PRIVATE)
        val companyName = sharedPrefs.getString("company_name", "Ar-Control Climatização") ?: "Ar-Control Climatização"
        val companyCnpj = sharedPrefs.getString("company_cnpj", "12.345.678/0001-90") ?: "12.345.678/0001-90"
        val companyPhone = sharedPrefs.getString("company_phone", "(11) 98765-4321") ?: "(11) 98765-4321"
        val companyEmail = sharedPrefs.getString("company_email", "contato@arcontrol.com.br") ?: "contato@arcontrol.com.br"

        val pdfDocument = PdfDocument()
        val paintText = Paint().apply {
            isAntiAlias = true
            color = Color.parseColor("#263238")
            textSize = 10f
        }
        val paintLine = Paint().apply {
            color = Color.parseColor("#CFD8DC")
            strokeWidth = 1f
        }
        val paintAccent = Paint().apply {
            color = Color.parseColor("#1E3A8A") // Rich Navy Blue
            isAntiAlias = true
        }

        var pageNum = 1
        var pageInfo = PdfDocument.PageInfo.Builder(595, 842, pageNum).create() // A4 Size
        var page = pdfDocument.startPage(pageInfo)
        var canvas = page.canvas

        fun drawHeader() {
            // Rounded header card with nice margin to avoid printer clipping
            val headerRect = RectF(30f, 25f, 565f, 95f)
            canvas.drawRoundRect(headerRect, 8f, 8f, paintAccent)

            paintText.color = Color.WHITE
            paintText.textSize = 16f
            paintText.isFakeBoldText = true
            val displayTitle = if (companyName.length > 30) companyName.take(28) + "..." else companyName
            canvas.drawText(displayTitle, 45f, 56f, paintText)

            paintText.textSize = 9.5f
            paintText.isFakeBoldText = false
            paintText.color = Color.parseColor("#93C5FD") // soft light blue
            canvas.drawText("Ordem de Serviço Digital | CNPJ: $companyCnpj", 45f, 78f, paintText)

            paintText.color = Color.WHITE
            paintText.textSize = 11f
            paintText.isFakeBoldText = true
            canvas.drawText("Nº OS: ${formatNumeroOrdem(os.numeroOrdem)}", 430f, 54f, paintText)

            paintText.textSize = 9.5f
            paintText.isFakeBoldText = false
            paintText.color = Color.parseColor("#93C5FD")
            canvas.drawText("Gerado em: ${formatDate(os.dataCriacao)}", 410f, 76f, paintText)

            // Reset Paint
            paintText.color = Color.parseColor("#263238")
            paintText.isFakeBoldText = false
            paintText.textSize = 10f
        }

        drawHeader()
        var y = 130f

        // Helper to check for space and dynamically spawn new pages
        fun checkAndCreateNewPage(neededHeight: Float) {
            if (y + neededHeight > 785f) {
                pdfDocument.finishPage(page)
                pageNum++
                pageInfo = PdfDocument.PageInfo.Builder(595, 842, pageNum).create()
                page = pdfDocument.startPage(pageInfo)
                canvas = page.canvas
                drawHeader()
                y = 130f
            }
        }

        fun drawSectionTitle(title: String) {
            val titlePaint = Paint().apply {
                color = Color.parseColor("#1E3A8A")
                textSize = 10.5f
                isFakeBoldText = true
                isAntiAlias = true
            }
            val titleBgPaint = Paint().apply {
                color = Color.parseColor("#F0F6FC")
            }
            val accentBarPaint = Paint().apply {
                color = Color.parseColor("#1E3A8A")
            }
            // Left margin = 30f, Right margin = 565f
            canvas.drawRoundRect(RectF(30f, y - 16f, 565f, y + 4f), 4f, 4f, titleBgPaint)
            canvas.drawRect(RectF(30f, y - 16f, 34f, y + 4f), accentBarPaint)
            canvas.drawText(title.uppercase(), 42f, y - 2f, titlePaint)
            y += 20f
        }

        // 2. Cliente Box
        checkAndCreateNewPage(120f)
        drawSectionTitle("Dados do Cliente")
        paintText.textSize = 10f

        paintText.isFakeBoldText = true
        paintText.color = Color.parseColor("#1E3A8A")
        canvas.drawText("Cliente:", 40f, y, paintText)
        paintText.isFakeBoldText = false
        paintText.color = Color.parseColor("#263238")
        canvas.drawText(cliente.nome, 105f, y, paintText)

        paintText.isFakeBoldText = true
        paintText.color = Color.parseColor("#1E3A8A")
        canvas.drawText("CPF/CNPJ:", 310f, y, paintText)
        paintText.isFakeBoldText = false
        paintText.color = Color.parseColor("#263238")
        canvas.drawText(cliente.documento, 390f, y, paintText)

        y += 18f

        paintText.isFakeBoldText = true
        paintText.color = Color.parseColor("#1E3A8A")
        canvas.drawText("E-mail:", 40f, y, paintText)
        paintText.isFakeBoldText = false
        paintText.color = Color.parseColor("#263238")
        canvas.drawText(cliente.email, 105f, y, paintText)

        paintText.isFakeBoldText = true
        paintText.color = Color.parseColor("#1E3A8A")
        canvas.drawText("Telefone:", 310f, y, paintText)
        paintText.isFakeBoldText = false
        paintText.color = Color.parseColor("#263238")
        canvas.drawText(cliente.telefone, 390f, y, paintText)

        y += 18f

        paintText.isFakeBoldText = true
        paintText.color = Color.parseColor("#1E3A8A")
        canvas.drawText("Endereço:", 40f, y, paintText)
        paintText.isFakeBoldText = false
        paintText.color = Color.parseColor("#263238")
        canvas.drawText(cliente.endereco, 105f, y, paintText)
        y += 30f

        // 3. Técnico Box
        checkAndCreateNewPage(100f)
        drawSectionTitle("Responsável Técnico")
        paintText.textSize = 10f
        if (tecnico != null) {
            paintText.isFakeBoldText = true
            paintText.color = Color.parseColor("#1E3A8A")
            canvas.drawText("Técnico:", 40f, y, paintText)
            paintText.isFakeBoldText = false
            paintText.color = Color.parseColor("#263238")
            canvas.drawText(tecnico.nome, 105f, y, paintText)

            paintText.isFakeBoldText = true
            paintText.color = Color.parseColor("#1E3A8A")
            canvas.drawText("CREA/CFT:", 310f, y, paintText)
            paintText.isFakeBoldText = false
            paintText.color = Color.parseColor("#263238")
            canvas.drawText(tecnico.creaCft.ifBlank { "N/I" }, 390f, y, paintText)

            y += 18f

            paintText.isFakeBoldText = true
            paintText.color = Color.parseColor("#1E3A8A")
            canvas.drawText("E-mail:", 40f, y, paintText)
            paintText.isFakeBoldText = false
            paintText.color = Color.parseColor("#263238")
            canvas.drawText(tecnico.email.ifBlank { "Não informado" }, 105f, y, paintText)

            paintText.isFakeBoldText = true
            paintText.color = Color.parseColor("#1E3A8A")
            canvas.drawText("Telefone:", 310f, y, paintText)
            paintText.isFakeBoldText = false
            paintText.color = Color.parseColor("#263238")
            canvas.drawText(tecnico.telefone.ifBlank { "Não informado" }, 390f, y, paintText)
        } else {
            paintText.color = Color.parseColor("#78909C")
            canvas.drawText("Nenhum técnico específico associado à esta O.S.", 40f, y, paintText)
        }
        y += 30f

        // Empresa Responsável Box
        checkAndCreateNewPage(100f)
        drawSectionTitle("Empresa Emissora / Credenciada")
        paintText.textSize = 10f

        paintText.isFakeBoldText = true
        paintText.color = Color.parseColor("#1E3A8A")
        canvas.drawText("Razão Social:", 40f, y, paintText)
        paintText.isFakeBoldText = false
        paintText.color = Color.parseColor("#263238")
        canvas.drawText(companyName, 120f, y, paintText)

        paintText.isFakeBoldText = true
        paintText.color = Color.parseColor("#1E3A8A")
        canvas.drawText("CNPJ:", 310f, y, paintText)
        paintText.isFakeBoldText = false
        paintText.color = Color.parseColor("#263238")
        canvas.drawText(companyCnpj, 390f, y, paintText)

        y += 18f

        paintText.isFakeBoldText = true
        paintText.color = Color.parseColor("#1E3A8A")
        canvas.drawText("E-mail:", 40f, y, paintText)
        paintText.isFakeBoldText = false
        paintText.color = Color.parseColor("#263238")
        canvas.drawText(companyEmail, 120f, y, paintText)

        paintText.isFakeBoldText = true
        paintText.color = Color.parseColor("#1E3A8A")
        canvas.drawText("Telefone:", 310f, y, paintText)
        paintText.isFakeBoldText = false
        paintText.color = Color.parseColor("#263238")
        canvas.drawText(companyPhone, 390f, y, paintText)

        y += 30f

        // 4. Equipamento Box
        checkAndCreateNewPage(120f)
        drawSectionTitle("Dados do Equipamento")
        paintText.textSize = 10f
        if (equipamento != null) {
            paintText.isFakeBoldText = true
            paintText.color = Color.parseColor("#1E3A8A")
            canvas.drawText("TAG / ID:", 40f, y, paintText)
            paintText.isFakeBoldText = false
            paintText.color = Color.parseColor("#263238")
            canvas.drawText(equipamento.tag, 115f, y, paintText)

            paintText.isFakeBoldText = true
            paintText.color = Color.parseColor("#1E3A8A")
            canvas.drawText("Marca/Modelo:", 310f, y, paintText)
            paintText.isFakeBoldText = false
            paintText.color = Color.parseColor("#263238")
            canvas.drawText("${equipamento.marca} / ${equipamento.modelo}", 390f, y, paintText)

            y += 18f

            paintText.isFakeBoldText = true
            paintText.color = Color.parseColor("#1E3A8A")
            canvas.drawText("Nº Série:", 40f, y, paintText)
            paintText.isFakeBoldText = false
            paintText.color = Color.parseColor("#263238")
            canvas.drawText(equipamento.numeroSerie.ifBlank { "N/I" }, 115f, y, paintText)

            paintText.isFakeBoldText = true
            paintText.color = Color.parseColor("#1E3A8A")
            canvas.drawText("Tipo:", 310f, y, paintText)
            paintText.isFakeBoldText = false
            paintText.color = Color.parseColor("#263238")
            canvas.drawText(equipamento.tipo, 390f, y, paintText)

            y += 18f

            paintText.isFakeBoldText = true
            paintText.color = Color.parseColor("#1E3A8A")
            canvas.drawText("Capacidade:", 40f, y, paintText)
            paintText.isFakeBoldText = false
            paintText.color = Color.parseColor("#263238")
            canvas.drawText("${equipamento.capacidadeBtu} BTU/h", 115f, y, paintText)

            paintText.isFakeBoldText = true
            paintText.color = Color.parseColor("#1E3A8A")
            canvas.drawText("Gás Refrig.:", 310f, y, paintText)
            paintText.isFakeBoldText = false
            paintText.color = Color.parseColor("#263238")
            canvas.drawText(equipamento.fluidoRefrigerante, 390f, y, paintText)

            y += 18f

            if (ambiente != null) {
                paintText.isFakeBoldText = true
                paintText.color = Color.parseColor("#1E3A8A")
                canvas.drawText("Ambiente:", 40f, y, paintText)
                paintText.isFakeBoldText = false
                paintText.color = Color.parseColor("#263238")
                canvas.drawText(ambiente.nome, 115f, y, paintText)

                if (ambiente.endereco.isNotBlank()) {
                    paintText.isFakeBoldText = true
                    paintText.color = Color.parseColor("#1E3A8A")
                    canvas.drawText("Endereço:", 305f, y, paintText)
                    paintText.isFakeBoldText = false
                    paintText.color = Color.parseColor("#263238")
                    canvas.drawText(ambiente.endereco, 390f, y, paintText)
                }
                y += 18f
            }
        } else {
            paintText.color = Color.parseColor("#78909C")
            canvas.drawText("Nenhum equipamento específico associado à esta O.S.", 40f, y, paintText)
        }
        y += 18f

        // 5. Diagnóstico e Serviços
        checkAndCreateNewPage(190f)
        drawSectionTitle("Detalhes do Atendimento")
        paintText.textSize = 10f

        paintText.isFakeBoldText = true
        paintText.color = Color.parseColor("#1E3A8A")
        canvas.drawText("Serviço:", 40f, y, paintText)
        paintText.isFakeBoldText = false
        paintText.color = Color.parseColor("#263238")
        canvas.drawText(os.tipoServico, 105f, y, paintText)

        paintText.isFakeBoldText = true
        paintText.color = Color.parseColor("#1E3A8A")
        canvas.drawText("Status O.S.:", 310f, y, paintText)
        paintText.isFakeBoldText = false
        paintText.color = Color.parseColor("#263238")
        canvas.drawText(os.status, 390f, y, paintText)

        y += 18f

        paintText.isFakeBoldText = true
        paintText.color = Color.parseColor("#1E3A8A")
        canvas.drawText("Chamado:", 40f, y, paintText)
        paintText.isFakeBoldText = false
        paintText.color = Color.parseColor("#263238")
        canvas.drawText(os.dataChamado.ifBlank { "Não informado" }, 105f, y, paintText)

        paintText.isFakeBoldText = true
        paintText.color = Color.parseColor("#1E3A8A")
        canvas.drawText("Conclusão:", 310f, y, paintText)
        paintText.isFakeBoldText = false
        paintText.color = Color.parseColor("#263238")
        canvas.drawText(os.dataAgendada.ifBlank { "Pendente" }, 390f, y, paintText)

        y += 18f

        paintText.isFakeBoldText = true
        paintText.color = Color.parseColor("#1E3A8A")
        canvas.drawText("Prioridade:", 40f, y, paintText)
        paintText.isFakeBoldText = false
        paintText.color = Color.parseColor("#263238")
        canvas.drawText(os.prioridade, 105f, y, paintText)

        paintText.isFakeBoldText = true
        paintText.color = Color.parseColor("#1E3A8A")
        canvas.drawText("Valor Total:", 310f, y, paintText)
        paintText.isFakeBoldText = false
        paintText.color = Color.parseColor("#263238")
        canvas.drawText("R$ ${String.format(Locale.getDefault(), "%.2f", os.valorServico)}", 390f, y, paintText)

        y += 24f

        paintText.isFakeBoldText = true
        paintText.textSize = 10f
        paintText.color = Color.parseColor("#1E3A8A")
        canvas.drawText("Ocorrência / Sintoma Relatado:", 40f, y, paintText)
        paintText.isFakeBoldText = false
        paintText.color = Color.parseColor("#263238")
        y += 15f
        y = drawWrappedText(canvas, os.descricao, 40f, y, 510f, paintText)
        y += 12f

        paintText.isFakeBoldText = true
        paintText.textSize = 10f
        paintText.color = Color.parseColor("#1E3A8A")
        canvas.drawText("Diagnóstico Técnico Realizado:", 40f, y, paintText)
        paintText.isFakeBoldText = false
        paintText.color = Color.parseColor("#263238")
        y += 15f
        y = drawWrappedText(canvas, os.diagnosticoTecnico.ifBlank { "Nenhum diagnóstico técnico registrado." }, 40f, y, 510f, paintText)
        y += 25f

        // 5.1 Medições Técnicas se houver
        if (os.correnteEletrica.isNotEmpty() || os.temperatura.isNotEmpty() || os.pressaoGasAlta.isNotEmpty() || os.pressaoGasBaixa.isNotEmpty()) {
            checkAndCreateNewPage(110f)
            drawSectionTitle("Medições Técnicas Realizadas")
            paintText.textSize = 10f

            paintText.isFakeBoldText = true
            paintText.color = Color.parseColor("#1E3A8A")
            canvas.drawText("Corrente Elétrica:", 40f, y, paintText)
            paintText.isFakeBoldText = false
            paintText.color = Color.parseColor("#263238")
            canvas.drawText("${os.correnteEletrica.ifEmpty { "N/I" }} A", 150f, y, paintText)

            paintText.isFakeBoldText = true
            paintText.color = Color.parseColor("#1E3A8A")
            canvas.drawText("Temp. (ºC):", 300f, y, paintText)
            paintText.isFakeBoldText = false
            paintText.color = Color.parseColor("#263238")
            canvas.drawText("${os.temperatura.ifEmpty { "N/I" }} °C", 390f, y, paintText)

            y += 18f

            paintText.isFakeBoldText = true
            paintText.color = Color.parseColor("#1E3A8A")
            canvas.drawText("Pressão Alta:", 40f, y, paintText)
            paintText.isFakeBoldText = false
            paintText.color = Color.parseColor("#263238")
            canvas.drawText("${os.pressaoGasAlta.ifEmpty { "N/I" }} PSI", 150f, y, paintText)

            paintText.isFakeBoldText = true
            paintText.color = Color.parseColor("#1E3A8A")
            canvas.drawText("Pressão Baixa:", 300f, y, paintText)
            paintText.isFakeBoldText = false
            paintText.color = Color.parseColor("#263238")
            canvas.drawText("${os.pressaoGasBaixa.ifEmpty { "N/I" }} PSI", 390f, y, paintText)
            y += 30f
        }

        // 5.2 Peças Utilizadas - Modern Table layout
        val listPecas = deserializeSelectedPecas(os.pecasTrocadas)
        if (listPecas.isNotEmpty()) {
            checkAndCreateNewPage(120f)
            drawSectionTitle("Peças e Acessórios Utilizados")

            val headerHeight = 22f
            val tableBgPaint = Paint().apply { color = Color.parseColor("#1E3A8A") }
            val headerTextPaint = Paint().apply {
                color = Color.WHITE
                textSize = 9.5f
                isFakeBoldText = true
                isAntiAlias = true
            }

            canvas.drawRoundRect(RectF(30f, y, 565f, y + headerHeight), 4f, 4f, tableBgPaint)

            canvas.drawText("ITEM", 40f, y + 14f, headerTextPaint)
            canvas.drawText("DESCRIÇÃO DA PEÇA / ACESSÓRIO", 80f, y + 14f, headerTextPaint)
            canvas.drawText("MARCA", 360f, y + 14f, headerTextPaint)
            canvas.drawText("VALOR (R$)", 480f, y + 14f, headerTextPaint)

            y += headerHeight + 4f

            val rowHeight = 20f
            val cellTextPaint = Paint().apply {
                color = Color.parseColor("#263238")
                textSize = 9f
                isAntiAlias = true
            }
            val borderPaint = Paint().apply {
                color = Color.parseColor("#E2E8F0")
                strokeWidth = 1f
                style = Paint.Style.STROKE
            }
            val rowBgPaintAlt = Paint().apply {
                color = Color.parseColor("#F8FAFC")
            }

            var index = 1
            var totalPecas = 0.0
            for (peca in listPecas) {
                checkAndCreateNewPage(rowHeight)

                if (index % 2 == 0) {
                    canvas.drawRect(RectF(30f, y, 565f, y + rowHeight), rowBgPaintAlt)
                }

                canvas.drawRect(RectF(30f, y, 565f, y + rowHeight), borderPaint)

                canvas.drawText(String.format(Locale.getDefault(), "%02d", index), 40f, y + 13f, cellTextPaint)

                val originalName = peca.nome
                val displayName = if (originalName.length > 50) originalName.take(47) + "..." else originalName
                canvas.drawText(displayName, 80f, y + 13f, cellTextPaint)

                val brandName = peca.marca.ifBlank { "Genérica" }
                val displayBrand = if (brandName.length > 20) brandName.take(17) + "..." else brandName
                canvas.drawText(displayBrand, 360f, y + 13f, cellTextPaint)

                canvas.drawText(String.format(Locale.getDefault(), "%.2f", peca.preco), 480f, y + 13f, cellTextPaint)

                totalPecas += peca.preco
                y += rowHeight
                index++
            }

            checkAndCreateNewPage(rowHeight)
            val subtotalBgPaint = Paint().apply {
                color = Color.parseColor("#F1F5F9") // Slate-100 highlight
            }
            canvas.drawRect(RectF(30f, y, 565f, y + rowHeight), subtotalBgPaint)
            canvas.drawRect(RectF(30f, y, 565f, y + rowHeight), borderPaint)

            val totalTextPaint = Paint().apply {
                color = Color.parseColor("#1E3A8A") // Rich Navy
                textSize = 9f
                isFakeBoldText = true
                isAntiAlias = true
            }
            canvas.drawText("SUBTOTAL PEÇAS / ACESSÓRIOS", 80f, y + 12f, totalTextPaint)
            canvas.drawText(String.format(Locale.getDefault(), "%.2f", totalPecas), 480f, y + 12f, totalTextPaint)
            y += rowHeight + 25f
        }

        // 6. Signatures (Drawn Base64 bitmaps)
        checkAndCreateNewPage(135f)
        val sigY = y + 10f
        canvas.drawLine(40f, sigY + 50f, 260f, sigY + 50f, paintLine)
        canvas.drawLine(330f, sigY + 50f, 550f, sigY + 50f, paintLine)

        paintText.textSize = 9f
        canvas.drawText("Assinatura do Técnico", 100f, sigY + 65f, paintText)
        canvas.drawText("Assinatura do Cliente (${os.assinaturaClienteNome.ifEmpty { "Cliente" }})", 360f, sigY + 65f, paintText)

        // Draw signatures
        if (os.assinaturaDigital.isNotEmpty()) {
            val sigBitmap = base64ToBitmap(os.assinaturaDigital)
            if (sigBitmap != null) {
                val destRect = RectF(340f, sigY, 540f, sigY + 48f)
                canvas.drawBitmap(sigBitmap, null, destRect, null)
            }
        }

        if (tecnico != null) {
            paintText.textSize = 10f
            paintText.isFakeBoldText = true
            canvas.drawText(tecnico.nome, 60f, sigY + 30f, paintText)
            paintText.textSize = 9f
            paintText.isFakeBoldText = false
            canvas.drawText("Registro: ${tecnico.creaCft}", 60f, sigY + 42f, paintText)
        }

        pdfDocument.finishPage(page)

        val file = File(context.cacheDir, "Ordem_Servico_${os.id}.pdf")
        try {
            val fos = FileOutputStream(file)
            pdfDocument.writeTo(fos)
            pdfDocument.close()
            fos.close()
            return file
        } catch (e: Exception) {
            e.printStackTrace()
            pdfDocument.close()
            return null
        }
    }

    // Helper to draw wrapped text in a coordinate block
    private fun drawWrappedText(canvas: Canvas, text: String, x: Float, yStart: Float, maxWidth: Float, paint: Paint): Float {
        var curY = yStart
        val lines = text.split("\n")
        for (rawLine in lines) {
            val words = rawLine.split(" ")
            var line = StringBuilder()
            for (word in words) {
                if (word.isEmpty()) continue
                val testLine = if (line.isEmpty()) word else "${line} $word"
                val width = paint.measureText(testLine)
                if (width > maxWidth) {
                    canvas.drawText(line.toString(), x, curY, paint)
                    curY += paint.textSize + 4f
                    line = StringBuilder(word)
                } else {
                    line = StringBuilder(testLine)
                }
            }
            if (line.isNotEmpty()) {
                canvas.drawText(line.toString(), x, curY, paint)
                curY += paint.textSize + 4f
            }
        }
        return curY
    }

    // Generates PMOC PDF Report with multiple pages
    fun generatePmocPdf(
        context: Context,
        pmoc: PmocReport,
        cliente: Cliente,
        tecnico: Tecnico?,
        equipamentos: List<Equipamento>,
        ambientes: List<Ambiente>,
        logbookEntries: List<PmocLogbookEntry> = emptyList()
    ): File? {
        val sharedPrefs = context.getSharedPreferences("climagest_prefs", Context.MODE_PRIVATE)
        val companyName = sharedPrefs.getString("company_name", "Ar-Control Climatização") ?: "Ar-Control Climatização"
        val companyCnpj = sharedPrefs.getString("company_cnpj", "12.345.678/0001-90") ?: "12.345.678/0001-90"
        val companyPhone = sharedPrefs.getString("company_phone", "(11) 98765-4321") ?: "(11) 98765-4321"
        val companyEmail = sharedPrefs.getString("company_email", "contato@arcontrol.com.br") ?: "contato@arcontrol.com.br"

        val pdfDocument = PdfDocument()
        val paintText = Paint()
        val paintLine = Paint().apply {
            color = Color.LTGRAY
            strokeWidth = 1f
        }
        val paintAccent = Paint().apply {
            color = Color.parseColor("#01579B") // Dark Blue
        }
        val paintSecondaryBg = Paint().apply {
            color = Color.parseColor("#F5F5F5") // Soft background card
        }

        var pageNum = 1
        var pageInfo = PdfDocument.PageInfo.Builder(595, 842, pageNum).create()
        var page = pdfDocument.startPage(pageInfo)
        var canvas = page.canvas

        // Header Background for Page 1
        canvas.drawRect(RectF(0f, 0f, 595f, 95f), paintAccent)

        // Title
        paintText.color = Color.WHITE
        paintText.textSize = 18f
        paintText.isFakeBoldText = true
        canvas.drawText("PLANO DE MANUTENÇÃO, OPERAÇÃO E CONTROLE", 30f, 38f, paintText)

        paintText.textSize = 13f
        paintText.isFakeBoldText = false
        canvas.drawText("$companyName - Relatório PMOC Oficial (Lei 13.589/18)", 30f, 58f, paintText)

        paintText.textSize = 11f
        canvas.drawText("Mês de Referência: ${pmoc.mesRef}", 30f, 78f, paintText)
        canvas.drawText("PMOC Nº: ${pmoc.numeroPmoc}", 430f, 78f, paintText)

        paintText.color = Color.BLACK
        paintText.textSize = 10f

        var y = 130f

        // Helper to check for space and dynamically spawn new pages
        fun checkAndCreateNewPage(neededHeight: Float) {
            if (y + neededHeight > 780f) {
                pdfDocument.finishPage(page)
                pageNum++
                pageInfo = PdfDocument.PageInfo.Builder(595, 842, pageNum).create()
                page = pdfDocument.startPage(pageInfo)
                canvas = page.canvas
                
                // Draw header on new page
                canvas.drawRect(RectF(0f, 0f, 595f, 50f), paintAccent)
                paintText.color = Color.WHITE
                paintText.textSize = 10f
                paintText.isFakeBoldText = true
                canvas.drawText("PLANO DE MANUTENÇÃO, OPERAÇÃO E CONTROLE (PMOC) - CONTINUAÇÃO", 30f, 28f, paintText)
                canvas.drawText("Ref: ${pmoc.mesRef} | Nº: ${pmoc.numeroPmoc}", 380f, 28f, paintText)
                paintText.color = Color.BLACK
                paintText.isFakeBoldText = false
                
                y = 80f
            }
        }

        fun drawSectionTitle(title: String) {
            checkAndCreateNewPage(35f)
            val titlePaint = Paint().apply {
                color = Color.parseColor("#01579B")
                textSize = 11f
                isFakeBoldText = true
            }
            val titleBgPaint = Paint().apply {
                color = Color.parseColor("#E1F5FE")
            }
            canvas.drawRect(RectF(30f, y - 18f, 565f, y + 4f), titleBgPaint)
            canvas.drawText(title.uppercase(), 40f, y - 2f, titlePaint)
            y += 20f
        }

        // 1. Cliente Box
        drawSectionTitle("Identificação do Estabelecimento / Cliente")
        canvas.drawText("Razão Social/Nome: ${cliente.nome}", 40f, y, paintText)
        canvas.drawText("CPF/CNPJ: ${cliente.documento}", 340f, y, paintText)
        y += 18f
        canvas.drawText("E-mail: ${cliente.email}", 40f, y, paintText)
        canvas.drawText("Telefone: ${cliente.telefone}", 340f, y, paintText)
        y += 18f
        canvas.drawText("Endereço: ${cliente.endereco}", 40f, y, paintText)
        y += 30f

        // 2. Técnico Box
        drawSectionTitle("Responsável Técnico PMOC")
        if (tecnico != null) {
            canvas.drawText("Nome Técnico: ${tecnico.nome}", 40f, y, paintText)
            canvas.drawText("Registro CREA/CFT: ${tecnico.creaCft}", 340f, y, paintText)
            y += 18f
            canvas.drawText("E-mail: ${tecnico.email}", 40f, y, paintText)
            canvas.drawText("Telefone: ${tecnico.telefone}", 340f, y, paintText)
            y += 18f
            canvas.drawText("ART / TRT Nº: ${pmoc.filtroObs.ifEmpty { "Não Informado" }}", 40f, y, paintText)
        } else {
            canvas.drawText("Não há técnico de campo cadastrado para este PMOC.", 40f, y, paintText)
        }
        y += 30f

        // Empresa Responsável / Credenciada Box
        drawSectionTitle("Empresa Emissora / Credenciada")
        canvas.drawText("Razão Social: $companyName", 40f, y, paintText)
        canvas.drawText("CNPJ: $companyCnpj", 340f, y, paintText)
        y += 18f
        canvas.drawText("E-mail Comercial: $companyEmail", 40f, y, paintText)
        canvas.drawText("Telefone Comercial: $companyPhone", 340f, y, paintText)
        y += 30f

        val selectedIds = pmoc.equipamentosSelecionadosIds.split(",")
            .filter { it.isNotEmpty() }
        
        val filteredEquips = if (selectedIds.isNotEmpty()) {
            equipamentos.filter { it.id in selectedIds }
        } else {
            equipamentos
        }

        // 3b. Relação de Ambientes sob controle do PMOC Table (SWAPPED BEFORE EQUIPMENTS! - 6º alteração)
        drawSectionTitle("Relação de Ambientes sob controle do PMOC")
        val ambientesSobControle = ambientes.filter { amb ->
            filteredEquips.any { eq -> eq.ambienteId == amb.id }
        }

        if (ambientesSobControle.isNotEmpty()) {
            paintText.isFakeBoldText = true
            canvas.drawText("Nome do Ambiente", 40f, y, paintText)
            canvas.drawText("Área", 160f, y, paintText)
            canvas.drawText("Ocupação", 210f, y, paintText)
            canvas.drawText("Qtd Equip.", 290f, y, paintText)
            canvas.drawText("Carga (BTU)", 370f, y, paintText)
            canvas.drawText("Carga (TR)", 470f, y, paintText)
            paintText.isFakeBoldText = false
            y += 6f
            canvas.drawLine(30f, y, 565f, y, paintLine)
            y += 15f
            
            for (amb in ambientesSobControle) {
                checkAndCreateNewPage(18f)
                val eqsInAmb = filteredEquips.filter { it.ambienteId == amb.id }
                val qtdEquips = eqsInAmb.size
                val cargaBtu = eqsInAmb.sumOf { it.capacidadeBtu }
                val cargaTR = cargaBtu / 12000.0
                
                var displayAmbName = amb.nome
                if (displayAmbName.length > 20) {
                    displayAmbName = displayAmbName.substring(0, 17) + "..."
                }
                canvas.drawText(displayAmbName, 40f, y, paintText)
                canvas.drawText(String.format("%.1f m²", amb.areaM2), 160f, y, paintText)
                canvas.drawText("${amb.fixos + amb.flutuantes} pes.", 210f, y, paintText)
                canvas.drawText("$qtdEquips und.", 290f, y, paintText)
                canvas.drawText(String.format("%,d BTU", cargaBtu), 370f, y, paintText)
                canvas.drawText(String.format("%.2f TR", cargaTR), 470f, y, paintText)
                y += 16f
            }

            // Destaque - Total Geral (Ultima linha em destaque - 5º alteração)
            checkAndCreateNewPage(24f)
            canvas.drawLine(30f, y, 565f, y, paintLine)
            y += 14f

            paintText.isFakeBoldText = true
            canvas.drawText("TOTAL GERAL", 40f, y, paintText)
            val totalArea = ambientesSobControle.sumOf { it.areaM2 }
            val totalOcupantes = ambientesSobControle.sumOf { it.fixos + it.flutuantes }
            val totalQtdEquips = filteredEquips.size
            val totalCargaBtu = filteredEquips.sumOf { it.capacidadeBtu }
            val totalCargaTr = totalCargaBtu / 12000.0

            canvas.drawText(String.format("%.1f m²", totalArea), 160f, y, paintText)
            canvas.drawText("$totalOcupantes pes.", 210f, y, paintText)
            canvas.drawText("$totalQtdEquips und.", 290f, y, paintText)
            canvas.drawText(String.format("%,d BTU", totalCargaBtu), 370f, y, paintText)
            canvas.drawText(String.format("%.2f TR", totalCargaTr), 470f, y, paintText)
            paintText.isFakeBoldText = false
            y += 16f
            canvas.drawLine(30f, y, 565f, y, paintLine)
        } else {
            canvas.drawText("Nenhum ambiente associado aos equipamentos sob controle.", 40f, y, paintText)
            y += 16f
        }
        y += 20f

        // 3. Equipamentos Selecionados Box (SWAPPED AFTER ENVIRONMENTS! - 6º alteração)
        drawSectionTitle("Relação de Equipamentos Sob Controle do PMOC")

        if (filteredEquips.isNotEmpty()) {
            paintText.isFakeBoldText = true
            canvas.drawText("TAG", 40f, y, paintText)
            canvas.drawText("Marca/Modelo/Tipo", 110f, y, paintText)
            canvas.drawText("Capacidade", 310f, y, paintText)
            canvas.drawText("Fluido", 390f, y, paintText)
            canvas.drawText("Ambiente", 460f, y, paintText) // Added Ambiente, removed Status column (4º alteração)
            paintText.isFakeBoldText = false
            y += 6f
            canvas.drawLine(30f, y, 565f, y, paintLine)
            y += 15f
            
            for (eq in filteredEquips) {
                checkAndCreateNewPage(18f)
                val ambName = ambientes.find { it.id == eq.ambienteId }?.nome ?: "N/A"
                canvas.drawText(eq.tag, 40f, y, paintText)
                
                var displayBrandModel = "${eq.marca} ${eq.modelo} (${eq.tipo})"
                if (displayBrandModel.length > 32) {
                    displayBrandModel = displayBrandModel.substring(0, 29) + "..."
                }
                canvas.drawText(displayBrandModel, 110f, y, paintText)
                canvas.drawText("${eq.capacidadeBtu} BTU", 310f, y, paintText)
                canvas.drawText(eq.fluidoRefrigerante, 390f, y, paintText)
                
                var displayAmb = ambName
                if (displayAmb.length > 18) {
                    displayAmb = displayAmb.substring(0, 15) + "..."
                }
                canvas.drawText(displayAmb, 460f, y, paintText)
                y += 16f
            }
        } else {
            canvas.drawText("Nenhum equipamento foi associado ou selecionado para este PMOC.", 40f, y, paintText)
            y += 16f
        }
        y += 20f

        // 3c. CAPACIDADE TOTAL INSTALADA Box
        drawSectionTitle("Capacidade Total Instalada")
        val totalEquipamentosCount = filteredEquips.size
        val somaTotalBtus = filteredEquips.sumOf { it.capacidadeBtu }
        val equivalenciaTR = somaTotalBtus / 12000.0
        
        checkAndCreateNewPage(100f)
        val rectBox = RectF(30f, y, 565f, y + 85f)
        canvas.drawRect(rectBox, paintSecondaryBg)
        
        val borderPaint = Paint().apply {
            color = Color.parseColor("#01579B")
            style = Paint.Style.STROKE
            strokeWidth = 1f
        }
        canvas.drawRect(rectBox, borderPaint)
        
        paintText.isFakeBoldText = true
        canvas.drawText("Total de Equipamentos:", 45f, y + 20f, paintText)
        paintText.isFakeBoldText = false
        canvas.drawText("$totalEquipamentosCount equipamentos", 200f, y + 20f, paintText)
        
        paintText.isFakeBoldText = true
        canvas.drawText("Capacidade Total:", 45f, y + 38f, paintText)
        paintText.isFakeBoldText = false
        canvas.drawText(String.format("%,d BTU/h", somaTotalBtus), 200f, y + 38f, paintText)
        
        paintText.isFakeBoldText = true
        canvas.drawText("Equivalência (TR):", 45f, y + 56f, paintText)
        paintText.isFakeBoldText = false
        canvas.drawText(String.format("%.2f TR (tonelada de refrigeração)", equivalenciaTR), 200f, y + 56f, paintText)
        
        y += 105f
        checkAndCreateNewPage(40f)
        
        val warningPaint = Paint().apply {
            textSize = 9.5f
            isAntiAlias = true
        }

        if (somaTotalBtus > 60000) {
            warningPaint.color = Color.parseColor("#C62828")
            warningPaint.isFakeBoldText = true
            canvas.drawText("Sistema acima de 60.000 BTU/h — PMOC obrigatório conforme Lei 13.589/2018.", 35f, y, warningPaint)
        } else {
            warningPaint.color = Color.parseColor("#2E7D32")
            warningPaint.isFakeBoldText = true
            canvas.drawText("Sistema abaixo do limite federal obrigatório de 60.000 BTU/h,", 35f, y, warningPaint)
            y += 14f
            canvas.drawText("porém recomenda-se manutenção preventiva conforme boas práticas.", 35f, y, warningPaint)
        }
        y += 30f

        // 4. Rotinas ativas de manutenção contendo as datas preenchidas se houver logbook (1º alteração)
        val routines = if (pmoc.rotinasJson.isNotEmpty()) {
            pmoc.rotinasJson.split("\n").filter { it.contains("|") }.map {
                val parts = it.split("|", limit = 2)
                Pair(parts[0], parts[1])
            }
        } else {
            listOf(
                Pair("Limpeza e inspeção dos filtros de ar", "Mensal"),
                Pair("Limpeza da bandeja de condensado", "Mensal"),
                Pair("Limpeza das aletas e serpentina do evaporador", "Trimestral"),
                Pair("Verificação e limpeza do dreno de condensado", "Mensal"),
                Pair("Inspeção do dreno, motor e turbina do ventilador", "Trimestral"),
                Pair("Aplicação de biocida / higienizador", "Trimestral"),
                Pair("Verificação elétrica da unidade interna", "Semestral"),
                Pair("Limpeza das aletas e serpentina do condensador", "Trimestral"),
                Pair("Inspeção do motor e hélice do ventilador/condensador", "Trimestral"),
                Pair("Verificação de vazamento de gás refrigerante", "Trimestral"),
                Pair("Medição de pressão do ciclo frigorífico", "Semestral"),
                Pair("Verificação e aperto das conexões elétricas", "Semestral"),
                Pair("Lubrificação de partes móveis", "Semestral"),
                Pair("Teste funcional geral (frio, quente, ventilação)", "Trimestral"),
                Pair("Verificação da fixação e vedação", "Semestral")
            )
        }

        if (filteredEquips.isNotEmpty() && routines.isNotEmpty()) {
            for (eq in filteredEquips) {
                // Section Title for this equipment's maintenance activities
                checkAndCreateNewPage(50f)
                val eqTitlePaint = Paint().apply {
                    color = Color.parseColor("#1B5E20") // Green accent for equipments
                    textSize = 10.5f
                    isFakeBoldText = true
                }
                val eqBgPaint = Paint().apply {
                    color = Color.parseColor("#E8F5E9")
                }
                canvas.drawRect(RectF(30f, y - 18f, 565f, y + 4f), eqBgPaint)
                canvas.drawText("ATIVIDADES DA ROTINA - EQUIPAMENTO TAG: ${eq.tag} (${eq.marca})", 40f, y - 2f, eqTitlePaint)
                y += 20f

                // Table Header
                paintText.isFakeBoldText = true
                canvas.drawText("Atividade / Procedimento", 40f, y, paintText)
                canvas.drawText("Frequência", 380f, y, paintText)
                canvas.drawText("Data de Exec.", 465f, y, paintText)
                paintText.isFakeBoldText = false
                y += 6f
                canvas.drawLine(30f, y, 565f, y, paintLine)
                y += 15f

                for ((index, rt) in routines.withIndex()) {
                    checkAndCreateNewPage(18f)
                    // Alternating background for comfortable visual scanning
                    if (index % 2 == 0) {
                        canvas.drawRect(RectF(30f, y - 11f, 565f, y + 5f), paintSecondaryBg)
                    }
                    canvas.drawText(rt.first, 40f, y, paintText)
                    canvas.drawText(rt.second, 380f, y, paintText)
                    
                    // Match the date if execution is completed in the logbook database!
                    val entry = logbookEntries.find { it.equipamentoId == eq.id && it.atividade == rt.first }
                    val dateText = if (entry != null && entry.concluido && entry.dataRealizacao.isNotEmpty()) {
                        entry.dataRealizacao
                    } else {
                        "[    /    /        ]"
                    }
                    canvas.drawText(dateText, 465f, y, paintText)
                    y += 16f
                }
                y += 15f // Space after equipment block
            }
        }

        // 5. Procedimentos Operacionais (Full width sequential layout)
        val ops = if (pmoc.procedimentosOperacionaisJson.isNotEmpty()) {
            pmoc.procedimentosOperacionaisJson.split("\n===\n").filter { it.contains(":::") }.map {
                val parts = it.split(":::", limit = 2)
                Pair(parts[0], parts[1])
            }
        } else {
            listOf(
                Pair("1. Acionamento e desligamento", "Acionar com antecedência máx de 30 minutos antes da ocupação. Desligar ao final ou se desocupado por mais de 2 horas. Aguardar 3 minutos entre acionamentos."),
                Pair("2. Setpoint e faixas de operação", "Temperatura entre 23°C e 26°C (verão) ou 21°C e 23°C (inverno). Umidade entre 40% e 65%. Vedado ajuste abaixo de 20°C."),
                Pair("3. Ventilação e renovação de ar", "Manter portas e janelas fechadas. Não obstruir grelhas de insuflamento ou tomadas de ar externo. Manter renovação desobstruída."),
                Pair("4. Responsabilidades de operação", "Operador(es) habilitado(s): [Nome]. Qualquer anormalidade - ruídos, odores, vazamentos - deve ser relatada ao RT: [RT], CREA/CFT."),
                Pair("5. Programação de horários", "Dias úteis: [Horário] às [Horário]. Sábados: [Horário]. Domingos: Não opera. Utilizar temporizador automático."),
                Pair("6. Registro de funcionamento", "Registrar mensalmente no logbook: temperatura, ocorrências, alarmes, data e nome. Manter disponível para fiscalizações.")
            )
        }

        if (ops.isNotEmpty()) {
            checkAndCreateNewPage(45f)
            val titlePaintOps = Paint().apply {
                color = Color.parseColor("#01579B")
                textSize = 11f
                isFakeBoldText = true
            }
            val bgPaintOps = Paint().apply {
                color = Color.parseColor("#E1F5FE")
            }
            canvas.drawRect(RectF(30f, y - 18f, 565f, y + 4f), bgPaintOps)
            canvas.drawText("PROCEDIMENTOS OPERACIONAIS DE MANUTENÇÃO E OPERAÇÃO", 40f, y - 2f, titlePaintOps)
            y += 20f

            val contentWidth = 515f
            for (op in ops) {
                checkAndCreateNewPage(40f)
                
                // Draw title
                paintText.isFakeBoldText = true
                paintText.textSize = 10f
                paintText.color = Color.parseColor("#01579B")
                y = drawWrappedText(canvas, op.first, 40f, y, contentWidth, paintText)
                
                // Draw description
                paintText.isFakeBoldText = false
                paintText.textSize = 9f
                paintText.color = Color.BLACK
                y = drawWrappedText(canvas, op.second, 40f, y, contentWidth, paintText)
                y += 8f
            }
            y += 15f
        }

        // 6. Procedimentos Emergenciais (Full width sequential layout)
        val ems = if (pmoc.procedimentosEmergenciaisJson.isNotEmpty()) {
            pmoc.procedimentosEmergenciaisJson.split("\n===\n").filter { it.contains(":::") }.map {
                val parts = it.split(":::", limit = 2)
                Pair(parts[0], parts[1])
            }
        } else {
            listOf(
                Pair("E1. Vazamento de água (condensado)", "Desligar pelo disjuntor. Proteger computadores. Acionar imediatamente o RT. Não ligar até o reparo."),
                Pair("E2. Odor, fumaça ou superaquecimento", "Desligar pelo disjuntor. Chamar Bombeiros (193) se risco de fogo. Nunca use água. Chamar RT."),
                Pair("E3. Falha elétrica ou desligamento", "Anotar erro. Aguardar 3 minutos para religar. Se persistir, chamar RT. Não forçar funcionamento."),
                Pair("E4. Suspeita de vazamento de gás", "Desligar pelo disjuntor. Abrir portas/janelas. Evacuar. Não acionar tomadas. Chamar RT."),
                Pair("E5. Qualidade do ar comprometida", "Parar sistema. Ventilar e evacuar. Chamar SAMU (192) se mal-estar. RT faz avaliação microbiológica."),
                Pair("E6. Contatos de emergência", "R. Técnico: [RT/Telefone]\nEmpresa: [Nome]\nSAMU: 192 / Bombeiros: 193 / Civil: 199")
            )
        }

        if (ems.isNotEmpty()) {
            checkAndCreateNewPage(45f)
            val titlePaintEms = Paint().apply {
                color = Color.parseColor("#C62828") // Red tint for Emergenicais
                textSize = 11f
                isFakeBoldText = true
            }
            val bgPaintEms = Paint().apply {
                color = Color.parseColor("#FFEBEE")
            }
            canvas.drawRect(RectF(30f, y - 18f, 565f, y + 4f), bgPaintEms)
            canvas.drawText("PROCEDIMENTOS DE EMERGÊNCIA", 40f, y - 2f, titlePaintEms)
            y += 20f

            val contentWidth = 515f
            for (em in ems) {
                checkAndCreateNewPage(40f)
                
                // Draw title
                paintText.isFakeBoldText = true
                paintText.textSize = 10f
                paintText.color = Color.parseColor("#C62828")
                y = drawWrappedText(canvas, em.first, 40f, y, contentWidth, paintText)
                
                // Draw description
                paintText.isFakeBoldText = false
                paintText.textSize = 9f
                paintText.color = Color.BLACK
                y = drawWrappedText(canvas, em.second, 40f, y, contentWidth, paintText)
                y += 8f
            }
            y += 15f
        }

        // 7. Assinaturas
        checkAndCreateNewPage(110f)
        
        val sigY = y + 25f
        paintText.color = Color.BLACK
        paintText.textSize = 9f
        canvas.drawLine(30f, sigY + 50f, 260f, sigY + 50f, paintLine)
        canvas.drawLine(330f, sigY + 50f, 550f, sigY + 50f, paintLine)

        paintText.textSize = 9f
        canvas.drawText("Responsável Técnico (CFT/CREA)", 90f, sigY + 65f, paintText)
        canvas.drawText("Contratante / Cliente Autorizado", 365f, sigY + 65f, paintText)

        // Draw Signatures bitmaps if base64 present
        if (pmoc.assinaturaDigitalTecnico.isNotEmpty()) {
            val sigTec = base64ToBitmap(pmoc.assinaturaDigitalTecnico)
            if (sigTec != null) {
                canvas.drawBitmap(sigTec, null, RectF(55f, sigY, 235f, sigY + 45f), null)
            }
        }
        if (pmoc.assinaturaDigitalCliente.isNotEmpty()) {
            val sigCli = base64ToBitmap(pmoc.assinaturaDigitalCliente)
            if (sigCli != null) {
                canvas.drawBitmap(sigCli, null, RectF(345f, sigY, 525f, sigY + 45f), null)
            }
        }

        pdfDocument.finishPage(page)

        // Save PDF file
        val file = File(context.cacheDir, "Relatorio_PMOC_${pmoc.id}.pdf")
        try {
            val fos = FileOutputStream(file)
            pdfDocument.writeTo(fos)
            pdfDocument.close()
            fos.close()
            return file
        } catch (e: Exception) {
            e.printStackTrace()
            pdfDocument.close()
            return null
        }
    }

    // Share PDF file using FileProvider and implicit Android Intent
    fun sharePdf(context: Context, file: File) {
        try {
            val uri: Uri = FileProvider.getUriForFile(
                context,
                "com.priorizedev.brizza.fileprovider",
                file
            )
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "application/pdf"
                putExtra(Intent.EXTRA_STREAM, uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(Intent.createChooser(intent, "Compartilhar Relatório PDF"))
        } catch (e: Exception) {
            Toast.makeText(context, "Erro ao compartilhar arquivo: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    // Generates Complete Customer Profile, Environments, Equipments and Maintenance history PDF
    fun generateClientFullReportPdf(
        context: Context,
        cliente: Cliente,
        ambientes: List<com.priorizedev.brizza.data.model.Ambiente>,
        equipamentos: List<Equipamento>,
        ordens: List<OrdemServico>
    ): File? {
        val sharedPrefs = context.getSharedPreferences("climagest_prefs", Context.MODE_PRIVATE)
        val companyName = sharedPrefs.getString("company_name", "Ar-Control Climatização") ?: "Ar-Control Climatização"
        val companyCnpj = sharedPrefs.getString("company_cnpj", "12.345.678/0001-90") ?: "12.345.678/0001-90"
        val companyPhone = sharedPrefs.getString("company_phone", "(11) 98765-4321") ?: "(11) 98765-4321"
        val companyEmail = sharedPrefs.getString("company_email", "contato@arcontrol.com.br") ?: "contato@arcontrol.com.br"

        val pdfDocument = PdfDocument()
        var pageNum = 1
        var pageInfo = PdfDocument.PageInfo.Builder(595, 842, pageNum).create()
        var page = pdfDocument.startPage(pageInfo)
        var canvas = page.canvas

        val paintText = Paint()
        val paintLine = Paint().apply {
            color = Color.LTGRAY
            strokeWidth = 1f
        }
        val paintAccent = Paint().apply {
            color = Color.parseColor("#4F46E5") // Indigo Theme Primary
        }

        var y = 130f

        fun drawHeader() {
            val headerRect = RectF(0f, 0f, 595f, 95f)
            canvas.drawRect(headerRect, paintAccent)

            paintText.color = Color.WHITE
            paintText.textSize = 20f
            paintText.isFakeBoldText = true
            canvas.drawText("FICHA COMPLETA DO CLIENTE", 30f, 38f, paintText)

            paintText.textSize = 14f
            paintText.isFakeBoldText = false
            canvas.drawText("Empresa Responsável: $companyName", 30f, 60f, paintText)

            paintText.textSize = 11f
            canvas.drawText("Gerado em: ${SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).format(Date())}", 30f, 80f, paintText)

            paintText.color = Color.BLACK
            paintText.textSize = 10f
        }

        fun checkAndCreateNewPage(neededHeight: Float) {
            if (y + neededHeight > 780f) {
                pdfDocument.finishPage(page)
                pageNum++
                pageInfo = PdfDocument.PageInfo.Builder(595, 842, pageNum).create()
                page = pdfDocument.startPage(pageInfo)
                canvas = page.canvas
                drawHeader()
                y = 130f
                paintLine.apply {
                    color = Color.LTGRAY
                    strokeWidth = 1f
                }
            }
        }

        // Header
        drawHeader()

        fun drawSectionTitle(title: String) {
            checkAndCreateNewPage(45f)
            val titlePaint = Paint().apply {
                color = Color.parseColor("#4F46E5")
                textSize = 12f
                isFakeBoldText = true
            }
            val titleBgPaint = Paint().apply {
                color = Color.parseColor("#EEF2FF")
            }
            canvas.drawRect(RectF(30f, y - 18f, 565f, y + 4f), titleBgPaint)
            canvas.drawText(title.uppercase(), 40f, y - 2f, titlePaint)
            y += 20f
        }

        // 1. Cliente Box
        drawSectionTitle("Dados de Cadastro")
        canvas.drawText("Nome / Razão Social: ${cliente.nome}", 40f, y, paintText)
        canvas.drawText("Doc: ${cliente.documento}", 340f, y, paintText)
        y += 18f
        canvas.drawText("Telefone: ${cliente.telefone}", 40f, y, paintText)
        canvas.drawText("E-mail: ${cliente.email.ifEmpty { "Não Atribuído" }}", 340f, y, paintText)
        y += 18f
        canvas.drawText("Endereço: ${cliente.endereco}", 40f, y, paintText)
        y += 30f

        // 1b. Empresa Responsável Box
        drawSectionTitle("Empresa Responsável / Credenciada")
        canvas.drawText("Razão Social: $companyName", 40f, y, paintText)
        canvas.drawText("CNPJ: $companyCnpj", 340f, y, paintText)
        y += 18f
        canvas.drawText("E-mail Comercial: $companyEmail", 40f, y, paintText)
        canvas.drawText("Telefone Comercial: $companyPhone", 340f, y, paintText)
        y += 30f

        // 2. Ambientes
        drawSectionTitle("Ambientes Atendidos")
        if (ambientes.isNotEmpty()) {
            paintText.isFakeBoldText = true
            canvas.drawText("Ambiente", 40f, y, paintText)
            canvas.drawText("Área (m²)", 250f, y, paintText)
            canvas.drawText("Carga Térmica (BTU/h)", 400f, y, paintText)
            paintText.isFakeBoldText = false
            y += 6f
            canvas.drawLine(30f, y, 565f, y, paintLine)
            y += 16f
            for (amb in ambientes.take(5)) {
                canvas.drawText(amb.nome, 40f, y, paintText)
                canvas.drawText("${amb.areaM2}", 250f, y, paintText)
                canvas.drawText("${amb.cargaTermicaBtu} BTU/h", 400f, y, paintText)
                y += 16f
            }
            if (ambientes.size > 5) {
                canvas.drawText("+ ${ambientes.size - 5} ambientes...", 40f, y, paintText)
                y += 16f
            }
        } else {
            canvas.drawText("Nenhum ambiente cadastrado.", 40f, y, paintText)
            y += 16f
        }
        y += 20f

        // 3. Equipamentos
        drawSectionTitle("Equipamentos / Máquinas")
        if (equipamentos.isNotEmpty()) {
            paintText.isFakeBoldText = true
            canvas.drawText("TAG", 40f, y, paintText)
            canvas.drawText("Marca / Tipo", 100f, y, paintText)
            canvas.drawText("Ambiente", 250f, y, paintText)
            canvas.drawText("Capacidade", 380f, y, paintText)
            canvas.drawText("Status", 480f, y, paintText)
            paintText.isFakeBoldText = false
            y += 6f
            canvas.drawLine(30f, y, 565f, y, paintLine)
            y += 16f
            for (eq in equipamentos.take(6)) {
                checkAndCreateNewPage(18f)
                canvas.drawText(eq.tag, 40f, y, paintText)
                canvas.drawText("${eq.marca} (${eq.tipo})", 100f, y, paintText)
                
                val amb = ambientes.find { it.id == eq.ambienteId }
                val ambienteNome = amb?.nome ?: "Geral"
                val ambCut = if (ambienteNome.length > 20) ambienteNome.take(17) + "..." else ambienteNome
                canvas.drawText(ambCut, 250f, y, paintText)
                
                canvas.drawText("${eq.capacidadeBtu} BTU/h", 380f, y, paintText)
                
                // Color status text
                val statusPaint = Paint().apply {
                    color = when (eq.status.lowercase()) {
                        "ativo" -> Color.parseColor("#059669")
                        "manutenção", "manutencao" -> Color.parseColor("#D97706")
                        else -> Color.parseColor("#64748B")
                    }
                    textSize = 10f
                    isFakeBoldText = true
                }
                canvas.drawText(eq.status.uppercase(), 480f, y, statusPaint)
                y += 16f
            }
            if (equipamentos.size > 6) {
                canvas.drawText("+ ${equipamentos.size - 6} equipamentos...", 40f, y, paintText)
                y += 16f
            }
        } else {
            canvas.drawText("Nenhum equipamento cadastrado.", 40f, y, paintText)
            y += 16f
        }
        y += 20f

        // 4. Ultimas Manutenções
        drawSectionTitle("Histórico de Manutenções (Últimas OS)")
        val osFiltradas = ordens.filter { it.clienteId == cliente.id }
        if (osFiltradas.isNotEmpty()) {
            paintText.isFakeBoldText = true
            canvas.drawText("Nº OS / Data", 40f, y, paintText)
            canvas.drawText("Tipo de Serviço", 170f, y, paintText)
            canvas.drawText("Equipamento (Ambiente)", 310f, y, paintText)
            canvas.drawText("Status", 490f, y, paintText)
            paintText.isFakeBoldText = false
            y += 6f
            canvas.drawLine(30f, y, 565f, y, paintLine)
            y += 16f
            for (o in osFiltradas.take(10)) {
                checkAndCreateNewPage(18f)
                val dateStr = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(Date(o.dataCriacao))
                val formattedOS = formatNumeroOrdem(o.numeroOrdem)
                
                val eqItem = equipamentos.find { it.id == o.equipamentoId }
                val ambItem = eqItem?.let { eq -> ambientes.find { it.id == eq.ambienteId } }
                val equipmentDisplayName = if (eqItem != null) {
                    val brand = eqItem.marca
                    val ambName = ambItem?.nome ?: "Geral"
                    "$brand ($ambName)"
                } else {
                    "Sem Equipamento"
                }
                val equipCut = if (equipmentDisplayName.length > 25) equipmentDisplayName.take(22) + "..." else equipmentDisplayName
                
                canvas.drawText("$formattedOS - $dateStr", 40f, y, paintText)
                canvas.drawText(o.tipoServico, 170f, y, paintText)
                canvas.drawText(equipCut, 310f, y, paintText)
                canvas.drawText(o.status, 490f, y, paintText)
                y += 16f
            }
            if (osFiltradas.size > 10) {
                checkAndCreateNewPage(18f)
                canvas.drawText("+ ${osFiltradas.size - 10} ordens no histórico...", 40f, y, paintText)
                y += 16f
            }
        } else {
            canvas.drawText("Nenhuma ordem de serviço cadastrada para este cliente.", 40f, y, paintText)
            y += 16f
        }

        pdfDocument.finishPage(page)

        val file = File(context.cacheDir, "Ficha_Cliente_${cliente.id}.pdf")
        try {
            val fos = FileOutputStream(file)
            pdfDocument.writeTo(fos)
            pdfDocument.close()
            fos.close()
            return file
        } catch (e: Exception) {
            e.printStackTrace()
            pdfDocument.close()
            return null
        }
    }

    // Generates PDF of a preventive maintenance program
    fun generateProgramaPreventivoPdf(
        context: Context,
        prog: com.priorizedev.brizza.data.model.ProgramaPreventivo,
        cliente: Cliente?,
        tecnico: com.priorizedev.brizza.data.model.Tecnico?,
        equipamentos: List<com.priorizedev.brizza.data.model.Equipamento>,
        ambientes: List<com.priorizedev.brizza.data.model.Ambiente>
    ): File? {
        val sharedPrefs = context.getSharedPreferences("climagest_prefs", Context.MODE_PRIVATE)
        val companyName = sharedPrefs.getString("company_name", "Ar-Control Climatização") ?: "Ar-Control Climatização"
        val companyCnpj = sharedPrefs.getString("company_cnpj", "12.345.678/0001-90") ?: "12.345.678/0001-90"
        val companyPhone = sharedPrefs.getString("company_phone", "(11) 98765-4321") ?: "(11) 98765-4321"
        val companyEmail = sharedPrefs.getString("company_email", "contato@arcontrol.com.br") ?: "contato@arcontrol.com.br"

        val pdfDocument = PdfDocument()
        var pageNum = 1
        var pageInfo = PdfDocument.PageInfo.Builder(595, 842, pageNum).create()
        var page = pdfDocument.startPage(pageInfo)
        var canvas = page.canvas

        val paintText = Paint()
        val paintTitle = Paint()

        // Colors
        val primaryColor = 0xFF0D9488.toInt() // Teal
        val textColor = 0xFF1F2937.toInt() // Gray 800
        val secondaryTextColor = 0xFF4B5563.toInt() // Gray 600
        val lightGray = 0xFFF3F4F6.toInt() // Gray 100

        var y = 40f

        fun drawHeader() {
            // Draw teal banner
            val paintBanner = Paint()
            paintBanner.color = primaryColor
            canvas.drawRect(30f, y, 565f, y + 60f, paintBanner)

            paintTitle.color = Color.WHITE
            paintTitle.textSize = 16f
            paintTitle.isFakeBoldText = true
            paintTitle.textAlign = Paint.Align.CENTER
            canvas.drawText("PROGRAMA DE MANUTENÇÃO PREVENTIVA", 297.5f, y + 26f, paintTitle)

            paintTitle.textSize = 10f
            paintTitle.isFakeBoldText = false
            canvas.drawText("Plano de Atendimento Periódico e Higienização", 297.5f, y + 44f, paintTitle)

            y += 75f

            // Company info block
            paintText.color = textColor
            paintText.textSize = 10f
            paintText.isFakeBoldText = true
            canvas.drawText(companyName, 35f, y, paintText)
            paintText.isFakeBoldText = false
            paintText.color = secondaryTextColor
            paintText.textSize = 9f
            canvas.drawText("CNPJ: $companyCnpj", 35f, y + 14f, paintText)
            canvas.drawText("Contato: $companyPhone | $companyEmail", 35f, y + 26f, paintText)

            y += 45f
        }

        fun checkAndCreateNewPage(neededHeight: Float) {
            if (y + neededHeight > 800f) {
                pdfDocument.finishPage(page)
                pageNum++
                pageInfo = PdfDocument.PageInfo.Builder(595, 842, pageNum).create()
                page = pdfDocument.startPage(pageInfo)
                canvas = page.canvas
                y = 40f
                drawHeader()
            }
        }

        fun drawSectionTitle(title: String) {
            checkAndCreateNewPage(30f)
            val paintRect = Paint()
            paintRect.color = primaryColor
            canvas.drawRect(30f, y, 35f, y + 15f, paintRect)

            paintText.color = textColor
            paintText.textSize = 11f
            paintText.isFakeBoldText = true
            canvas.drawText(title, 42f, y + 12f, paintText)
            y += 24f
        }

        drawHeader()

        // 1. Client & Program details
        drawSectionTitle("INFORMAÇÕES GERAIS")
        paintText.textSize = 10f
        paintText.isFakeBoldText = false
        paintText.color = textColor

        canvas.drawText("Cliente:", 35f, y, paintText)
        paintText.isFakeBoldText = true
        canvas.drawText(cliente?.nome ?: "Não atribuído", 110f, y, paintText)

        paintText.isFakeBoldText = false
        canvas.drawText("Endereço:", 35f, y + 15f, paintText)
        canvas.drawText(cliente?.endereco ?: "Não informado", 110f, y + 15f, paintText)

        canvas.drawText("Técnico Resp.:", 290f, y, paintText)
        paintText.isFakeBoldText = true
        canvas.drawText(tecnico?.nome ?: "Não atribuído", 370f, y, paintText)

        paintText.isFakeBoldText = false
        canvas.drawText("Periodicidade:", 290f, y + 15f, paintText)
        paintText.isFakeBoldText = true
        canvas.drawText(prog.periodo.uppercase(Locale.getDefault()), 370f, y + 15f, paintText)

        paintText.isFakeBoldText = false
        canvas.drawText("Próxima Data:", 290f, y + 30f, paintText)
        paintText.isFakeBoldText = true
        canvas.drawText(prog.dataAgendada, 370f, y + 30f, paintText)

        y += 48f

        // 2. Equipment listing
        drawSectionTitle("CRONOGRAMA DE EQUIPAMENTOS")
        
        // Header of table
        val paintTableBg = Paint()
        paintTableBg.color = lightGray
        canvas.drawRect(30f, y, 565f, y + 20f, paintTableBg)

        paintText.textSize = 9f
        paintText.isFakeBoldText = true
        paintText.color = textColor
        canvas.drawText("Equipamento", 35f, y + 14f, paintText)
        canvas.drawText("Setor/Ambiente", 250f, y + 14f, paintText)
        canvas.drawText("Status", 420f, y + 14f, paintText)
        canvas.drawText("Responsável / Data", 495f, y + 14f, paintText)

        y += 26f

        val parsed = parseEquipamentosList(prog.equipamentosSerialized)
        parsed.forEach { triple ->
            val eqId = triple.first
            val eqStatus = triple.second
            val eqDate = triple.third

            val eqItem = equipamentos.find { it.id == eqId }
            val eqAmb = eqItem?.let { item -> ambientes.find { it.id == item.ambienteId } }

            checkAndCreateNewPage(22f)

            val eqName = eqItem?.let { "${it.marca} ${it.capacidadeBtu} BTU/h" } ?: "Equipamento Removido"
            val eqNameCut = if (eqName.length > 30) eqName.take(27) + "..." else eqName
            val ambName = eqAmb?.nome ?: "Geral"
            val ambNameCut = if (ambName.length > 25) ambName.take(22) + "..." else ambName

            paintText.isFakeBoldText = false
            paintText.textSize = 9f
            paintText.color = textColor
            canvas.drawText(eqNameCut, 35f, y, paintText)
            canvas.drawText(ambNameCut, 250f, y, paintText)

            val statusPaint = Paint()
            statusPaint.textSize = 8f
            statusPaint.isFakeBoldText = true
            if (eqStatus == "Concluído") {
                statusPaint.color = 0xFF16A34A.toInt() // Green
                canvas.drawText("CONCLUÍDO", 420f, y, statusPaint)
                canvas.drawText(eqDate, 495f, y, paintText)
            } else {
                statusPaint.color = 0xFFD97706.toInt() // Orange
                canvas.drawText("PENDENTE", 420f, y, statusPaint)
                canvas.drawText("-", 495f, y, paintText)
            }

            // Divider line
            val paintDivider = Paint()
            paintDivider.color = 0xFFE5E7EB.toInt() // Gray 200
            canvas.drawLine(30f, y + 6f, 565f, y + 6f, paintDivider)

            y += 18f
        }

        pdfDocument.finishPage(page)

        val outputFile = File(context.cacheDir, "Programa_Preventivo_${prog.id}.pdf")
        try {
            val fos = FileOutputStream(outputFile)
            pdfDocument.writeTo(fos)
            pdfDocument.close()
            fos.close()
            return outputFile
        } catch (e: Exception) {
            e.printStackTrace()
            pdfDocument.close()
            return null
        }
    }

    private fun parseEquipamentosList(serialized: String): List<Triple<String, String, String>> {
        if (serialized.isBlank()) return emptyList()
        return serialized.split(";").filter { it.isNotBlank() }.map { part ->
            val fields = part.split("|")
            val id = fields.getOrNull(0) ?: ""
            val status = fields.getOrNull(1) ?: "Pendente"
            val date = fields.getOrNull(2) ?: ""
            Triple(id, status, date)
        }
    }
}
