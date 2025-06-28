package ru.hd.hdbotc.model;

public enum ScriptStyles {

    TRANSPARENT_CLASSIC("Прозрачный с обычным шрифтом"),
    WHITE_CLASSIC("Белый фон с обычным шрифтом"),
    TRANSPARENT_AMATIC("Прозрачный с узким шрифтом"),
    BLACK_PRINT("Черно-белый принтер"),
    ;
    final String name;

    ScriptStyles(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }
}
