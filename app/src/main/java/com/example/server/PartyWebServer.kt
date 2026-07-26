package com.example.server

import fi.iki.elonen.NanoHTTPD
import com.example.data.JellyfinItem
import com.example.data.QrSongRequest
import org.json.JSONArray
import org.json.JSONObject
import java.io.InputStream
import java.net.NetworkInterface
import java.util.Collections

class PartyWebServer(
    port: Int = 8080,
    private val onRequestReceived: (guestName: String, songTitle: String) -> QrSongRequest,
    private val getAvailableSongs: () -> List<JellyfinItem>
) : NanoHTTPD(port) {

    override fun serve(session: IHTTPSession): Response {
        val uri = session.uri
        val method = session.method

        if (method == Method.GET && (uri == "/" || uri == "/index.html" || uri.startsWith("/jellymusic") || uri.startsWith("/cardamu"))) {
            return newFixedLengthResponse(Response.Status.OK, "text/html", getWebPageHtml())
        }

        if (method == Method.GET && uri == "/api/songs") {
            val songs = getAvailableSongs()
            val jsonArray = JSONArray()
            songs.forEach { song ->
                val obj = JSONObject().apply {
                    put("id", song.id)
                    put("name", song.name)
                    put("artist", song.artists?.joinToString(", ") ?: (song.album ?: "Artista desconocido"))
                }
                jsonArray.put(obj)
            }
            return newFixedLengthResponse(
                Response.Status.OK,
                "application/json",
                jsonArray.toString()
            ).apply {
                addHeader("Access-Control-Allow-Origin", "*")
            }
        }

        if (method == Method.POST && uri == "/api/request") {
            return try {
                // Read the input stream directly for application/json
                val inputStream = session.inputStream
                val jsonString = inputStream.bufferedReader().use { it.readText() }

                val jsonArray = JSONArray(jsonString)
                if (jsonArray.length() == 0) {
                    return newFixedLengthResponse(
                        Response.Status.BAD_REQUEST,
                        "application/json",
                        "{\"success\": false, \"message\": \"El cuerpo de la petición está vacío o no es un array válido.\"}"
                    )
                }

                val requestObject = jsonArray.getJSONObject(0) // Get the first object from the array
                val guestName = requestObject.optString("guestName", "Invitado")
                val songTitle = requestObject.optString("songTitle", "")

                if (songTitle.isBlank()) {
                    return newFixedLengthResponse(
                        Response.Status.BAD_REQUEST,
                        "application/json",
                        "{\"success\": false, \"message\": \"Falta el nombre de la canción\"}"
                    )
                }

                val req = onRequestReceived(guestName, songTitle)

                val responseObj = JSONObject().apply {
                    put("success", true)
                    put("message", "¡Canción agregada con éxito!")
                    put("guestName", req.guestName)
                    put("songTitle", req.songTitle)
                }

                newFixedLengthResponse(
                    Response.Status.OK,
                    "application/json",
                    responseObj.toString()
                ).apply {
                    addHeader("Access-Control-Allow-Origin", "*")
                }
            } catch (e: Exception) {
                newFixedLengthResponse(
                    Response.Status.INTERNAL_ERROR,
                    "application/json",
                    "{\"success\": false, \"error\": \"${e.message}\"}"
                )
            }
        }

        if (method == Method.OPTIONS) {
            return newFixedLengthResponse(Response.Status.OK, MIME_PLAINTEXT, "").apply {
                addHeader("Access-Control-Allow-Origin", "*")
                addHeader("Access-Control-Allow-Methods", "GET, POST, OPTIONS")
                addHeader("Access-Control-Allow-Headers", "Content-Type")
            }
        }

        return newFixedLengthResponse(Response.Status.NOT_FOUND, MIME_PLAINTEXT, "Página no encontrada")
    }

    private fun getWebPageHtml(): String {
        return """
            <!DOCTYPE html>
            <html lang="es">
            <head>
                <meta charset="UTF-8">
                <meta name="viewport" content="width=device-width, initial-scale=1.0">
                <title>🎉 Fiesta de Música - Pedir Canción</title>
                <style>
                    * { box-sizing: border-box; margin: 0; padding: 0; font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, Helvetica, Arial, sans-serif; }
                    body { background: #0A0D14; color: #FFFFFF; display: flex; justify-content: center; padding: 20px 12px; min-height: 100vh; }
                    .card { background: #141824; border: 1px solid #1E2638; border-radius: 20px; width: 100%; max-width: 440px; padding: 24px; box-shadow: 0 10px 30px rgba(0,0,0,0.5); }
                    .header { text-align: center; margin-bottom: 20px; }
                    .badge { background: linear-gradient(135deg, #00E5FF, #00E676); color: #000; font-weight: bold; font-size: 12px; padding: 4px 12px; border-radius: 20px; display: inline-block; margin-bottom: 8px; }
                    h1 { font-size: 22px; font-weight: 800; color: #FFF; margin-bottom: 4px; }
                    p.sub { font-size: 13px; color: #9E9E9E; }
                    .form-group { margin-bottom: 16px; }
                    label { font-size: 12px; font-weight: 600; color: #B0BEC5; display: block; margin-bottom: 6px; }
                    input[type="text"] { width: 100%; background: #0D111A; border: 1px solid #2A344A; color: #FFF; padding: 12px 14px; border-radius: 12px; font-size: 14px; outline: none; transition: border 0.2s; }
                    input[type="text"]:focus { border-color: #00E5FF; }
                    .btn-submit { width: 100%; background: linear-gradient(135deg, #00E5FF, #00E676); color: #000; font-weight: 800; font-size: 15px; border: none; padding: 14px; border-radius: 12px; cursor: pointer; margin-top: 8px; transition: transform 0.1s, opacity 0.2s; }
                    .btn-submit:active { transform: scale(0.98); }
                    .btn-submit:disabled { opacity: 0.5; cursor: not-allowed; }
                    .songs-list { max-height: 220px; overflow-y: auto; margin-top: 10px; border-radius: 10px; background: #0D111A; border: 1px solid #1E2638; }
                    .song-item { padding: 10px 12px; border-bottom: 1px solid #1A2234; cursor: pointer; transition: background 0.2s; display: flex; justify-content: space-between; align-items: center; }
                    .song-item:last-child { border-bottom: none; }
                    .song-item:hover { background: #182236; }
                    .song-title { font-size: 13px; font-weight: bold; color: #FFF; }
                    .song-artist { font-size: 11px; color: #9E9E9E; }
                    .btn-add { background: #00E5FF; color: #000; border: none; border-radius: 14px; padding: 4px 10px; font-size: 11px; font-weight: bold; }
                    .status-msg { margin-top: 14px; padding: 12px; border-radius: 10px; text-align: center; font-size: 13px; font-weight: bold; display: none; }
                    .status-msg.success { background: rgba(0, 230, 118, 0.15); color: #00E676; border: 1px solid #00E676; }
                    .status-msg.error { background: rgba(255, 82, 82, 0.15); color: #FF5252; border: 1px solid #FF5252; }
                </style>
            </head>
            <body>
                <div class="card">
                    <div class="header">
                        <span class="badge">🎵 FIESTA EN VIVO</span>
                        <h1>Pedir Canción al Anfitrión</h1>
                        <p class="sub">Elige un tema para que se agregue automáticamente a la playlist de la fiesta</p>
                    </div>

                    <div class="form-group">
                        <label for="guestName">TU NOMBRE / APODO</label>
                        <input type="text" id="guestName" placeholder="Ej: Carlos" maxlength="30">
                    </div>

                    <div class="form-group">
                        <label for="songSearch">BUSCAR O ESCRIBIR CANCIÓN</label>
                        <input type="text" id="songSearch" placeholder="Escribe el título o artista..." oninput="filterSongs()">
                    </div>

                    <div id="songsContainer" class="songs-list">
                        <div style="padding: 12px; text-align: center; color: #888; font-size: 12px;">Cargando catálogo de música...</div>
                    </div>

                    <button class="btn-submit" id="btnSendManual" onclick="sendRequest()">➕ Enviar Canción Manual</button>

                    <div id="statusBox" class="status-msg"></div>
                </div>

                <script>
                    let songsCatalog = [];

                    async function loadSongs() {
                        try {
                            const res = await fetch('/api/songs');
                            if (res.ok) {
                                songsCatalog = await res.json();
                                renderSongs(songsCatalog);
                            }
                        } catch (e) {
                            document.getElementById('songsContainer').innerHTML = '<div style="padding:12px; text-align:center; color:#888; font-size:12px;">Escribe el nombre abajo y presiona Enviar</div>';
                        }
                    }

                    function renderSongs(songs) {
                        const container = document.getElementById('songsContainer');
                        if (!songs || songs.length === 0) {
                            container.innerHTML = '<div style="padding:12px; text-align:center; color:#888; font-size:12px;">Sin resultados. Escribe tu canción abajo.</div>';
                            return;
                        }
                        container.innerHTML = songs.map(function(s) {
                            return '<div class="song-item" onclick="selectSong(\'' + escapeHtml(s.name) + '\')">' +
                                '<div>' +
                                    '<div class="song-title">' + escapeHtml(s.name) + '</div>' +
                                    '<div class="song-artist">' + escapeHtml(s.artist) + '</div>' +
                                '</div>' +
                                '<button class="btn-add">Pedir</button>' +
                            '</div>';
                        }).join('');
                    }

                    function filterSongs() {
                        const q = document.getElementById('songSearch').value.toLowerCase().trim();
                        if (!q) {
                            renderSongs(songsCatalog.slice(0, 15));
                            return;
                        }
                        const filtered = songsCatalog.filter(s => 
                            s.name.toLowerCase().includes(q) || s.artist.toLowerCase().includes(q)
                        );
                        renderSongs(filtered);
                    }

                    function selectSong(title) {
                        document.getElementById('songSearch').value = title;
                        sendRequest();
                    }

                    function escapeHtml(str) {
                        return str ? str.replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/>/g, '&gt;').replace(/"/g, '&quot;') : '';
                    }

                    async function sendRequest() {
                        const guestName = document.getElementById('guestName').value.trim() || 'Invitado';
                        const songTitle = document.getElementById('songSearch').value.trim();

                        const statusBox = document.getElementById('statusBox');
                        statusBox.style.display = 'none';

                        if (!songTitle) {
                            showStatus('Ingresa o selecciona una canción', false);
                            return;
                        }

                        const btn = document.getElementById('btnSendManual');
                        btn.disabled = true;

                        try {
                            const res = await fetch('/api/request', {
                                method: 'POST',
                                headers: { 'Content-Type': 'application/json' },
                                body: JSON.stringify({ guestName, songTitle })
                            });

                            const data = await res.json();
                            if (res.ok && data.success) {
                                showStatus('✨ ¡"' + data.songTitle + '" añadida a la Playlist de la Fiesta!', true);
                                document.getElementById('songSearch').value = '';
                            } else {
                                showStatus(data.message || 'Error al enviar petición', false);
                            }
                        } catch (e) {
                            showStatus('¡Petición enviada!', true);
                        } finally {
                            btn.disabled = false;
                        }
                    }

                    function showStatus(msg, isSuccess) {
                        const statusBox = document.getElementById('statusBox');
                        statusBox.className = 'status-msg ' + (isSuccess ? 'success' : 'error');
                        statusBox.innerText = msg;
                        statusBox.style.display = 'block';
                    }

                    loadSongs();
                </script>
            </body>
            </html>
        """.trimIndent()
    }

    companion object {
        fun getLocalIpAddress(): String {
            try {
                val interfaces = Collections.list(NetworkInterface.getNetworkInterfaces())
                for (intf in interfaces) {
                    val addrs = Collections.list(intf.inetAddresses)
                    for (addr in addrs) {
                        if (!addr.isLoopbackAddress) {
                            val sAddr = addr.hostAddress
                            if (sAddr != null && !sAddr.contains(":")) { // IPv4
                                return sAddr
                            }
                        }
                    }
                }
            } catch (ignored: Exception) {}
            return "192.168.1.100"
        }
    }
}
