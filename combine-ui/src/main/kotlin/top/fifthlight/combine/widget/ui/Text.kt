package top.fifthlight.combine.widget.ui

import androidx.compose.runtime.Composable
import org.koin.compose.koinInject
import top.fifthlight.combine.data.Text
import top.fifthlight.combine.data.TextFactory
import top.fifthlight.combine.modifier.Modifier
import top.fifthlight.combine.paint.Color
import top.fifthlight.combine.ui.style.LocalColorTheme
import top.fifthlight.combine.ui.style.LocalTextStyle
import top.fifthlight.combine.ui.style.TextStyle
import top.fifthlight.combine.widget.base.BaseText

@Composable
fun Text(
    text: String,
    modifier: Modifier = Modifier,
    color: Color = LocalColorTheme.current.foreground,
    textStyle: TextStyle = LocalTextStyle.current,
) {
    val textFactory: TextFactory = koinInject()
    if (textStyle.haveStyle) {
        val text = textStyle.applyOnString(textFactory, text)
        BaseText(
            text = text,
            modifier = modifier,
            color = color,
        )
    } else {
        BaseText(
            text = text,
            modifier = modifier,
            color = color,
        )
    }
}

@Composable
fun Text(
    text: Text,
    modifier: Modifier = Modifier,
    color: Color = LocalColorTheme.current.foreground,
    textStyle: TextStyle = LocalTextStyle.current,
) {
    val textFactory: TextFactory = koinInject()
    if (textStyle.haveStyle) {
        val text = textStyle.applyOnText(textFactory, text)
        BaseText(
            text = text,
            modifier = modifier,
            color = color,
        )
    } else {
        BaseText(
            text = text,
            modifier = modifier,
            color = color,
        )
    }
}
