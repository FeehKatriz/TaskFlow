package com.example.taskflow.data.repository

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import android.util.Log
import com.example.taskflow.ui.tarefa.Arquivo
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageReference

class ArquivoRepository {
    private val storage = FirebaseStorage.getInstance()
    private val storageRef = storage.reference

    /**
     * Carrega todos os arquivos anexados a uma tarefa específica
     */
    fun carregarArquivos(
        tarefaId: String,
        callback: (Result<List<Arquivo>>) -> Unit
    ) {
        val tarefaStorageRef = storageRef.child("tarefas/$tarefaId")

        tarefaStorageRef.listAll()
            .addOnSuccessListener { listResult ->
                if (listResult.items.isEmpty()) {
                    callback(Result.success(emptyList()))
                    return@addOnSuccessListener
                }

                val arquivos = mutableListOf<Arquivo>()
                var processedCount = 0

                listResult.items.forEach { itemRef ->
                    itemRef.metadata
                        .addOnSuccessListener { metadata ->
                            val arquivo = Arquivo(
                                nome = itemRef.name,
                                mimeType = metadata.contentType ?: "",
                                ref = itemRef
                            )
                            arquivos.add(arquivo)
                            processedCount++

                            if (processedCount == listResult.items.size) {
                                callback(Result.success(arquivos))
                            }
                        }
                        .addOnFailureListener { e ->
                            Log.w("ArquivoRepository", "Erro ao obter metadata do arquivo ${itemRef.name}", e)
                            // Adiciona arquivo mesmo sem metadata
                            val arquivo = Arquivo(
                                nome = itemRef.name,
                                mimeType = "",
                                ref = itemRef
                            )
                            arquivos.add(arquivo)
                            processedCount++

                            if (processedCount == listResult.items.size) {
                                callback(Result.success(arquivos))
                            }
                        }
                }
            }
            .addOnFailureListener { e ->
                Log.e("ArquivoRepository", "Erro ao listar arquivos da tarefa $tarefaId", e)
                callback(Result.failure(e))
            }
    }

    /**
     * Faz upload de um arquivo para uma tarefa específica
     */
    fun uploadArquivo(
        tarefaId: String,
        fileUri: Uri,
        context: Context,
        callback: (Result<String>) -> Unit
    ) {
        val fileName = obterNomeArquivoOriginal(context, fileUri)
        val fileRef = storageRef.child("tarefas/$tarefaId/$fileName")

        fileRef.putFile(fileUri)
            .addOnSuccessListener {
                Log.d("ArquivoRepository", "✅ Arquivo enviado - Tarefa: $tarefaId, Arquivo: $fileName")
                callback(Result.success(fileName))
            }
            .addOnFailureListener { e ->
                Log.e("ArquivoRepository", "❌ Erro ao enviar arquivo", e)
                callback(Result.failure(e))
            }
    }

    /**
     * Obtém a URL de download de um arquivo
     */
    fun obterDownloadUrl(
        fileRef: StorageReference,
        callback: (Result<Uri>) -> Unit
    ) {
        fileRef.downloadUrl
            .addOnSuccessListener { uri ->
                callback(Result.success(uri))
            }
            .addOnFailureListener { e ->
                Log.e("ArquivoRepository", "Erro ao obter URL de download", e)
                callback(Result.failure(e))
            }
    }

    /**
     * Deleta um arquivo específico
     */
    fun deletarArquivo(
        tarefaId: String,
        nomeArquivo: String,
        callback: (Result<Unit>) -> Unit
    ) {
        val fileRef = storageRef.child("tarefas/$tarefaId/$nomeArquivo")

        fileRef.delete()
            .addOnSuccessListener {
                Log.d("ArquivoRepository", "✅ Arquivo deletado - Tarefa: $tarefaId, Arquivo: $nomeArquivo")
                callback(Result.success(Unit))
            }
            .addOnFailureListener { e ->
                Log.e("ArquivoRepository", "❌ Erro ao deletar arquivo", e)
                callback(Result.failure(e))
            }
    }

    /**
     * Deleta todos os arquivos de uma tarefa
     */
    fun deletarTodosArquivosDaTarefa(
        tarefaId: String,
        callback: (Result<Unit>) -> Unit
    ) {
        val tarefaStorageRef = storageRef.child("tarefas/$tarefaId")

        tarefaStorageRef.listAll()
            .addOnSuccessListener { listResult ->
                if (listResult.items.isEmpty()) {
                    callback(Result.success(Unit))
                    return@addOnSuccessListener
                }

                var deletedCount = 0
                val totalFiles = listResult.items.size

                listResult.items.forEach { itemRef ->
                    itemRef.delete()
                        .addOnSuccessListener {
                            deletedCount++
                            if (deletedCount == totalFiles) {
                                Log.d("ArquivoRepository", "✅ Todos os arquivos da tarefa $tarefaId foram deletados")
                                callback(Result.success(Unit))
                            }
                        }
                        .addOnFailureListener { e ->
                            deletedCount++
                            Log.w("ArquivoRepository", "Erro ao deletar arquivo ${itemRef.name}", e)
                            if (deletedCount == totalFiles) {
                                callback(Result.success(Unit))
                            }
                        }
                }
            }
            .addOnFailureListener { e ->
                Log.e("ArquivoRepository", "Erro ao listar arquivos para deletar", e)
                callback(Result.failure(e))
            }
    }

    /**
     * Obtém o tamanho de um arquivo em bytes
     */
    fun obterTamanhoArquivo(
        fileRef: StorageReference,
        callback: (Result<Long>) -> Unit
    ) {
        fileRef.metadata
            .addOnSuccessListener { metadata ->
                callback(Result.success(metadata.sizeBytes))
            }
            .addOnFailureListener { e ->
                Log.e("ArquivoRepository", "Erro ao obter tamanho do arquivo", e)
                callback(Result.failure(e))
            }
    }

    // ==================== UTILITÁRIOS ====================

    /**
     * Obtém o nome original do arquivo usando ContentResolver
     */
    private fun obterNomeArquivoOriginal(context: Context, fileUri: Uri): String {
        var nomeArquivo = "arquivo_${System.currentTimeMillis()}"
        
        context.contentResolver.query(fileUri, null, null, null, null)?.use { cursor ->
            if (cursor.moveToFirst()) {
                val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if (nameIndex >= 0) {
                    nomeArquivo = cursor.getString(nameIndex)
                }
            }
        }
        
        return nomeArquivo
    }

    /**
     * Verifica se um arquivo existe no storage
     */
    fun verificarArquivoExiste(
        tarefaId: String,
        nomeArquivo: String,
        callback: (Result<Boolean>) -> Unit
    ) {
        val fileRef = storageRef.child("tarefas/$tarefaId/$nomeArquivo")

        fileRef.metadata
            .addOnSuccessListener {
                callback(Result.success(true))
            }
            .addOnFailureListener {
                callback(Result.success(false))
            }
    }
}