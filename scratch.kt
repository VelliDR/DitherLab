fun main() {
    var s = (12345L.toUInt() % 4294967296u)
    s = (1664525u * s + 1013904223u)
    println(s.toDouble() / 4294967296.0)
    s = (1664525u * s + 1013904223u)
    println(s.toDouble() / 4294967296.0)
}
