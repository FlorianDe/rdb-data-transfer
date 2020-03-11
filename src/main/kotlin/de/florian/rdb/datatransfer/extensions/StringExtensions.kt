package de.florian.rdb.datatransfer.extensions

fun CharSequence.isInt() = "$this".toIntOrNull()!=null