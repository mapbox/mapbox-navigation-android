package com.google.flatbuffers

internal val Table.offset: Int
    get() = this.bb_pos
