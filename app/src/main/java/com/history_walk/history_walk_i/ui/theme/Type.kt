package com.history_walk.history_walk_i.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.history_walk.history_walk_i.R.font.inknut_antiqua_regular
import com.history_walk.history_walk_i.R.font.jacquard_24_regular
import com.history_walk.history_walk_i.R.font.jacques_francois_regular


val Inknut_Antiqua = FontFamily(
    Font(inknut_antiqua_regular, FontWeight.Normal)
)


val Jacquard_24 = FontFamily(
    Font(jacquard_24_regular, FontWeight.Normal)
)


val Jacques_Francois = FontFamily(
    Font(jacques_francois_regular, FontWeight.Normal)
)


val Typography = Typography(
    titleSmall = TextStyle(
        fontFamily = Inknut_Antiqua,
        fontWeight = FontWeight.Normal,
        fontSize = 24.sp
    ),
    titleMedium = TextStyle(
        fontFamily = Jacques_Francois,
        fontWeight = FontWeight.Normal,
        fontSize = 45.sp
    ),
    titleLarge = TextStyle(
        fontFamily = Jacquard_24,
        fontWeight = FontWeight.Normal,
        fontSize = 45.sp
    ),
    displayLarge = TextStyle(
        fontFamily = Jacques_Francois,
        fontWeight = FontWeight.Normal,
        fontSize = 24.sp
    ),
    displayMedium = TextStyle(
        fontFamily = Jacques_Francois,
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp
    ),
    displaySmall = TextStyle(
        fontFamily = Jacques_Francois,
        fontWeight = FontWeight.Normal,
        fontSize = 13.5.sp
    ),
    labelLarge = TextStyle(
        fontFamily = Inknut_Antiqua,
        fontWeight = FontWeight.Normal,
        fontSize = 18.sp
    )
)