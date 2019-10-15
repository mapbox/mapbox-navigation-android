package com.mapbox.services.android.navigation.v5.utils.extensions

inline fun <R1, T> ifNonNull(r1: R1?, func: (R1) -> T): T? =
    if (r1 != null) {
        func(r1)
    } else {
        null
    }

inline fun <R1, R2, T> ifNonNull(r1: R1?, r2: R2?, func: (R1, R2) -> T): T? =
    if (r1 != null && r2 != null) {
        func(r1, r2)
    } else {
        null
    }

inline fun <R1, R2, R3, T> ifNonNull(r1: R1?, r2: R2?, r3: R3?, func: (R1, R2, R3) -> T): T? =
    if (r1 != null && r2 != null && r3 != null) {
        func(r1, r2, r3)
    } else {
        null
    }

inline fun <R1, R2, R3, R4, T> ifNonNull(
    r1: R1?,
    r2: R2?,
    r3: R3?,
    r4: R4?,
    func: (R1, R2, R3, R4) -> T
): T? =
    if (r1 != null && r2 != null && r3 != null && r4 != null) {
        func(r1, r2, r3, r4)
    } else {
        null
    }

inline fun <R1, R2, R3, R4, R5, T> ifNonNull(
    r1: R1?,
    r2: R2?,
    r3: R3?,
    r4: R4?,
    r5: R5?,
    func: (R1, R2, R3, R4, R5) -> T
): T? =
    if (r1 != null && r2 != null && r3 != null && r4 != null && r5 != null) {
        func(r1, r2, r3, r4, r5)
    } else {
        null
    }

inline fun <R1, R2, R3, R4, R5, R6, T> ifNonNull(
    r1: R1?,
    r2: R2?,
    r3: R3?,
    r4: R4?,
    r5: R5?,
    r6: R6?,
    func: (R1, R2, R3, R4, R5, R6) -> T
): T? =
    if (r1 != null && r2 != null && r3 != null && r4 != null && r5 != null && r6 != null) {
        func(r1, r2, r3, r4, r5, r6)
    } else {
        null
    }

inline fun <R1, R2, R3, R4, R5, R6, R7, T> ifNonNull(
    r1: R1?,
    r2: R2?,
    r3: R3?,
    r4: R4?,
    r5: R5?,
    r6: R6?,
    r7: R7?,
    func: (R1, R2, R3, R4, R5, R6, R7) -> T
): T? =
    if (r1 != null && r2 != null && r3 != null && r4 != null && r5 != null && r6 != null && r7 != null) {
        func(r1, r2, r3, r4, r5, r6, r7)
    } else {
        null
    }

inline fun <R1, R2, R3, R4, R5, R6, R7, R8, T> ifNonNull(
    r1: R1?,
    r2: R2?,
    r3: R3?,
    r4: R4?,
    r5: R5?,
    r6: R6?,
    r7: R7?,
    r8: R8?,
    func: (R1, R2, R3, R4, R5, R6, R7, R8) -> T
): T? =
    if (r1 != null && r2 != null && r3 != null && r4 != null && r5 != null && r6 != null && r7 != null && r8 != null) {
        func(r1, r2, r3, r4, r5, r6, r7, r8)
    } else {
        null
    }

inline fun <R1, R2, R3, R4, R5, R6, R7, R8, R9, T> ifNonNull(
    r1: R1?,
    r2: R2?,
    r3: R3?,
    r4: R4?,
    r5: R5?,
    r6: R6?,
    r7: R7?,
    r8: R8?,
    r9: R9?,
    func: (R1, R2, R3, R4, R5, R6, R7, R8, R9) -> T
): T? =
    if (r1 != null && r2 != null && r3 != null && r4 != null && r5 != null && r6 != null && r7 != null && r8 != null && r9 != null) {
        func(r1, r2, r3, r4, r5, r6, r7, r8, r9)
    } else {
        null
    }

inline fun <R1, R2, R3, R4, R5, R6, R7, R8, R9, R10, T> ifNonNull(
    r1: R1?,
    r2: R2?,
    r3: R3?,
    r4: R4?,
    r5: R5?,
    r6: R6?,
    r7: R7?,
    r8: R8?,
    r9: R9?,
    r10: R10?,
    func: (R1, R2, R3, R4, R5, R6, R7, R8, R9, R10) -> T
): T? =
    if (r1 != null && r2 != null && r3 != null && r4 != null && r5 != null && r6 != null && r7 != null && r8 != null && r9 != null && r10 != null) {
        func(r1, r2, r3, r4, r5, r6, r7, r8, r9, r10)
    } else {
        null
    }

inline fun <R1, R2, R3, R4, R5, R6, R7, R8, R9, R10, R11, T> ifNonNull(
    r1: R1?,
    r2: R2?,
    r3: R3?,
    r4: R4?,
    r5: R5?,
    r6: R6?,
    r7: R7?,
    r8: R8?,
    r9: R9?,
    r10: R10?,
    r11: R11?,
    func: (R1, R2, R3, R4, R5, R6, R7, R8, R9, R10, R11) -> T
): T? =
    if (r1 != null && r2 != null && r3 != null && r4 != null && r5 != null && r6 != null && r7 != null && r8 != null && r9 != null && r10 != null && r11 != null) {
        func(r1, r2, r3, r4, r5, r6, r7, r8, r9, r10, r11)
    } else {
        null
    }
