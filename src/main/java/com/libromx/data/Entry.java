package com.libromx.data;

/** Una entrada de cualquiera de los tres libros. */
public class Entry {
    public String title = "";
    public String desc = "";
    /** Solo misiones: kill, collect o craft. */
    public String type = "kill";
    /** Solo misiones: id del mob u objeto, por ejemplo minecraft:creeper. */
    public String target = "";
    /** Solo dificultades: peaceful, easy, normal o hard. */
    public String base = "normal";

    public Entry() {
    }

    public Entry(String title, String desc, String type, String target, String base) {
        this.title = title;
        this.desc = desc;
        this.type = type;
        this.target = target;
        this.base = base;
    }
}
