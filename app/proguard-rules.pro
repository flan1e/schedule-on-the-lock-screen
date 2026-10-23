# Room и Compose поставляют собственные consumer-правила.
# Здесь оставлены только правила для reflect-доступа к enum-константам (на будущее).
-keepclassmembers enum * {
    public static **[] values();
    public static ** valueOf(java.lang.String);
}
