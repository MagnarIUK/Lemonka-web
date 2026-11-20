package com.magnariuk.lemonka.ui.theme

import com.magnariuk.lemonka.auth.LoginStyle
import com.magnariuk.lemonka.features.admin.AdminStyle
import com.magnariuk.lemonka.features.calculator.CalculatorStyle
import com.magnariuk.lemonka.features.home.HomeStyle
import com.magnariuk.lemonka.features.profile.ProfileStyle
import com.magnariuk.lemonka.features.sheet.SheetStyle
import com.magnariuk.lemonka.features.subtitles.SubtitlesStyle
import com.magnariuk.lemonka.ui.components.*
import com.magnariuk.lemonka.ui.core.DragDropStyle
import com.magnariuk.lemonka.ui.core.Styled
import com.magnariuk.lemonka.ui.layout.AppLayoutStyle
import kotlinx.css.CssBuilder

/** All component styles, combined into the single `/static/app.css`. Register new components here. */
object Stylesheet {
    private val registry: List<Styled> = listOf(
        Theme,
        LayoutStyle,
        DragDropStyle,
        IconStyle,
        ButtonStyle,
        TextFieldStyle,
        SelectStyle,
        CardStyle,
        TableStyle,
        UploadStyle,
        DialogStyle,
        ToastStyle,
        AppLayoutStyle,
        // pages
        LoginStyle,
        HomeStyle,
        ProfileStyle,
        AdminStyle,
        CalculatorStyle,
        SubtitlesStyle,
        SheetStyle,
    )

    /**
     * Built once at startup. Each component is rendered on its own and the results are joined in order:
     * kotlin-css would otherwise move every @media block of one big builder ahead of later normal rules,
     * letting those rules silently override the phone/tablet styles.
     */
    val css: String by lazy {
        registry.joinToString("\n") { CssBuilder(allowClasses = false).apply { with(it) { styles() } }.toString() }
    }
}
