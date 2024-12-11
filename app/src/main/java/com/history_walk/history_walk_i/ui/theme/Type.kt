package com.history_walk.history_walk_i.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.TextUnitType
import com.history_walk.history_walk_i.R.font.inknut_antiqua_regular
import com.history_walk.history_walk_i.R.font.jacquard_24_regular


val InknutAntiqua = FontFamily(
    Font(inknut_antiqua_regular, FontWeight.Normal)
)


val Jacquard_24 = FontFamily(
    Font(jacquard_24_regular, FontWeight.Normal)
)


val TypographyForIntroScreen = Typography(
    titleMedium = TextStyle(
        fontFamily = InknutAntiqua,
        fontWeight = FontWeight.Normal,
        fontSize = TextUnit(24f, TextUnitType.Sp)
    ),
    titleLarge = TextStyle(
        fontFamily = Jacquard_24,
        fontWeight = FontWeight.Normal,
        fontSize = TextUnit(45f, TextUnitType.Sp)
    )
)