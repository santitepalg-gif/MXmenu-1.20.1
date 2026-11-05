package com.libromx.client;

import com.libromx.data.BookData;

/** Copia del contenido de los libros que el servidor le manda al cliente. */
public final class ClientData {
    public static BookData data = new BookData();
    public static boolean op = false;

    private ClientData() {
    }
}
