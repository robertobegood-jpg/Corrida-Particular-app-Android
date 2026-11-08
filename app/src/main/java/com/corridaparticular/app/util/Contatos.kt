package com.corridaparticular.app.util

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.ContactsContract.CommonDataKinds.Phone
import android.provider.ContactsContract.CommonDataKinds.StructuredPostal
import androidx.core.content.ContextCompat
import com.corridaparticular.app.data.Endereco

/** Lê nome, telefone e endereços de um contato escolhido na agenda do celular. */
object Contatos {

    data class Dados(val nome: String, val telefone: String, val enderecos: List<Endereco>)

    /** Abre a lista de contatos do Android para escolher um número. */
    fun intentEscolher(): Intent = Intent(Intent.ACTION_PICK, Phone.CONTENT_URI)

    fun temPermissao(ctx: Context): Boolean =
        ContextCompat.checkSelfPermission(ctx, Manifest.permission.READ_CONTACTS) == PackageManager.PERMISSION_GRANTED

    fun ler(ctx: Context, uri: Uri): Dados? {
        val cr = ctx.contentResolver
        var nome = ""
        var fone = ""
        var contatoId = -1L
        runCatching {
            cr.query(uri, arrayOf(Phone.DISPLAY_NAME, Phone.NUMBER, Phone.CONTACT_ID), null, null, null)?.use { c ->
                if (c.moveToFirst()) {
                    nome = c.getString(0).orEmpty()
                    fone = c.getString(1).orEmpty()
                    contatoId = c.getLong(2)
                }
            }
        }
        if (nome.isBlank() && fone.isBlank()) return null

        // Endereços só podem ser lidos com a permissão de contatos.
        val enderecos = mutableListOf<Endereco>()
        if (contatoId >= 0 && temPermissao(ctx)) {
            runCatching {
                cr.query(
                    StructuredPostal.CONTENT_URI,
                    arrayOf(StructuredPostal.FORMATTED_ADDRESS, StructuredPostal.TYPE, StructuredPostal.LABEL),
                    "${StructuredPostal.CONTACT_ID} = ?",
                    arrayOf(contatoId.toString()),
                    null,
                )?.use { c ->
                    while (c.moveToNext()) {
                        val texto = c.getString(0).orEmpty()
                            .split('\n').map { it.trim() }.filter { it.isNotEmpty() }.joinToString(", ")
                        if (texto.isBlank()) continue
                        val apelido = when (c.getInt(1)) {
                            StructuredPostal.TYPE_HOME -> "Casa"
                            StructuredPostal.TYPE_WORK -> "Trabalho"
                            else -> c.getString(2).orEmpty()
                        }
                        enderecos.add(Endereco(apelido = apelido, texto = texto))
                    }
                }
            }
        }
        return Dados(nome.trim(), fone.trim(), enderecos)
    }

    /** Compara telefones ignorando formatação e o 55 do país. */
    fun mesmoNumero(a: String, b: String): Boolean {
        fun n(s: String) = s.filter { it.isDigit() }.trimStart('0').removePrefix("55").takeLast(11)
        val x = n(a); val y = n(b)
        return x.length >= 8 && y.length >= 8 && (x == y || x.takeLast(8) == y.takeLast(8))
    }
}
