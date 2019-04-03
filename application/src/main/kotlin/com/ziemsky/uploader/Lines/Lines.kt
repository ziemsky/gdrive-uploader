package com.ziemsky.uploader.Lines

import java.util.stream.Stream


class Lines(vararg lines: String) {

    private val lines: List<Line> = lines.map { line -> Line(line) }

    fun stream(): Stream<Line> {
        return lines.stream()
    }
}

